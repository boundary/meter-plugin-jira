package com.bmc.truesight.meter.plugin.jira;

import java.util.Arrays;

/**
 * @author Santosh Patil
 */
public class JiraPluginConfigurationItem {

    private String hostName;
    private String port;
    private String userName;
    private String password;
    private Long pollInterval;
    private String fields[];
    private String protocolType;
    private String app_id;

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }
    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public JiraPluginConfigurationItem() {
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getPollInterval() {
        return pollInterval;
    }

    @Override
    public String toString() {
        return "JiraPluginConfigurationItem{" + "hostName=" + hostName + ", port=" + port + ", userName=" + userName + ", password=" + password + ", pollInterval=" + pollInterval + ", fields=" + fields + ", protocolType=" + protocolType + ", app_id=" + app_id + ", source=" + source + '}';
    }

    public void setPollInterval(Long pollInterval) {
        this.pollInterval = pollInterval;
    }
}
