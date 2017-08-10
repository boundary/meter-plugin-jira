package com.bmc.truesight.meter.plugin.jira.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmc.truesight.meter.plugin.jira.JiraPluginConfigurationItem;
import com.bmc.truesight.saas.jira.beans.Configuration;
import com.bmc.truesight.saas.jira.beans.FieldItem;
import com.bmc.truesight.saas.jira.beans.TSIEvent;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.ParsingException;
import com.bmc.truesight.saas.jira.util.StringUtil;
import com.bmc.truesight.saas.jira.util.Util;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Santosh Patil
 *
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String dateToString(Date date) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(date);
    }

    public static String getFieldValues(String fieldValue[]) {
        StringBuffer fieldValues = new StringBuffer();
        if (fieldValue != null && fieldValue.length > 0) {
            for (String val : fieldValue) {
                fieldValues.append(val);
            }
        }
        return fieldValues.toString();
    }

    public static Map<JsonElement, String> getKeyAndValueFromJsonObjectForRemedy(JsonObject jsonObjects, String jsonKey) {
        Map<JsonElement, String> values = new HashMap<>();
        JsonObject jsonObject = jsonObjects.get(jsonKey).getAsJsonObject();
        if (jsonObject.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> elements = ((JsonObject) jsonObject).entrySet();
            if (elements != null) {
                // Iterate JSON Elements with Key values
                elements.stream().forEach((en) -> {
                    values.put(en.getValue(), en.getKey());
                });
            }
        }
        return values;
    }

    public static String encodeBase64(final String encodeToken) {
        byte[] encoded = Base64.encodeBase64(encodeToken.getBytes());
        return new String(encoded);
    }

    public static void send(final Event.EventSeverity severity, final String strShortSummary, final String strSummary, final String hostName, final String source) {
        EventSinkStandardOutput output = new EventSinkStandardOutput();
        Event event = new Event(severity, strShortSummary,
                strSummary, hostName, source, null);
        output.emit(event);
    }

    public static String remove(String removeString, String originalString) {

        originalString = originalString.replace(removeString, "").trim();
        return originalString;
    }

    public static Event eventMeterTSI(final String title, final String message, String severity) {
        Event event = new Event(title, message);
        return event;
    }

    /*public static Template updateConfiguration(Template template, JiraPluginConfigurationItem config) throws ParsingException {
        Configuration configuration = template.getConfig();
        configuration.setJiraHostName(config.getHostName());
        if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
            try {
                Integer port = Integer.parseInt(config.getPort());
                configuration.setJiraPort(port);
            } catch (NumberFormatException ex) {
                System.err.println("Port (" + config.getPort() + ") is not a valid port, using default port.");
            }
        }
        configuration.setJiraUserName(config.getUserName());
        configuration.setJiraPassword(config.getPassword());
        JsonNode fieldNode = convertTOJsont(config.getFields());
        ObjectMapper mapper = new ObjectMapper();

        if (!fieldNode.isNull()) {
            JsonNode filterConfiguration = fieldNode.get(com.bmc.truesight.saas.jira.util.Constants.CONFIG_FILTER_NODE);
            if (filterConfiguration != null) {
                ObjectReader obReader = mapper.reader(new TypeReference<List<String>>() {
                });
                try {
                    JsonNode condFields = filterConfiguration.get(com.bmc.truesight.saas.jira.util.Constants.CONFIG_ISSUES_TYPE_CONDFIELDS_NODE_NAME);
                    if (condFields != null) {
                        List<String> condList = null;
                        condList = obReader.readValue(condFields);
                        configuration.setIssueTypeConditionFields(condList);
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);

                }
                try {
                    JsonNode statusFields = filterConfiguration.get(com.bmc.truesight.saas.jira.util.Constants.CONFIG_CONDSTATUSFIELDS_NODE_NAME);
                    if (statusFields != null) {
                        List<String> condList = null;
                        condList = obReader.readValue(statusFields);
                        configuration.setStatusConditionFields(condList);
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);

                }
                try {
                    JsonNode priorityFields = filterConfiguration.get(com.bmc.truesight.saas.jira.util.Constants.CONFIG_PRIORITY_CONDITION_FIELDS);
                    List<String> condList = null;
                    if (priorityFields != null) {
                        condList = obReader.readValue(priorityFields);
                    }
                    configuration.setPriorityConditionFields(condList);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            // Read the payload details and map to pojo
            try {
                JsonNode payloadNode = fieldNode.get(com.bmc.truesight.saas.jira.util.Constants.EVENTDEF_NODE_NAME);
                String payloadString = mapper.writeValueAsString(payloadNode);
                TSIEvent event = mapper.readValue(payloadString, TSIEvent.class);
                template.setEventDefinition(event);
            } catch (IOException e) {
                throw new ParsingException(StringUtil.format(com.bmc.truesight.saas.jira.util.Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{}));
            }

            // Iterate over the properties and if it starts with '@', put it to
            // itemValueMap
            Iterator<Map.Entry<String, JsonNode>> nodes = fieldNode.fields();
            Map<String, FieldItem> fieldItemMap = new HashMap<>();
            while (nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry<String, JsonNode>) nodes.next();
                if (entry.getKey().startsWith(com.bmc.truesight.saas.jira.util.Constants.PLACEHOLDER_START_TOKEN)) {
                    try {
                        String placeholderNode = mapper.writeValueAsString(entry.getValue());
                        FieldItem placeholderDefinition = mapper.readValue(placeholderNode, FieldItem.class);
                        fieldItemMap.put(entry.getKey(), placeholderDefinition);
                    } catch (IOException e) {
                        throw new ParsingException(StringUtil.format(com.bmc.truesight.saas.jira.util.Constants.PAYLOAD_PROPERTY_NOT_FOUND, new Object[]{entry.getKey()}));
                    }
                }
            }
            template.setFieldItemMap(fieldItemMap);
        }
        return template;
    }*/
    public static Template updateConfiguration(Template template, JiraPluginConfigurationItem config) {
        Configuration configuration = template.getConfig();
        configuration.setJiraHostName(config.getHostName());
        if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
            try {
                Integer port = Integer.parseInt(config.getPort());
                configuration.setJiraPort(port);
            } catch (NumberFormatException ex) {
                System.err.println("Port (" + config.getPort() + ") is not a valid port, using default port.");
            }
        }
        configuration.setJiraUserName(config.getUserName());
        configuration.setJiraPassword(config.getPassword());
        template.setConfig(configuration);
        return template;
    }

    public static JsonNode convertTOJsont(String[] configurationItem) {
        StringBuilder filedJson = new StringBuilder();
        JsonNode fieldNode = null;
        for (String filedVal : configurationItem) {
            filedJson.append(filedVal);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            fieldNode = objectMapper.readTree(filedJson.toString());
        } catch (IOException ex) {
            System.err.println("Exception occured while parsing json String {}" + ex.getMessage());

        }
        return fieldNode;
    }
}
