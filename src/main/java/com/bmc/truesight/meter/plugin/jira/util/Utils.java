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
import com.bmc.truesight.saas.jira.beans.TSIEvent;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.JiraApiInstantiationFailedException;
import com.bmc.truesight.saas.jira.exception.ParsingException;
import com.bmc.truesight.saas.jira.impl.GenericTemplateParser;
import com.bmc.truesight.saas.jira.util.Constants;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Santosh Patil
 *
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    static String JQL_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm";

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

    public static Template updateConfiguration(Template template, JiraPluginConfigurationItem config) throws ParsingException, JiraApiInstantiationFailedException {
        Configuration configuration = template.getConfig();
        configuration.setJiraHostName(config.getHostName());
        GenericTemplateParser parser = new GenericTemplateParser();
        if (config.getPort() != null && !config.getPort().trim().isEmpty()) {
            try {
                configuration.setJiraPort(config.getPort());
            } catch (NumberFormatException ex) {
                LOG.error("Port (" + config.getPort() + ") is not a valid port, using default port.");
            }
        }
        configuration.setJiraUserName(config.getUserName());
        configuration.setJiraPassword(config.getPassword());
        configuration.setProtocolType(config.getProtocolType());
        template.setConfig(configuration);
        if (!config.getApp_id().isEmpty() && config.getApp_id() != null && template.getEventDefinition().getProperties() != null && template.getEventDefinition().getProperties().size() > 0) {
            Map<String, String> defPropertyMap = template.getEventDefinition().getProperties();
            TSIEvent event = template.getEventDefinition();
            Map<String, String> propertyMap = template.getEventDefinition().getProperties();
            template.getEventDefinition().getProperties().keySet().forEach(key -> {
                if (Constants.APPLICATION_ID.equalsIgnoreCase(key)) {
                    defPropertyMap.put(key, config.getApp_id());
                } else {
                    defPropertyMap.put(key, propertyMap.get(key));
                }
            });
            event.setProperties(defPropertyMap);
            template.setEventDefinition(event);
        }
        template = parser.ignoreFields(template);
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
            LOG.error("Exception occured while parsing json String {}" + ex.getMessage());

        }
        return fieldNode;
    }

    public static String getJQLTimeFormat(long timestamp, String serverTimezone) {

        ZonedDateTime utcTime = ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);

        ZonedDateTime serverDateTime = utcTime
                .withZoneSameInstant(ZoneId.of(serverTimezone));

        return DateTimeFormatter.ofPattern(JQL_TIMESTAMP_FORMAT)
                .format(serverDateTime);

    }
}
