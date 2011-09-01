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
package nl.strohalm.cyclos.services.sms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.sms.SmsMailingDAO;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.sms.SmsMailing;
import nl.strohalm.cyclos.entities.sms.SmsMailingQuery;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.elements.SendSmsDTO;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.CurrentTransactionData.TransactionCommitListener;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service implementation for sms mailings
 * 
 * @author luis
 */
public class SmsMailingServiceImpl implements SmsMailingService, DisposableBean {

    private static final Log    LOG           = LogFactory.getLog(SmsMailingServiceImpl.class);

    private SmsMailingDAO       smsMailingDao;
    private FetchService        fetchService;
    private GroupService        groupService;
    private TransactionTemplate transactionTemplate;
    private MessageService      messageService;
    private SettingsService     settingsService;
    private MemberService       memberService;
    private Map<Long, Thread>   activeThreads = Collections.synchronizedMap(new HashMap<Long, Thread>());

    public void destroy() throws Exception {
        for (final Thread thread : activeThreads.values()) {
            try {
                thread.interrupt();
            } catch (final Exception e) {
                // Ignore
            }
        }
        activeThreads.clear();
    }

    public void resumeUnfinished() {
        final SmsMailingQuery query = new SmsMailingQuery();
        query.setFinished(false);
        final List<SmsMailing> unfinishedMailings = search(query);
        for (final SmsMailing mailing : unfinishedMailings) {
            sendSmsMessages(mailing.getId());
        }
    }

    public List<SmsMailing> search(final SmsMailingQuery query) {
        if (LoggedUser.isValid()) {
            final Element loggedElement = LoggedUser.element();
            if (loggedElement instanceof Member) {
                // Ensure that brokers only see mailings sent by himself
                query.setBroker((Member) loggedElement);
            } else {
                // Ensure admins will only see groups he can manage
                final AdminGroup adminGroup = fetchService.fetch((AdminGroup) LoggedUser.group(), AdminGroup.Relationships.MANAGES_GROUPS);
                final Collection<MemberGroup> groups = query.getGroups();
                if (CollectionUtils.isEmpty(groups)) {
                    query.setGroups(adminGroup.getManagesGroups());
                } else {
                    groups.retainAll(adminGroup.getManagesGroups());
                }
            }
        }
        return smsMailingDao.search(query);
    }

    public SmsMailing sendFreeToGroups(final SmsMailing smsMailing) {
        smsMailing.setFree(true);
        return doSend(smsMailing, false);
    }

    public SmsMailing sendPaidToGroups(final SmsMailing smsMailing) {
        smsMailing.setFree(false);
        return doSend(smsMailing, false);
    }

