package com.bmc.truesight.meter.plugin.jira.util;

/**
 *
 * @author Santosh Patil
 * @Date 09-10-2017
 */
public enum Metrics {

    JIRA_HEARTBEAT("JIRA_HEARTBEAT"),
    JIRA_INGESTION_SUCCESS_COUNT("JIRA_INGESTION_SUCCESS_COUNT"),
    JIRA_INGESTION_FAILURE_COUNT("JIRA_INGESTION_FAILURE_COUNT"),
    JIRA_INGESTION_EXCEPTION("JIRA_INGESTION_EXCEPTION");
    private final String name;

    Metrics(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Get the metric
     *
     * @return
     */
    public String getMetricName() {
        return name;
    }

}
