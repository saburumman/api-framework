package model;

import java.time.Instant;

public class APIInfo {
	public String name;
	public String endpoint;
    public String method;
    public String priority;
    public String payload;
    public int lastStatusCode = 0;

    // New fields for recovery email logic
    public Instant firstFailureTime;
    public Instant recoveryTime;

    public APIInfo(String name, String endpoint, String method, String priority, String payload) {
        this.name = name;
        this.endpoint = endpoint;
        this.method = method;
        this.priority = priority;
        this.payload = payload != null ? payload : "";
    }

    // === Getters ===
    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getPriority() {
        return priority;
    }

    public String getPayload() {
        return payload;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public Instant getFirstFailureTime() {
        return firstFailureTime;
    }

    public Instant getRecoveryTime() {
        return recoveryTime;
    }

    // === Setters ===
    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public void setFirstFailureTime(Instant firstFailureTime) {
        this.firstFailureTime = firstFailureTime;
    }

    public void setRecoveryTime(Instant recoveryTime) {
        this.recoveryTime = recoveryTime;
    }

    @Override
    public String toString() {
        return String.format("APIInfo{name='%s', endpoint='%s', method='%s', priority='%s'}",
                name, endpoint, method, priority);
    }
}
