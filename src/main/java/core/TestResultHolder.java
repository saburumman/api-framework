package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestResultHolder {

    private static final List<Map<String, Object>> allResults = new ArrayList<>();

    public static synchronized void addResults(List<Map<String, Object>> results) {
        allResults.addAll(results);
    }

    public static synchronized List<Map<String, Object>> getAllResults() {
        return new ArrayList<>(allResults);
    }

    public static synchronized void clear() {
        allResults.clear();
    }
}
