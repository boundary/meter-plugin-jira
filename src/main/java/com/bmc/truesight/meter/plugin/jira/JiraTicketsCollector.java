package com.bmc.truesight.meter.plugin.jira;

import com.bmc.truesight.meter.plugin.jira.beans.RpcResponse;
import com.bmc.truesight.meter.plugin.jira.util.PluginConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.meter.plugin.jira.util.Utils;
import com.bmc.truesight.saas.jira.beans.JiraEventResponse;
import com.bmc.truesight.saas.jira.beans.Result;
import com.bmc.truesight.saas.jira.beans.Success;
import com.bmc.truesight.saas.jira.beans.TSIEvent;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.JiraApiInstantiationFailedException;
import com.bmc.truesight.saas.jira.exception.JiraLoginFailedException;
import com.bmc.truesight.saas.jira.exception.JiraReadFailedException;
import com.bmc.truesight.saas.jira.exception.ParsingException;
import com.bmc.truesight.saas.jira.impl.JiraReader;
import com.bmc.truesight.saas.jira.integration.adapter.JiraEntryEventAdapter;
import com.bmc.truesight.saas.jira.util.Constants;
import com.boundary.plugin.sdk.Collector;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.Measurement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
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

    public JiraTicketsCollector(JiraPluginConfigurationItem config, Template template) throws ParsingException, com.bmc.truesight.saas.jira.exception.ParsingException, JiraApiInstantiationFailedException, JiraLoginFailedException {
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
        Long pollInterval = config.getPollInterval() * 60 * 1000;
        Long lastPoll = null;
        while (true) {
            JiraReader jiraReader = null;
            try {
                jiraReader = new JiraReader(template);
            } catch (JiraApiInstantiationFailedException ex) {
                System.err.println("Jira Api instantiation failed exception {} " + ex.getMessage());
            } catch (JiraLoginFailedException ex) {
                System.err.println(ex.getMessage());
            }
            EventSinkAPI eventSinkAPI = new EventSinkAPI();
            EventSinkStandardOutput eventSinkAPIstd = new EventSinkStandardOutput();
            JiraEventResponse jiraResponse = null;
            JiraEntryEventAdapter adapter = new JiraEntryEventAdapter();
            boolean isConnectionOpen = false;
            long totalTickets = 0;
            int startAt = 0;
            int totalFailure = 0;
            int iteration = 1;
            int totalSuccessful = 0;
            int validRecords = 0;
            int totalRecordsRead = 0;
            int limit = 0;
            int chunkSize = PluginConstants.METER_CHUNK_SIZE;
            long totalJiraRecords = 0;
            Long currentMili = Calendar.getInstance().getTimeInMillis();
            boolean readNext = true;
            Long pastMili = null;
            if (lastPoll == null) {
                pastMili = currentMili - pollInterval;
            } else {
                pastMili = lastPoll;
            }
            lastPoll = currentMili;
            List<String> limitExceededEventIds = new ArrayList<>();
            template.getConfig().setStartDateTime(new Date(pastMili));
            template.getConfig().setEndDateTime(new Date(currentMili));
            try {
                isConnectionOpen = eventSinkAPI.openConnection();
                if (isConnectionOpen) {
                    System.err.println("JSON RPC Socket connection successful");
                } else {
                    System.err.println("JSON RPC Socket connection failed");
                    break;
                }
                if (isConnectionOpen) {
                    Map<String, List<String>> errorsMap = new HashMap<>();
                    boolean isValid = false;
                    try {
                        isValid = jiraReader.validateCredentials();
                    } catch (JiraLoginFailedException ex) {
                        System.err.println("Jira login faild exception {} " + ex.getMessage());
                    } catch (JiraApiInstantiationFailedException ex) {
                        System.err.println("Jira Api instantiation failed exception {} " + ex.getMessage());
                    }
                    if (isValid) {
                        System.err.println("Starting event reading & ingestion to tsi for (DateTime:" + Utils.dateToString(template.getConfig().getStartDateTime()) + " to DateTime:" + Utils.dateToString(template.getConfig().getEndDateTime()) + ")");
                        try {
                            totalTickets = jiraReader.getAvailableRecordsCount();
                        } catch (JiraReadFailedException ex) {
                            totalTickets = -1;
                            System.err.println("Exception occured while getting total tickets count {} " + ex.getMessage());
                        } catch (ParseException ex) {
                            System.err.println("Exception occured while parsing responce {} " + ex.getMessage());
                        } catch (JiraApiInstantiationFailedException ex) {
                            System.err.println("Jira Api instantiation failed exception {} " + ex.getMessage());
                        }
                        if (totalTickets != 0 && totalTickets != -1) {
                            while (readNext) {
                                System.err.println("Iteration : " + iteration);
                                try {
                                    jiraResponse = jiraReader.readJiraTickets(startAt, chunkSize, adapter);
                                } catch (JiraReadFailedException ex) {
                                    System.err.println("Exception occured while reading jira tickets {} " + ex.getMessage());
                                } catch (JiraApiInstantiationFailedException ex) {
                                    System.err.println("Jira Api instantiation failed exception, while reading jira tickets {} " + ex.getMessage());
                                }
                                totalRecordsRead += (jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventList().size());
                                if (jiraResponse.getTotalCountAvailable() != totalTickets) {
                                    System.err.println("Total available tickets matching the filter criteria has changed from " + totalTickets + " to " + jiraResponse.getTotalCountAvailable());
                                    totalTickets = jiraResponse.getTotalCountAvailable();
                                }
                                startAt = startAt + (jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventList().size());
                                validRecords += jiraResponse.getValidEventList().size();
                                int recordsCount = jiraResponse.getValidEventList().size() + jiraResponse.getInvalidEventIdsList().size();
                                totalJiraRecords += recordsCount;
                                if (recordsCount < chunkSize && totalRecordsRead < totalTickets) {
                                    System.err.println(" Request Sent to jira (startFrom:" + startAt + ",chunkSize:" + chunkSize + "), Response Got(Valid Events:" + jiraResponse.getValidEventList().size() + ", Invalid Events:" + jiraResponse.getInvalidEventList().size() + ", totalRecordsRead: (" + totalRecordsRead + "/" + totalTickets + ")");
                                    chunkSize = recordsCount;
                                } else if (recordsCount <= chunkSize) {
                                    System.err.println(" Request Sent to jira (startFrom:" + startAt + ",chunkSize:" + chunkSize + "), Response Got(Valid Events:" + jiraResponse.getValidEventList().size() + ", Invalid Events:" + jiraResponse.getInvalidEventList().size() + ", totalRecordsRead: (" + totalRecordsRead + "/" + totalTickets + ")");
                                }
                                if (totalJiraRecords < totalTickets && (totalJiraRecords + chunkSize) > totalTickets) {
                                    //assuming the long value would be in int range always
                                    chunkSize = ((int) (totalTickets) - totalRecordsRead);
                                } else if (totalRecordsRead >= totalTickets) {
                                    readNext = false;
                                }
                                if (recordsCount == 0) {
                                    readNext = false;
                                    continue;
                                }
                                iteration += 1;
                                limit += jiraResponse.getInvalidEventList().size();
                                limitExceededEventIds.addAll(jiraResponse.getInvalidEventIdsList());
                                if (jiraResponse.getInvalidEventList().size() > 0) {
                                    List<String> eventIds = new ArrayList<>();
                                    for (TSIEvent event : jiraResponse.getInvalidEventList()) {
                                        eventIds.add(event.getProperties().get(com.bmc.truesight.saas.jira.util.Constants.FIELD_FETCH_KEY));
                                    }
                                    System.err.println("following ids are larger than allowed limits [" + String.join(",", eventIds) + "]");
                                }
                                List<TSIEvent> eventsList;
                                if (jiraResponse.getValidEventList().size() > 0) {
                                    eventsList = Utils.updateCreatedAtAsLastModifiedDate(jiraResponse.getValidEventList(), pastMili);
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
                                            for (com.bmc.truesight.saas.jira.beans.Error error : result.getErrors()) {
                                                String id = "";
                                                String msg = error.getMessage().trim();
                                                id = eventsList.get(error.getIndex()).getProperties().get(Constants.FIELD_FETCH_KEY);
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
                                        if (result.getSuccess() == Success.FALSE) {
                                            for (com.bmc.truesight.saas.jira.beans.Error error : result.getErrors()) {
                                                String id = "";
                                                String msg = error.getMessage().trim();
                                                id = eventsList.get(error.getIndex()).getProperties().get(Constants.FIELD_FETCH_KEY);
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
                                }
                            }//loop end here
                            System.err.println("________________________" + "" + " Ingestion to TrueSight Intelligence final status: Total JIRA Records = " + totalJiraRecords + ", Total Valid Records Sent to TSI = " + validRecords + ", Successfully TSI Accepted = " + totalSuccessful + ", larger than allowed limits count  = " + limit + " & Ids = " + limitExceededEventIds + " , TSI Rejected Records = " + totalFailure + " ______");
                            if (totalFailure > 0) {
                                System.err.println("________________________  Errors (Messages), [Reference Ids] ______");
                                errorsMap.keySet().forEach(msg -> {
                                    System.err.println(msg + " (" + errorsMap.get(msg).size() + "), " + errorsMap.get(msg));
                                });
                            }
                        } //Total END here
                        else if (totalTickets == 0) {
                            System.err.println("{} " + PluginConstants.JIRA_IM_NO_DATA_AVAILABLE);
                            eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, PluginConstants.JIRA_IM_NO_DATA_AVAILABLE, Event.EventSeverity.INFO.toString()));
                        }
                    }
                } else {
                    eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, "Invalid credentials", Event.EventSeverity.ERROR.toString()));
                }
            } catch (IOException ex) {
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
            Long now = Calendar.getInstance().getTimeInMillis();
            Long elapsedTime = now - lastPoll;
            Long timeToSleep = null;
            if (elapsedTime > pollInterval) {
                timeToSleep = 0l;
            } else {
                timeToSleep = pollInterval - elapsedTime;
            }

            if (timeToSleep > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(timeToSleep);
                } catch (InterruptedException ex) {
                    System.err.println("Interrupted Exception :" + ex.getMessage());
                    eventSinkAPIstd.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, ex.getMessage(), Event.EventSeverity.ERROR.toString()));
                }
            }
        }//infinite while loop end
    }

}
