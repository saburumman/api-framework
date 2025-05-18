package core;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.http.Method;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

public class APIBase {
    private final String baseUrl;
    private String authToken;

    public APIBase(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Step("Sending {method} request to {endpoint}")
    public Map<String, Object> sendRequest(String method, String endpoint, String payload) {
        Map<String, Object> result = new HashMap<>();

        try {
            RestAssured.baseURI = baseUrl;
            Method reqMethod = Method.valueOf(method.toUpperCase());

            RequestSpecification request = RestAssured
                .given()
                .filter(new AllureRestAssured()) // Allure filter for API logging
                .header("Content-Type", "application/json");

            if (authToken != null && !authToken.isEmpty()) {
                request.header("Authorization", "Bearer " + authToken);
            }

            if (payload != null && !payload.isEmpty()) {
                request.body(payload);
            }

            Response response = request.when().request(reqMethod, endpoint);

            int status = response.getStatusCode();
            result.put("status_code", status);
            result.put("endpoint", endpoint);
            result.put("response", response.getBody().asString());
            result.put("method", method);
            result.put("payload", payload);
            result.put("auth_token", authToken);
            result.put("base_url", baseUrl);

        } catch (Exception e) {
            result.put("status_code", 500);
            result.put("error", e.getMessage());
            System.out.println("Error in API Request: " + e.getMessage());
        }

        return result;
    }
}
