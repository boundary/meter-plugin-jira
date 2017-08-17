package com.bmc.truesight.meter.plugin.jira;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.bmc.truesight.meter.plugin.jira.beans.RpcResponse;
import com.bmc.truesight.meter.plugin.jira.util.PluginConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.meter.plugin.jira.util.Utils;
import com.bmc.truesight.saas.jira.api.JiraAPI;
import com.bmc.truesight.saas.jira.beans.Configuration;
import com.bmc.truesight.saas.jira.beans.JIRAEventResponse;
import com.bmc.truesight.saas.jira.beans.Result;
import com.bmc.truesight.saas.jira.beans.Success;
import com.bmc.truesight.saas.jira.beans.TSIEvent;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.ParsingException;
import com.bmc.truesight.saas.jira.integration.adapter.JiraEntryEventAdapter;
import com.bmc.truesight.saas.jira.util.Constants;
import com.bmc.truesight.saas.jira.util.Util;
import com.bmc.truesight.saas.jira.util.searchQueryBuilder;
import com.boundary.plugin.sdk.Collector;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.Measurement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Santosh Patil
 * @Date 04-08-2017
 */
public class JiraTicketsCollector implements Collector {

    private static final Logger LOG = LoggerFactory.getLogger(JiraTicketsCollector.class);
    private final JiraPluginConfigurationItem config;
    private final Template template;
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public JiraTicketsCollector(JiraPluginConfigurationItem config, Template template) throws ParsingException {
        this.config = config;
        this.template = template;
        Utils.updateConfiguration(this.template, config);
    }

    @Override
    public Measurement[] getMeasures() {
        return null;
    }

