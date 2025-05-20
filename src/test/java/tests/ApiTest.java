package tests;

import org.testng.annotations.Test;
import io.qameta.allure.*;
import core.Main;

import java.util.List;
import java.util.Map;

public class ApiTest {

    @Test(description = "Run Full API Scenario with Allure Steps")
    @Epic("API Execution")
    @Feature("Full Flow")
    @Story("Initial Run + Retries")
    @Severity(SeverityLevel.CRITICAL)
    public void executeApiFlow() {
        stepStartScenario();
        List<Map<String, Object>> results = Main.runFullScenario();
        stepCheckResults(results);
    }

    @Step("Starting the API scenario")
    public void stepStartScenario() {
        System.out.println("API scenario started");
    }

    @Step("Checking API results")
    public void stepCheckResults(List<Map<String, Object>> results) {
        for (Map<String, Object> result : results) {
            int statusCode = (int) result.get("status_code");
            String apiName = (String) result.get("api");
            if (statusCode != 200) {
                System.out.println("API FAILED: " + apiName + " with status: " + statusCode);
            }
        }
    }
}
