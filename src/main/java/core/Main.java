package core;

import config.PrioritiesReader;
import model.APIInfo;
import utils.EmailSender;

import io.qameta.allure.Step;

import java.util.*;

public class Main {

    public static List<Map<String, Object>> runFullScenario() {
        System.out.println("Initializing API test execution...");

        // Load priorities.yaml and parse APIs
        PrioritiesReader reader = new PrioritiesReader();
        reader.load("priorities.yaml");  // make sure this reads your YAML properly!

        System.out.println("Environment: " + reader.env);
        System.out.println("Base URL: " + reader.baseUrl);

        // Create shared APIBase and RetryHandler instances once
        APIBase apiBase = new APIBase(reader.baseUrl);
        RetryHandler retryHandler = new RetryHandler();

        // Get requested API group from system property
        String selectedGroup = System.getProperty("apiGroup");
        if (selectedGroup == null || selectedGroup.isEmpty()) {
            System.out.println("⚠️ No specific API group provided. Running all groups in priorities.yaml...");
        } else if (!reader.groupedApis.containsKey(selectedGroup)) {
            System.out.println("❌ Group '" + selectedGroup + "' not found. Exiting.");
            return Collections.emptyList();
        }

        // Handle login API (auth token acquisition)
        if (reader.loginApi != null) {
            System.out.println("Attempting to acquire auth token...");
            AuthHandler authHandler = new AuthHandler(apiBase);
            String token = acquireToken(authHandler, reader.loginApi);
            if (token != null) {
                apiBase.setAuthToken(token);
                System.out.println("Auth token acquired: " + token);
            } else {
                System.out.println("Failed to acquire auth token. Exiting.");
                return Collections.emptyList();
            }
        } else {
            System.out.println("No login API configured.");
        }

        List<Map<String, Object>> allResults = new ArrayList<>();

        if (selectedGroup != null && !selectedGroup.isEmpty()) {
            // Run only the specified group
            reader.apis = reader.groupedApis.get(selectedGroup);
            System.out.println("Running group: " + selectedGroup + ", APIs count: " + reader.apis.size());
            List<Map<String, Object>> groupResults = runAndRetryGroup(apiBase, retryHandler, reader.apis);
            allResults.addAll(groupResults);
            TestResultHolder.addResults(groupResults);
        } else {
            // Run all groups
            for (Map.Entry<String, List<APIInfo>> entry : reader.groupedApis.entrySet()) {
                System.out.println("Running group: " + entry.getKey() + ", APIs count: " + entry.getValue().size());
                reader.apis = entry.getValue();
                List<Map<String, Object>> groupResults = runAndRetryGroup(apiBase, retryHandler, reader.apis);
                allResults.addAll(groupResults);
                TestResultHolder.addResults(groupResults);
            }
        }

        return allResults;
    }

    // Helper method to run APIs and retry failures
    private static List<Map<String, Object>> runAndRetryGroup(APIBase apiBase, RetryHandler retryHandler, List<APIInfo> apis) {
        List<Map<String, Object>> results = new ArrayList<>();

        // First run
        List<Map<String, Object>> firstRunResults = runAndTrackFailures(apiBase, apis, retryHandler);
        results.addAll(firstRunResults);

        // Retry failed APIs up to 2 times
        for (int retryAttempt = 1; retryAttempt <= 2 && !retryHandler.getFailedAPIs().isEmpty(); retryAttempt++) {
            System.out.println("Starting Retry #" + retryAttempt + " for failed APIs...");
            List<Map<String, Object>> retryResults = retryFailedApis(apiBase, retryHandler);
            results.addAll(retryResults);
            trackFailuresFromResults(retryResults, retryHandler, apis);
        }

        return results;
    }

    private static void trackFailuresFromResults(List<Map<String, Object>> results, RetryHandler retryHandler, List<APIInfo> apis) {
        retryHandler.clearFailures();
        for (Map<String, Object> result : results) {
            int statusCode = getStatusCode(result);
            if (statusCode != 200) {
                APIInfo failedApi = findApiByName(apis, (String) result.get("api"));
                if (failedApi != null) {
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
        return result;
    }

    @Step("Logging API status")
    public static void logStatus(APIInfo api, Map<String, Object> result) {
        int statusCode = getStatusCode(result);
        if (statusCode == 200) {
            System.out.println(" API succeeded: " + api.name);
        } else {
            System.out.println(" API failed: " + api.name + " (Status: " + statusCode + ")");
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
            if (getStatusCode(result) != 200) {
                retryHandler.trackFailure(api);
            }
        }
        return runResults;
    }

    @Step("Retrying Failed APIs")
    public static List<Map<String, Object>> retryFailedApis(APIBase apiBase, RetryHandler retryHandler) {
        List<APIInfo> failedApis = new ArrayList<>(retryHandler.getFailedAPIs());
        retryHandler.clearFailures();
        List<Map<String, Object>> retryResults = new ArrayList<>();
        for (APIInfo api : failedApis) {
            Map<String, Object> result = executeApi(apiBase, api);
            retryResults.add(result);
            logStatus(api, result);
            if (getStatusCode(result) != 200) {
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
}
