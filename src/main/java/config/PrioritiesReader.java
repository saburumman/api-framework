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

            env = obj.get("env").toString();
            baseUrl = obj.get("base_url").toString();

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

        } catch (Exception e) {
            System.err.println("Failed to load YAML file.");
            e.printStackTrace();
        }
    }
}
