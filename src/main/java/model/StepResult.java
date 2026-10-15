package model;

import java.util.*;

/** What happened when a single flow step ran. */
public class StepResult {
    public String flowName;
    public String stepName;
    public String method;
    public String baseUrl;
    public String endpoint;
    public String payload = "";
    public String authToken = "";
    public int statusCode = -1;
    public String responseBody = "";
    public long timeMs;
    public int attempts;
    public boolean passed;
    public boolean skipped;
    public List<String> failures = new ArrayList<>();
    public Map<String, Object> extracted = new LinkedHashMap<>();

    /** Same keys as the classic group runner, so EmailSender / Finalizer can use it unchanged. */
    public Map<String, Object> toResultMap() {
        Map<String, Object> map = new HashMap<>();
        String label = flowName + " > " + stepName;
        if (!passed) {
            label += skipped ? " [SKIPPED]" : " [FAILED: " + String.join("; ", failures) + "]";
        }
        map.put("api", label);
        map.put("flow", flowName);
        map.put("step", stepName);
        map.put("method", method);
        map.put("base_url", baseUrl);
        map.put("endpoint", endpoint);
        map.put("payload", payload);
        map.put("auth_token", authToken);
        map.put("status_code", statusCode);
        map.put("response", responseBody);
        map.put("time_ms", timeMs);
        map.put("passed", passed);
        return map;
    }
}
