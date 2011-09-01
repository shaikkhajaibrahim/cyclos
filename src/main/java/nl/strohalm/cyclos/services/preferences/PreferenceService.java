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

package nl.strohalm.cyclos.services.preferences;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.preferences.AdminNotificationPreference;
import nl.strohalm.cyclos.entities.members.preferences.AdminNotificationPreferenceQuery;
import nl.strohalm.cyclos.entities.members.preferences.NotificationPreference;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service Interface that handles the broker preferences and notification preferences
 */
public interface PreferenceService extends Service {

    @MemberAction(@Permission(module = "memberPreferences", operation = "manageNotifications"))
    @AdminAction(@Permission(module = "adminMemberPreferences", operation = "manageNotifications"))
    @BrokerAction(@Permission(module = "brokerPreferences", operation = "manageNotifications"))
    @PathToMember("")
    public MemberSmsStatus saveSmsStatusPreferences(final Member member, final boolean isAcceptFreeMailing, final boolean isAcceptPaidMailing, final boolean isAllowChargingSms, boolean hasNotificationsBySms);

    /**
     * Lists admins according to the given parameters
     */
    @SystemAction
    List<Administrator> listAdminsForNotification(AdminNotificationPreferenceQuery query);

    /**
     * Loads the notification preferences for the given administrator
     */
    @AdminAction
    @IgnoreMember
    AdminNotificationPreference load(Administrator admin, Relationship... fetch);

    /**
     * Loads a collection of Notification Preferences for a given member
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageNotifications"))
    @AdminAction(@Permission(module = "adminMemberPreferences", operation = "manageNotifications"))
    @BrokerAction(@Permission(module = "brokerPreferences", operation = "manageNotifications"))
    @PathToMember("")
    Collection<NotificationPreference> load(Member member);

    /**
     * Loads a specific notification preference
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageNotifications"))
    @AdminAction(@Permission(module = "adminMemberPreferences", operation = "manageNotifications"))
    @BrokerAction(@Permission(module = "brokerPreferences", operation = "manageNotifications"))
    @PathToMember("")
    NotificationPreference load(Member member, Message.Type type);

    /**
     * Returns the channels a member would receive a notification
     */
    @SystemAction
    Set<MessageChannel> receivedChannels(Member member, Message.Type type);

    /**
     * Checks whether the given member has chosen to be notified on messages of the given type (no matter which notification kind - internal message /
     * mail / sms)
     */
    @SystemAction
    boolean receivesMessage(Member member, Message.Type type);

    /**
     * Saves the notification preferences, returning the updated instance
     */
    @AdminAction
    @IgnoreMember
    AdminNotificationPreference save(AdminNotificationPreference preference);

    /**
     * Save a collection of notification preferences for a given member
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageNotifications"))
    @AdminAction(@Permission(module = "adminMemberPreferences", operation = "manageNotifications"))
    @BrokerAction(@Permission(module = "brokerPreferences", operation = "manageNotifications"))
    @PathToMember("")
    void save(Member member, Collection<NotificationPreference> prefs);

}