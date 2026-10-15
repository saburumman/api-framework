package core;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.Environments;
import config.PrioritiesReader;
import io.qameta.allure.Allure;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import model.Flow;
import model.FlowStep;
import model.StepResult;

import java.util.*;

/**
 * Runs a {@link Flow}: steps execute in order and share variables.
 * Each step can extract values from its response (saved as {{variables}}) and assert on
 * status, body fields and response time. By default the flow stops at the first failed step
 * and the remaining steps are reported as skipped.
 *
 * The token from login_api is available as {{authToken}}. A step can replace it by
 * extracting a new value into "authToken" (e.g. to switch user mid-flow).
 */
public class FlowRunner {

    public static final String AUTH_TOKEN_VAR = "authToken";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PrioritiesReader config;
    private final Map<String, Object> globalVars;

    public FlowRunner(PrioritiesReader config, Map<String, Object> globalVars) {
        this.config = config;
        this.globalVars = globalVars != null ? globalVars : new LinkedHashMap<>();
    }

    public List<StepResult> run(Flow flow) {
        APIBase api = new APIBase(config.baseUrl);
        Map<String, Object> vars = new LinkedHashMap<>(Environments.vars());
        vars.putAll(globalVars);
        vars.putAll(flow.vars);
        vars.put("baseUrl", config.baseUrl);
        vars.put("env", config.env);

        if (flow.login && config.loginApi != null) {
            String token = Allure.step("Login (" + config.loginApi.name + ")",
                    () -> new AuthHandler(api).getToken(config.loginApi));
            vars.put(AUTH_TOKEN_VAR, token);
        }

        List<StepResult> results = new ArrayList<>();
        boolean stop = false;
        for (FlowStep step : flow.steps) {
            if (stop) {
                results.add(skipped(flow, step));
                continue;
            }
            StepResult result = runStepWithRetries(api, flow, step, vars);
            results.add(result);
            if (!result.passed && !flow.continueOnFailure) {
                stop = true;
            }
        }
        return results;
    }

