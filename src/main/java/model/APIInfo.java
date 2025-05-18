package model;

// Represents metadata for a single API
public class APIInfo {
    public String name;
    public String endpoint;
    public String method;
    public String priority;
    public String payload;

    public APIInfo(String name, String endpoint, String method, String priority, String payload) {
        this.name = name;
        this.endpoint = endpoint;
        this.method = method;
        this.priority = priority;
        this.payload = payload;
    }
    
}

