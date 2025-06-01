package tests;

import core.TestResultHolder;
import utils.EmailSender;
import org.testng.annotations.AfterSuite;

import java.util.List;
import java.util.Map;

public class Finalizer {

    @AfterSuite
    public void sendFinalReport() {
        try {
            List<Map<String, Object>> failedResults = TestResultHolder.getFailedResults();

            if (failedResults != null && !failedResults.isEmpty()) {
                String allureReportUrl = System.getProperty("allure.report.url", "https://saburumman.github.io/api-framework/");

                System.out.println("Sending final email report with " + failedResults.size() + " failed API results.");
                EmailSender.sendResultsEmail(failedResults, "Failed API Summary + Allure Report", allureReportUrl);
                System.out.println("Final email report sent successfully.");
            } else {
                System.out.println("All APIs passed. No failed results to email.");
            }
        } catch (Exception e) {
            System.err.println("Exception during final report email sending:");
            e.printStackTrace();
        }
    }
}
