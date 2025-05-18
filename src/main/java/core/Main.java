package core;

import config.PrioritiesReader;
import model.APIInfo;
import utils.EmailSender;

import io.qameta.allure.Step;

import java.io.File;
import java.util.*;

public class Main {

    public static List<Map<String, Object>> runFullScenario() {

        System.out.println("Initializing API test execution...");

        PrioritiesReader reader = new PrioritiesReader();
        reader.load("priorities.yaml");

        System.out.println("Environment: " + reader.env);
        System.out.println("Base URL: " + reader.baseUrl);

        APIBase apiBase = new APIBase(reader.baseUrl);
        RetryHandler retryHandler = new RetryHandler();
        List<Map<String, Object>> results = new ArrayList<>();

        // Compress Allure report as tar.gz (assuming your method returns a File)
       // File allureArchive = EmailSender.compressAllureReportZip("allure-report");

        // If you want to send a URL to email instead of archive file,
        // define the URL here, e.g.:
        String allureReportUrl = "https://saburumman.github.io/api-framework/allure-report/index.html";

        if (reader.loginApi != null) {
            System.out.println("Attempting to acquire auth token...");
            AuthHandler authHandler = new AuthHandler(apiBase);
            String token = acquireToken(authHandler, reader.loginApi);
            if (token != null) {
                apiBase.setAuthToken(token);
               System.out.println("Auth token acquired: " + token);
            } else {
                System.out.println("Failed to acquire auth token. Exiting.");
                return results;
            }
        } else {
            System.out.println("No login API configured.");
        }

        // First Run
        List<Map<String, Object>> firstRunResults = runAndTrackFailures(apiBase, reader.apis, retryHandler);
        results.addAll(firstRunResults);
        // Send email with URL instead of file to match EmailSender signature
        EmailSender.sendResultsEmail(results, "API Test Results + Allure Report", allureReportUrl);

        // Retry #1
        if (!retryHandler.getFailedAPIs().isEmpty()) {
            System.out.println("Starting Retry #1 for failed APIs...");
            List<Map<String, Object>> retry1Results = retryFailedApis(apiBase, retryHandler);
            results.addAll(retry1Results);
            EmailSender.sendResultsEmail(results, "Failed API Test Results Try #1 + Allure Report", allureReportUrl);

            // Track failures from retry #1 for retry #2
            trackFailuresFromResults(retry1Results, retryHandler, reader.apis);

            // Retry #2
            if (!retryHandler.getFailedAPIs().isEmpty()) {
                System.out.println("Starting Retry #2 for remaining failed APIs...");
                List<Map<String, Object>> retry2Results = retryFailedApis(apiBase, retryHandler);
                EmailSender.sendResultsEmail(results, "Failed API Test Results Try #2 + Allure Report", allureReportUrl);
            }
        }

        return results;
    }

    // Helper method to track failures from retry results
    private static void trackFailuresFromResults(List<Map<String, Object>> results, RetryHandler retryHandler, List<APIInfo> apis) {
        retryHandler.clearFailures();
        for (Map<String, Object> result : results) {
            Object statusObj = result.get("status_code");
            int statusCode = statusObj instanceof Integer ? (Integer) statusObj : -1;
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

    @Step("Executing API: {api.name}")
    public static Map<String, Object> executeApi(APIBase apiBase, APIInfo api) {
        Map<String, Object> result = apiBase.sendRequest(api.method, api.endpoint, api.payload);
        if (result == null) {
            result = new HashMap<>();
        }
        result.put("api", api.name);
        result.put("endpoint", api.endpoint);
        result.put("method", api.method);
        result.put("auth_token", apiBase.getAuthToken());
        result.put("payload", api.payload != null ? api.payload : "");
        return result;
    }

    @Step("Logging API status for: {api.name}")
    public static void logStatus(APIInfo api, Map<String, Object> result) {
        Object statusObj = result.get("status_code");
        int statusCode = statusObj instanceof Integer ? (Integer) statusObj : -1;
        if (statusCode == 200) {
            System.out.println(" API succeeded: " + api.name);
        } else {
            System.out.println(" API failed: " + api.name + " (Status: " + statusCode + ")");
        }
    }

    @Step("Running API Set and Tracking Failures")
    public static List<Map<String, Object>> runAndTrackFailures(APIBase apiBase, List<APIInfo> apis, RetryHandler retryHandler) {
        retryHandler.clearFailures();  // Clear before starting
        List<Map<String, Object>> runResults = new ArrayList<>();
        for (APIInfo api : apis) {
            Map<String, Object> result = executeApi(apiBase, api);
            runResults.add(result);
            logStatus(api, result);
            Object statusObj = result.get("status_code");
            int statusCode = statusObj instanceof Integer ? (Integer) statusObj : -1;
            if (statusCode != 200) {
                retryHandler.trackFailure(api);
            }
        }
        return runResults;
    }

    @Step("Retrying Failed APIs")
    public static List<Map<String, Object>> retryFailedApis(APIBase apiBase, RetryHandler retryHandler) {
        List<APIInfo> failedApisBeforeRetry = new ArrayList<>(retryHandler.getFailedAPIs());
        retryHandler.clearFailures();  // Clear before retry to avoid duplicates
        List<Map<String, Object>> retryResults = new ArrayList<>();
        for (APIInfo api : failedApisBeforeRetry) {
            Map<String, Object> result = executeApi(apiBase, api);
            retryResults.add(result);
            logStatus(api, result);
            Object statusObj = result.get("status_code");
            int statusCode = statusObj instanceof Integer ? (Integer) statusObj : -1;
            if (statusCode != 200) {
                retryHandler.trackFailure(api);
            }
        }
        return retryResults;
    }

    public static APIInfo findApiByName(List<APIInfo> apis, String name) {
        if (name == null) return null;
        for (APIInfo api : apis) {
            if (api.name.equals(name)) {
                return api;
            }
        }
        return null;
    }
}
