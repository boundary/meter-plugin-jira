package com.bmc.truesight.meter.plugin.jira.util;

import com.bmc.truesight.saas.jira.beans.Configuration;
import com.bmc.truesight.saas.jira.beans.EventSource;
import com.bmc.truesight.saas.jira.beans.FieldItem;
import com.bmc.truesight.saas.jira.beans.TSIEvent;
import com.bmc.truesight.saas.jira.beans.Template;
import com.bmc.truesight.saas.jira.exception.ValidationException;
import com.bmc.truesight.saas.jira.in.TemplateValidator;
import com.bmc.truesight.saas.jira.util.Constants;
import com.bmc.truesight.saas.jira.util.StringUtil;
import java.util.Map;

/**
 *
 * @author vitiwari
 */
public class JiraTemplateValidator implements TemplateValidator {

    @Override
    public boolean validate(Template template) throws ValidationException {
        Configuration config = template.getConfig();
        TSIEvent payload = template.getEventDefinition();
        Map<String, FieldItem> fieldItemMap = template.getFieldItemMap();

        /*if (template.getFilter().isEmpty()) {
            throw new ValidationException(StringUtil.format(Constants.CONFIG_VALIDATION_FAILED, new Object[]{}));
        }*/
       
        // validate payload configuration
        if (payload.getTitle() != null && payload.getTitle().startsWith("@") && !fieldItemMap.containsKey(payload.getTitle())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING,
                    new Object[]{payload.getTitle()}));
        }

        // validate payload configuration
        for (String fpField : payload.getFingerprintFields()) {
            if (fpField != null && fpField.startsWith("@") && !fieldItemMap.containsKey(fpField)) {
                throw new ValidationException(
                        StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{fpField}));
            }
        }

        // validate payload configuration
        Map<String, String> properties = payload.getProperties();
        for (String key : properties.keySet()) {
            if (!StringUtil.isValidJavaIdentifier(key)) {
                throw new ValidationException(StringUtil.format(Constants.PROPERTY_NAME_INVALID, new Object[]{key.trim()}));
            }
            if (properties.get(key).startsWith("@") && !fieldItemMap.containsKey(properties.get(key))) {
                throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{properties.get(key)}));
            }
        }

        if (payload.getSeverity() != null && payload.getSeverity().startsWith("@") && !fieldItemMap.containsKey(payload.getSeverity())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getSeverity()}));
        }

        if (payload.getStatus() != null && payload.getStatus().startsWith("@") && !fieldItemMap.containsKey(payload.getStatus())) {
            throw new ValidationException(
                    StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getStatus()}));
        }

        if (payload.getCreatedAt() != null && payload.getCreatedAt().startsWith("@") && !fieldItemMap.containsKey(payload.getCreatedAt())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getCreatedAt()}));
        }

        if (payload.getEventClass() != null && payload.getEventClass().startsWith("@") && !fieldItemMap.containsKey(payload.getEventClass())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{payload.getEventClass()}));
        }

        //valiadting source
        EventSource source = payload.getSource();
        if (source.getName() != null && source.getName().startsWith("@") && !fieldItemMap.containsKey(source.getName())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getName()}));
        }
        if (source.getType() != null && source.getType().startsWith("@") && !fieldItemMap.containsKey(source.getType())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getType()}));
        }
        if (source.getRef() != null && source.getRef().startsWith("@") && !fieldItemMap.containsKey(source.getRef())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{source.getRef()}));
        }

        EventSource sender = payload.getSender();
        if (sender.getName() != null && sender.getName().startsWith("@") && !fieldItemMap.containsKey(sender.getName())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getName()}));
        }
        if (sender.getType() != null && sender.getType().startsWith("@") && !fieldItemMap.containsKey(sender.getType())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getType()}));
        }
        if (sender.getRef() != null && sender.getRef().startsWith("@") && !fieldItemMap.containsKey(sender.getRef())) {
            throw new ValidationException(StringUtil.format(Constants.PAYLOAD_PLACEHOLDER_DEFINITION_MISSING, new Object[]{sender.getRef()}));
        }
        return true;
    }

}