    public SmsMailing sendToMember(final SmsMailing smsMailing) {
        smsMailing.setFree(true);
        return doSend(smsMailing, true);
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

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSmsMailingDao(final SmsMailingDAO smsMailingDao) {
        this.smsMailingDao = smsMailingDao;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void validate(final SmsMailing smsMailing, final boolean isMemberRequired) throws ValidationException {
        getValidator(isMemberRequired).validate(smsMailing);
    }

    @SuppressWarnings("unchecked")
    private SmsMailing doSend(SmsMailing smsMailing, final boolean isMemberRequired) {
        validate(smsMailing, isMemberRequired);
        smsMailing.setBy(LoggedUser.element());
        smsMailing.setDate(Calendar.getInstance());
        smsMailing.setSentSms(0);

        // For admins, ensure the groups are correctly set
        boolean hasSomethingToSend = true;
        if (!smsMailing.isSingleMember() && LoggedUser.isAdministrator()) {
            final GroupQuery groupQuery = new GroupQuery();
            groupQuery.setManagedBy((AdminGroup) LoggedUser.group());
            groupQuery.setOnlyActive(true);
            final Collection<MemberGroup> managedGroups = (Collection<MemberGroup>) groupService.search(groupQuery);
            Collection<MemberGroup> groups = smsMailing.getGroups();
            if (CollectionUtils.isEmpty(managedGroups)) {
                // No managed group - ensure no group is selected
                groups = new ArrayList<MemberGroup>();
                hasSomethingToSend = false;
            } else if (CollectionUtils.isEmpty(groups)) {
                // No groups means send to all managed groups
                groups = new ArrayList<MemberGroup>(managedGroups);
            } else {
                // Ensure only managed groups will be used
                groups.retainAll(managedGroups);
            }
            // Ensure no removed groups are kept
            for (final Iterator<MemberGroup> iterator = groups.iterator(); iterator.hasNext();) {
                final MemberGroup current = iterator.next();
                if (current.getStatus() != Group.Status.NORMAL) {
                    iterator.remove();
                }
            }
            smsMailing.setGroups(groups);
        }

        smsMailing = smsMailingDao.insert(smsMailing);

        // Send each SMS
        if (hasSomethingToSend) {
            final MemberCustomField smsCustomField = settingsService.getSmsCustomField();
            if (smsCustomField == null) {
                throw new IllegalStateException("No custom field was set as SMS field under local settings");
            }
            smsMailingDao.assignUsersToSend(smsMailing, smsCustomField);
            // Ensure that after committing the transaction, the sms messages will be sent
            final Long id = smsMailing.getId();
            CurrentTransactionData.addTransactionCommitListener(new TransactionCommitListener() {
                public void onTransactionCommit() {
                    sendSmsMessages(id);
                }
            });
        }

        return smsMailing;
    }

    private Validator getValidator(final boolean isMemberRequired) {
        final Validator validator = new Validator("smsMailing");
        validator.property("text").required().maxLength(160);
        if (isMemberRequired) {
            validator.property("member").required().add(new PropertyValidation() {
                private static final long serialVersionUID = -20792899778722444L;

                public ValidationError validate(final Object object, final Object property, final Object value) {
                    // Ensure the member has a mobile phone set
                    final Member member = (Member) value;
                    if (member == null) {
                        return null;
                    }

                    final MemberCustomField smsCustomField = settingsService.getSmsCustomField();
                    if (memberService.hasValueForField(member, smsCustomField)) {
                        return null;
                    }

                    return new ValidationError("smsMailing.error.noMobilePhone");
                }
            });
        }

        return validator;
    }

    private boolean sendPendingInTransaction(final Long smsMailingId) {
        final SmsMailing smsMailing = smsMailingDao.load(smsMailingId);
        final Member member = smsMailingDao.getNextMemberToSend(smsMailing);
        if (member == null) {
            // No more to send
            smsMailing.setFinished(true);
            return false;
        }

        // Send the SMS mailing
        final SendSmsDTO sms = new SendSmsDTO();
        sms.setTargetMember(member);
        if (!smsMailing.isFree()) {
            sms.setChargedMember(member);
        }
        sms.setSmsMailing(smsMailing);
        sms.setText(smsMailing.getText());
        messageService.sendSms(sms);

        smsMailingDao.removeMemberFromPending(smsMailing, member);
        return true;
    }

    private synchronized void sendSmsMessages(final Long smsMailingId) {
        if (activeThreads.containsKey(smsMailingId)) {
            return;
        }
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        // Send one single message in another transaction
                        final boolean result = transactionTemplate.execute(new TransactionCallback<Boolean>() {
                            public Boolean doInTransaction(final TransactionStatus status) {
                                return sendPendingInTransaction(smsMailingId);
                            }
                        });
                        // If no more messages, remove the thread
                        if (!result) {
                            activeThreads.remove(smsMailingId);
                            return;
                        }
                    }
                } catch (final Exception e) {
                    LOG.error("Error while sending the SMS mailing with id = " + smsMailingId, e);
                }
            }
        };
        thread.setName("Sending SMS mailing for mailing id " + smsMailingId);
        activeThreads.put(smsMailingId, thread);
        thread.start();
    }
}
