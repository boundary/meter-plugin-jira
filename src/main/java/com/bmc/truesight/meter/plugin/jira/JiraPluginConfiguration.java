package com.bmc.truesight.meter.plugin.jira;

import com.boundary.plugin.sdk.PluginConfiguration;
import java.util.ArrayList;

/**
 * @author Santosh Patil
 */
public class JiraPluginConfiguration implements PluginConfiguration {

    private ArrayList<JiraPluginConfigurationItem> items;

    @Override
    public String toString() {
        return "JiraPluginConfiguration [items=" + items + "|||, pollInterval=" + pollInterval + "]";
    }

    private int pollInterval;

    public JiraPluginConfiguration() {

    }

    public ArrayList<JiraPluginConfigurationItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<JiraPluginConfigurationItem> items) {
        this.items = items;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

}
