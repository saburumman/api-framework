package core;

import model.APIInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestResultHolder {

    private static final List<Map<String, Object>> allResults = new ArrayList<>();
    private static final List<Map<String, Object>> failedResults = new ArrayList<>();

    public static void addResults(List<Map<String, Object>> results) {
        allResults.addAll(results);
        for (Map<String, Object> result : results) {
            int statusCode = (int) result.getOrDefault("status_code", -1);
            if (statusCode != 200) {
                failedResults.add(result);
            }
        }
    }

    public static List<Map<String, Object>> getAllResults() {
        return allResults;
    }

    public static List<Map<String, Object>> getFailedResults() {
        return failedResults;
    }

    public static List<APIInfo> getFailedAPIInfos() {
        return failedResults.stream()
            .map(TestResultHolder::mapToApiInfo)
            .collect(Collectors.toList());
    }

    private static APIInfo mapToApiInfo(Map<String, Object> map) {
        String name = (String) map.getOrDefault("name", "Unknown");
        String endpoint = (String) map.getOrDefault("endpoint", "");
        String method = (String) map.getOrDefault("method", "GET");
        String priority = (String) map.getOrDefault("priority", "");
        String payload = (String) map.getOrDefault("payload", "");

        APIInfo apiInfo = new APIInfo(name, endpoint, method, priority, payload);

        Object statusObj = map.get("status_code");
        if (statusObj instanceof Integer) {
            apiInfo.setLastStatusCode((Integer) statusObj);
        } else if (statusObj instanceof String) {
            try {
                apiInfo.setLastStatusCode(Integer.parseInt((String) statusObj));
            } catch (NumberFormatException e) {
                apiInfo.setLastStatusCode(-1);
            }
        } else {
            apiInfo.setLastStatusCode(-1);
        }

        // Optional: Parse Instant fields if stored as Strings or Long timestamps
        Object firstFail = map.get("firstFailureTime");
        if (firstFail instanceof Instant) {
            apiInfo.setFirstFailureTime((Instant) firstFail);
        } else if (firstFail instanceof String) {
            try {
                apiInfo.setFirstFailureTime(Instant.parse((String) firstFail));
            } catch (Exception ignored) {}
        }

        Object recovery = map.get("recoveryTime");
        if (recovery instanceof Instant) {
            apiInfo.setRecoveryTime((Instant) recovery);
        } else if (recovery instanceof String) {
            try {
                apiInfo.setRecoveryTime(Instant.parse((String) recovery));
            } catch (Exception ignored) {}
        }

        return apiInfo;
    }

    // Optional: Convert APIInfo back to Map<String,Object>
    public static Map<String, Object> apiInfoToMap(APIInfo apiInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", apiInfo.getName());
        map.put("endpoint", apiInfo.getEndpoint());
        map.put("method", apiInfo.getMethod());
        map.put("priority", apiInfo.getPriority());
        map.put("payload", apiInfo.getPayload());
        map.put("status_code", apiInfo.getLastStatusCode());
        if (apiInfo.getFirstFailureTime() != null)
            map.put("firstFailureTime", apiInfo.getFirstFailureTime().toString());
        if (apiInfo.getRecoveryTime() != null)
            map.put("recoveryTime", apiInfo.getRecoveryTime().toString());

        return map;
    }
}