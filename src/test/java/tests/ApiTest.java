package tests;

import config.PrioritiesReader;
import core.Main;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Health check: runs every API group in priorities.yaml (one test per group, with retries).
 * Run a single group with -DapiGroup=dvld.
 */
@Listeners({io.qameta.allure.testng.AllureTestNg.class})
public class ApiTest {

    @DataProvider(name = "groups")
    public Object[][] groups() {
        String only = System.getProperty("apiGroup");
        PrioritiesReader reader = new PrioritiesReader();
        reader.load("priorities.yaml");
        List<String> names = new ArrayList<>(reader.groupedApis.keySet());
        if (only != null && !only.isEmpty()) {
            if (!names.contains(only)) throw new IllegalArgumentException("Group '" + only + "' not found. Available: " + names);
            names = List.of(only);
        }
        Object[][] data = new Object[names.size()][1];
        for (int i = 0; i < names.size(); i++) data[i][0] = names.get(i);
        return data;
    }

    @Test(dataProvider = "groups", description = "Run API group with retries")
    @Epic("API Execution")
    @Feature("Health Check")
    @Story("Initial Run + Retries")
    @Severity(SeverityLevel.CRITICAL)
    public void executeApiGroup(String group) {
        Allure.getLifecycle().updateTestCase(tc -> tc.setName("API group: " + group));
        String previous = System.getProperty("apiGroup");
        System.setProperty("apiGroup", group);
        try {
            List<Map<String, Object>> results = Main.runFullScenario();
            Assert.assertNotNull(results, "Results list should not be null");
            Assert.assertFalse(results.isEmpty(), "No results for group '" + group + "' (check login and config)");
            checkResults(results);
        } finally {
            if (previous == null) System.clearProperty("apiGroup"); else System.setProperty("apiGroup", previous);
        }
    }

    @Step("Checking API results")
    public void checkResults(List<Map<String, Object>> results) {
        List<String> failed = new ArrayList<>();
        for (Map<String, Object> result : results) {
            Object status = result.get("status_code");
            if (!(status instanceof Integer) || (Integer) status != 200) {
                failed.add(result.get("api") + " (status " + status + ")");
            }
        }
        if (!failed.isEmpty()) Assert.fail("Failed APIs after retries: " + String.join(", ", failed));
    }
}
