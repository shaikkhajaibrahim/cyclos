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
package nl.strohalm.cyclos.services.elements;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;
import nl.strohalm.cyclos.entities.sms.SmsLog;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.elements.exceptions.MemberWontReceiveNotificationException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for messages
 * @author luis
 */
public interface MessageService extends Service {

    /**
     * Loads a message
     */
    Message load(Long id, Relationship... fetch);

    /**
     * Perform a given action on multiple messages
     */
    @AdminAction(@Permission(module = "adminMemberMessages", operation = "manage"))
    @MemberAction(@Permission(module = "memberMessages", operation = "manage"))
    @OperatorAction(@Permission(module = "operatorMessages", operation = "manage"))
    @IgnoreMember
    void performAction(MessageAction action, Long... ids);

    /**
     * Removes all messages on trash after a time period defined in LocalSettings.deleteMessagesOnTrashAfter
     */
    void purgeExpiredMessagesOnTrash(Calendar time);

    /**
     * Loads a message, enforcing it is marked as read
     */
    Message read(Long id, Relationship... fetch);

    /**
     * Resumes sending unfinished mass messages
     */
    void resumeUnfinished();

    /**
     * Searches for messages
     */
    List<Message> search(MessageQuery query);

    /**
     * Sends a message from system to a group
     */
    @AdminAction(@Permission(module = "adminMemberMessages", operation = "sendToGroup"))
    @IgnoreMember
    Message sendFromAdminToGroup(SendMessageToGroupDTO message);

    /**
     * Sends a message from system to member
     * @throws MemberWontReceiveNotificationException When the member decided not to receive notifications from the administration
     */
    @AdminAction(@Permission(module = "adminMemberMessages", operation = "sendToMember"))
    @PathToMember("toMember")
    Message sendFromAdminToMember(SendMessageToMemberDTO message) throws MemberWontReceiveNotificationException;

    /**
     * Sends a message from system to a group
     */
    @BrokerAction(@Permission(module = "brokerMessages", operation = "sendToMembers"))
    @IgnoreMember
    Message sendFromBrokerToMembers(SendMessageFromBrokerToMembersDTO message);

    /**
     * Sends a message to system
     */
    @MemberAction(@Permission(module = "memberMessages", operation = "sendToAdministration"))
    @OperatorAction(@Permission(module = "operatorMessages", operation = "sendToAdministration"))
    @IgnoreMember
    Message sendFromMemberToAdmin(SendMessageToAdminDTO message);

    /**
     * Sends a message from member to member
     * @throws MemberWontReceiveNotificationException When the member decided not to receive notifications from other members
     */
    @MemberAction(@Permission(module = "memberMessages", operation = "sendToMember"))
    @OperatorAction(@Permission(module = "operatorMessages", operation = "sendToMember"))
    @IgnoreMember
    Message sendFromMemberToMember(SendMessageToMemberDTO message) throws MemberWontReceiveNotificationException;

    /**
     * Sends the given message from a system internal procedure
     */
    void sendFromSystem(SendMessageFromSystemDTO message);

    /**
     * Sends an SMS to a member
     */
    SmsLog sendSms(SendSmsDTO params);

    /**
     * Sends an SMS to a member in background
     */
    void sendSmsAfterTransactionCommit(SendSmsDTO params);

    /**
     * Validates the given message
     */
    void validate(SendMessageDTO message) throws ValidationException;
}
