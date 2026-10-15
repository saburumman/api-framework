package tests;

import config.FlowsReader;
import config.PrioritiesReader;
import core.FlowRunner;
import core.TestResultHolder;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import model.Flow;
import model.StepResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs every flow in flows.yaml as its own TestNG test (one row in Allure per flow).
 *
 * Filter what runs with:
 *   -Dflow=enroll_course,browse_files      run only these flows
 *   -DflowTags=smoke                       run only flows tagged "smoke"
 */
@Listeners({io.qameta.allure.testng.AllureTestNg.class})
public class FlowTest {

    private PrioritiesReader config;
    private FlowsReader flowsReader;

    @BeforeClass(alwaysRun = true)
    public void loadConfig() {
        config = new PrioritiesReader();
        config.load("priorities.yaml");
        flowsReader = new FlowsReader().load("flows.yaml");
    }

    @DataProvider(name = "flows")
    public Object[][] flows() {
        if (flowsReader == null) loadConfig();
        List<Flow> selected = flowsReader.selected();
        Object[][] data = new Object[selected.size()][1];
        for (int i = 0; i < selected.size(); i++) data[i][0] = selected.get(i);
        return data;
    }

    @Test(dataProvider = "flows", description = "API flow")
    @Epic("API Flows")
    @Severity(SeverityLevel.CRITICAL)
    public void runFlow(Flow flow) {
        Allure.getLifecycle().updateTestCase(tc -> {
            tc.setName("Flow: " + flow.name);
            if (!flow.description.isEmpty()) tc.setDescription(flow.description);
        });
        Allure.feature(flow.name);
        flow.tags.forEach(Allure::story);

        List<StepResult> results = new FlowRunner(config, flowsReader.globalVars).run(flow);

        TestResultHolder.addResults(results.stream().map(StepResult::toResultMap).collect(Collectors.toList()));

        List<String> failures = results.stream()
                .filter(r -> !r.passed && !r.skipped)
                .map(r -> "Step '" + r.stepName + "' (" + r.method + " " + r.endpoint + ", status "
                        + r.statusCode + "): " + String.join("; ", r.failures))
                .collect(Collectors.toList());
        long skipped = results.stream().filter(r -> r.skipped).count();

        if (!failures.isEmpty()) {
            Assert.fail("Flow '" + flow.name + "' failed:\n  " + String.join("\n  ", failures)
                    + (skipped > 0 ? "\n  (" + skipped + " later step(s) skipped)" : ""));
        }
    }
}
