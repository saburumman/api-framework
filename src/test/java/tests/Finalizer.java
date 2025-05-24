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
            List<Map<String, Object>> allResults = TestResultHolder.getAllResults();

            if (allResults != null && !allResults.isEmpty()) {
                // Allow override via system property if needed
                String allureReportUrl = System.getProperty("allure.report.url", "https://saburumman.github.io/api-framework/");

                System.out.println("📩 Sending final email report with " + allResults.size() + " API results.");
                EmailSender.sendResultsEmail(allResults, "Final API Test Summary + Allure Report", allureReportUrl);
                System.out.println("✅ Final email report sent successfully.");
            } else {
                System.out.println("⚠️ No API test results collected. Email not sent.");
            }
        } catch (Exception e) {
            System.err.println("❌ Exception during final report email sending:");
            e.printStackTrace();
        }
    }
}
