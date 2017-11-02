package com.bmc.truesight.meter.plugin.jira;

import com.bmc.truesight.meter.plugin.jira.util.PluginConstants;
import com.bmc.truesight.meter.plugin.jira.util.JiraTemplateValidator;
import com.bmc.truesight.meter.plugin.jira.util.Utils;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.JiraApiInstantiationFailedException;
import com.bmc.truesight.saas.jira.exception.JiraLoginFailedException;
import com.bmc.truesight.saas.jira.exception.ParsingException;
import com.bmc.truesight.saas.jira.exception.ValidationException;
import com.bmc.truesight.saas.jira.impl.GenericTemplateParser;
import com.bmc.truesight.saas.jira.impl.GenericTemplatePreParser;
import com.bmc.truesight.saas.jira.in.TemplateParser;
import com.bmc.truesight.saas.jira.in.TemplatePreParser;
import com.bmc.truesight.saas.jira.in.TemplateValidator;
import com.boundary.plugin.sdk.CollectorDispatcher;
import com.boundary.plugin.sdk.Event;
import com.boundary.plugin.sdk.EventSink;
import com.boundary.plugin.sdk.EventSinkAPI;
import com.boundary.plugin.sdk.EventSinkStandardOutput;
import com.boundary.plugin.sdk.MeasurementSink;
import com.boundary.plugin.sdk.Plugin;
import com.boundary.plugin.sdk.PluginRunner;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Santosh Patil
 * @Date 02-08-2017
 */
public class JiraPlugin implements Plugin<JiraPluginConfiguration> {

    JiraPluginConfiguration configuration;
    CollectorDispatcher dispatcher;
    EventSink eventOutput;
    EventSinkAPI eventSinkAPI;
    private static final Logger LOG = LoggerFactory.getLogger(JiraPlugin.class);

    public static void main(String[] args) {
        PluginRunner plugin = new PluginRunner("com.bmc.truesight.meter.plugin.jira.JiraPlugin");
        plugin.run();
    }

    @Override
    public void setMeasureOutput(MeasurementSink ms) {
    }

    @Override
    public void setEventOutput(final EventSink output) {
        this.eventOutput = output;
    }

    @Override
    public void setConfiguration(JiraPluginConfiguration configuration) {
        this.configuration = configuration;
        this.eventOutput = new EventSinkStandardOutput();
        this.eventSinkAPI = new EventSinkAPI();
    }

    @Override
    public void loadConfiguration() {
        Gson gson = new Gson();
        String param = System.getenv(PluginConstants.TSP_PLUGIN_PARAMS);
        LOG.debug("System environment has parameter available as  ,{}",param);
        try {
            JiraPluginConfiguration pluginConfiguration = null;
        	if(param == null || param == ""){
        		pluginConfiguration = gson.fromJson(new FileReader("param.json"), JiraPluginConfiguration.class);
        	}else{
        		pluginConfiguration = gson.fromJson(param, JiraPluginConfiguration.class);
        	}
            setConfiguration(pluginConfiguration);
        } catch (JsonParseException e) {
            System.err.println("Exception occured while getting the param.json data" + e.getMessage());
            eventOutput.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
        } catch (IOException e) {
            System.err.println("IOException occured while getting the param.json data" + e.getMessage());
            eventOutput.emit(Utils.eventMeterTSI(PluginConstants.JIRA_PLUGIN_TITLE_MSG, e.getMessage(), Event.EventSeverity.ERROR.toString()));
        }
    }

    @Override
    public void setDispatcher(CollectorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        ArrayList<JiraPluginConfigurationItem> items = configuration.getItems();
        if (items != null && items.size() > 0) {
            for (JiraPluginConfigurationItem config : items) {
                boolean isTemplateParsingSuccessful = false;
                boolean isTemplateValidationSuccessful = false;
                TemplateParser templateParser = new GenericTemplateParser();
                TemplatePreParser templatePreParser = new GenericTemplatePreParser();
                Template template = null;
                try {
                    Template defaultTemplate = new Template();
                    defaultTemplate = templatePreParser.loadDefaults();
                    template = templateParser.readParseConfigJson(defaultTemplate, Utils.getFieldValues(config.getFields()));
                    isTemplateParsingSuccessful = true;
                } catch (ParsingException ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                } catch (Exception ex) {
                    System.err.println("Parsing failed - " + ex.getMessage());
                }
                if (isTemplateParsingSuccessful) {
                    TemplateValidator templateValidator = new JiraTemplateValidator();
                    try {
                        templateValidator.validate(template);
                        isTemplateValidationSuccessful = true;
                    } catch (ValidationException ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    } catch (Exception ex) {
                        System.err.println("Validation failed - " + ex.getMessage());
                    }
                } else {
                    System.exit(1);
                }
                if (isTemplateValidationSuccessful) {
                    try {
                        dispatcher.addCollector(new JiraTicketsCollector(config, template));
                    } catch (ParsingException ex) {
                        System.err.println("Parsing failed -" + ex.getMessage());
                    } catch (JiraApiInstantiationFailedException ex) {
                        System.err.println("Jira Api instantiation failed exception -" + ex.getMessage());
                    } catch (JiraLoginFailedException ex) {
                        System.err.println(ex.getMessage());
                    }

                } else {
                    System.exit(1);
                }
            }
            dispatcher.run();
        }
    }
}
