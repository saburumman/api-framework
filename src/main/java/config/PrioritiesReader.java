package config;

import model.APIInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class PrioritiesReader {
    public String env;
    public String baseUrl;
    public List<APIInfo> apis = new ArrayList<>();
    public APIInfo loginApi = null;
    public Map<String, List<APIInfo>> groupedApis = new HashMap<>();

    public void load(String path) {
        System.out.println("Loading YAML file: " + path);
        Yaml yaml = new Yaml();

        try (InputStream in = PrioritiesReader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("YAML file not found: " + path);
            }

            Map<String, Object> obj = yaml.load(in);
            System.out.println("YAML loaded successfully.");

            // Picks stg / preprod / production (ENV setting, else "env:" in the yaml) and its base URL
            baseUrl = Environments.resolve(obj);
            env = Environments.active();

            // Load login API
            if (obj.containsKey("login_api")) {
                Map<String, Object> loginObj = (Map<String, Object>) obj.get("login_api");
                loginApi = new APIInfo(
                        loginObj.get("name").toString(),
                        loginObj.get("endpoint").toString(),
                        loginObj.get("method").toString(),
                        loginObj.getOrDefault("priority", "high").toString(),
                        loginObj.get("payload") != null ? loginObj.get("payload").toString() : null
                );
                if (loginObj.get("form") instanceof Map) loginApi.form.putAll((Map<String, Object>) loginObj.get("form"));
                if (loginObj.get("headers") instanceof Map) loginApi.headers.putAll((Map<String, Object>) loginObj.get("headers"));
                if (loginObj.get("token_path") != null) loginApi.tokenPath = loginObj.get("token_path").toString();
            }

            // Load grouped APIs
            if (obj.containsKey("groups")) {
                Map<String, List<Map<String, Object>>> groupMap = (Map<String, List<Map<String, Object>>>) obj.get("groups");
                for (Map.Entry<String, List<Map<String, Object>>> entry : groupMap.entrySet()) {
                    String groupName = entry.getKey();
                    List<APIInfo> groupApis = new ArrayList<>();
                    for (Map<String, Object> api : entry.getValue()) {
                        groupApis.add(new APIInfo(
                                api.get("name").toString(),
                                api.get("endpoint").toString(),
                                api.get("method").toString(),
                                api.getOrDefault("priority", "medium").toString(),
                                api.get("payload") != null ? api.get("payload").toString() : null
                        ));
                    }
                    groupedApis.put(groupName, groupApis);
                }
            }

            // Load flat list of APIs
            if (obj.containsKey("apis")) {
                List<Map<String, Object>> apiList = (List<Map<String, Object>>) obj.get("apis");
                for (Map<String, Object> api : apiList) {
                    apis.add(new APIInfo(
                            api.get("name").toString(),
                            api.get("endpoint").toString(),
                            api.get("method").toString(),
                            api.getOrDefault("priority", "medium").toString(),
                            api.get("payload") != null ? api.get("payload").toString() : null
                    ));
                }
            }

        } catch (IllegalStateException e) {
            // Configuration mistakes (unknown environment, missing base_url) should stop the run with a clear message
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to load YAML file.");
            e.printStackTrace();
        }
    }
}