    private StepResult runStepWithRetries(APIBase api, Flow flow, FlowStep step, Map<String, Object> vars) {
        StepResult result = null;
        int maxAttempts = 1 + Math.max(0, step.retries);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final int n = attempt;
            String title = step.name + (maxAttempts > 1 ? " (attempt " + n + "/" + maxAttempts + ")" : "");
            StepResult[] holder = new StepResult[1];
            try {
                Allure.step(title, () -> {
                    holder[0] = runStep(api, flow, step, vars);
                    holder[0].attempts = n;
                    if (!holder[0].passed) {
                        throw new AssertionError(String.join("\n", holder[0].failures));
                    }
                });
            } catch (Throwable ignored) {
                // failure is already recorded in holder[0]; Allure marks the step as failed
            }
            result = holder[0];
            if (result.passed) break;
            if (attempt < maxAttempts) sleep(step.retryDelayMs);
        }
        if (result.passed) {
            vars.putAll(result.extracted);
        }
        return result;
    }

    private StepResult runStep(APIBase api, Flow flow, FlowStep step, Map<String, Object> vars) {
        StepResult r = new StepResult();
        r.flowName = flow.name;
        r.stepName = step.name;
        r.method = step.method;
        r.baseUrl = config.baseUrl;
        r.endpoint = step.endpoint;

        try {
            // 1. Resolve {{placeholders}}
            r.endpoint = TemplateEngine.resolveToString(step.endpoint, vars);
            r.payload = buildPayload(step.payload, vars);
            Map<String, String> headers = toStringMap(TemplateEngine.resolve(step.headers, vars));
            Map<String, String> query = toStringMap(TemplateEngine.resolve(step.query, vars));
            Map<String, String> form = toStringMap(TemplateEngine.resolve(step.form, vars));
            if (!form.isEmpty()) r.payload = form.toString();
            String token = step.auth && vars.get(AUTH_TOKEN_VAR) != null ? String.valueOf(vars.get(AUTH_TOKEN_VAR)) : null;
            r.authToken = token == null ? "" : token;

            // 2. Block write requests on read-only environments (production by default)
            String blocked = writeBlockedReason(step.method);
            if (blocked != null) {
                r.failures.add(blocked);
                r.passed = false;
                return r;
            }

            // 3. Send
            long start = System.currentTimeMillis();
            Response response = form.isEmpty()
                    ? api.send(step.method, r.endpoint, token, headers, query, r.payload)
                    : api.send(step.method, r.endpoint, token, headers, query, null, form);
            r.timeMs = System.currentTimeMillis() - start;
            r.statusCode = response.getStatusCode();
            r.responseBody = response.getBody().asString();

            // 4. Assert
            checkStatus(step, r);
            if (step.maxTimeMs > 0 && r.timeMs > step.maxTimeMs) {
                r.failures.add("Took " + r.timeMs + " ms, limit is " + step.maxTimeMs + " ms");
            }
            JsonPath json = null;
            if (!step.expectBody.isEmpty() || !step.extract.isEmpty()) {
                json = parseJson(r);
            }
            if (json != null) {
                checkBody(step, json, vars, r);
                extract(step, json, r);
            }
        } catch (Exception e) {
            r.failures.add(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        r.passed = r.failures.isEmpty();
        if (!r.extracted.isEmpty()) {
            Allure.addAttachment("Extracted variables", "text/plain", r.extracted.toString());
        }
        return r;
    }

    /** Null if allowed, otherwise why the request was blocked. */
    public static String writeBlockedReason(String method) {
        String m = method == null ? "GET" : method.toUpperCase();
        boolean safe = m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS");
        if (safe || !Environments.isReadOnly() || Boolean.getBoolean("allowWrites")) return null;
        return "Blocked " + m + " request: environment '" + Environments.active()
                + "' is read-only. Run with -DallowWrites=true if you really mean it, "
                + "or limit this flow with envs: [stg, preprod].";
    }

    // ---------- assertions ----------

    private void checkStatus(FlowStep step, StepResult r) {
        if (step.expectStatus.isEmpty()) {
            if (r.statusCode < 200 || r.statusCode >= 300) {
                r.failures.add("Expected a 2xx status but got " + r.statusCode);
            }
        } else if (!step.expectStatus.contains(r.statusCode)) {
            r.failures.add("Expected status " + step.expectStatus + " but got " + r.statusCode);
        }
    }

    private JsonPath parseJson(StepResult r) {
        try {
            JsonPath json = new JsonPath(r.responseBody);
            json.get(""); // forces parsing so invalid JSON fails here
            return json;
        } catch (Exception e) {
            r.failures.add("Response is not valid JSON, cannot check body/extract values");
            return null;
        }
    }

    private void checkBody(FlowStep step, JsonPath json, Map<String, Object> vars, StepResult r) {
        for (Map.Entry<String, Object> e : step.expectBody.entrySet()) {
            String path = e.getKey();
            Object expected = TemplateEngine.resolve(e.getValue(), vars);
            Object actual;
            try {
                actual = json.get(normalisePath(path));
            } catch (Exception ex) {
                r.failures.add("Body path '" + path + "' could not be read: " + ex.getMessage());
                continue;
            }
            String problem = Matchers.check(actual, expected);
            if (problem != null) {
                r.failures.add("Body '" + path + "': " + problem);
            }
        }
    }

    private void extract(FlowStep step, JsonPath json, StepResult r) {
        for (Map.Entry<String, String> e : step.extract.entrySet()) {
            Object value;
            try {
                value = json.get(normalisePath(e.getValue()));
            } catch (Exception ex) {
                value = null;
            }
            if (value == null) {
                r.failures.add("Could not extract '" + e.getKey() + "' from path '" + e.getValue() + "' (value is missing or null)");
            } else {
                r.extracted.put(e.getKey(), value);
            }
        }
    }

    /** Accepts "$.data.id", "$data.id" or "data.id" (REST Assured GPath syntax). "$" means the root. */
    static String normalisePath(String path) {
        String p = path.trim();
        if (p.equals("$")) return "";
        if (p.startsWith("$.")) return p.substring(2);
        if (p.startsWith("$")) return p.substring(1);
        return p;
    }

    // ---------- helpers ----------

    private String buildPayload(Object payload, Map<String, Object> vars) throws Exception {
        if (payload == null) return "";
        Object resolved = TemplateEngine.resolve(payload, vars);
        if (resolved instanceof String) return (String) resolved;
        return MAPPER.writeValueAsString(resolved);
    }

    private static Map<String, String> toStringMap(Object o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o instanceof Map) {
            ((Map<?, ?>) o).forEach((k, v) -> out.put(String.valueOf(k), v == null ? "" : String.valueOf(v)));
        }
        return out;
    }

    private StepResult skipped(Flow flow, FlowStep step) {
        StepResult r = new StepResult();
        r.flowName = flow.name;
        r.stepName = step.name;
        r.method = step.method;
        r.baseUrl = config.baseUrl;
        r.endpoint = step.endpoint;
        r.skipped = true;
        r.passed = false;
        r.statusCode = 0;
        r.failures.add("Skipped because an earlier step failed");
        Allure.step(step.name + " (skipped - earlier step failed)");
        return r;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Body matchers usable in expect.body. */
    static final class Matchers {
        private Matchers() {}

        /** Returns null if OK, otherwise a description of the mismatch. */
        static String check(Object actual, Object expected) {
            if (expected instanceof String && ((String) expected).startsWith("@")) {
                String rule = (String) expected;
                if (rule.equals("@notNull")) return actual != null ? null : "expected a value but was null/missing";
                if (rule.equals("@isNull")) return actual == null ? null : "expected null but was " + actual;
                if (rule.equals("@notEmpty")) return !isEmpty(actual) ? null : "expected not empty but was " + actual;
                if (rule.equals("@isEmpty")) return isEmpty(actual) ? null : "expected empty but was " + actual;
                if (rule.startsWith("@contains:")) {
                    String part = rule.substring("@contains:".length());
                    return actual != null && String.valueOf(actual).contains(part) ? null
                            : "expected to contain '" + part + "' but was " + actual;
                }
                if (rule.startsWith("@matches:")) {
                    String regex = rule.substring("@matches:".length());
                    return actual != null && String.valueOf(actual).matches(regex) ? null
                            : "expected to match /" + regex + "/ but was " + actual;
                }
                if (rule.startsWith("@size:")) {
                    int size = Integer.parseInt(rule.substring("@size:".length()).trim());
                    int actualSize = sizeOf(actual);
                    return actualSize == size ? null : "expected size " + size + " but was " + actualSize;
                }
                if (rule.startsWith("@minSize:")) {
                    int size = Integer.parseInt(rule.substring("@minSize:".length()).trim());
                    int actualSize = sizeOf(actual);
                    return actualSize >= size ? null : "expected at least " + size + " items but was " + actualSize;
                }
                return "unknown matcher " + rule;
            }
            if (expected == null) return actual == null ? null : "expected null but was " + actual;
            if (actual == null) return "expected " + expected + " but was null/missing";
            // Compare as text so 1 == "1" and true == "true" (YAML vs JSON typing differences)
            return String.valueOf(actual).equals(String.valueOf(expected)) ? null
                    : "expected " + expected + " but was " + actual;
        }

        private static boolean isEmpty(Object o) {
            return o == null || sizeOf(o) == 0;
        }

        private static int sizeOf(Object o) {
            if (o == null) return 0;
            if (o instanceof Collection) return ((Collection<?>) o).size();
            if (o instanceof Map) return ((Map<?, ?>) o).size();
            return String.valueOf(o).length();
        }
    }
}
