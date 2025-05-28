package core;

import config.PrioritiesReader;
import model.APIInfo;
import utils.EmailSender;

import io.qameta.allure.Step;

import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static String allureReportUrl = "https://saburumman.github.io/api-framework/index.html";

    public static List<Map<String, Object>> runFullScenario() {
        Logger.info("Initializing API test execution...");

        String selectedGroup = System.getProperty("apiGroup");
        if (selectedGroup == null || selectedGroup.isEmpty()) {
            Logger.info("No API group specified. Use -DapiGroup=<group> when running.");
            return Collections.emptyList();
        }

        PrioritiesReader reader = new PrioritiesReader();
        reader.load("priorities.yaml");

        Logger.info("Environment: " + reader.env);
        Logger.info("Base URL: " + reader.baseUrl);

        if (!reader.groupedApis.containsKey(selectedGroup)) {
            Logger.info("Group '" + selectedGroup + "' not found. Exiting.");
            return Collections.emptyList();
        }

        reader.apis = reader.groupedApis.get(selectedGroup);
        Logger.info("Loaded group: " + selectedGroup + ", APIs count: " + reader.apis.size());

        APIBase apiBase = initializeApiContext(reader);
        if (apiBase == null) return Collections.emptyList();

        RetryHandler retryHandler = new RetryHandler();
        Map<String, Map<String, Object>> latestResultsMap = runScenarioWithRetries(apiBase, reader.apis, retryHandler);

        List<APIInfo> failedAfterThreeRuns = new ArrayList<>(retryHandler.getFailedAPIs());
        if (!failedAfterThreeRuns.isEmpty()) {
            List<Map<String, Object>> failedApiMaps = toApiInfoMaps(failedAfterThreeRuns);
            EmailSender.sendFailureEmail(failedApiMaps, allureReportUrl);
            handleFinalRetry(apiBase, failedAfterThreeRuns);
        }

        List<Map<String, Object>> finalResults = new ArrayList<>(latestResultsMap.values());
        TestResultHolder.addResults(finalResults);
        return finalResults;
    }

    private static APIBase initializeApiContext(PrioritiesReader reader) {
        APIBase apiBase = new APIBase(reader.baseUrl);
        if (reader.loginApi != null) {
            Logger.info("Attempting to acquire auth token...");
            AuthHandler authHandler = new AuthHandler(apiBase);
            String token = acquireToken(authHandler, reader.loginApi);
            if (token != null) {
                apiBase.setAuthToken(token);
                Logger.info("Auth token acquired: " + token);
            } else {
                Logger.info("Failed to acquire auth token. Exiting.");
                return null;
            }
        } else {
            Logger.info("No login API configured.");
        }
        return apiBase;
    }

    private static Map<String, Map<String, Object>> runScenarioWithRetries(APIBase apiBase, List<APIInfo> apis, RetryHandler retryHandler) {
        Map<String, Map<String, Object>> resultsMap = new LinkedHashMap<>();

        List<Map<String, Object>> firstRun = runAndTrackFailures(apiBase, apis, retryHandler);
        for (Map<String, Object> result : firstRun) {
            resultsMap.put((String) result.get("api"), result);
        }

        for (int retryAttempt = 1; retryAttempt <= 2 && !retryHandler.getFailedAPIs().isEmpty(); retryAttempt++) {
            Logger.info("Starting Retry #" + retryAttempt + " for failed APIs...");
            List<Map<String, Object>> retryResults = retryFailedApis(apiBase, retryHandler);
            for (Map<String, Object> result : retryResults) {
                resultsMap.put((String) result.get("api"), result);
            }
            trackFailuresFromResults(retryResults, retryHandler, apis);
        }

        return resultsMap;
    }

    private static void handleFinalRetry(APIBase apiBase, List<APIInfo> failedAfterThreeRuns) {

        try {
            Logger.info("Waiting 10 minutes before final retry...");
            Thread.sleep(600_000);
        } catch (InterruptedException e) {
            Logger.info("Wait interrupted: " + e.getMessage());
        }

        RetryHandler finalRetryHandler = new RetryHandler();
        List<Map<String, Object>> finalRetryResults = retryFailedApis(apiBase, failedAfterThreeRuns, finalRetryHandler);

        List<APIInfo> backToNormal = new ArrayList<>();
        List<APIInfo> stillFailing = new ArrayList<>();
        for (APIInfo api : failedAfterThreeRuns) {
            if (!finalRetryHandler.getFailedAPIs().contains(api)) {
                backToNormal.add(api);
            } else {
                stillFailing.add(api);
            }
        }

       
        if (!backToNormal.isEmpty()) {
        	EmailSender.sendBackToNormalSuccess(backToNormal, allureReportUrl);
        }

        if (!stillFailing.isEmpty()) {
            EmailSender.sendStillFailed(toApiInfoMaps(stillFailing), allureReportUrl); 
    }
    }
    
    private static APIInfo mapToApiInfo(Map<String, Object> map) {
        return new APIInfo(
            map.get("api").toString(),
            map.get("endpoint").toString(),
            map.getOrDefault("method", "GET").toString(),  // Default method fallback
            map.getOrDefault("priority", "medium").toString(),
            map.getOrDefault("payload", "").toString()
        );
    }

    private static void trackFailuresFromResults(List<Map<String, Object>> results, RetryHandler retryHandler, List<APIInfo> apis) {
        retryHandler.clearFailures();
        for (Map<String, Object> result : results) {
            int statusCode = getStatusCode(result);
            if (statusCode != HttpStatus.OK.code) {
                APIInfo failedApi = findApiByName(apis, (String) result.get("api"));
                if (failedApi != null) {
                    failedApi.lastStatusCode = statusCode; // Track status code in APIInfo
                    retryHandler.trackFailure(failedApi);
                }
            }
        }
    }

    @Step("Acquiring auth token")
    public static String acquireToken(AuthHandler handler, APIInfo loginApi) {
        return handler.getToken(loginApi);
    }

    @Step("Executing API")
    public static Map<String, Object> executeApi(APIBase apiBase, APIInfo api) {
        Map<String, Object> result = apiBase.sendRequest(api.method, api.endpoint, api.payload);
        if (result == null) result = new HashMap<>();
        result.put("api", api.name);
        result.put("endpoint", api.endpoint);
        result.put("method", api.method);
        result.put("auth_token", apiBase.getAuthToken());
        result.put("payload", api.payload != null ? api.payload : "");

        int statusCode = getStatusCode(result);
        api.lastStatusCode = statusCode; // Store status code for reporting

        return result;
    }

    @Step("Logging API status")
    public static void logStatus(APIInfo api, Map<String, Object> result) {
        int statusCode = getStatusCode(result);
        if (statusCode == HttpStatus.OK.code) {
            Logger.info(" API succeeded: " + api.name);
        } else {
            Logger.info(" API failed: " + api.name + " (Status: " + statusCode + ")");
        }
    }

    @Step("Running API Set and Tracking Failures")
    public static List<Map<String, Object>> runAndTrackFailures(APIBase apiBase, List<APIInfo> apis, RetryHandler retryHandler) {
        retryHandler.clearFailures();
        List<Map<String, Object>> runResults = new ArrayList<>();
        for (APIInfo api : apis) {
            Map<String, Object> result = executeApi(apiBase, api);
            runResults.add(result);
            logStatus(api, result);
            if (getStatusCode(result) != HttpStatus.OK.code) {
                retryHandler.trackFailure(api);
            }
        }
        return runResults;
    }

    @Step("Retrying Failed APIs")
    public static List<Map<String, Object>> retryFailedApis(APIBase apiBase, RetryHandler retryHandler) {
        return retryFailedApis(apiBase, new ArrayList<>(retryHandler.getFailedAPIs()), retryHandler);
    }

    public static List<Map<String, Object>> retryFailedApis(APIBase apiBase, List<APIInfo> apisToRetry, RetryHandler retryHandler) {
        retryHandler.clearFailures();
        List<Map<String, Object>> retryResults = new ArrayList<>();
        for (APIInfo api : apisToRetry) {
            Map<String, Object> result = executeApi(apiBase, api);
            retryResults.add(result);
            logStatus(api, result);
            if (getStatusCode(result) != HttpStatus.OK.code) {
                retryHandler.trackFailure(api);
            }
        }
        return retryResults;
    }

    public static APIInfo findApiByName(List<APIInfo> apis, String name) {
        return apis.stream().filter(api -> api.name.equals(name)).findFirst().orElse(null);
    }

    private static int getStatusCode(Map<String, Object> result) {
        Object statusObj = result.get("status_code");
        return statusObj instanceof Integer ? (Integer) statusObj : -1;
    }

    // Converts List<APIInfo> to List<Map<String,Object>> suitable for EmailSender methods
    private static List<Map<String, Object>> toApiInfoMaps(List<APIInfo> apiInfos) {
        return apiInfos.stream().map(api -> {
            Map<String, Object> map = new HashMap<>();
            map.put("api", api.name);
            map.put("endpoint", api.endpoint);
            map.put("status_code", api.lastStatusCode != 0 ? api.lastStatusCode : -1);
            return map;
        }).collect(Collectors.toList());
    }
}

class Logger {
    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }
}

enum HttpStatus {
    OK(200);

    public final int code;
    HttpStatus(int code) {
        this.code = code;
    }
}
