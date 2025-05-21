package tests;

import io.qameta.allure.*;
import org.testng.annotations.Test;

public class SimpleAllureDebugTest {

    @Test(description = "Simple test with Allure step and attachment")
    @Epic("Debug")
    @Feature("Allure Basic Test")
    @Severity(SeverityLevel.MINOR)
    public void simpleAllureTest() {
    	  Allure.step("Step 1: Starting test");
          Allure.addAttachment("Test Message", "text/plain", "This is a debug attachment", ".txt");
          System.out.println("Running simple test");
    }
}