    @Override
    public void run() {
        while (true) {
            EventSinkAPI eventSinkAPI = new EventSinkAPI();
            EventSinkStandardOutput eventSinkAPIstd = new EventSinkStandardOutput();
            System.err.println("{} validation is successful!");
            JIRAEventResponse jiraResponse = null;
            JiraEntryEventAdapter adapter = new JiraEntryEventAdapter();
            boolean isConnectionOpen = false;
            long totalTickets = 0;
            int startAt = 0;
            int totalFailure = 0;
            int iteration = 1;
            int totalSuccessful = 0;
            int validRecords = 0;
            int totalRecordsRead = 0;
            boolean isFound = false;
            int limit = 0;
            long totalJiraRecords = 0;
            List<String> limitExceededEventIds = new ArrayList<>();
            try {
                Date endDate = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                cal.add(Calendar.MINUTE, (int) -config.getPollInterval());
                Date startDate = cal.getTime();
                System.err.println("Starting event reading & ingestion to tsi for (DateTime:" + Util.JiraformatedDateAndTime(startDate) + " to DateTime:" + Util.JiraformatedDateAndTime(endDate) + ")");
                isConnectionOpen = eventSinkAPI.openConnection();
                if (isConnectionOpen) {
                    System.err.println("JSON RPC Socket connection successful");
                } else {
                    System.err.println("JSON RPC Socket connection failed");
                    break;
                }
                if (isConnectionOpen) {
                    Map<String, List<String>> errorsMap = new HashMap<>();
                    JiraRestClient client = JiraAPI.getJiraRestClient(config.getHostName(), config.getPort(), config.getUserName(), config.getPassword(), config.getProtocalType());
                    boolean isValid = JiraAPI.isValidCredentials(client, config.getUserName());
                    if (isValid) {
                        try {
                            Configuration configuration = template.getConfig();
                            String searchQuery = searchQueryBuilder.buildJQLQuery(template.getFilter(), Util.JiraformatedDateAndTime(startDate), Util.JiraformatedDateAndTime(endDate));
                            try {
                                String finalSearchUrl = Util.jqlBuilder(Util.getURL(config.getHostName(),
                                        config.getPort(), config.getUserName(), config.getPassword(), config.getProtocalType()), 0, startAt, searchQuery, Constants.JIRA_NONE_FIELD);
                                JsonNode responseNode = JiraAPI.search(finalSearchUrl, Util.getAuthCode(config.getUserName(), config.getPassword()), configuration);
                                if (!responseNode.isNull()) {
                                    try {
                                        totalTickets = responseNode.get(PluginConstants.JSON_ISSUES_TOTAL_FILED_KEY).asLong();
                                        isFound = true;
                                    } catch (NullPointerException ex) {
                                        System.err.println("{} Exception occured while getting the total tickets count" + responseNode.get(PluginConstants.ERROR_NODE).toString());
                                        eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, responseNode.get(PluginConstants.ERROR_NODE).toString(), Event.EventSeverity.ERROR.toString()));
                                    }
                                }
                                if (isFound) {
                                    if (totalTickets != 0) {
                                        for (int i = 0; i <= totalTickets; i += PluginConstants.METER_CHUNK_SIZE) {
                                            System.err.println("Iteration Satrted {} " + iteration);
                                            String searchUrl = Util.jqlBuilder(Util.getURL(config.getHostName(),
                                                    config.getPort(), config.getUserName(), config.getPassword(), config.getProtocalType()), PluginConstants.METER_CHUNK_SIZE, startAt, searchQuery, "");
                                            JsonNode response = JiraAPI.search(searchUrl, Util.getAuthCode(config.getUserName(), config.getPassword()), configuration);
                                            if (!response.isNull()) {
                                                JsonNode responseFiledsNode = response.get(Constants.JSON_ISSUES_NODE);
                                                if (!responseFiledsNode.isNull()) {
                                                    jiraResponse = adapter.eventList(responseFiledsNode, template, config.getRequestType());
                                                }
                                                totalRecordsRead += (jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventList().size());
                                                System.err.println(" Request Sent to jira (startFrom:" + startAt + ",chunkSize:" + PluginConstants.METER_CHUNK_SIZE + "), Response Got(Valid Events:" + jiraResponse.getValidEventList().size() + ", Invalid Events:" + jiraResponse.getInvalidEventList().size() + ", totalRecordsRead: (" + totalRecordsRead + "/" + totalTickets + ")");
                                                startAt = startAt + (jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventList().size());
                                                validRecords += jiraResponse.getValidEventList().size();
                                                totalJiraRecords+= (jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventList().size());
                                                iteration += 1;
                                                limit += jiraResponse.getInvalidEventList().size();
                                                limitExceededEventIds.addAll(jiraResponse.getInvalidEventIdsList());
                                                if (jiraResponse.getInvalidEventList().size() > 0) {
                                                    System.err.println("following " + config.getRequestType() + " ids are large than allowed limits");
                                                    List<String> eventIds = new ArrayList<>();
                                                    for (TSIEvent event : jiraResponse.getInvalidEventList()) {
                                                        eventIds.add(event.getProperties().get(com.bmc.truesight.saas.jira.util.Constants.FILED_KEY));
                                                    }
                                                    System.err.println("following " + config.getRequestType() + " ids are larger than allowed limits [" + String.join(",", eventIds) + "]");
                                                }
                                                List<TSIEvent> eventsList = jiraResponse.getValidEventList();
                                                if (eventsList.size() > 0) {
                                                    Gson gson = new Gson();
                                                    String eventJson = gson.toJson(eventsList);
                                                    StringBuilder sendEventToTSI = new StringBuilder();
                                                    sendEventToTSI.append(PluginConstants.JIRA_PROXY_EVENT_JSON_START_STRING).append(eventJson).append(PluginConstants.JIRA_PROXY_EVENT_JSON_END_STRING);
                                                    String resultJson = eventSinkAPI.emit(sendEventToTSI.toString());
                                                    ObjectMapper mapper = new ObjectMapper();
                                                    RpcResponse rpcResponse = mapper.readValue(resultJson, RpcResponse.class);
                                                    if (rpcResponse.getResult() == null || rpcResponse.getError() != null) {
                                                        System.err.println("Event Ingestion failed ->" + rpcResponse.getError());
                                                    } else {
                                                        Result result = rpcResponse.getResult().getResult();
                                                        if (result.getAccepted() != null) {
                                                            totalSuccessful += result.getAccepted().size();
                                                        }
                                                        if (result.getErrors() != null) {
                                                            totalFailure += result.getErrors().size();
                                                        }
                                                        if (result.getSuccess() == Success.PARTIAL) {
                                                            for (com.bmc.truesight.saas.remedy.integration.beans.Error error : result.getErrors()) {
                                                                String id = "";
                                                                String msg = error.getMessage().trim();
                                                                id = eventsList.get(error.getIndex()).getProperties().get(Constants.FIELD_ID);
                                                                if (errorsMap.containsKey(msg)) {
                                                                    List<String> errorsId = errorsMap.get(msg);
                                                                    errorsId.add(id);
                                                                    errorsMap.put(msg, errorsId);
                                                                } else {
                                                                    List<String> errorsId = new ArrayList<String>();
                                                                    errorsId.add(id);
                                                                    errorsMap.put(msg, errorsId);
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    System.err.println(eventsList.size() + " Events found for the interval, DateTime:" + Utils.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Utils.dateToString(template.getConfig().getEndDateTime()));
                                                }
                                            }
                                        }
                                        System.err.println("________________________" + config.getRequestType() + " ingestion to TrueSight Intelligence final status: Total JIRA Records = " + totalJiraRecords + ", Total Valid Records Sent to TSI = " + validRecords + ", Successfully TSI Accepted = " + totalSuccessful + ", larger than allowed limits count  = " + limit + " & Ids = "+limitExceededEventIds+" , TSI Rejected Records = " + totalFailure + " ______");
                                        if (totalFailure > 0) {
                                            System.err.println("________________________  Errors (No of times seen), [Reference Ids] ______");
                                            errorsMap.keySet().forEach(msg -> {
                                                System.err.println(msg + " (" + errorsMap.get(msg).size() + "), " + errorsMap.get(msg));
                                            });
                                        }
                                    } //Total END here
                                    else {
                                        System.err.println("{} " + PluginConstants.JIRA_IM_NO_DATA_AVAILABLE);
                                        eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, PluginConstants.JIRA_IM_NO_DATA_AVAILABLE, Event.EventSeverity.INFO.toString()));
                                    }
                                }
                            } catch (ParsingException ex) {
                                eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
                            }
                        } catch (ParseException ex) {
                            eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
                        }
                    } else {
                        System.err.println("Invalid credentials");
                        eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, "Invalid credentials", Event.EventSeverity.ERROR.toString()));
                    }
                }

            } catch (ParseException | IOException ex) {
                System.err.println("Interrupted Exception :" + ex.getMessage());
                eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
            } finally {
                if (isConnectionOpen) {
                    boolean isConnectionClosed = eventSinkAPI.closeConnection();
                    if (isConnectionClosed) {
                        System.err.println("JSON RPC Socket connection successfuly closed");
                    } else {
                        System.err.println("Closing JSON RPC Socket connection failed");
                    }
                }
            }

            try {
                TimeUnit.MINUTES.sleep(config.getPollInterval());
            } catch (InterruptedException ex) {
                System.err.println("Interrupted Exception :" + ex.getMessage());
                eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
            }
        }//infinite while loop end
    }

}
