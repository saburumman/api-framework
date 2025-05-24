package tests;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import core.Main;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;



@Listeners({io.qameta.allure.testng.AllureTestNg.class})
public class FailedTest {

	
	
	 @Test(description = "Run Failed API's")
	    @Epic("API Execution")
	    @Feature("Full Flow")
	    @Story("Initial Run + Retries")
	    @Severity(SeverityLevel.CRITICAL)
	    public void executeApiFlow() {
	        System.setProperty("apiGroup", "files");
	        stepStartScenario();
	        List<Map<String, Object>> results = Main.runFullScenario();
	        stepCheckResults(results);

	        // Assertions here to ensure the results are valid
	        Assert.assertNotNull(results, "Results list should not be null");
	        Assert.assertFalse(results.isEmpty(), "Results list should not be empty");
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
	                // Fail test immediately on API failure
	                Assert.fail("API " + apiName + " failed with status: " + statusCode);
	            }
	        }
	    }
}
