package config;

import model.Flow;
import model.FlowStep;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/** Loads flow definitions from flows.yaml (see the comments at the top of that file for the format). */
public class FlowsReader {

    public Map<String, Object> globalVars = new LinkedHashMap<>();
    public Map<String, Flow> flows = new LinkedHashMap<>();

    public FlowsReader load(String path) {
        try (InputStream in = FlowsReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Flows file not found on classpath: " + path);
            }
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) return this;

            globalVars.putAll(asMap(root.get("vars")));

            for (Map.Entry<String, Object> entry : asMap(root.get("flows")).entrySet()) {
                flows.put(entry.getKey(), parseFlow(entry.getKey(), asMap(entry.getValue())));
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path + ": " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Flows to run, filtered by -Dflow=name1,name2 and/or -DflowTags=smoke,regression,
     * and by each flow's "envs:" list against the active environment (stg/preprod/production).
     * With no filter, every flow allowed on the active environment is returned.
     */
    public List<Flow> selected() {
        Set<String> names = csv(System.getProperty("flow"));
        Set<String> tags = csv(System.getProperty("flowTags"));
        List<Flow> result = new ArrayList<>();
        for (Flow flow : flows.values()) {
            boolean nameOk = names.isEmpty() || names.contains(flow.name);
            boolean tagOk = tags.isEmpty() || flow.tags.stream().anyMatch(tags::contains);
            boolean envOk = flow.envs.isEmpty() || Environments.active() == null
                    || flow.envs.contains(Environments.active());
            if (nameOk && tagOk && envOk) {
                result.add(flow);
            } else if (nameOk && tagOk) {
                System.out.println("[ENV] Skipping flow '" + flow.name + "' - it only runs on " + flow.envs);
            }
        }
        if (!names.isEmpty()) {
            for (String n : names) {
                if (!flows.containsKey(n)) {
                    throw new IllegalArgumentException("Flow '" + n + "' not found. Available: " + flows.keySet());
                }
            }
        }
        return result;
    }

    private Flow parseFlow(String name, Map<String, Object> m) {
        Flow flow = new Flow();
        flow.name = name;
        flow.description = str(m.get("description"), "");
        flow.tags = strList(m.get("tags"));
        for (String e : strList(m.get("envs"))) flow.envs.add(Environments.normalise(e));
        flow.login = bool(m.get("login"), true);
        flow.continueOnFailure = bool(m.get("continueOnFailure"), false);
        flow.vars.putAll(asMap(m.get("vars")));

        Object steps = m.get("steps");
        if (!(steps instanceof List) || ((List<?>) steps).isEmpty()) {
            throw new IllegalArgumentException("Flow '" + name + "' has no steps.");
        }
        int i = 1;
        for (Object s : (List<?>) steps) {
            flow.steps.add(parseStep(name, i++, asMap(s)));
        }
        return flow;
    }

    private FlowStep parseStep(String flowName, int index, Map<String, Object> m) {
        FlowStep step = new FlowStep();
        step.name = str(m.get("name"), "Step " + index);
        step.method = str(m.get("method"), "GET").toUpperCase();
        step.endpoint = str(m.get("endpoint"), null);
        if (step.endpoint == null) {
            throw new IllegalArgumentException("Flow '" + flowName + "', step '" + step.name + "' has no endpoint.");
        }
        step.payload = m.get("payload");
        step.headers.putAll(asMap(m.get("headers")));
        step.query.putAll(asMap(m.get("query")));
        step.form.putAll(asMap(m.get("form")));
        step.auth = bool(m.get("auth"), true);
        step.retries = (int) num(m.get("retries"), 0);
        step.retryDelayMs = num(m.get("retryDelayMs"), 1000);

        Map<String, Object> expect = asMap(m.get("expect"));
        Object status = expect.get("status");
        if (status instanceof List) {
            for (Object o : (List<?>) status) step.expectStatus.add(Integer.parseInt(String.valueOf(o)));
        } else if (status != null) {
            step.expectStatus.add(Integer.parseInt(String.valueOf(status)));
        }
        step.expectBody.putAll(asMap(expect.get("body")));
        step.maxTimeMs = num(expect.get("maxTimeMs"), 0);

        for (Map.Entry<String, Object> e : asMap(m.get("extract")).entrySet()) {
            step.extract.put(e.getKey(), String.valueOf(e.getValue()));
        }
        return step;
    }

    // ---------- small helpers ----------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    private static String str(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }

    private static boolean bool(Object o, boolean def) {
        return o == null ? def : Boolean.parseBoolean(String.valueOf(o));
    }

    private static long num(Object o, long def) {
        return o == null ? def : Long.parseLong(String.valueOf(o));
    }

    private static List<String> strList(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<?>) o) out.add(String.valueOf(x));
        else if (o != null) out.addAll(csv(String.valueOf(o)));
        return out;
    }

    private static Set<String> csv(String s) {
        Set<String> out = new LinkedHashSet<>();
        if (s == null) return out;
        for (String part : s.split(",")) if (!part.trim().isEmpty()) out.add(part.trim());
        return out;
    }
}
