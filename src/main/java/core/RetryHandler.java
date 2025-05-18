package core;

import model.APIInfo;
import java.util.*;

// Handles retry logic for failed API calls
public class RetryHandler {
    private final List<APIInfo> failedAPIs = new ArrayList<>();

    public void trackFailure(APIInfo api) {
        failedAPIs.add(api);
    }

    public List<APIInfo> getFailedAPIs() {
        return new ArrayList<>(failedAPIs);
    }

    // Retries failed APIs a number of times with delay, returns recovered ones
    public List<APIInfo> retry(APIBase apiBase, int times, long delayMs) {
        List<APIInfo> recovered = new ArrayList<>();

        for (int i = 0; i < times; i++) {
            List<APIInfo> stillFailed = new ArrayList<>();

            for (APIInfo api : failedAPIs) {
                Map<String, Object> result = apiBase.sendRequest(api.method, api.endpoint, api.payload);

                if ((int) result.get("status_code") == 200) {
                    recovered.add(api);
                } else {
                    stillFailed.add(api);
                }
            }

            // Update the failed list for the next retry
            failedAPIs.clear();
            failedAPIs.addAll(stillFailed);

            if (failedAPIs.isEmpty()) break; // No more failures, stop early

            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return recovered;
    }

    // Clears the failure list
    public void clearFailures() {
        failedAPIs.clear();
    }
}
