package core;

import model.APIInfo;
import java.util.Map;

// Handles login to fetch authorization token
public class AuthHandler {
    private final APIBase apiBase;

    public AuthHandler(APIBase apiBase) {
        this.apiBase = apiBase;
    }

    public String getToken(APIInfo loginApi) {
        Map<String, Object> result = apiBase.sendRequest(loginApi.method, loginApi.endpoint, loginApi.payload);
        
        if (result == null) {
            throw new IllegalStateException("API response is null. Check your login endpoint or payload.");
        }

        Object statusCodeObj = result.get("status_code");
        if (statusCodeObj == null) {
            throw new IllegalStateException("Response missing 'status_code'. Raw result: " + result);
        }

        int statusCode = (int) statusCodeObj;
        if (statusCode == 200) {
            String body = String.valueOf(result.get("response"));
            if (body == null) {
                throw new IllegalStateException("Response body is null despite 200 status.");
            }

            // Improved regex to extract the token
            String token = body.replaceAll(".*\"token\"\\s*:\\s*\"(.*?)\".*", "$1");
            System.out.println("Extracted Token: " + token);  // Log the token for debugging
            return token;
        }

        throw new RuntimeException("Failed to get token. Status: " + statusCode + " | Response: " + result);
    }
}
