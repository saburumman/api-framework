package model;

import java.util.*;

/** One request inside a flow, as described in flows.yaml. */
public class FlowStep {
    public String name;
    public String method = "GET";
    public String endpoint;
    /** String (raw JSON) or Map/List (serialised to JSON). May contain {{placeholders}}. */
    public Object payload;
    public Map<String, Object> headers = new LinkedHashMap<>();
    public Map<String, Object> query = new LinkedHashMap<>();
    /** Form fields (x-www-form-urlencoded). When set, sent instead of payload. */
    public Map<String, Object> form = new LinkedHashMap<>();
    /** Send the Bearer token ({{authToken}}) with this step. Default true. */
    public boolean auth = true;

    /** Accepted status codes. Empty = any 2xx. */
    public List<Integer> expectStatus = new ArrayList<>();
    /** JSON path -> expected value or matcher (@notNull, @notEmpty, @isNull, @contains:x, @matches:regex). */
    public Map<String, Object> expectBody = new LinkedHashMap<>();
    /** Fail the step if the response is slower than this (0 = no limit). */
    public long maxTimeMs = 0;

    /** Variable name -> JSON path to save from the response, for later steps. */
    public Map<String, String> extract = new LinkedHashMap<>();

    /** Extra attempts if this step fails (e.g. eventually-consistent APIs). Default 0. */
    public int retries = 0;
    public long retryDelayMs = 1000;

    @Override
    public String toString() {
        return method + " " + endpoint + " (" + name + ")";
    }
}
