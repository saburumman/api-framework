package tests;

import org.testng.annotations.Test;
import org.testng.annotations.Listeners;
import io.qameta.allure.*;
import io.qameta.allure.testng.AllureTestNg;
import core.Main;

import java.util.List;
import java.util.Map;

@Listeners({AllureTestNg.class})
public class ApiTest {

    @Test(description = "Run Full API Scenario with Allure Steps")
    @Epic("API Execution")
    @Feature("Full Flow")
    @Story("Initial Run + Retries")
    @Severity(SeverityLevel.CRITICAL)
    public void executeApiFlow() {
        List<Map<String, Object>> results = Main.runFullScenario();

        // Log all failed APIs — do NOT fail the test, just report
        for (Map<String, Object> result : results) {
            int statusCode = (int) result.get("status_code");
            String apiName = (String) result.get("api");
            if (statusCode != 200) {
                System.out.println("API FAILED: " + apiName + " with status: " + statusCode);
            }
        }

        System.out.println("API monitoring run completed. Check logs or email reports for details.");
    }
}
