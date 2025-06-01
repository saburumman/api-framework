package tests;

import core.APIBase;
import core.AuthHandler;
import core.Main;
import core.RetryHandler;
import core.TestResultHolder;
import model.APIInfo;
import utils.EmailSender;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BackToNormalTry {

    public static String allureReportUrl = "https://saburumman.github.io/api-framework/index.html";

    public static void main(String[] args) {
        try {
            System.out.println("Waiting 10 minutes before re-trying failed APIs...");
            TimeUnit.MINUTES.sleep(10);

            List<APIInfo> failedApis = TestResultHolder.getFailedAPIInfos();
            if (failedApis == null || failedApis.isEmpty()) {
                System.out.println("No failed APIs to re-try. Skipping post-retry suite.");
                return;
            }

            APIBase apiBase = Main.initializeApiContextFromLatest();
            RetryHandler retryHandler = new RetryHandler();

            System.out.println("Re-running failed APIs after wait...");
            List<Map<String, Object>> retryResults = Main.retryFailedApis(apiBase, failedApis, retryHandler);

            // 💡 Categorize the retry outcome
            List<APIInfo> recovered = new ArrayList<>();
            List<APIInfo> stillFailing = new ArrayList<>();

            for (APIInfo api : failedApis) {
                if (api.getLastStatusCode() == 200) {
                    api.setRecoveryTime(java.time.Instant.now());
                    recovered.add(api);
                } else {
                    stillFailing.add(api);
                }
            }

            // 📨 Decide email subject based on results
            String emailSubject;
            if (stillFailing.isEmpty() && !recovered.isEmpty()) {
                emailSubject = "All APIs Back to Normal After Retry";
            } else if (!stillFailing.isEmpty() && recovered.isEmpty()) {
                emailSubject = "APIs Still Failing After Retry";
            } else if (!stillFailing.isEmpty() && !recovered.isEmpty()) {
                emailSubject = "Some APIs Recovered, Some Still Failing After Retry";
            } else {
                emailSubject = "No API Failures Detected"; // edge case
            }

            EmailSender.sendResultsEmail(retryResults, emailSubject, Main.allureReportUrl);

        } catch (Exception e) {
            System.err.println("Exception during post-retry processing:");
            e.printStackTrace();
        }
    }
}