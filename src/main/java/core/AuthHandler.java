package core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.Environments;
import io.restassured.response.Response;
import model.APIInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logs in with login_api from priorities.yaml and returns the access token.
 * Supports JSON logins (payload:) and OAuth2-style form logins (form:), e.g. grant_type=password.
 * Values may use {{env:NAME}} (from .env / env vars) and environment settings such as {{auth_url}}.
 */
public class AuthHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final APIBase apiBase;

    public AuthHandler(APIBase apiBase) {
        this.apiBase = apiBase;
    }

    public String getToken(APIInfo loginApi) {
        Map<String, Object> vars = Environments.vars();
        String endpoint = TemplateEngine.resolveToString(loginApi.endpoint, vars);
        String payload = loginApi.form.isEmpty() ? TemplateEngine.resolveToString(loginApi.payload, vars) : null;
        Map<String, String> form = toStringMap(TemplateEngine.resolve(loginApi.form, vars));
        Map<String, String> headers = toStringMap(TemplateEngine.resolve(loginApi.headers, vars));

        Response response;
        try {
            response = apiBase.send(loginApi.method, endpoint, null, headers, null, payload, form);
        } catch (Exception e) {
            throw new RuntimeException("Login request to " + endpoint + " failed: " + e.getMessage(), e);
        }
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        if (statusCode != 200) {
            throw new RuntimeException("Login failed. Status: " + statusCode + " | Response: " + body);
        }
        return extractToken(body, loginApi.tokenPath);
    }

    /** Uses tokenPath if given, otherwise the first token / access_token field anywhere in the JSON. */
    static String extractToken(String body, String tokenPath) {
        try {
            JsonNode root = MAPPER.readTree(body);
            if (tokenPath != null && !tokenPath.isEmpty()) {
                String pointer = "/" + FlowRunner.normalisePath(tokenPath).replace(".", "/");
                JsonNode node = root.at(pointer);
                if (node.isTextual() && !node.asText().isEmpty()) return node.asText();
                throw new IllegalStateException("No token at '" + tokenPath + "' in login response: " + body);
            }
            for (String field : new String[]{"access_token", "token", "accessToken"}) {
                JsonNode node = root.findValue(field);
                if (node != null && node.isTextual() && !node.asText().isEmpty()) {
                    return node.asText();
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Login response is not valid JSON: " + body, e);
        }
        throw new IllegalStateException("Login succeeded but no token field was found in: " + body);
    }

    private static Map<String, String> toStringMap(Object o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o instanceof Map) {
            ((Map<?, ?>) o).forEach((k, v) -> out.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
        }
        return out;
    }
}
