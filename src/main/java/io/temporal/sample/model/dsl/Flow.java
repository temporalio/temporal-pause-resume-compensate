package io.temporal.sample.model.dsl;

import java.util.List;

public class Flow {
    private String id;
    private int version;
    private String description;
    private List<FlowAction> actions;

    public Flow() {}

    public Flow(String id, int version, String description, List<FlowAction> actions) {
        this.id = id;
        this.version = version;
        this.description = description;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FlowAction> getActions() {
        return actions;
    }

    public void setActions(List<FlowAction> actions) {
        this.actions = actions;
    }
}
