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
package nl.strohalm.cyclos.aop;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.elements.SendMessageFromSystemDTO;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;

public class MessageHelper {


    private SettingsService settingsService;
    private MessageService messageService;

    public MessageHelper(SettingsService settingsService, MessageService messageService) {
        this.settingsService = settingsService;
        this.messageService = messageService;
    }

    void sendMemberMessage(String subject, String body, String sms, Member member, Message.Type type, Entity... entities) {
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        final LocalSettings localSettings = settingsService.getLocalSettings();

        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, member, entities);
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, member, entities);
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, member, entities);

        message.setType(type);
        message.setEntity(member);
        message.setToMember(member);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);
        messageService.sendFromSystem(message);
    }

}
