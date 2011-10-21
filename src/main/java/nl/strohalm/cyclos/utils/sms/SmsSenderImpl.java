/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.utils.sms;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.dao.members.MessageDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.alerts.ErrorLogService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.elements.SendMessageFromSystemDTO;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.SpringHelper;
import mp.platform.cyclone.webservices.external.ExternalWebServiceHelper;
import mp.platform.cyclone.webservices.external.sms.SmsSenderWebService;
import mp.platform.cyclone.webservices.model.MemberVO;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * Send SMS using a Web Service, whose URL is set in the local settings
 *
 * @author Jefferson Magno
 */
public class SmsSenderImpl implements SmsSender, LocalSettingsChangeListener, ServletContextAware {

    private static class VoidSender implements SmsSenderWebService {
        public boolean send(final String cyclosId, final MemberVO member, final String text) {
            return false;
        }

        @Override
        public boolean sendToNonMember(String cyclosId, String mobileNumber, String text) {
            return false;
        }
    }

    private boolean initialized;
    private ServletContext servletContext;
    private SmsSenderWebService smsWebService;

    private ErrorLogService errorLogService;
    private CustomFieldService customFieldService;
    private FetchService fetchService;
    private MemberHelper memberHelper;
    private SettingsService settingsService;
    private MessageService messageService;

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        smsWebService = null;
    }

    @SuppressWarnings("unchecked")
    public boolean send(Member member, String text) {
        maybeInitialize();
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String cyclosId = localSettings.getCyclosId();
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        final List<MemberCustomField> memberCustomFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        List<MemberCustomField> customFields = CustomFieldHelper.onlyForGroup(memberCustomFields, member.getMemberGroup());
        customFields = CustomFieldHelper.onlyBasic(memberCustomFields);
        final MemberVO memberVO = memberHelper.toVO(member, customFields, false);

        final MessageSettings messageSettings = settingsService.getMessageSettings();
        String prefix = messageSettings.getSmsMessagePrefix();
        final Map<String, String> variables = new HashMap<String, String>();
        variables.put("system_name", localSettings.getApplicationName());
        prefix = StringUtils.trimToEmpty(MessageProcessingHelper.processVariables(prefix, variables));

        text = StringUtils.trimToEmpty(prefix + " " + text);
        try {
            return getSmsWebService().send(cyclosId, memberVO, text);
        } catch (final Exception e) {
            final Map<String, String> params = new HashMap<String, String>();
            params.put("username", member.getUsername());
            params.put("name", member.getName());
            params.put("sms", text);
            errorLogService.insert(e, settingsService.getLocalSettings().getSendSmsWebServiceUrl(), params);
            return false;
        }
    }

    @Override
    public boolean send(String mobileNumber, String text, Member fallbackMember, String fallbackSubject, String fallbackText, Entity obj) {
        boolean success = send(mobileNumber, text);
        if (!success) {
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            final LocalSettings localSettings = settingsService.getLocalSettings();

            final String processedSubject = MessageProcessingHelper.processVariables(fallbackSubject, obj, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(fallbackText, obj, localSettings);
    
            message.setType(Message.Type.SMS_FAILED);
            //message.setEntity(obj);
            message.setToMember(fallbackMember);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            messageService.sendFromSystem(message);
        }
        return success;
    }

    @Override
    public boolean send(String mobileNumber, String text) {
        maybeInitialize();
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String cyclosId = localSettings.getCyclosId();

        final MessageSettings messageSettings = settingsService.getMessageSettings();
        String prefix = messageSettings.getSmsMessagePrefix();
        final Map<String, String> variables = new HashMap<String, String>();
        variables.put("system_name", localSettings.getApplicationName());
        prefix = StringUtils.trimToEmpty(MessageProcessingHelper.processVariables(prefix, variables));

        text = StringUtils.trimToEmpty(prefix + " " + text);
        try {
            return getSmsWebService().sendToNonMember(cyclosId, mobileNumber, text);
        } catch (final Exception e) {
            final Map<String, String> params = new HashMap<String, String>();
            params.put("mobileNumber", mobileNumber);
            params.put("sms", text);
            errorLogService.insert(e, settingsService.getLocalSettings().getSendSmsWebServiceUrl(), params);
            return false;
        }
    }

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private SmsSenderWebService getSmsWebService() throws IOException {
        if (smsWebService == null) {
            final String url = settingsService.getLocalSettings().getSendSmsWebServiceUrl();
            if (StringUtils.isEmpty(url)) {
                smsWebService = new VoidSender();
            } else {
                // Create the proxy
                smsWebService = ExternalWebServiceHelper.proxyFor(SmsSenderWebService.class, url);
            }
        }
        return smsWebService;
    }

    private synchronized void maybeInitialize() {
        // As the standard setter injection was causing problems with other beans, for recursive injection,
        // which caused unproxied instances to be injected, the setter injection is no longer used here
        if (!initialized) {
            settingsService = SpringHelper.bean(servletContext, "settingsService");
            settingsService.addListener(this);
            customFieldService = SpringHelper.bean(servletContext, "customFieldService");
            fetchService = SpringHelper.bean(servletContext, "fetchService");
            memberHelper = SpringHelper.bean(servletContext, "memberHelper");
            errorLogService = SpringHelper.bean(servletContext, "errorLogService");
            messageService = SpringHelper.bean(servletContext, "messageService");
            initialized = true;
        }
    }

}
