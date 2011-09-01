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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import nl.strohalm.cyclos.dao.members.MessageDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;
import nl.strohalm.cyclos.entities.members.messages.Message.Direction;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.entities.sms.SmsLog;
import nl.strohalm.cyclos.entities.sms.SmsLog.ErrorType;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.elements.exceptions.MemberWontReceiveNotificationException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.preferences.MessageChannel;
import nl.strohalm.cyclos.services.preferences.PreferenceService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.sms.ISmsContext;
import nl.strohalm.cyclos.services.sms.SmsLogService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.LinkGenerator;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.WorkerThreads;
import nl.strohalm.cyclos.utils.CurrentTransactionData.TransactionCommitListener;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.sms.SmsSender;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation for message service
 * @author luis
 */
public class MessageServiceImpl implements MessageService, DisposableBean {

    public static class RequiredWhenFromAdminValidation implements PropertyValidation {
        private static final long serialVersionUID = -3593591846871843393L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            if (LoggedUser.isValid() && LoggedUser.isAdministrator()) {
                return RequiredValidation.instance().validate(object, property, value);
            }
            return null;
        }
    }

    /**
     * Ensure the message receiver is not the same logged user
     * @author luis
     */
    public static class SameFromAndToValidation implements PropertyValidation {

        private static final long serialVersionUID = -3649308826565152361L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final SendMessageToMemberDTO dto = (SendMessageToMemberDTO) object;
            final Member loggedMember = (Member) (LoggedUser.isValid() && LoggedUser.isMember() ? LoggedUser.element() : null);
            final Member toMember = dto.getToMember();
            if (loggedMember != null && loggedMember.equals(toMember)) {
                return new InvalidError();
            }
            return null;
        }
    }

    /**
     * A thread used to generate an sms log in a separate transaction
     * 
     * @author luis
     */
    private class CreateSmsLogThread extends Thread {
        private SendSmsDTO params;
        private ErrorType  errorType;
        private boolean    freeBaseUsed;
        private SmsLog     log;

        private CreateSmsLogThread(final SendSmsDTO params, final ErrorType errorType, final boolean freeBaseUsed) {
            this.params = params;
            this.errorType = errorType;
            this.freeBaseUsed = freeBaseUsed;
        }

        public SmsLog getLog() {
            return log;
        }

        @Override
        public void run() {
            final SmsLog newLog = new SmsLog();
            try {
                // Execute the insert in a new transaction
                log = transactionTemplate.execute(new TransactionCallback<SmsLog>() {
                    public SmsLog doInTransaction(final TransactionStatus status) {
                        newLog.setDate(Calendar.getInstance());
                        newLog.setTargetMember(params.getTargetMember());
                        newLog.setChargedMember(params.getChargedMember());
                        newLog.setErrorType(errorType);
                        newLog.setFreeBaseUsed(freeBaseUsed);
                        newLog.setMessageType(params.getMessageType());
                        newLog.setSmsMailing(params.getSmsMailing());
                        newLog.setSmsType(params.getSmsType());
                        smsLogService.save(newLog);
                        return newLog;
                    }
                });
            } catch (final Exception e) {
                LOG.warn("Error saving the SMS log: " + newLog, e);
            }
        }
    }

    private class SmsSenderThreads extends WorkerThreads<SendSmsDTO> {

        public SmsSenderThreads(final String name, final int threadCount) {
            super(name, threadCount);
        }

        @Override
        protected void process(final SendSmsDTO params) {
            final boolean success = transactionTemplate.execute(new TransactionCallback<Boolean>() {
                public Boolean doInTransaction(final TransactionStatus status) {
                    try {
                        sendSms(params);
                        return true;
                    } catch (final Exception e) {
                        status.setRollbackOnly();
                        LOG.error("Error sending SMS", e);
                        return false;
                    }
                }
            });

            // Ensure any created pending account status is processed
            if (success) {
                accountStatusHandler.processFromCurrentTransaction();
            }
            CurrentTransactionData.cleanup();
        }
    }

    private static final Log     LOG                       = LogFactory.getLog(MessageServiceImpl.class);
    private MessageDAO           messageDao;
    private FetchService         fetchService;
    private MemberService        memberService;
    private PaymentService       paymentService;
    private PreferenceService    preferenceService;
    private SettingsService      settingsService;
    private SmsLogService        smsLogService;
    private LinkGenerator        linkGenerator;
    private MailHandler          mailHandler;
    private SmsSender            smsSender;
    private GroupService         groupService;
    private TransactionTemplate  transactionTemplate;
    private int                  maxSmsThreads;
    private SmsSenderThreads     smsSenderThreads;
    private AccountStatusHandler accountStatusHandler;
    private Map<Long, Thread>    activeBulkMessagesThreads = Collections.synchronizedMap(new HashMap<Long, Thread>());

    public void destroy() throws Exception {
        if (smsSenderThreads != null) {
            smsSenderThreads.interrupt();
            smsSenderThreads = null;
        }
        // Stop all bulk messages
        for (final Thread thread : activeBulkMessagesThreads.values()) {
            try {
                thread.interrupt();
            } catch (final Exception e) {
                // Ignore
            }
        }
        activeBulkMessagesThreads.clear();
    }

    public Message load(final Long id, final Relationship... fetch) {
        return messageDao.load(id, fetch);
    }

    public void performAction(final MessageAction action, final Long... ids) {
        for (final Long id : ids) {
            final Message message = messageDao.load(id);
            checkMessageOwner(message);
            if (action == MessageAction.DELETE) {
                final Thread thread = activeBulkMessagesThreads.get(id);
                if (thread != null) {
                    thread.interrupt();
                    messageDao.removeAllPending(message);
                }
                messageDao.delete(message.getId());
            } else {
                switch (action) {
                    case MOVE_TO_TRASH:
                        message.setRemovedAt(Calendar.getInstance());
                        break;
                    case RESTORE:
                        message.setRemovedAt(null);
                        break;
                    case MARK_AS_READ:
                        message.setRead(true);
                        break;
                    case MARK_AS_UNREAD:
                        message.setRead(false);
                        break;
                }
                messageDao.update(message);
            }
        }
    }

    public void purgeExpiredMessagesOnTrash(final Calendar time) {
        final TimePeriod timePeriod = settingsService.getLocalSettings().getDeleteMessagesOnTrashAfter();
        if (timePeriod == null || timePeriod.getNumber() <= 0) {
            return;
        }
        final Calendar limit = timePeriod.remove(DateHelper.truncate(time));
        messageDao.removeMessagesOnTrashBefore(limit);
    }

    public Message read(final Long id, final Relationship... fetch) {
        final Message message = load(id, fetch);
        message.setRead(true);
        return message;
    }

    public void resumeUnfinished() {
        final List<Long> ids = messageDao.listUnfinishedIds();
        for (final Long id : ids) {
            bulkSend(id);
        }
    }

    public List<Message> search(final MessageQuery query) {
        if (LoggedUser.isValid()) {
            query.setGetter(LoggedUser.element());
        }
        return messageDao.search(query);
    }

    public Message sendFromAdminToGroup(final SendMessageToGroupDTO message) {
        // Check if the admin manages each group
        AdminGroup group = LoggedUser.group();
        group = fetchService.fetch(group, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managesGroups = group.getManagesGroups();
        for (final MemberGroup memberGroup : message.getToGroups()) {
            if (!managesGroups.contains(memberGroup)) {
                throw new PermissionDeniedException();
            }
        }
        return doSendBulk(message);
    }

    public Message sendFromAdminToMember(final SendMessageToMemberDTO message) throws MemberWontReceiveNotificationException {
        return doSendSingle(message);
    }

    public Message sendFromBrokerToMembers(final SendMessageFromBrokerToMembersDTO message) {
        return doSendBulk(message);
    }

    public Message sendFromMemberToAdmin(final SendMessageToAdminDTO message) {
        return doSendSingle(message);
    }

    public Message sendFromMemberToMember(final SendMessageToMemberDTO message) throws MemberWontReceiveNotificationException {
        return doSendSingle(message);
    }

    public void sendFromSystem(final SendMessageFromSystemDTO message) {
        final Entity entity = message.getEntity();
        String link = "";
        if (entity != null && linkGenerator != null) {
            link = linkGenerator.generateLinkFor(message.getToMember(), entity);
        }
        final String body = StringUtils.replace(message.getBody(), "#link#", link);
        message.setBody(body);
        message.setHtml(true);
        doSendSingle(message);
    }

    public SmsLog sendSms(final SendSmsDTO params) {
        final Member target = params.getTargetMember();
        final MemberCustomField customField = settingsService.getSmsCustomField();
        if (customField == null || !memberService.hasValueForField(target, customField)) {
            // Either no custom field, or the member didn't have value for the mobile phone
            return logSms(params, ErrorType.NO_PHONE, false);
        }
        Member charged = params.getChargedMember();
        final boolean freeMailing = params.getSmsMailing() != null && params.getSmsMailing().isFree();
        ErrorType errorType = null;
        boolean boughtNewMessages = false;
        MemberSmsStatus memberSmsStatus = null;
        int additionalChargedSms = 0;
        boolean statusChanged = false;
        boolean freeBaseUsed = false;
        ISmsContext smsContext = null;
        Transfer transfer = null;
        if (!freeMailing) {
            // Charge the SMS
            if (charged == null) {
                charged = target;
                params.setChargedMember(charged);
            }
            charged = fetchService.reload(charged, Element.Relationships.GROUP);
            smsContext = groupService.getSmsContext(charged.getMemberGroup());
            memberSmsStatus = memberService.getSmsStatus(charged);
            additionalChargedSms = smsContext.getAdditionalChargedSms(charged);
            final int freeSms = smsContext.getFreeSms(charged);
            if (memberSmsStatus.getFreeSmsSent() < freeSms) {
                // There are free messages left
                memberSmsStatus.setFreeSmsSent(memberSmsStatus.getFreeSmsSent() + 1);
                freeBaseUsed = true;
                statusChanged = true;
            } else if (memberSmsStatus.getPaidSmsLeft() > 0) {
                // There are paid messages left
                memberSmsStatus.setPaidSmsLeft(memberSmsStatus.getPaidSmsLeft() - 1);
                statusChanged = true;
            } else {
                // Check if paid messages are enabled
                if (additionalChargedSms > 0) {
                    // Paid messages are enabled
                    if (memberSmsStatus.isAllowChargingSms()) {
                        // The member allows charge
                        final TransferDTO chargeDTO = buildSmsChargeDto(charged, smsContext);
                        try {
                            if (chargeDTO == null) {
                                throw new UnexpectedEntityException();
                            }
                            transfer = (Transfer) paymentService.insertWithoutNotification(chargeDTO);
                            // The status will only be updated if the SMS sending is successful, to avoid updating the status and having to undo later
                            boughtNewMessages = true;
                        } catch (final NotEnoughCreditsException e) {
                            errorType = ErrorType.NOT_ENOUGH_FUNDS;
                        } catch (final MaxAmountPerDayExceededException e) {
                            errorType = ErrorType.NOT_ENOUGH_FUNDS;
                        } catch (final UpperCreditLimitReachedException e) {
                            errorType = ErrorType.NOT_ENOUGH_FUNDS;
                        } catch (final UnexpectedEntityException e) {
                            throw new ValidationException("The SMS charging is not well configured. Please, check the charging transfer type.");
                        }
                    } else {
                        // The member have disallowed charging
                        errorType = ErrorType.ALLOW_CHARGING_DISABLED;
                    }
                } else {
                    if (freeSms == 0) {
                        throw new ValidationException("SMS cannot be sent as both free messages and aditional messages are zero");
                    } else {
                        errorType = ErrorType.NO_SMS_LEFT;
                    }
                }
            }
        }
        // Send the message itself if no error so far
        if (errorType == null) {
            try {
                if (!smsSender.send(target, params.getText())) {
                    throw new Exception();
                }
                // The message was sent. Update the member sms status
                if (boughtNewMessages) {
                    final int left = additionalChargedSms - 1;
                    Calendar expiration = null;
                    if (left > 0) {
                        TimePeriod additionalChargedPeriod = smsContext.getAdditionalChargedPeriod(charged);
                        if (additionalChargedPeriod == null) {
                            additionalChargedPeriod = TimePeriod.ONE_MONTH;
                        }
                        expiration = additionalChargedPeriod.add(Calendar.getInstance());
                    }
                    memberSmsStatus.setPaidSmsLeft(left);
                    memberSmsStatus.setPaidSmsExpiration(expiration);
                    statusChanged = true;
                }
            } catch (final Exception e) {
                errorType = ErrorType.SEND_ERROR;
                if (transfer != null) {
                    // The message couldn't be sent and the payment was already performed. Chargeback.
                    paymentService.chargebackSmsCharge(transfer);
                }
                // Ensure the status won't be changed on errors
                statusChanged = false;
            }
        }
        // Update the SMS status if it has changed
        if (statusChanged) {
            memberService.updateSmsStatus(memberSmsStatus);
        }
        // Generate the SMS log
        return logSms(params, errorType, freeBaseUsed);
    }

    public synchronized void sendSmsAfterTransactionCommit(final SendSmsDTO params) {
        if (smsSenderThreads == null) {
            smsSenderThreads = new SmsSenderThreads("SMS sender for " + settingsService.getLocalSettings().getApplicationName(), maxSmsThreads);
        }
        CurrentTransactionData.addTransactionCommitListener(new TransactionCommitListener() {
            public void onTransactionCommit() {
                smsSenderThreads.enqueue(params);
            }
        });
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setLinkGenerator(final LinkGenerator linkGenerator) {
        this.linkGenerator = linkGenerator;
    }

    public void setMailHandler(final MailHandler mailHandler) {
        this.mailHandler = mailHandler;
    }

    public void setMaxSmsThreads(final int maxSmsThreads) {
        this.maxSmsThreads = maxSmsThreads;
    }

    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    public void setMessageDao(final MessageDAO messageDao) {
        this.messageDao = messageDao;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setPreferenceService(final PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSmsLogService(final SmsLogService smsLogService) {
        this.smsLogService = smsLogService;
    }

    public void setSmsSender(final SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void validate(final SendMessageDTO message) throws ValidationException {
        getValidator(message.getClass()).validate(message);
    }

    private Validator basicToMemberValidator() {
        final Validator validator = basicValidator();
        validator.property("toMember").required();
        return validator;
    }

    private Validator basicValidator() {
        final Validator validator = new Validator("message");
        validator.property("subject").required();
        validator.property("body").required();
        return validator;
    }

    /**
     * Returns a Message instance, filled with data from the given DTO
     */
    private Message buildFromDTO(final SendMessageDTO dto, final Message.Direction direction) {
        return buildFromDTO(dto, dto, direction);
    }

    /**
     * Returns a Message instance, filled with data from the given DTO
     */
    private Message buildFromDTO(final SendMessageDTO original, final SendMessageDTO dto, final Message.Direction direction) {
        final Message message = new Message();
        message.setDate(Calendar.getInstance());
        message.setHtml(dto.isHtml());
        message.setSubject(dto.getSubject());
        message.setBody(dto.getBody());
        if (!(dto instanceof SendMessageFromSystemDTO)) {
            message.setFromMember((Member) (LoggedUser.isValid() && !LoggedUser.isAdministrator() ? LoggedUser.accountOwner() : null));
        }
        message.setType(dto.getType());
        message.setDirection(direction);
        if (direction == Message.Direction.OUTGOING) {
            message.setRead(true);
        }
        if (dto instanceof SendMessageToMemberDTO) {
            message.setToMember(((SendMessageToMemberDTO) dto).getToMember());
        } else if (dto instanceof SendMessageToGroupDTO) {
            message.setToGroups(((SendMessageToGroupDTO) dto).getToGroups());
        }
        if (message.isFromAMember() && message.isToAMember()) {
            message.setCategory(null);
        } else {
            message.setCategory(dto.getCategory());
        }
        return message;
    }

    private TransferDTO buildSmsChargeDto(final Member member, final ISmsContext smsContext) {
        final MemberGroupSettings memberSettings = member.getMemberGroup().getMemberSettings();
        final TransferType smsChargeTransferType = memberSettings.getSmsChargeTransferType();
        final BigDecimal smsChargeAmount = smsContext.getAdditionalChargeAmount(member);

        // There are no charge settings, so don't charge
        if (smsChargeTransferType == null || smsChargeAmount == null) {
            return null;
        }

        // Build charge DTO
        final TransferDTO transferDto = new TransferDTO();
        if (smsChargeTransferType.isFromMember()) {
            transferDto.setFromOwner(member);
        } else {
            transferDto.setFromOwner(SystemAccountOwner.instance());
        }
        transferDto.setToOwner(SystemAccountOwner.instance());
        transferDto.setTransferType(smsChargeTransferType);
        transferDto.setDescription(smsChargeTransferType.getDescription());
        transferDto.setAmount(smsChargeAmount);
        transferDto.setAutomatic(true);
        return transferDto;
    }

    private void bulkSend(final Long id) {
        if (activeBulkMessagesThreads.containsKey(id)) {
            return;
        }
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            // Send one single message in another transaction
                            final boolean result = transactionTemplate.execute(new TransactionCallback<Boolean>() {
                                public Boolean doInTransaction(final TransactionStatus status) {
                                    return sendPendingInTransaction(id);
                                }
                            });
                            // If no more messages, remove the thread
                            if (!result) {
                                activeBulkMessagesThreads.remove(id);
                            }
                            CurrentTransactionData.runCurrentTransactionCommitListeners();
                        } finally {
                            CurrentTransactionData.cleanup();
                        }
                    }
                } catch (final Exception e) {
                    LOG.error("Error while sending bulk messages for message id = " + id, e);
                }
            }
        };
        thread.setName("Sending bulk messages for outgoing message id " + id);
        activeBulkMessagesThreads.put(id, thread);
        thread.start();
    }

    /**
     * Checks if the message owner is the logged member
     */
    private Message checkMessageOwner(Message message) {
        if (message == null) {
            return null;
        }
        message = fetchService.fetch(message, Message.Relationships.FROM_MEMBER, Message.Relationships.TO_MEMBER);
        final Member loggedMember = (Member) (LoggedUser.isValid() && !LoggedUser.isAdministrator() ? LoggedUser.accountOwner() : null);
        final Member owner = message.getOwner();
        if ((loggedMember == null && owner != null) || (loggedMember != null && !loggedMember.equals(owner))) {
            throw new PermissionDeniedException();
        }
        return message;
    }

    private Message doSendBulk(final SendMessageDTO message) {
        // Validate the message parameters
        validate(message);

        final Message senderCopy = insertSenderCopy(message);

        // Start sending after the transaction is committed, so it won't happen that the message is not visible yet
        CurrentTransactionData.addTransactionCommitListener(new TransactionCommitListener() {
            public void onTransactionCommit() {
                bulkSend(senderCopy.getId());
            }
        });

        return senderCopy;
    }

    private Message doSendSingle(final SendMessageDTO message) {
        // Validate the message parameters
        validate(message);

        Set<MessageChannel> messageChannels = null;
        if (message instanceof SendMessageToMemberDTO) {
            // If is a message to member, get the received channels
            final SendMessageToMemberDTO toMemberMessage = (SendMessageToMemberDTO) message;
            messageChannels = preferenceService.receivedChannels(toMemberMessage.getToMember(), message.getType());
            if (toMemberMessage.requiresMemberToReceive() && CollectionUtils.isEmpty(messageChannels)) {
                // The message dto requires the member to receive and his preferences say he doesn't - throw an exception
                throw new MemberWontReceiveNotificationException();
            }
        }

        // Then insert the sender copy
        final Message senderCopy = insertSenderCopy(message);
        final Message toSend = buildFromDTO(message, Direction.INCOMING);
        String sms = null;
        if (message instanceof SendMessageFromSystemDTO) {
            sms = ((SendMessageFromSystemDTO) message).getSms();
        }
        send(toSend, sms, messageChannels);
        return senderCopy;
    }

    private Validator getValidator(final Class<? extends SendMessageDTO> type) {
        if (type == SendDirectMessageToMemberDTO.class) {
            final Validator toMember = basicToMemberValidator();
            toMember.property("toMember").add(new SameFromAndToValidation());
            toMember.property("category").add(new RequiredWhenFromAdminValidation());
            return toMember;
        } else if (type == SendMessageToAdminDTO.class) {
            final Validator toAdmin = basicValidator();
            toAdmin.property("category").required();
            return toAdmin;
        } else if (type == SendMessageFromBrokerToMembersDTO.class) {
            final Validator toRegisteredMembers = basicValidator();
            return toRegisteredMembers;
        } else if (type == SendMessageToGroupDTO.class) {
            final Validator toGroup = basicValidator();
            toGroup.property("toGroups").required();
            toGroup.property("category").add(new RequiredWhenFromAdminValidation());
            return toGroup;
        } else if (type == SendMessageFromSystemDTO.class) {
            final Validator fromSystem = basicToMemberValidator();
            fromSystem.property("type").required();
            return fromSystem;
        } else {
            throw new IllegalArgumentException("Unexpected type " + type);
        }
    }

    private Message insertSenderCopy(final SendMessageDTO dto) throws MemberWontReceiveNotificationException {
        // Validate the message being replied
        final Message inReplyTo = checkMessageOwner(dto.getInReplyTo());

        // Update the original message as replied
        markAsReplied(inReplyTo);

        if (dto instanceof SendMessageFromSystemDTO) {
            // There is no sender copy for messages from system (a.k.a notifications)
            return null;
        }

        // Insert the sender copy
        Message message = buildFromDTO(dto, Message.Direction.OUTGOING);
        message = messageDao.insert(message);

        // If the message is bulk, assign all members which should receive the messages
        if (dto instanceof SendMessageToGroupDTO) {
            final SendMessageToGroupDTO toGroup = (SendMessageToGroupDTO) dto;
            messageDao.assignPendingToSendByGroups(message, toGroup.getToGroups());
        } else if (dto instanceof SendMessageFromBrokerToMembersDTO) {
            final Member broker = LoggedUser.element();
            messageDao.assignPendingToSendByBroker(message, broker);
        }

        return message;
    }

    private SmsLog logSms(final SendSmsDTO params, final ErrorType errorType, final boolean freeBaseUsed) {
        final CreateSmsLogThread thread = new CreateSmsLogThread(params, errorType, freeBaseUsed);
        thread.start();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // Ignore
        }
        return thread.getLog();
    }

    /**
     * Marks a given message as replied
     */
    private void markAsReplied(final Message message) {
        if (message != null) {
            message.setReplied(true);
            messageDao.update(message);
        }
    }

    /**
     * Sends the message, retuning a set containing the channels actually delivered
     */
    private Set<MessageChannel> send(final Message message, final String smsMessage, Set<MessageChannel> messageChannels) {
        final Member toMember = fetchService.fetch(message.getToMember(), Element.Relationships.GROUP);
        message.setCategory(fetchService.fetch(message.getCategory()));
        final Set<MessageChannel> result = EnumSet.noneOf(MessageChannel.class);

        if (toMember != null) {
            final MemberGroup group = toMember.getMemberGroup();
            final Type type = message.getType();

            // Check which message channels will be used
            if (messageChannels == null) {
                messageChannels = preferenceService.receivedChannels(toMember, type);
            }
            if (CollectionUtils.isEmpty(messageChannels)) {
                // Nothing to send for this member
                return result;
            }

            // Send an e-mail if needed
            final String email = toMember.getEmail();
            if (messageChannels.contains(MessageChannel.EMAIL) && StringUtils.isNotEmpty(email)) {
                final InternetAddress replyToAddress = LoggedUser.isValid() ? mailHandler.getInternetAddress(LoggedUser.element()) : null;
                final boolean isHtml = message.isHtml();
                final String subject = mailHandler.processSubject(message.getSubject());
                final String body = mailHandler.processBody(message.getBody(), message.getFromMember(), message.getCategory(), isHtml);
                final InternetAddress toAddress = mailHandler.getInternetAddress(toMember);
                mailHandler.sendAfterTransactionCommit(subject, replyToAddress, toAddress, body, isHtml);
                result.add(MessageChannel.EMAIL);
            }

            // Check if we need to send a SMS
            if (StringUtils.isNotEmpty(smsMessage) && messageChannels.contains(MessageChannel.SMS) && group.getSmsMessages().contains(type)) {
                // Send the SMS
                final SendSmsDTO sendDTO = new SendSmsDTO();
                sendDTO.setTargetMember(toMember);
                sendDTO.setMessageType(type);
                sendDTO.setText(smsMessage);
                sendSmsAfterTransactionCommit(sendDTO);
                result.add(MessageChannel.SMS);
            }

            // If the user does not want the internal message, return now
            if (!messageChannels.contains(MessageChannel.MESSAGE)) {
                // If not, return now
                return result;
            }
            result.add(MessageChannel.MESSAGE);

        }

        // Insert the internal message
        messageDao.insert(message);

        return result;
    }

    private boolean sendPendingInTransaction(final Long id) {
        final Message message = messageDao.load(id);
        final Member member = messageDao.getNextMemberToSend(message);
        if (member == null) {
            // No more to send
            message.setFinished(true);
            return false;
        }

        // Send the message
        final Message toSend = new Message();
        // Even when from broker the FROM_ADMIN_TO_GROUP is used
        toSend.setType(Message.Type.FROM_ADMIN_TO_GROUP);
        toSend.setDate(Calendar.getInstance());
        toSend.setDirection(Message.Direction.INCOMING);
        toSend.setFromMember(message.getFromMember());
        toSend.setToMember(member);
        toSend.setSubject(message.getSubject());
        toSend.setBody(message.getBody());
        toSend.setHtml(message.isHtml());
        final Set<MessageChannel> deliveredChannels = send(toSend, null, null);
        if (deliveredChannels.contains(MessageChannel.MESSAGE)) {
            message.setMessagesSent(message.getMessagesSent() + 1);
        }
        if (deliveredChannels.contains(MessageChannel.EMAIL)) {
            message.setEmailsSent(message.getEmailsSent() + 1);
        }

        // Remove the member from the pending to send
        messageDao.markAsSent(message, member);

        return true;
    }
}