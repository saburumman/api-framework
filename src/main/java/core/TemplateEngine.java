package core;

import config.Secrets;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {{placeholders}} in strings, maps and lists.
 *
 * Supported placeholders:
 *   {{myVar}}          value saved earlier (flow vars, global vars, or a previous step's "extract")
 *   {{env:NAME}}       system property / environment variable NAME (see {@link Secrets})
 *   {{$timestamp}}     current epoch millis
 *   {{$uuid}}          random UUID
 *   {{$randomInt}}     random int 0..99999
 *   {{$randomEmail}}   unique email, e.g. qa_1697357000000_4821@test.com
 *
 * If a whole string is exactly one placeholder (e.g. "{{courseId}}") the raw value is returned,
 * so numbers and booleans keep their type when used inside a YAML payload map.
 */
public final class TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    private TemplateEngine() {}

    public static Object resolve(Object value, Map<String, Object> vars) {
        if (value == null) return null;
        if (value instanceof String) return resolveString((String) value, vars);
        if (value instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                out.put(String.valueOf(resolveString(String.valueOf(e.getKey()), vars)), resolve(e.getValue(), vars));
            }
            return out;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) out.add(resolve(item, vars));
            return out;
        }
        return value;
    }

    /** Always returns a String (used for endpoints, headers, string payloads). */
    public static String resolveToString(String value, Map<String, Object> vars) {
        Object resolved = resolveString(value, vars);
        return resolved == null ? null : String.valueOf(resolved);
    }

    private static Object resolveString(String text, Map<String, Object> vars) {
        if (text == null) return null;
        Matcher whole = PLACEHOLDER.matcher(text);
        if (whole.matches()) {
            return lookup(whole.group(1), vars);
        }
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(lookup(m.group(1), vars))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Object lookup(String key, Map<String, Object> vars) {
        switch (key) {
            case "$timestamp":   return String.valueOf(System.currentTimeMillis());
            case "$uuid":        return UUID.randomUUID().toString();
            case "$randomInt":   return ThreadLocalRandom.current().nextInt(100000);
            case "$randomEmail": return "qa_" + System.currentTimeMillis() + "_"
                                        + ThreadLocalRandom.current().nextInt(10000) + "@test.com";
            default: break;
        }
        if (key.startsWith("env:")) {
            return Secrets.require(key.substring(4).trim());
        }
        if (vars != null && vars.containsKey(key)) {
            return vars.get(key);
        }
        throw new IllegalStateException("Unknown variable {{" + key + "}}. Available: "
                + (vars == null ? "[]" : vars.keySet()));
    }
}
