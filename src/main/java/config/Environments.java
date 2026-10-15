package config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Picks the target environment (stg / preprod / production) and its settings.
 *
 * Choose the environment with ENV (any of these, first match wins):
 *   -DENV=preprod   |   ENV=preprod environment variable   |   ENV=preprod in .env
 * If ENV is not set, the "env:" value in priorities.yaml is used.
 *
 * Aliases: staging/stage -> stg, pre-prod/pre_prod/uat -> preprod, prod/live -> production.
 *
 * Per-environment secrets: Secrets looks for <ENV>_<KEY> before <KEY>, e.g.
 *   PREPROD_API_LOGIN_EMAIL=...   (used only when ENV=preprod)
 *   API_LOGIN_EMAIL=...           (fallback for every environment)
 */
public final class Environments {

    public static final List<String> KNOWN = Arrays.asList("stg", "preprod", "production");

    private static String active;
    private static boolean readOnly;
    private static final Map<String, Object> VARS = new LinkedHashMap<>();

    private Environments() {}

    /** The active environment name (stg, preprod, production), or null before priorities.yaml is loaded. */
    public static String active() {
        return active;
    }

    /** True when the environment is marked read_only (production by default): flows may only send GET/HEAD/OPTIONS. */
    public static boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Settings of the active environment usable as {{placeholders}}: every key under
     * environments.<env> (e.g. {{auth_url}}), plus {{baseUrl}} and {{env}}.
     */
    public static Map<String, Object> vars() {
        return new LinkedHashMap<>(VARS);
    }

    public static boolean isProduction() {
        return "production".equals(active);
    }

    /** Normalises user input like "Staging" or "PROD" to stg / preprod / production. */
    public static String normalise(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "staging": case "stage": case "stg":            return "stg";
            case "pre-prod": case "pre_prod": case "preprod":
            case "uat":                                          return "preprod";
            case "prod": case "live": case "production":         return "production";
            default:                                             return n;
        }
    }

    /**
     * Resolves the environment from ENV / the yaml default and returns its base URL.
     * Called by PrioritiesReader.
     */
    @SuppressWarnings("unchecked")
    static String resolve(Map<String, Object> yamlRoot) {
        // Do not use Secrets.get here: it applies the env prefix, which isn't known yet.
        String requested = firstNonBlank(System.getProperty("ENV"), System.getProperty("env"),
                System.getenv("ENV"), Secrets.dotEnvValue("ENV"));
        String fallback = yamlRoot.get("env") == null ? null : String.valueOf(yamlRoot.get("env"));
        String name = normalise(firstNonBlank(requested, fallback));
        if (name == null) {
            throw new IllegalStateException("No environment selected. Set ENV=stg|preprod|production "
                    + "(in .env, as an environment variable, or -DENV=...) or set env: in priorities.yaml.");
        }

        Map<String, Object> envs = yamlRoot.get("environments") instanceof Map
                ? (Map<String, Object>) yamlRoot.get("environments") : Collections.emptyMap();

        String baseUrl;
        if (envs.isEmpty()) {
            // Old single-environment format: base_url at the top level
            baseUrl = yamlRoot.get("base_url") == null ? null : String.valueOf(yamlRoot.get("base_url"));
            readOnly = false;
        } else {
            Object cfgObj = envs.get(name);
            if (!(cfgObj instanceof Map)) {
                throw new IllegalStateException("Unknown environment '" + name + "'. Configured in priorities.yaml: "
                        + envs.keySet());
            }
            Map<String, Object> cfg = (Map<String, Object>) cfgObj;
            VARS.clear();
            cfg.forEach((k, v) -> VARS.put(k, v == null ? "" : String.valueOf(v).trim()));
            baseUrl = cfg.get("base_url") == null ? null : String.valueOf(cfg.get("base_url")).trim();
            readOnly = Boolean.parseBoolean(String.valueOf(cfg.getOrDefault("read_only", "production".equals(name))));
        }

        active = name;

        // -DbaseUrl / BASE_URL (also <ENV>_BASE_URL) still override for one-off runs
        String override = firstNonBlank(System.getProperty("baseUrl"), Secrets.get("BASE_URL"));
        if (override != null) baseUrl = override;

        VARS.put("baseUrl", baseUrl);
        VARS.put("base_url", baseUrl);
        VARS.put("env", name);

        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("No base_url set for environment '" + name
                    + "'. Fill it in under environments." + name + " in priorities.yaml.");
        }

        System.out.println("[ENV] Running against: " + name.toUpperCase(Locale.ROOT) + " -> " + baseUrl
                + (readOnly ? (Boolean.getBoolean("allowWrites")
                        ? "  (READ-ONLY, but -DallowWrites=true: write requests WILL be sent)"
                        : "  (READ-ONLY: write requests are blocked)") : ""));
        writeAllureEnvironment(baseUrl);
        return baseUrl;
    }

    /** Shows the environment on the Allure report's overview page. */
    private static void writeAllureEnvironment(String baseUrl) {
        String dir = System.getProperty("allure.results.directory", "allure-results");
        try {
            File folder = new File(dir);
            if (!folder.exists() && !folder.mkdirs()) return;
            try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(folder, "environment.properties")),
                    StandardCharsets.UTF_8)) {
                w.write("Environment=" + active + "\n");
                w.write("Base.URL=" + baseUrl.replace(":", "\\:") + "\n");
                w.write("Read.Only=" + readOnly + "\n");
            }
        } catch (Exception e) {
            System.err.println("[ENV] Could not write Allure environment.properties: " + e.getMessage());
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }
}
