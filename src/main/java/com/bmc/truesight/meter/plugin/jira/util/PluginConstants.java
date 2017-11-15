package com.bmc.truesight.meter.plugin.jira.util;

/**
 *
 * @author Santosh Patil
 */
public class PluginConstants {

    public static String JIRA_PLUGIN_TITLE_MSG = "Jira Plugin";
    public static final String JIRA_PROXY_EVENT_JSON_START_STRING = "{ \"jsonrpc\": \"2.0\", \"id\":1, \"method\": \"proxy_event\", \"params\": {  \"data\":";
    public static final String JIRA_PROXY_EVENT_JSON_END_STRING = " } }";
    public final static String JIRA_IM_NO_DATA_AVAILABLE = "No data available";
    public final static String JIRA_CM_NO_DATA_AVAILABLE = "No data available for jira software";
    public static final int JIRA_WAIT_MS_BEFORE_NEXT_RETRY = 100;
	public static final String TSP_PLUGIN_PARAMS = "TSP_PLUGIN_PARAMS";
    public static String JSON_ISSUES_TOTAL_FILED_KEY = "total";
    public static int  METER_CHUNK_SIZE=100;
    public static String ERROR_NODE="errorMessages";
    public static int METRIC_VALUE=1;
    public static int METRIC_NONE_VALUE=0;
    
}
