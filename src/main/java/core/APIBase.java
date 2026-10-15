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

    /**
     * Low-level request used by the flow engine. Sets the base URI on this request only
     * (no global RestAssured state), so it is safe if tests run in parallel.
     *
     * @param token   Bearer token to send, or null for none
     * @param headers extra headers (may be null)
     * @param query   query parameters (may be null)
     * @param body    JSON body (may be null/empty)
     */
    public Response send(String method, String endpoint, String token,
                         Map<String, String> headers, Map<String, String> query, String body) {
        return send(method, endpoint, token, headers, query, body, null);
    }

    /**
     * Same as above; when {@code form} is non-empty the request is sent as
     * application/x-www-form-urlencoded with those fields instead of a JSON body.
     * An endpoint starting with http(s):// is called as-is (e.g. a token server on another host).
     */
    public Response send(String method, String endpoint, String token,
                         Map<String, String> headers, Map<String, String> query, String body,
                         Map<String, String> form) {
        boolean isForm = form != null && !form.isEmpty();
        RequestSpecification request = RestAssured.given().filter(new AllureRestAssured());
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            request.baseUri(baseUrl);
        }
        if (isForm) {
            request.contentType("application/x-www-form-urlencoded");
            request.formParams(new HashMap<>(form));
        } else {
            request.header("Content-Type", "application/json");
        }

        if (token != null && !token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
        if (headers != null) {
            headers.forEach(request::header);
        }
        if (query != null && !query.isEmpty()) {
            request.queryParams(new HashMap<>(query));
        }
        if (!isForm && body != null && !body.isEmpty()) {
            request.body(body);
        }
        return request.when().request(Method.valueOf(method.toUpperCase()), endpoint);
    }

    @Step("Sending {method} request to {endpoint}")
    public Map<String, Object> sendRequest(String method, String endpoint, String payload) {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", endpoint);
        result.put("method", method);
        result.put("payload", payload);
        result.put("auth_token", authToken);
        result.put("base_url", baseUrl);

        try {
            Response response = send(method, endpoint, authToken, null, null, payload);
            result.put("status_code", response.getStatusCode());
            result.put("response", response.getBody().asString());
        } catch (Exception e) {
            // -1 = the request never got a response (DNS, timeout, connection refused...).
            // Previously this was reported as a fake 500, which hid real network problems.
            result.put("status_code", -1);
            result.put("error", e.getMessage());
            result.put("response", "Request failed before a response was received: " + e);
            System.out.println("Error in API Request: " + e.getMessage());
        }

        return result;
    }
}
