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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.members.AdminNotificationPreferenceDAO;
import nl.strohalm.cyclos.dao.members.NotificationPreferenceDAO;
import nl.strohalm.cyclos.dao.members.brokerings.DefaultBrokerCommissionDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.brokerings.DefaultBrokerCommission;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.members.preferences.AdminNotificationPreference;
import nl.strohalm.cyclos.entities.members.preferences.AdminNotificationPreferenceQuery;
import nl.strohalm.cyclos.entities.members.preferences.NotificationPreference;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.sms.ISmsContext;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;

/**
 * Service implementation for broker default commissions and notification preferences
 * @author jeancarlo
 * @author Jefferson Magno
 */
public class PreferenceServiceImpl implements PreferenceService {

    private AdminNotificationPreferenceDAO adminNotificationPreferenceDao;
    private DefaultBrokerCommissionDAO     defaultBrokerCommissionDao;
    private FetchService                   fetchService;
    private NotificationPreferenceDAO      notificationPreferenceDao;
    private MemberService                  memberService;
    private ChannelService                 channelService;
    private GroupService                   groupService;

    public List<Administrator> listAdminsForNotification(final AdminNotificationPreferenceQuery query) {
        return adminNotificationPreferenceDao.searchAdmins(query);
    }

    public AdminNotificationPreference load(final Administrator admin, final Relationship... fetch) {
        if (!admin.equals(LoggedUser.element())) {
            throw new PermissionDeniedException();
        }

        return adminNotificationPreferenceDao.load(admin, fetch);
    }

    public Collection<NotificationPreference> load(final Member member) {
        return notificationPreferenceDao.load(member);
    }

    public List<DefaultBrokerCommission> load(final Member broker, final Relationship... fetch) {
        return defaultBrokerCommissionDao.load(broker, fetch);
    }

    public NotificationPreference load(final Member member, final Type type) {
        final Collection<NotificationPreference> preferences = load(member);
        for (final NotificationPreference preference : preferences) {
            if (preference.getType() == type) {
                return preference;
            }
        }
        return null;
    }

    public Set<MessageChannel> receivedChannels(final Member member, final Type type) {
        try {
            final NotificationPreference preference = notificationPreferenceDao.load(member, type);
            final EnumSet<MessageChannel> channels = EnumSet.noneOf(MessageChannel.class);
            if (preference.isEmail()) {
                channels.add(MessageChannel.EMAIL);
            }
            if (preference.isMessage()) {
                channels.add(MessageChannel.MESSAGE);
            }
            if (preference.isSms()) {
                channels.add(MessageChannel.SMS);
            }
            return channels;
        } catch (final EntityNotFoundException e) {
            return Collections.emptySet();
        }
    }

    public boolean receivesMessage(final Member member, final Message.Type type) {
        try {
            final NotificationPreference preference = notificationPreferenceDao.load(member, type);
            return preference.isEmail() || preference.isMessage() || preference.isSms();
        } catch (final EntityNotFoundException e) {
            return false;
        }
    }

    public AdminNotificationPreference save(final AdminNotificationPreference preference) {
        final Administrator loggedAdmin = LoggedUser.element();
        if (preference.getAdmin() != null && !preference.getAdmin().equals(loggedAdmin)) {
            throw new PermissionDeniedException();
        }

        preference.setAdmin(loggedAdmin);
        try {
            final AdminNotificationPreference current = load(loggedAdmin);
            preference.setId(current.getId());
            return adminNotificationPreferenceDao.update(preference);
        } catch (final EntityNotFoundException e) {
            return adminNotificationPreferenceDao.insert(preference);
        }
    }

    public void save(Member member, final Collection<NotificationPreference> prefs) {
        member = fetchService.fetch(member, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.SMS_MESSAGES));
        final Collection<Type> smsMessages = member.getMemberGroup().getSmsMessages();
        for (final NotificationPreference preference : prefs) {
            // If SMS is not enabled for this message type (group setting), disable it
            if (preference.isSms() && !smsMessages.contains(preference.getType())) {
                preference.setSms(false);
            }
            preference.setMember(member);
            save(preference);
        }
    }

    public MemberSmsStatus saveSmsStatusPreferences(Member member, final boolean isAcceptFreeMailing, final boolean isAcceptPaidMailing, final boolean isAllowChargingSms, final boolean hasNotificationsBySms) {
        final MemberSmsStatus memberSmsStatus = memberService.getSmsStatus(member);
        memberSmsStatus.setAcceptFreeMailing(isAcceptFreeMailing);
        memberSmsStatus.setAcceptPaidMailing(isAcceptPaidMailing);
        memberSmsStatus.setAllowChargingSms(isAllowChargingSms);

        // Check if we must set the allowChargingSms flag
        final ISmsContext smsContext = groupService.getSmsContext(member.getMemberGroup());
        final int smsFree = smsContext.getFreeSms(member);
        final int smsAdditionalCharged = smsContext.getAdditionalChargedSms(member);
        if (smsAdditionalCharged == 1 && smsFree == 0) {
            member = fetchService.fetch(member, Member.Relationships.CHANNELS);
            final boolean smsChannelEnabled = member.getChannels().contains(channelService.getSmsChannel());
            final boolean allowChargingSms = smsChannelEnabled || isAcceptPaidMailing || hasNotificationsBySms;
            memberSmsStatus.setAllowChargingSms(allowChargingSms);
        }

        return memberService.updateSmsStatus(memberSmsStatus);
    }

    public void setAdminNotificationPreferenceDao(final AdminNotificationPreferenceDAO adminNotificationPreferenceDao) {
        this.adminNotificationPreferenceDao = adminNotificationPreferenceDao;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setDefaultBrokerCommissionDao(final DefaultBrokerCommissionDAO brokerPreferenceDao) {
        defaultBrokerCommissionDao = brokerPreferenceDao;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    public void setNotificationPreferenceDao(final NotificationPreferenceDAO notificationPreferenceDAO) {
        notificationPreferenceDao = notificationPreferenceDAO;
    }

    private NotificationPreference save(NotificationPreference notificationPreference) {
        if (notificationPreference.isTransient()) {
            notificationPreference = notificationPreferenceDao.insert(notificationPreference);
        } else {
            notificationPreference = notificationPreferenceDao.update(notificationPreference);
        }
        return notificationPreference;
    }

}