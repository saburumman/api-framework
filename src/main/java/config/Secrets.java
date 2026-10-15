package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads secrets and environment-specific values without hardcoding them in the repo.
 *
 * Lookup order for a key such as "API_LOGIN_EMAIL":
 *   1. JVM system property      (-DAPI_LOGIN_EMAIL=...)
 *   2. OS environment variable   (API_LOGIN_EMAIL=...)   - used in CI via GitHub Secrets
 *   3. .env file in the project folder (API_LOGIN_EMAIL=...) - used when running locally
 *
 * Environment-specific values win over generic ones: when ENV=preprod, PREPROD_API_LOGIN_EMAIL
 * is tried (in all three sources) before API_LOGIN_EMAIL.
 *
 * The .env file location can be changed with -DenvFile=path/to/file.
 * .env is git-ignored; copy .env.example to .env and fill in your values.
 */
public final class Secrets {

    private static Map<String, String> dotEnv;

    private Secrets() {}

    /** Returns the value, or null if it is not set anywhere. */
    public static String get(String key) {
        String env = Environments.active();
        if (env != null) {
            String specific = getRaw(env.toUpperCase(java.util.Locale.ROOT) + "_" + key);
            if (specific != null) return specific;
        }
        return getRaw(key);
    }

    private static String getRaw(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) value = System.getenv(key);
        if (isBlank(value)) value = dotEnv().get(key);
        return isBlank(value) ? null : value;
    }

    /** Value from the .env file only (used while the environment itself is being chosen). */
    static String dotEnvValue(String key) {
        String value = dotEnv().get(key);
        return isBlank(value) ? null : value;
    }

    /** Returns the value, or the default if it is not set anywhere. */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /** Returns the value, or throws a clear error explaining how to set it. */
    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException("Missing required setting '" + key + "'. "
                    + (Environments.active() != null ? "Environment is '" + Environments.active() + "' - you can also use "
                        + Environments.active().toUpperCase(java.util.Locale.ROOT) + "_" + key + ". " : "")
                    + "Add it to the .env file in the project folder (see .env.example), "
                    + "set it as an environment variable, or pass -D" + key + "=<value>.");
        }
        return value;
    }

    private static synchronized Map<String, String> dotEnv() {
        if (dotEnv == null) {
            dotEnv = new HashMap<>();
            Path file = Paths.get(System.getProperty("envFile", ".env"));
            if (Files.isRegularFile(file)) {
                try {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (String raw : lines) {
                        String line = raw.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (line.startsWith("export ")) line = line.substring(7).trim();
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                                || value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        dotEnv.put(key, value);
                    }
                    System.out.println("[CONFIG] Loaded " + dotEnv.size() + " value(s) from " + file.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[CONFIG] Could not read " + file.toAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
        return dotEnv;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
