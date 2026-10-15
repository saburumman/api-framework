package model;

import java.util.*;

/** An ordered list of steps that share variables, defined under "flows:" in flows.yaml. */
public class Flow {
    public String name;
    public String description = "";
    public List<String> tags = new ArrayList<>();
    /** Environments this flow may run on (e.g. [stg, preprod]). Empty = all environments. */
    public List<String> envs = new ArrayList<>();
    /** Log in with login_api from priorities.yaml before the first step. Default true. */
    public boolean login = true;
    /** Keep running later steps after a failure. Default false (stop the flow). */
    public boolean continueOnFailure = false;
    /** Starting variables for this flow (merged over the global vars). */
    public Map<String, Object> vars = new LinkedHashMap<>();
    public List<FlowStep> steps = new ArrayList<>();

    @Override
    public String toString() {
        return name;
    }
}
