package tests;

import config.Environments;
import config.Secrets;
import core.TestResultHolder;
import utils.EmailSender;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sends the report email when a suite finishes.
 *
 * MAIL_SEND_ON (in .env / env var / -D):
 *   always    (default) - email after every run, pass or fail
 *   failures            - email only when something failed
 *   never               - no email
 */
public class Finalizer {

    @AfterSuite(alwaysRun = true)
    public void sendFinalReport(ITestContext context) {
        try {
            String mode = Secrets.get("MAIL_SEND_ON", "always").trim().toLowerCase(Locale.ROOT);
            List<Map<String, Object>> all = new ArrayList<>(TestResultHolder.getAllResults());
            List<Map<String, Object>> failed = new ArrayList<>(TestResultHolder.getFailedResults());

            String suite = context.getSuite().getName();
            String env = Environments.active() == null ? "" : Environments.active().toUpperCase(Locale.ROOT);
            String reportUrl = System.getProperty("allure.report.url",
                    Secrets.get("ALLURE_REPORT_URL", "https://saburumman.github.io/api-framework/"));

            if (mode.equals("never")) {
                System.out.println("[EMAIL] MAIL_SEND_ON=never - not sending.");
                return;
            }
            if (all.isEmpty()) {
                System.out.println("[EMAIL] No results recorded in " + suite + " - not sending.");
                return;
            }
            if (failed.isEmpty() && mode.equals("failures")) {
                System.out.println("[EMAIL] All APIs passed and MAIL_SEND_ON=failures - not sending.");
                return;
            }

            String subject = failed.isEmpty()
                    ? "[" + env + "] " + suite + ": all " + all.size() + " checks passed"
                    : "[" + env + "] " + suite + ": " + failed.size() + " of " + all.size() + " checks FAILED";

            System.out.println("[EMAIL] Sending: " + subject);
            // Failures-only mode lists just the failures; otherwise the full run is included
            EmailSender.sendResultsEmail(mode.equals("failures") ? failed : all, subject, reportUrl);
        } catch (Exception e) {
            System.err.println("Exception during final report email sending:");
            e.printStackTrace();
        }
    }
}
