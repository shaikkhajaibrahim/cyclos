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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.guarantees.Certification;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligation;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentRequestTicket;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel.Authorizer;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.entities.members.TransactionFeedback;
import nl.strohalm.cyclos.entities.members.TransactionFeedbackRequest;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;
import nl.strohalm.cyclos.entities.members.brokerings.Brokering;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.access.ChangeLoginPasswordDTO;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.guarantees.CertificationService;
import nl.strohalm.cyclos.services.accounts.guarantees.PaymentObligationService;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.PosPinBlockedException;
import nl.strohalm.cyclos.services.elements.BrokeringService;
import nl.strohalm.cyclos.services.elements.ChangeBrokerDTO;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.elements.SendMessageFromSystemDTO;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.preferences.MessageChannel;
import nl.strohalm.cyclos.services.preferences.PreferenceService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.tokens.ResetPinTokenData;
import nl.strohalm.cyclos.services.tokens.SenderRedeemTokenData;
import nl.strohalm.cyclos.services.tokens.TokenService;
import nl.strohalm.cyclos.services.transactions.DoExternalPaymentDTO;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.TicketService;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;

import nl.strohalm.cyclos.utils.sms.SmsSender;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Aspect used to notify members about various events on the system
 *
 * @author jefferson
 */
@Aspect
public class MessageAspect {
    private static final float PRECISION_DELTA = 0.0001F;
    private AccountService accountService;
    private BrokeringService brokeringService;
    private CertificationService certificationService;
    private ChannelService channelService;
    private ElementService elementService;
    private FetchService fetchService;
    private GroupService groupService;
    private InvoiceService invoiceService;
    private MessageService messageService;
    private PaymentObligationService paymentObligationService;
    private SettingsService settingsService;
    private TicketService ticketService;
    private TokenService tokenService;
    private TokenMessages tokenMessages;
    private MessageHelper messageHelper;
    /**
     * Store, for each member, the time in millis when the low units alert was sent. Used to not send the alert twice a day
     */
    private final Set<Long> sentLowUnits = Collections.synchronizedSet(new HashSet<Long>());
    private Calendar lastPaymentDate;
    private MessageResolver messageResolver;
    private PreferenceService preferenceService;
    private SmsSender smsSender;

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.InvoiceService.accept*(..))", returning = "invoice", argNames = "invoice")
    public void acceptedInvoiceNotification(final Invoice invoice) {
        // Get the destination
        final Member destinationMember = invoice.getFromMember();

        if (destinationMember == null) {
            // The invoice was sent from system
            return;
        }

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getInvoiceAcceptedSubject();
        final String body = messageSettings.getInvoiceAcceptedMessage();
        final String sms = messageSettings.getInvoiceAcceptedSms();

        // Process message body
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, invoice.getTo(), invoice);
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, invoice.getTo(), invoice);
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, invoice.getTo(), invoice);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.INVOICE);
        message.setEntity(invoice);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);

        notifyTransactionFeedbackRequest(invoice.getTransfer());
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.PaymentService.insertWithNotification(..)) && args(dto)", argNames = "transfer, dto", returning = "transfer")
    public void automaticPaymentReceivedNotification(Transfer transfer, final TransferDTO dto) {
        transfer = fetchService.fetch(transfer, RelationshipHelper.nested(Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER), Transfer.Relationships.TYPE);
        if (transfer.isRoot() && !transfer.isToSystem()) {
            // Get the destination
            final Member destinationMember = (Member) transfer.getTo().getOwner();
            final Set<MessageChannel> channels = preferenceService.receivedChannels(destinationMember, Message.Type.PAYMENT);
            if (!channels.isEmpty()) {
                AccountStatus status = null;
                final boolean sendSmsNotification = transfer.getType().isAllowSmsNotification() && channels.contains(MessageChannel.SMS);
                if (sendSmsNotification) {
                    status = accountService.getStatus(new GetTransactionsDTO(transfer.getTo()));
                }

                // Get the message settings
                final LocalSettings localSettings = settingsService.getLocalSettings();
                final MessageSettings messageSettings = settingsService.getMessageSettings();
                String subject = null;
                String body = null;
                String sms = null;

                if (transfer.getAccountFeeLog() != null) {
                    subject = messageSettings.getAccountFeeReceivedSubject();
                    body = messageSettings.getAccountFeeReceivedMessage();
                    if (sendSmsNotification) {
                        sms = messageSettings.getAccountFeeReceivedSms();
                    }

                    // Process message content
                    subject = MessageProcessingHelper.processVariables(subject, localSettings, destinationMember, transfer, transfer.getAccountFeeLog().getAccountFee());
                    body = MessageProcessingHelper.processVariables(body, localSettings, destinationMember, transfer, transfer.getAccountFeeLog().getAccountFee());
                    if (sendSmsNotification) {
                        sms = MessageProcessingHelper.processVariables(sms, localSettings, destinationMember, transfer, transfer.getAccountFeeLog().getAccountFee(), status);
                    }
                } else {
                    // Check if the transfer has been processed or awaits authorization
                    if (transfer.getProcessDate() == null) {
                        subject = messageSettings.getPendingPaymentReceivedSubject();
                        body = messageSettings.getPendingPaymentReceivedMessage();
                        if (sendSmsNotification) {
                            sms = messageSettings.getPendingPaymentReceivedSms();
                        }
                    } else {
                        subject = messageSettings.getPaymentReceivedSubject();
                        body = messageSettings.getPaymentReceivedMessage();
                        if (sendSmsNotification) {
                            sms = messageSettings.getPaymentReceivedSms();
                        }
                    }

                    // Process message content
                    subject = MessageProcessingHelper.processVariables(subject, localSettings, transfer.getFrom().getOwner(), transfer);
                    body = MessageProcessingHelper.processVariables(body, localSettings, transfer.getFrom().getOwner(), transfer);
                    if (sendSmsNotification) {
                        sms = MessageProcessingHelper.processVariables(sms, localSettings, transfer.getFrom().getOwner(), transfer, status);
                    }
                }

                // Create the DTO
                final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
                message.setType(Message.Type.PAYMENT);
                message.setEntity(transfer);
                message.setToMember(destinationMember);
                message.setSubject(subject);
                message.setBody(body);
                message.setSms(sms);

                // Send the message
                messageService.sendFromSystem(message);
            }
        }
    }

    @AfterThrowing(value = "target(nl.strohalm.cyclos.services.access.AccessService)", throwing = "e", argNames = "e")
    public void blockedCredentialsNotification(final BlockedCredentialsException e) {
        final User user = e.getUser();
        if (user instanceof MemberUser) {
            boolean skipSms = false;
            try {
                // Try to get the current channel from the web services context, which will only return something when running on web services
                final Channel currentChannel = (Channel) Class.forName("mp.platform.cyclone.webservices.WebServiceContext").getMethod("getChannel").invoke(null);
                if (currentChannel != null) {
                    // Ok, we are running on a web service. skip if this is the SMS channel, as it has it's own notification
                    final Channel smsChannel = channelService.getSmsChannel();
                    if (currentChannel.equals(smsChannel)) {
                        // Do not notify by SMS
                        skipSms = true;
                    }
                }
            } catch (final Exception ex) {
                // SMS won't be skipped
            }

            final Member member = (Member) user.getElement();

            // Get message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            String subject;
            String body;
            String sms;
            switch (e.getCredentialsType()) {
                case LOGIN_PASSWORD:
                    subject = messageSettings.getLoginBlockedSubject();
                    body = messageSettings.getLoginBlockedMessage();
                    sms = skipSms ? null : messageSettings.getLoginBlockedSms();
                    break;
                case TRANSACTION_PASSWORD:
                    subject = messageSettings.getMaxTransactionPasswordTriesSubject();
                    body = messageSettings.getMaxTransactionPasswordTriesMessage();
                    sms = skipSms ? null : messageSettings.getMaxTransactionPasswordTriesSms();
                    break;
                case PIN:
                    subject = messageSettings.getPinBlockedSubject();
                    body = messageSettings.getPinBlockedMessage();
                    sms = skipSms ? null : messageSettings.getPinBlockedSms();
                    break;
                case CARD_SECURITY_CODE:
                    subject = messageSettings.getCardSecurityCodeBlockedSubject();
                    body = messageSettings.getCardSecurityCodeBlockedMessage();
                    sms = skipSms ? null : messageSettings.getCardSecurityCodeBlockedSms();
                    break;
                default:
                    throw e;
            }

            // Process message content
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, member, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, member, localSettings);
            final String processedSms = sms == null ? null : MessageProcessingHelper.processVariables(sms, member, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.ACCESS);
            message.setToMember(member);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.InvoiceService.cancel*(..))", returning = "invoice", argNames = "invoice")
    public void cancelledInvoiceNotification(final Invoice invoice) {
        if (!invoice.isToSystem()) {
            // Get the destination
            final Member destinationMember = invoice.getToMember();

            // Get the message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = messageSettings.getInvoiceCancelledSubject();
            final String body = messageSettings.getInvoiceCancelledMessage();
            final String sms = messageSettings.getInvoiceCancelledSms();

            // Process message body
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, invoice.getFrom(), invoice);
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, invoice.getFrom(), invoice);
            final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, invoice.getFrom(), invoice);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.INVOICE);
            message.setEntity(invoice);
            message.setToMember(destinationMember);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.CertificationService.cancelCertificationAsMember(..)) && args(certificationId)", argNames = "certificationId")
    public void certificationCanceledNotification(final Long certificationId) {
        // Load the certification
        final Certification certification = certificationService.load(certificationId, Certification.Relationships.BUYER, Certification.Relationships.ISSUER);

        certificationStatusChangedNotification(certification);
    }

    @Around(value = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.CertificationService.save(..)) && args(certification)", argNames = "certification")
    public Certification certificationIssuedNotification(final ProceedingJoinPoint pjp, Certification certification) throws Throwable {
        // Check if it's a new certification
        final boolean isInsert = certification.isTransient();

        // Save the certification
        certification = (Certification) pjp.proceed();

        // If the certification has been registered and is active, notify buyer
        if (isInsert && certification.getStatus() == Certification.Status.ACTIVE) {

            // Get local and message settings
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final MessageSettings messageSettings = settingsService.getMessageSettings();

            // Get the destination
            final Member buyer = certification.getBuyer();

            // Get the message settings
            final String subjectBuyer = messageSettings.getCertificationIssuedSubject();
            final String bodyBuyer = messageSettings.getCertificationIssuedMessage();
            final String smsBuyer = messageSettings.getCertificationIssuedSms();

            // Process message content
            final String processedSubjectBuyer = MessageProcessingHelper.processVariables(subjectBuyer, certification, localSettings);
            final String processedBodyBuyer = MessageProcessingHelper.processVariables(bodyBuyer, certification, localSettings);
            final String processedSmsBuyer = MessageProcessingHelper.processVariables(smsBuyer, certification, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO messageToBuyer = new SendMessageFromSystemDTO();
            messageToBuyer.setEntity(certification);
            messageToBuyer.setType(Message.Type.CERTIFICATION);
            messageToBuyer.setToMember(buyer);
            messageToBuyer.setSubject(processedSubjectBuyer);
            messageToBuyer.setBody(processedBodyBuyer);
            messageToBuyer.setSms(processedSmsBuyer);

            // Send the message
            messageService.sendFromSystem(messageToBuyer);
        }

        return certification;
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.CertificationService.processCertifications(..))", argNames = "certifications", returning = "certifications")
    public void certificationListStatusChangedNotification(final List<Certification> certifications) {
        for (final Certification certification : certifications) {
            certificationStatusChangedNotification(certification);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.CertificationService.changeStatus(..)) && args(certificationId, newStatus)", argNames = "certificationId, newStatus")
    public void certificationStatusChangedNotification(final Long certificationId, final Certification.Status newStatus) {
        // Load the certification
        final Certification certification = certificationService.load(certificationId, Certification.Relationships.BUYER, Certification.Relationships.ISSUER);

        certificationStatusChangedNotification(certification);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.CommissionService.acceptBrokerCommissionContract*(..))", returning = "brokerCommissionContract", argNames = "brokerCommissionContract")
    public void commissionContractAcceptedNotification(final BrokerCommissionContract brokerCommissionContract) {
        // Get the destination
        final Member destinationMember = brokerCommissionContract.getBrokering().getBroker();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getCommissionContractAcceptedSubject();
        final String body = messageSettings.getCommissionContractAcceptedMessage();
        final String sms = messageSettings.getCommissionContractAcceptedSms();

        // Process message content
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, brokerCommissionContract, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, brokerCommissionContract, localSettings);
        final String processedSms = MessageProcessingHelper.processVariables(sms, brokerCommissionContract, localSettings);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setEntity(brokerCommissionContract);
        message.setType(Message.Type.BROKERING);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.CommissionService.cancelBrokerCommissionContract*(..))", returning = "brokerCommissionContract", argNames = "brokerCommissionContract")
    public void commissionContractCancelledNotification(final BrokerCommissionContract brokerCommissionContract) {
        // Get the destination
        final Member destinationMember = brokerCommissionContract.getBrokering().getBrokered();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getCommissionContractCancelledSubject();
        final String body = messageSettings.getCommissionContractCancelledMessage();
        final String sms = messageSettings.getCommissionContractCancelledSms();

        // Process message content
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, brokerCommissionContract, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, brokerCommissionContract, localSettings);
        final String processedSms = MessageProcessingHelper.processVariables(sms, brokerCommissionContract, localSettings);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setEntity(brokerCommissionContract);
        message.setType(Message.Type.BROKERING);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.CommissionService.denyBrokerCommissionContract*(..))", returning = "brokerCommissionContract", argNames = "brokerCommissionContract")
    public void commissionContractDeniedNotification(final BrokerCommissionContract brokerCommissionContract) {
        // Get the destination
        final Member destinationMember = brokerCommissionContract.getBrokering().getBroker();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getCommissionContractDeniedSubject();
        final String body = messageSettings.getCommissionContractDeniedMessage();
        final String sms = messageSettings.getCommissionContractDeniedSms();

        // Process message content
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, brokerCommissionContract, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, brokerCommissionContract, localSettings);
        final String processedSms = MessageProcessingHelper.processVariables(sms, brokerCommissionContract, localSettings);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setEntity(brokerCommissionContract);
        message.setType(Message.Type.BROKERING);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.InvoiceService.deny*(..))", returning = "invoice", argNames = "invoice")
    public void deniedInvoiceNotification(final Invoice invoice) {
        if (!invoice.isFromSystem()) {
            // Get the destination
            final Member destinationMember = invoice.getFromMember();

            // Get the message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = messageSettings.getInvoiceDeniedSubject();
            final String body = messageSettings.getInvoiceDeniedMessage();
            final String sms = messageSettings.getInvoiceDeniedSms();

            // Process message content
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, invoice.getTo(), invoice);
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, invoice.getTo(), invoice);
            final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, invoice.getTo(), invoice);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.INVOICE);
            message.setEntity(invoice);
            message.setToMember(destinationMember);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.ads.AdService.notifyExpiredAds(..))", returning = "ads", argNames = "ads")
    public void expiredAdsNotification(final List<Ad> ads) throws Throwable {
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Get message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getAdExpirationSubject();
        final String body = messageSettings.getAdExpirationMessage();
        final String sms = messageSettings.getAdExpirationSms();

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.AD_EXPIRATION);

        for (final Ad ad : ads) {
            // Get the destination
            final Member owner = ad.getOwner();
            message.setToMember(owner);

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, ad, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, ad, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, ad, localSettings);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Process link and send message
            message.setEntity(ad);
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.BrokeringService.removeExpiredBrokerings(..))", returning = "brokerings", argNames = "brokerings")
    public void expiredBrokeringsNotification(final List<Brokering> brokerings) {
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Get message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getBrokeringExpirationSubject();
        final String body = messageSettings.getBrokeringExpirationMessage();
        final String sms = messageSettings.getBrokeringExpirationSms();

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.BROKERING);

        for (final Brokering brokering : brokerings) {
            // Get the destination
            final Member member = brokering.getBroker();
            message.setToMember(member);

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, member, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, member, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, member, localSettings);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.InvoiceService.expireInvoice(..))", returning = "invoice", argNames = "invoice")
    public void expiredInvoiceNotification(final Invoice invoice) {
        // Get the settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Send message to the sender of the invoice
        if (!invoice.isFromSystem()) {
            final String subjectSender = messageSettings.getSentInvoiceExpiredSubject();
            final String bodySender = messageSettings.getSentInvoiceExpiredMessage();
            final String smsSender = messageSettings.getSentInvoiceExpiredSms();
            final String processedSubjectSender = MessageProcessingHelper.processVariables(subjectSender, localSettings, invoice.getTo(), invoice);
            final String processedBodySender = MessageProcessingHelper.processVariables(bodySender, localSettings, invoice.getTo(), invoice);
            final String processedSmsSender = MessageProcessingHelper.processVariables(smsSender, localSettings, invoice.getTo(), invoice);
            final SendMessageFromSystemDTO messageSender = new SendMessageFromSystemDTO();
            messageSender.setType(Message.Type.INVOICE);
            messageSender.setEntity(invoice);
            messageSender.setToMember(invoice.getFromMember());
            messageSender.setSubject(processedSubjectSender);
            messageSender.setBody(processedBodySender);
            messageSender.setSms(processedSmsSender);
            messageService.sendFromSystem(messageSender);
        }

        // Send message to the receiver of the invoice
        if (!invoice.isToSystem()) {
            final String subjectReceiver = messageSettings.getReceivedInvoiceExpiredSubject();
            final String bodyReceiver = messageSettings.getReceivedInvoiceExpiredMessage();
            final String smsReceiver = messageSettings.getReceivedInvoiceExpiredSms();
            final String processedSubjectReceiver = MessageProcessingHelper.processVariables(subjectReceiver, localSettings, invoice.getFrom(), invoice);
            final String processedBodyReceiver = MessageProcessingHelper.processVariables(bodyReceiver, localSettings, invoice.getFrom(), invoice);
            final String processedSmsReceiver = MessageProcessingHelper.processVariables(smsReceiver, localSettings, invoice.getFrom(), invoice);
            final SendMessageFromSystemDTO messageReceiver = new SendMessageFromSystemDTO();
            messageReceiver.setType(Message.Type.INVOICE);
            messageReceiver.setEntity(invoice);
            messageReceiver.setToMember(invoice.getToMember());
            messageReceiver.setSubject(processedSubjectReceiver);
            messageReceiver.setBody(processedBodyReceiver);
            messageReceiver.setSms(processedSmsReceiver);
            messageService.sendFromSystem(messageReceiver);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.LoanService.alertExpiredLoans(..))", argNames = "payments", returning = "payments")
    public void expiredLoansNotification(final List<LoanPayment> payments) {
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getLoanExpirationSubject();
        final String body = messageSettings.getLoanExpirationMessage();
        final String sms = messageSettings.getLoanExpirationSms();

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.LOAN);

        for (final LoanPayment payment : payments) {
            // Set entity for link processing
            final Loan loan = payment.getLoan();
            message.setEntity(loan);

            // Get the destination
            final Member member = (Member) loan.getTransfer().getTo().getOwner();
            message.setToMember(member);

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, loan, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, loan, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, loan, localSettings);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.PaymentService.confirmPayment(..)) && args(ticketString)", returning = "payment", argNames = "ticketString, payment")
    public void externalChannelPaymentConfirmed(final String ticketString, final Payment payment) {
        final PaymentRequestTicket prTicket = (PaymentRequestTicket) ticketService.load(ticketString, PaymentRequestTicket.Relationships.TO_CHANNEL);
        externalChannelPaymentNotification(payment, prTicket.getFromChannel(), prTicket.getToChannel());
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.PaymentService.insertExternalPayment(..)) && args(dto)", returning = "payment", argNames = "dto, payment")
    public void externalChannelPaymentPerformed(final DoExternalPaymentDTO dto, final Payment payment) {
        final String channelInternalName = dto.getChannel();
        final Channel channel = channelService.loadByInternalName(channelInternalName);
        externalChannelPaymentNotification(payment, channel, null);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.TicketService.expirePaymentRequestTicket(..)) && args(ticket)", returning = "returningTicket", argNames = "ticket, returningTicket")
    public void externalChannelPaymentRequestExpired(final PaymentRequestTicket ticket, final PaymentRequestTicket returningTicket) {
        // Get local and message settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final MessageSettings messageSettings = settingsService.getMessageSettings();

        final Channel smsChannel = channelService.getSmsChannel();

        // Only notify the payer if not on SMS channel
        final Channel toChannel = ticket.getToChannel();
        final boolean skipToSms = toChannel.equals(smsChannel);
        final Map<String, Object> toVariables = ticket.getVariableValues(localSettings);
        toVariables.put("channel", toChannel.getDisplayName());

        // Get payer message
        final Member payer = ticket.getFrom();
        final String subjectPayer = messageSettings.getExternalChannelPaymentRequestExpiredPayerSubject();
        final String bodyPayer = messageSettings.getExternalChannelPaymentRequestExpiredPayerMessage();
        final String smsPayer = skipToSms ? null : messageSettings.getExternalChannelPaymentRequestExpiredPayerSms();

        // Process payer message content
        final String processedSubjectPayer = MessageProcessingHelper.processVariables(subjectPayer, toVariables);
        final String processedBodyPayer = MessageProcessingHelper.processVariables(bodyPayer, toVariables);
        final String processedSmsPayer = smsPayer == null ? null : MessageProcessingHelper.processVariables(smsPayer, toVariables);

        // Create the payer DTO
        final SendMessageFromSystemDTO messageToPayer = new SendMessageFromSystemDTO();
        messageToPayer.setToMember(payer);
        messageToPayer.setType(Message.Type.EXTERNAL_PAYMENT);
        messageToPayer.setSubject(processedSubjectPayer);
        messageToPayer.setBody(processedBodyPayer);
        messageToPayer.setSms(processedSmsPayer);

        // Send message to payer
        messageService.sendFromSystem(messageToPayer);

        // Only notify the payee if not on SMS channel
        final Channel fromChannel = ticket.getFromChannel();
        final boolean skipFromSms = fromChannel.equals(smsChannel);
        final Map<String, Object> variableValues = ticket.getVariableValues(localSettings);
        variableValues.put("channel", fromChannel.getDisplayName());

        // Get receiver message
        final Member receiver = ticket.getTo();
        final String subjectReceiver = messageSettings.getExternalChannelPaymentRequestExpiredReceiverSubject();
        final String bodyReceiver = messageSettings.getExternalChannelPaymentRequestExpiredReceiverMessage();
        final String smsReceiver = skipFromSms ? null : messageSettings.getExternalChannelPaymentRequestExpiredReceiverSms();

        // Process receiver message content
        final String processedSubjectReceiver = MessageProcessingHelper.processVariables(subjectReceiver, variableValues);
        final String processedBodyReceiver = MessageProcessingHelper.processVariables(bodyReceiver, variableValues);
        final String processedSmsReceiver = smsReceiver == null ? null : MessageProcessingHelper.processVariables(smsReceiver, variableValues);

        // Create the receiver DTO
        final SendMessageFromSystemDTO messageToReceiver = new SendMessageFromSystemDTO();
        messageToReceiver.setToMember(receiver);
        messageToReceiver.setType(Message.Type.PAYMENT);
        messageToReceiver.setSubject(processedSubjectReceiver);
        messageToReceiver.setBody(processedBodyReceiver);
        messageToReceiver.setSms(processedSmsReceiver);

        // Send message to receiver
        messageService.sendFromSystem(messageToReceiver);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.LoanService.grant*(..))", argNames = "loan", returning = "loan")
    public void grantedLoanNotification(final Loan loan) {
        // Return when the loan is pending
        if (loan.getTransfer().getProcessDate() == null) {
            return;
        }

        // Get the destination
        final Member destinationMember = loan.getMember();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getLoanGrantedSubject();
        final String body = messageSettings.getLoanGrantedMessage();
        final String sms = messageSettings.getLoanGrantedSms();

        // Process message contents
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, destinationMember, loan);
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, destinationMember, loan);
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, destinationMember, loan);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.LOAN);
        message.setEntity(loan);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.acceptGuaranteeAsMember(..))", argNames = "guarantee", returning = "guarantee")
    public void guaranteeAcceptedNotification(final Guarantee guarantee) {
        doGuaranteeStatusChangedNotification(guarantee, null);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.cancelGuaranteeAsMember(..))", argNames = "guarantee", returning = "guarantee")
    public void guaranteeCancelledNotification(final Guarantee guarantee) {
        doGuaranteeStatusChangedNotification(guarantee, null);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.denyGuaranteeAsMember(..))", argNames = "guarantee", returning = "guarantee")
    public void guaranteeDeniedNotification(final Guarantee guarantee) {
        doGuaranteeStatusChangedNotification(guarantee, null);
    }

    @AfterReturning(pointcut = "(execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.requestGuarantee(..)) " +
            "|| execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.registerGuarantee(..)))", returning = "guarantee")
    public void guaranteePendingIssuerNotification(final Guarantee guarantee) throws Throwable {
        if (guarantee.getStatus() == Guarantee.Status.PENDING_ISSUER) { // only send notification if the status is PENDING_ISSUER
            // Get local and message settings
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final MessageSettings messageSettings = settingsService.getMessageSettings();

            // Get the destination
            final Member toMember = guarantee.getIssuer();

            // Get the message settings
            String subject = null;
            String body = null;
            String sms = null;
            if (guarantee.getGuaranteeType().getModel() == GuaranteeType.Model.WITH_BUYER_ONLY) {
                subject = messageSettings.getPendingBuyerOnlyGuaranteeIssuerSubject();
                body = messageSettings.getPendingBuyerOnlyGuaranteeIssuerMessage();
                sms = messageSettings.getPendingBuyerOnlyGuaranteeIssuerSms();
            } else {
                subject = messageSettings.getPendingGuaranteeIssuerSubject();
                body = messageSettings.getPendingGuaranteeIssuerMessage();
                sms = messageSettings.getPendingGuaranteeIssuerSms();
            }

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, guarantee, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, guarantee, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, guarantee, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setEntity(guarantee);
            message.setType(Message.Type.GUARANTEE);
            message.setToMember(toMember);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.changeStatus(..))", argNames = "guarantee", returning = "guarantee")
    public void guaranteeStatusChangedNotification(final Guarantee guarantee) {
        // we don't use the previous status in the case
        doGuaranteeStatusChangedNotification(guarantee, null);
    }

    @Around(value = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService.processGuarantee(..)) && args(guarantee,..)", argNames = "guarantee")
    public Guarantee guaranteeStatusChangedNotification(final ProceedingJoinPoint pjp, Guarantee guarantee) throws Throwable {
        final Guarantee.Status prevStatus = guarantee.getStatus();
        guarantee = (Guarantee) pjp.proceed();
        doGuaranteeStatusChangedNotification(guarantee, prevStatus);
        return guarantee;
    }

    @Around(value = "execution(* nl.strohalm.cyclos.services.elements.CommissionService.saveBrokerCommissionContract*(..)) && args(brokerCommissionContract)", argNames = "brokerCommissionContract")
    public BrokerCommissionContract newCommissionContractNotification(final ProceedingJoinPoint pjp, BrokerCommissionContract brokerCommissionContract) throws Throwable {
        final boolean isInsert = brokerCommissionContract.isTransient();

        brokerCommissionContract = (BrokerCommissionContract) pjp.proceed();

        // Send message if the contract is been inserted
        if (isInsert) {
            // Get message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = messageSettings.getNewCommissionContractSubject();
            final String body = messageSettings.getNewCommissionContractMessage();
            final String sms = messageSettings.getNewCommissionContractSms();

            // Process message content
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, brokerCommissionContract, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, brokerCommissionContract, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, brokerCommissionContract, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setEntity(brokerCommissionContract);
            message.setType(Message.Type.BROKERING);
            message.setToMember(brokerCommissionContract.getBrokering().getBrokered());
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }

        return brokerCommissionContract;

    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.TransferAuthorizationService.authorize*(..)) " +
            "|| execution(* nl.strohalm.cyclos.services.transactions.TransferAuthorizationService.deny*(..))", argNames = "transfer", returning = "transfer")
    public void paymentAuthorizedNotification(Transfer transfer) {
        transfer = fetchService.reload(transfer, Transfer.Relationships.AUTHORIZATIONS);
        if (CollectionUtils.isEmpty(transfer.getAuthorizations())) {
            return;
        }
        final AccountOwner fromOwner = transfer.getFromOwner();
        final AccountOwner toOwner = transfer.getToOwner();
        final List<Member> sendMessageTo = new ArrayList<Member>();
        final boolean loggedAsPayer = LoggedUser.isValid() && LoggedUser.isMember() && LoggedUser.accountOwner().equals(fromOwner);
        if (!loggedAsPayer && (fromOwner instanceof Member)) {
            sendMessageTo.add((Member) fromOwner);
        }
        final boolean loggedAsReceiver = LoggedUser.isValid() && LoggedUser.isMember() && LoggedUser.accountOwner().equals(toOwner);
        if (!loggedAsReceiver && (toOwner instanceof Member)) {
            sendMessageTo.add((Member) toOwner);
        }

        if (!sendMessageTo.isEmpty()) {
            // Get the message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            String subject;
            String body;
            String sms;
            switch (transfer.getStatus()) {
                case PROCESSED:
                    // Was authorized and processed
                    subject = messageSettings.getPendingPaymentAuthorizedSubject();
                    body = messageSettings.getPendingPaymentAuthorizedMessage();
                    sms = messageSettings.getPendingPaymentAuthorizedSms();
                    break;
                case PENDING:
                    // Was authorized but needs higher level
                    if (fromOwner instanceof Member) {
                        final Authorizer authorizer = transfer.getNextAuthorizationLevel().getAuthorizer();
                        final LocalSettings localSettings = settingsService.getLocalSettings();
                        final Member fromMember;
                        switch (authorizer) {
                            case BROKER:
                                // Notify the broker if he currently has to authorize
                                fromMember = fetchService.fetch((Member) fromOwner, Member.Relationships.BROKER);
                                final Member broker = fromMember.getBroker();
                                if (broker != null) {
                                    subject = messageSettings.getNewPendingPaymentByBrokerSubject();
                                    body = messageSettings.getNewPendingPaymentByBrokerMessage();
                                    sms = messageSettings.getNewPendingPaymentByBrokerSms();
                                    final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, fromMember, transfer);
                                    final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, fromMember, transfer);
                                    final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, fromMember, transfer);

                                    // Send the message
                                    final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
                                    message.setType(Message.Type.BROKERING);
                                    message.setEntity(transfer);
                                    message.setToMember(broker);
                                    message.setSubject(processedSubject);
                                    message.setBody(processedBody);
                                    message.setSms(processedSms);
                                    messageService.sendFromSystem(message);
                                }
                                break;
                            case PAYER:
                                // Notify the payer if he currently has to authorize
                                fromMember = (Member) fromOwner;
                                final Member toMember = (Member) toOwner;
                                subject = messageSettings.getNewPendingPaymentByPayerSubject();
                                body = messageSettings.getNewPendingPaymentByPayerMessage();
                                sms = messageSettings.getNewPendingPaymentByPayerSms();
                                final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, toMember, transfer);
                                final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, toMember, transfer);
                                final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, toMember, transfer);

                                // Send the message
                                final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
                                message.setType(Message.Type.PAYMENT);
                                message.setEntity(transfer);
                                message.setToMember(fromMember);
                                message.setSubject(processedSubject);
                                message.setBody(processedBody);
                                message.setSms(processedSms);
                                messageService.sendFromSystem(message);
                                break;
                        }
                    }
                    // If necessary, the message was already sent or the payment needs another authorization level
                    return;
                case DENIED:
                    // Was denied
                    subject = messageSettings.getPendingPaymentDeniedSubject();
                    body = messageSettings.getPendingPaymentDeniedMessage();
                    sms = messageSettings.getPendingPaymentDeniedSms();
                    break;
                default:
                    // Unknown status.
                    return;
            }

            // Send the messages
            sendPaymentMessages(transfer, sendMessageTo, subject, body, sms);
        }

        // Send the transaction feedback request message
        notifyTransactionFeedbackRequest(transfer);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.TransferAuthorizationService.cancel*(..))", argNames = "transfer", returning = "transfer")
    public void paymentCancelledNotification(final Transfer transfer) {
        final AccountOwner fromOwner = transfer.getFromOwner();
        final AccountOwner toOwner = transfer.getToOwner();
        final boolean loggedAsSender = LoggedUser.isValid() && LoggedUser.isMember() && LoggedUser.accountOwner().equals(fromOwner);
        final List<Member> sendMessageTo = new ArrayList<Member>();
        if (!loggedAsSender && (fromOwner instanceof Member)) {
            sendMessageTo.add((Member) fromOwner);
        }
        if (toOwner instanceof Member) {
            sendMessageTo.add((Member) toOwner);
        }

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getPendingPaymentCanceledSubject();
        final String body = messageSettings.getPendingPaymentCanceledMessage();
        final String sms = messageSettings.getPendingPaymentCanceledSms();

        // Send the messages
        sendPaymentMessages(transfer, sendMessageTo, subject, body, sms);
    }


    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.PaymentObligationService.changeStatus(..)) && args(paymentObligationId, newStatus)", argNames = "paymentObligationId, newStatus")
    public void paymentObligationPublishedNotification(final Long paymentObligationId, final PaymentObligation.Status newStatus) {
        // Load payment obligation
        final PaymentObligation paymentObligation = paymentObligationService.load(paymentObligationId, PaymentObligation.Relationships.SELLER);

        if (newStatus == PaymentObligation.Status.PUBLISHED) {
            // Get local and message settings
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final MessageSettings messageSettings = settingsService.getMessageSettings();

            // Notify the seller
            final Member toMember = paymentObligation.getSeller();
            final String subject = messageSettings.getPaymentObligationRegisteredSubject();
            final String body = messageSettings.getPaymentObligationRegisteredMessage();
            final String sms = messageSettings.getPaymentObligationRegisteredSms();

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, paymentObligation, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, paymentObligation, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, paymentObligation, localSettings);

            // Create the message DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setEntity(paymentObligation);
            message.setType(Message.Type.PAYMENT_OBLIGATION);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);
            message.setToMember(toMember);
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.guarantees.PaymentObligationService.reject(..)) && args(paymentObligationId)", argNames = "paymentObligationId")
    public void paymentObligationRejectedNotification(final Long paymentObligationId) {
        // Load payment obligation
        final PaymentObligation paymentObligation = paymentObligationService.load(paymentObligationId, PaymentObligation.Relationships.BUYER);

        // Get local and message settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final MessageSettings messageSettings = settingsService.getMessageSettings();

        // Notify the buyer
        final Member toMember = paymentObligation.getBuyer();
        final String subject = messageSettings.getPaymentObligationRejectedSubject();
        final String body = messageSettings.getPaymentObligationRejectedMessage();
        final String sms = messageSettings.getPaymentObligationRejectedSms();

        // Process message content
        final String processedSubject = MessageProcessingHelper.processVariables(subject, paymentObligation, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, paymentObligation, localSettings);
        final String processedSms = MessageProcessingHelper.processVariables(sms, paymentObligation, localSettings);

        // Create the message DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setEntity(paymentObligation);
        message.setType(Message.Type.PAYMENT_OBLIGATION);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);
        message.setToMember(toMember);
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "(execution(* nl.strohalm.cyclos.services.transactions.PaymentService.doPayment*(..)))", argNames = "payment", returning = "payment")
    public void paymentReceivedNotification(final Payment payment) {
        // A message is sent only when the payment is a transfer and the destination is a member (not system)
        MessageSettings messageSettings = settingsService.getMessageSettings();
        if (payment instanceof Transfer && payment.getToOwner() instanceof Member) {
            final Transfer transfer = (Transfer) payment;
            
            // Get the message settings
            String subject;
            String body;
            String sms;

            // Check if the transfer has been processed or awaits authorization
            if (transfer.getProcessDate() == null) {
                if (getAuthorizer(transfer) == Authorizer.RECEIVER) {
                    subject = messageSettings.getNewPendingPaymentByReceiverSubject();
                    body = messageSettings.getNewPendingPaymentByReceiverMessage();
                    sms = messageSettings.getNewPendingPaymentByReceiverSms();
                } else {
                    subject = messageSettings.getPendingPaymentReceivedSubject();
                    body = messageSettings.getPendingPaymentReceivedMessage();
                    sms = messageSettings.getPendingPaymentReceivedSms();
                }
            } else {
                subject = messageSettings.getPaymentReceivedSubject();
                body = messageSettings.getPaymentReceivedMessage();
                sms = messageSettings.getPaymentReceivedSms();
            }
            sendPaymentMessage(transfer, transfer.getTo(), transfer.getFrom(), subject, body, sms, Message.Type.PAYMENT);
            notifyBroker(transfer);

        }
        if (payment instanceof Transfer && payment.getFromOwner() instanceof Member) {
            final Transfer transfer = (Transfer) payment;
            sendPaymentMessage(transfer, transfer.getFrom(),transfer.getTo(), messageSettings.getPaymentSentSubject(),
                    messageSettings.getPaymentSentMessage(), messageSettings.getPaymentSentSms(), Message.Type.PAYMENT_SENT);

        }

        // Request a transaction feedback for the payment source
        notifyTransactionFeedbackRequest(payment);

        // Perform the low units notification, if needed
        notifyLowUnits(payment);

    }

    private Authorizer getAuthorizer(Transfer transfer) {
        final AuthorizationLevel nextAuthorizationLevel = transfer.getNextAuthorizationLevel();
        return nextAuthorizationLevel == null ? null : nextAuthorizationLevel.getAuthorizer();
    }

    private void notifyBroker(Transfer payment) {
        MessageSettings messageSettings = settingsService.getMessageSettings();
        LocalSettings localSettings = settingsService.getLocalSettings();
        Authorizer authorizer = getAuthorizer(payment);
        // Notify the broker
        final Member fromMember = fetchService.fetch(payment.isFromSystem() ? null : (Member) payment.getFromOwner(), Member.Relationships.BROKER);
        final Member broker = fromMember == null ? null : fromMember.getBroker();
        if (authorizer == Authorizer.BROKER && broker != null) {
            final String subject = messageSettings.getNewPendingPaymentByBrokerSubject();
            final String body = messageSettings.getNewPendingPaymentByBrokerMessage();
            final String sms = messageSettings.getNewPendingPaymentByBrokerSms();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, fromMember, payment);
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, fromMember, payment);
            final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, fromMember, payment);

            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.BROKERING);
            message.setEntity(payment);
            message.setToMember(broker);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }


    private void sendPaymentMessage(Transfer payment, Account receiver, Account otherSide, String subject, String body, String sms, 
                                    Message.Type type) {
        if (!payment.isRoot()) {
            return;
        }
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Get the transfer authorizer, if any
        final Authorizer authorizer = getAuthorizer(payment);

        // Get the destination
        final Member destinationMember = (Member) receiver.getOwner();
        final Set<MessageChannel> channels = preferenceService.receivedChannels(destinationMember, type);
        if (!channels.isEmpty()) {
            // The account status is only used in messages via SMS
            final boolean sendSmsNotification = payment.getType().isAllowSmsNotification() && channels.contains(MessageChannel.SMS);
            AccountStatus status = null;
            if (sendSmsNotification) {
                status = accountService.getStatus(new GetTransactionsDTO(receiver));
            }
            // Process message contents
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, otherSide.getOwner(), payment);
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, otherSide.getOwner(), payment);
            final String processedSms = sendSmsNotification ? MessageProcessingHelper.processVariables(sms, localSettings, otherSide.getOwner(), payment, status) : null;

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(type);
            message.setEntity(payment);
            message.setToMember(destinationMember);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }


    @AfterThrowing(pointcut = "execution(* nl.strohalm.cyclos.services.accounts.pos.MemberPosService.checkPin(..)) && args(member, pin, posId)", throwing = "ppbe", argNames = "ppbe, member, pin, posId")
    public void posPinBlockedNotification(final PosPinBlockedException ppbe, final Member member, final String pin, final Long posId) {

        // Get message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getPosPinBlockedSubject();
        final String body = messageSettings.getPosPinBlockedMessage();
        final String sms = messageSettings.getPosPinBlockedSms();

        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, member, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, member, localSettings);
        final String processedSms = MessageProcessingHelper.processVariables(sms, member, localSettings);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.ACCESS);
        message.setToMember(member);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }


    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ElementService.registerMember*(..)) && args(member, forceChange)", argNames = "member, forceChange")
    public void memberRegistered(final Member member, boolean forceChange) {
        sendRegistrationMessage(member);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ElementService.publicRegisterMember*(..)) && args(member, remoteAddress)", argNames = "member, remoteAddress")
    public void memberRegisteredMember(Member member, String remoteAddress) {
        sendRegistrationMessage(member);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ElementService.registerMemberByWebService*(..)) && args(*, member, *)", argNames = "member")
    public void memberRegisteredWS(Member member) {
        sendRegistrationMessage(member);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.tokens.TokenService.generateToken(..))", returning="transactionId", argNames = "transactionId")
    public void tokenGenerated(String transactionId) {
        Token token = tokenService.loadTokenByTransactionId(transactionId);
        tokenMessages().sendGenerateTokenMessages(token);
        //agent performs payment, so need to inform him separately
        //we do nolt send payment SMS
        if (token.isIfSendNotification()) {
            paymentReceivedNotification(token.getTransferFrom());
        }
    }
    
    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.tokens.TokenService.redeemToken(..)) && args(broker, tokenId, *, *)", argNames = "broker, tokenId")
    public void tokenRedeemed(Member broker, String tokenId) {
        tokenMessages().sendRedeemTokenMessages(broker, tokenService.loadTokenById(tokenId));
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.tokens.TokenService.senderRedeemToken(..)) && args(member, senderRedeemTokenData)", argNames = "member, senderRedeemTokenData")
    public void tokeRedeemedBySender(Member member, SenderRedeemTokenData senderRedeemTokenData) {
        sendRefundTokenMessage(senderRedeemTokenData);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.tokens.TokenService.resetPinToken(..)) && args(resetPinTokenData)", argNames = "resetPinTokenData")
    public void tokenPinResetByAdmin(ResetPinTokenData resetPinTokenData) {
        Token token = tokenService.loadTokenByTransactionId(resetPinTokenData.getTransactionId());
        tokenMessages().sendResetPinTokenMessages(token);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.tokens.TokenService.refundToken(..)) && args(member, senderRedeemTokenData)", argNames = "member, senderRedeemTokenData")
    public void tokenRefunded(Member member, SenderRedeemTokenData senderRedeemTokenData) {
        sendRefundTokenMessage(senderRedeemTokenData);
    }

    private void sendRefundTokenMessage(SenderRedeemTokenData senderRedeemTokenData) {
        Token token = tokenService.loadTokenByTransactionId(senderRedeemTokenData.getTransactionId());
        paymentReceivedNotification(token.getTransferTo());
    }

    @AfterReturning(pointcut = "(execution(* nl.strohalm.cyclos.services.transactions.InvoiceService.send*(..)) && args(invoice))", argNames = "invoice")
    public void receivedInvoiceNotification(final Invoice invoice) {
        // Get the destination
        final Member destinationMember = invoice.getToMember();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getInvoiceReceivedSubject();
        final String body = messageSettings.getInvoiceReceivedMessage();
        final String sms = messageSettings.getInvoiceReceivedSms();

        // Process message content
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, invoice.getFrom(), invoice);
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, invoice.getFrom(), invoice);
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, invoice.getFrom(), invoice);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.INVOICE);
        message.setEntity(invoice);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }
    

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ReferenceService.saveMemberReference(..)) ||" + "execution(* nl.strohalm.cyclos.services.elements.ReferenceService.saveMyReference(..))", argNames = "reference", returning = "reference")
    public void receivedReferenceNotification(final Reference reference) {
        // Get the destination
        final Member destinationMember = reference.getTo();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getReferenceReceivedSubject();
        final String body = messageSettings.getReferenceReceivedMessage();
        final String sms = messageSettings.getReferenceReceivedSms();

        // Process message contents
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Member fromMember = reference.getFrom();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, fromMember, reference);
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, fromMember, reference);
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, fromMember, reference);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.REFERENCE);
        message.setEntity(reference);
        message.setToMember(destinationMember);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @Before(value = "execution(* nl.strohalm.cyclos.services.elements.BrokeringService.changeBroker(..)) && args(dto)", argNames = "dto")
    public void removedBrokeringNotification(final ChangeBrokerDTO dto) {
        final Member member = dto.getMember();
        final Brokering oldBrokering = brokeringService.getActiveBrokering(member);
        final Member oldBroker = (oldBrokering == null) ? null : oldBrokering.getBroker();
        final Member newBroker = dto.getNewBroker();

        final boolean justSuspendCommission = (oldBroker != null && oldBroker.equals(newBroker) && dto.isSuspendCommission());
        if (!justSuspendCommission && oldBroker != null) {
            // Get message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = messageSettings.getBrokeringRemovedSubject();
            final String body = messageSettings.getBrokeringRemovedMessage();
            final String sms = messageSettings.getBrokeringRemovedSms();

            // Process message body
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, member, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, member, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, member, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.BROKERING);
            message.setToMember(oldBroker);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @Around(value = "execution(* nl.strohalm.cyclos.services.elements.ElementService.changeMemberGroup(..)) && args(member, newGroup, comments)", argNames = "member, newGroup, comments")
    public Member removedFromBrokerGroupNotification(final ProceedingJoinPoint pjp, Member member, final MemberGroup newGroup, final String comments) throws Throwable {
        member = (Member) elementService.load(member.getId(), Element.Relationships.GROUP);
        final MemberGroup oldGroup = member.getMemberGroup();
        member = (Member) pjp.proceed();

        // Send message to the ex-broker that he is a now a normal member
        if (oldGroup.isBroker() && newGroup.getStatus() != Group.Status.REMOVED && !newGroup.isBroker()) {
            // Get message settings
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = messageSettings.getRemovedFromBrokerGroupSubject();
            final String body = messageSettings.getRemovedFromBrokerGroupMessage();
            final String sms = messageSettings.getRemovedFromBrokerGroupSms();

            // Process message content
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final String processedSubject = MessageProcessingHelper.processVariables(subject, newGroup, localSettings);
            final String processedBody = MessageProcessingHelper.processVariables(body, newGroup, localSettings);
            final String processedSms = MessageProcessingHelper.processVariables(sms, newGroup, localSettings);

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.BROKERING);
            message.setToMember(member);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }

        // Send the return back to the caller
        return member;
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.PaymentService.processScheduled(..))", argNames = "transfer", returning = "transfer")
    public void scheduledPaymentProcessingAutomaticNotification(final Transfer transfer) {
        notifyScheduledPaymentProcessing(transfer, false);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.transactions.ScheduledPaymentService.processTransfer*(..))", argNames = "transfer", returning = "transfer")
    public void scheduledPaymentProcessingManualNotification(final Transfer transfer) {
        notifyScheduledPaymentProcessing(transfer, true);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.access.AccessService.changeMyPassword(..)) && args(params, remoteAddress)", argNames = "params, remoteAddress")
    public void changePassword(ChangeLoginPasswordDTO params, String remoteAddress) {
        if (params.getUser().getElement() instanceof Member) {
            sendChangePasswordMessage((Member) params.getUser().getElement());
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.access.AccessService.changeMemberCredentialsByWebService(..)) && args(memberUser, client, newCredentials)", argNames = "memberUser, client, newCredentials")
    public void changePasswordByWebService(MemberUser memberUser, ServiceClient client, String newCredentials) {
        sendChangePasswordMessage(memberUser.getMember());
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.access.AccessService.resetPasswordOnly(..)) && args(params)", argNames = "params")
    public void resetPasswordByAdmin(ChangeLoginPasswordDTO params) {
        changePasswordByAdmin(params);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.access.AccessService.changeMemberPassword(..)) && args(params)", argNames = "params")
    public void changePasswordByAdmin(ChangeLoginPasswordDTO params) {
        final MemberUser user = (MemberUser) fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        Member member = user.getMember();
        user.setLoginPassword(params.getNewPassword());
        MessageSettings messageSettings = settingsService.getMessageSettings();
        messageHelper().sendMemberMessage(messageSettings.getChangePasswordByAdminSubject(),
                messageSettings.getChangePasswordByAdminMessage(), messageSettings.getChangePasswordByAdminSms(),
                member, Message.Type.CHANGE_PASSWORD_BY_ADMIN, user);
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setBrokeringService(final BrokeringService brokeringService) {
        this.brokeringService = brokeringService;
    }

    public void setCertificationService(final CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setPaymentObligationService(final PaymentObligationService paymentObligationService) {
        this.paymentObligationService = paymentObligationService;
    }

    public void setPreferenceService(final PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTicketService(final TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void setSmsSender(SmsSender smsSender) {
        this.smsSender = smsSender;
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ReferenceService.saveTransactionFeedbackByAdmin(..))", returning = "transactionFeedback", argNames = "transactionFeedback")
    public void transactionFeedBackAdminCommentsNotification(final TransactionFeedback transactionFeedback) {
        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getTransactionFeedbackAdminCommentsSubject();
        final String body = messageSettings.getTransactionFeedbackAdminCommentsMessage();
        final String sms = messageSettings.getTransactionFeedbackAdminCommentsSms();

        // Process message body
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Send the notification to the feedback writer
        {
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());
            final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.TRANSACTION_FEEDBACK);
            message.setEntity(transactionFeedback);
            message.setToMember(transactionFeedback.getFrom());
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }

        // Send the notification to the feedback receiver
        {
            final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, transactionFeedback.getFrom(), transactionFeedback.getTransfer());
            final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, transactionFeedback.getFrom(), transactionFeedback.getTransfer());

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.TRANSACTION_FEEDBACK);
            message.setEntity(transactionFeedback);
            message.setToMember(transactionFeedback.getTo());
            message.setSubject(processedSubject);
            message.setBody(processedBody);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ReferenceService.saveTransactionFeedbackComments(..))", returning = "transactionFeedback", argNames = "transactionFeedback")
    public void transactionFeedBackReceivedNotification(final TransactionFeedback transactionFeedback) {
        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getTransactionFeedbackReceivedSubject();
        final String body = messageSettings.getTransactionFeedbackReceivedMessage();
        final String sms = messageSettings.getTransactionFeedbackReceivedSms();

        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Map<String, Object> extraVariables = new HashMap<String, Object>();
        final Payment payment = transactionFeedback.getPayment();
        final Calendar limit = payment.getType().getFeedbackReplyExpirationTime().add(Calendar.getInstance());
        extraVariables.put("limit", localSettings.getDateConverter().toString(limit));

        // Process the message
        String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, transactionFeedback.getFrom(), payment);
        processedSubject = MessageProcessingHelper.processVariables(processedSubject, extraVariables);
        String processedBody = MessageProcessingHelper.processVariables(body, localSettings, transactionFeedback.getFrom(), payment);
        processedBody = MessageProcessingHelper.processVariables(processedBody, extraVariables);
        String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, transactionFeedback.getFrom(), payment);
        processedSms = MessageProcessingHelper.processVariables(processedSms, extraVariables);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.TRANSACTION_FEEDBACK);
        message.setEntity(transactionFeedback);
        message.setToMember(transactionFeedback.getTo());
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    @AfterReturning(pointcut = "execution(* nl.strohalm.cyclos.services.elements.ReferenceService.saveTransactionFeedbackReplyComments(..))", returning = "transactionFeedback", argNames = "transactionFeedback")
    public void transactionFeedBackReplyNotification(final TransactionFeedback transactionFeedback) {
        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getTransactionFeedbackReplySubject();
        final String body = messageSettings.getTransactionFeedbackReplyMessage();
        final String sms = messageSettings.getTransactionFeedbackReplySms();

        // Process message body
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());
        final String processedBody = MessageProcessingHelper.processVariables(body, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());
        final String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, transactionFeedback.getTo(), transactionFeedback.getTransfer());

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.TRANSACTION_FEEDBACK);
        message.setEntity(transactionFeedback);
        message.setToMember(transactionFeedback.getFrom());
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    private void certificationStatusChangedNotification(final Certification certification) {

        // Only notify if it's an activation or suspension, SHCEDULED is none of these
        if (certification.getStatus() == Certification.Status.SCHEDULED) {
            return;
        }

        // Get local and message settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final MessageSettings messageSettings = settingsService.getMessageSettings();

        // Process new status string
        final String statusString = messageResolver.message("certification.status." + certification.getStatus().toString());
        final Map<String, Object> variables = certification.getVariableValues(localSettings);
        variables.put("status", statusString);

        // Get the destination (buyer)
        final Member buyer = certification.getBuyer();

        // Get the message settings (buyer)
        final String subjectBuyer = messageSettings.getCertificationStatusChangedSubject();
        final String bodyBuyer = messageSettings.getCertificationStatusChangedMessage();
        final String smsBuyer = messageSettings.getCertificationStatusChangedSms();

        // Process message content (buyer)
        final String processedSubjectBuyer = MessageProcessingHelper.processVariables(subjectBuyer, variables);
        final String processedBodyBuyer = MessageProcessingHelper.processVariables(bodyBuyer, variables);
        final String processedSmsBuyer = MessageProcessingHelper.processVariables(smsBuyer, variables);

        // Create the DTO (buyer)
        final SendMessageFromSystemDTO messageToBuyer = new SendMessageFromSystemDTO();
        messageToBuyer.setEntity(certification);
        messageToBuyer.setType(Message.Type.CERTIFICATION);
        messageToBuyer.setToMember(buyer);
        messageToBuyer.setSubject(processedSubjectBuyer);
        messageToBuyer.setBody(processedBodyBuyer);
        messageToBuyer.setSms(processedSmsBuyer);

        // Send the message (to buyer)
        messageService.sendFromSystem(messageToBuyer);

        // If the new status is "EXPIRED" notify the issuer too
        if (certification.getStatus() == Certification.Status.EXPIRED) {

            // Get the destination (issuer)
            final Member issuer = certification.getIssuer();

            // Get the message settings (issuer)
            final String subjectIssuer = messageSettings.getExpiredCertificationSubject();
            final String bodyIssuer = messageSettings.getExpiredCertificationMessage();
            final String smsIssuer = messageSettings.getExpiredCertificationSms();

            // Process message content (issuer)
            final String processedSubjectIssuer = MessageProcessingHelper.processVariables(subjectIssuer, certification, localSettings);
            final String processedBodyIssuer = MessageProcessingHelper.processVariables(bodyIssuer, certification, localSettings);
            final String processedSmsIssuer = MessageProcessingHelper.processVariables(smsIssuer, certification, localSettings);

            // Create the DTO (issuer)
            final SendMessageFromSystemDTO messageToIssuer = new SendMessageFromSystemDTO();
            messageToIssuer.setEntity(certification);
            messageToIssuer.setType(Message.Type.CERTIFICATION);
            messageToIssuer.setToMember(issuer);
            messageToIssuer.setSubject(processedSubjectIssuer);
            messageToIssuer.setBody(processedBodyIssuer);
            messageToIssuer.setSms(processedSmsIssuer);

            // Send the message (to buyer)
            messageService.sendFromSystem(messageToIssuer);
        }
    }

    private void doGuaranteeStatusChangedNotification(final Guarantee guarantee, final Guarantee.Status prevStatus) {
        // Get local and message settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final MessageSettings messageSettings = settingsService.getMessageSettings();

        // Process new status string
        final String statusString = messageResolver.message("guarantee.status." + guarantee.getStatus().toString());
        final Map<String, Object> variables = guarantee.getVariableValues(localSettings);
        variables.put("status", statusString);

        final Guarantee.Status newStatus = guarantee.getStatus();

        // If the guarantee was accepted, denied or cancelled, notify members
        if (newStatus == Guarantee.Status.ACCEPTED || newStatus == Guarantee.Status.REJECTED || newStatus == Guarantee.Status.CANCELLED) {

            // Check if the model of the guarantee is "buyer only"
            final boolean buyerOnly = guarantee.getGuaranteeType().getModel() == GuaranteeType.Model.WITH_BUYER_ONLY;

            // Get the destination
            final Member buyer = guarantee.getBuyer();
            final Member seller = guarantee.getSeller();
            final Member issuer = guarantee.getIssuer();

            // Get the message settings
            String subject = null;
            String body = null;
            String sms = null;
            if (buyerOnly) {
                subject = messageSettings.getBuyerOnlyGuaranteeStatusChangedSubject();
                body = messageSettings.getBuyerOnlyGuaranteeStatusChangedMessage();
                sms = messageSettings.getBuyerOnlyGuaranteeStatusChangedSms();
            } else {
                subject = messageSettings.getGuaranteeStatusChangedSubject();
                body = messageSettings.getGuaranteeStatusChangedMessage();
                sms = messageSettings.getGuaranteeStatusChangedSms();
            }

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, variables);
            final String processedBody = MessageProcessingHelper.processVariables(body, variables);
            final String processedSms = MessageProcessingHelper.processVariables(sms, variables);

            // Create the message DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setEntity(guarantee);
            message.setType(Message.Type.GUARANTEE);

            // Send the message to the buyer
            message.setToMember(buyer);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            messageService.sendFromSystem(message);

            // If the guarantee was cancelled, notify the issuer too
            if (newStatus == Guarantee.Status.CANCELLED) {
                message.setToMember(issuer);
                message.setSubject(processedSubject);
                message.setBody(processedBody);
                messageService.sendFromSystem(message);
            }

            // If the model is not "buyer only", notify the seller too
            if (!buyerOnly) {
                message.setToMember(seller);
                message.setSubject(processedSubject);
                message.setBody(processedBody);
                messageService.sendFromSystem(message);
            }
        } else if (newStatus == Guarantee.Status.WITHOUT_ACTION && prevStatus == Guarantee.Status.PENDING_ISSUER) { // If the guarantee has expired,
            // Notify the issuer
            // Get the destination
            final Member issuer = guarantee.getIssuer();

            // Get the message settings
            final String subject = messageSettings.getExpiredGuaranteeSubject();
            final String body = messageSettings.getExpiredGuaranteeMessage();
            final String sms = messageSettings.getExpiredGuaranteeSms();

            // Process message content
            final String processedSubject = MessageProcessingHelper.processVariables(subject, variables);
            final String processedBody = MessageProcessingHelper.processVariables(body, variables);
            final String processedSms = MessageProcessingHelper.processVariables(sms, variables);

            // Create the message DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setEntity(guarantee);
            message.setType(Message.Type.GUARANTEE);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setToMember(issuer);
            message.setSms(processedSms);

            // Send the message to the issuer
            messageService.sendFromSystem(message);
        }
    }

    /**
     * Notify the owner of the given account if it is getting low units. In a separated method as it need to be synchronized
     */
    private synchronized void doSendLowUnitsNotification(final MemberAccount account, final MemberGroupAccountSettings mgas) {
        final Calendar currentDate = DateHelper.truncate(Calendar.getInstance());
        if (lastPaymentDate == null || !lastPaymentDate.equals(currentDate)) {
            lastPaymentDate = currentDate;
            sentLowUnits.clear();
        }
        final Member fromOwner = account.getOwner();
        final BigDecimal lowUnits = mgas.getLowUnits() == null ? BigDecimal.ZERO : mgas.getLowUnits();
        final BigDecimal availableBalance = accountService.getStatus(new GetTransactionsDTO(account)).getAvailableBalance();
        final BigDecimal creditLimit = account.getCreditLimit();
        final Long memberId = fromOwner.getId();
        // ... check if the balance is smaller than low units, and ...
        if (availableBalance.add(creditLimit.abs()).compareTo(lowUnits) != 1) {
            // ... send the personal message only once a day (controlled by the sentLowUnits set)
            if (!sentLowUnits.contains(memberId)) {
                // Get the message settings
                final MessageSettings messageSettings = settingsService.getMessageSettings();
                final String subject = messageSettings.getLowUnitsSubject();
                final String body = messageSettings.getLowUnitsMessage();
                final String sms = messageSettings.getLowUnitsSms();

                // Process message content
                final LocalSettings localSettings = settingsService.getLocalSettings();
                final UnitsConverter converter = localSettings.getUnitsConverter(account.getType().getCurrency().getPattern());
                final Map<String, Object> variables = new HashMap<String, Object>();
                variables.putAll(fromOwner.getVariableValues(localSettings));
                variables.putAll(account.getVariableValues(localSettings));
                variables.put("balance", converter.toString(availableBalance));
                final String processedSubject = MessageProcessingHelper.processVariables(subject, variables);
                final String processedBody = MessageProcessingHelper.processVariables(body, variables);
                final String processedSms = MessageProcessingHelper.processVariables(sms, variables);

                // Create the DTO
                final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
                message.setType(Message.Type.ACCOUNT);
                message.setToMember(fromOwner);
                message.setSubject(processedSubject);
                message.setBody(processedBody);
                message.setSms(processedSms);

                // Send the message
                messageService.sendFromSystem(message);

                // Update table that controls duplicates
                sentLowUnits.add(memberId);
            }
        } else {
            sentLowUnits.remove(memberId);
        }
    }

    private void externalChannelPaymentNotification(final Payment payment, final Channel fromChannel, final Channel toChannel) {
        // Get local and message settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final boolean isPaymentConfirmation = toChannel != null;

        final Channel smsChannel = channelService.getSmsChannel();

        // if it isn't then is a payment from System
        if (payment.getFromOwner() instanceof Member) {
            final Channel channelToCheckForPayer = isPaymentConfirmation ? toChannel : fromChannel;
            final boolean skipSms = channelToCheckForPayer != null && channelToCheckForPayer.equals(smsChannel);

            // Get the origin
            final Member fromMember = (Member) payment.getFromOwner();

            // Get the message settings
            final String subject = messageSettings.getExternalChannelPaymentPerformedSubject();
            final String body = messageSettings.getExternalChannelPaymentPerformedMessage();
            final String sms = skipSms ? null : messageSettings.getExternalChannelPaymentPerformedSms();

            // Process message content
            final Map<String, Object> variableValues = payment.getVariableValues(localSettings);
            variableValues.put("channel", channelToCheckForPayer.getDisplayName());
            final AccountOwner toOwner = payment.getToOwner();
            String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, toOwner, payment);
            processedSubject = MessageProcessingHelper.processVariables(processedSubject, variableValues);
            String processedBody = MessageProcessingHelper.processVariables(body, localSettings, toOwner, payment);
            processedBody = MessageProcessingHelper.processVariables(processedBody, variableValues);
            String processedSms = null;
            if (sms != null) {
                processedSms = MessageProcessingHelper.processVariables(sms, localSettings, toOwner, payment);
                processedSms = MessageProcessingHelper.processVariables(processedSms, variableValues);
            }

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setToMember(fromMember);
            message.setType(Message.Type.EXTERNAL_PAYMENT);
            message.setEntity(payment);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
        // Now, notify the member that received the payment, only if not to channel is SMS
        final Channel channelToCheckForReceiver = isPaymentConfirmation ? fromChannel : toChannel;
        if (channelToCheckForReceiver == null || !channelToCheckForReceiver.equals(smsChannel)) {
            paymentReceivedNotification(payment);
        }
    }

    /**
     * Send the low units notification if needed
     */
    private void notifyLowUnits(final Payment payment) {
        if (!(payment instanceof Transfer)) {
            return;
        }
        final Account account = fetchService.fetch(payment.getFrom(), RelationshipHelper.nested(Account.Relationships.TYPE, AccountType.Relationships.CURRENCY), RelationshipHelper.nested(Transfer.Relationships.FROM, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP));
        if (!(account instanceof MemberAccount)) {
            return;
        }
        final MemberAccount memberAccount = (MemberAccount) account;
        final Group group = memberAccount.getMember().getGroup();
        final AccountType accountType = account.getType();
        final MemberGroupAccountSettings mgas = groupService.loadAccountSettings(group.getId(), accountType.getId());
        final BigDecimal lowUnits = mgas.getLowUnits() == null ? BigDecimal.ZERO : mgas.getLowUnits();
        // If low units message is used...
        if (lowUnits.floatValue() > PRECISION_DELTA && StringUtils.isNotEmpty(mgas.getLowUnitsMessage())) {
            doSendLowUnitsNotification(memberAccount, mgas);
        }
    }

    private void notifyScheduledPaymentProcessing(final Transfer transfer, final boolean manual) {
        // Get the required data
        final Member payer = transfer.isFromSystem() ? null : (Member) transfer.getFrom().getOwner();
        final Member payee = transfer.isToSystem() ? null : (Member) transfer.getTo().getOwner();
        final Authorizer authorizer = getAuthorizer(transfer);
        final MessageSettings messageSettings = settingsService.getMessageSettings();

        // Resolve the message
        String payerSubject;
        String payerBody;
        String payerSms;
        String payeeSubject;
        String payeeBody;
        String payeeSms;
        switch (transfer.getStatus()) {
            case PROCESSED:
                // Don't notify the payer when he manually pays
                if (manual) {
                    payerSubject = null;
                    payerBody = null;
                    payerSms = null;
                } else {
                    payerSubject = messageSettings.getScheduledPaymentProcessedSubject();
                    payerBody = messageSettings.getScheduledPaymentProcessedMessage();
                    payerSms = messageSettings.getScheduledPaymentProcessedSms();
                }
                payeeSubject = messageSettings.getPaymentReceivedSubject();
                payeeBody = messageSettings.getPaymentReceivedMessage();
                payeeSms = messageSettings.getPaymentReceivedSms();
                break;
            case PENDING:
                // Payer don't get notified
                payerSubject = null;
                payerBody = null;
                payerSms = null;
                // Check whether the payee should authorize
                if (authorizer == Authorizer.RECEIVER) {
                    payeeSubject = messageSettings.getNewPendingPaymentByReceiverSubject();
                    payeeBody = messageSettings.getNewPendingPaymentByReceiverMessage();
                    payeeSms = messageSettings.getNewPendingPaymentByReceiverSms();
                } else {
                    payeeSubject = messageSettings.getPendingPaymentReceivedSubject();
                    payeeBody = messageSettings.getPendingPaymentReceivedMessage();
                    payeeSms = messageSettings.getPendingPaymentReceivedSms();

                }
                break;
            case FAILED:
                payerSubject = messageSettings.getScheduledPaymentFailedToPayerSubject();
                payerBody = messageSettings.getScheduledPaymentFailedToPayerMessage();
                payerSms = messageSettings.getScheduledPaymentFailedToPayerSms();
                try {
                    final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
                    invoiceService.loadByPayment(scheduledPayment == null ? transfer : scheduledPayment);
                    payeeSubject = messageSettings.getScheduledPaymentFailedToPayeeSubject();
                    payeeBody = messageSettings.getScheduledPaymentFailedToPayeeMessage();
                    payeeSms = messageSettings.getScheduledPaymentFailedToPayeeSms();
                } catch (final EntityNotFoundException e) {
                    // Don't send message to payee when there's no associated invoice
                    payeeSubject = null;
                    payeeBody = null;
                    payeeSms = null;
                }
                break;
            default:
                // Unknown status here!!!
                return;
        }

        // Prepare the message
        final LocalSettings localSettings = settingsService.getLocalSettings();
        SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.PAYMENT);
        message.setEntity(transfer);

        // Send the message to the payer
        final Set<MessageChannel> payerChannels = payer == null || payerSubject == null ? null : preferenceService.receivedChannels(payer, Message.Type.PAYMENT);
        if (CollectionUtils.isNotEmpty(payerChannels)) {
            AccountStatus statusPayer = null;
            final boolean sendSmsNotification = transfer.getType().isAllowSmsNotification() && payerChannels.contains(MessageChannel.SMS);
            if (sendSmsNotification) {
                statusPayer = accountService.getStatus(new GetTransactionsDTO(transfer.getFrom()));
            }
            final AccountOwner payeeOwner = payee == null ? SystemAccountOwner.instance() : payee;
            message.setToMember(payer);
            message.setSubject(MessageProcessingHelper.processVariables(payerSubject, localSettings, payeeOwner, transfer));
            message.setBody(MessageProcessingHelper.processVariables(payerBody, localSettings, payeeOwner, transfer));
            if (sendSmsNotification) {
                message.setSms(MessageProcessingHelper.processVariables(payerSms, localSettings, payeeOwner, transfer, statusPayer));
            }
            messageService.sendFromSystem(message);
        }

        // Send the message to the payee
        final Set<MessageChannel> payeeChannels = payee == null || payeeSubject == null ? null : preferenceService.receivedChannels(payee, Message.Type.PAYMENT);
        if (CollectionUtils.isNotEmpty(payeeChannels)) {
            AccountStatus statusPayee = null;
            final boolean sendSmsNotification = transfer.getType().isAllowSmsNotification() && payeeChannels.contains(MessageChannel.SMS);
            if (sendSmsNotification) {
                statusPayee = accountService.getStatus(new GetTransactionsDTO(transfer.getTo()));
            }
            final AccountOwner payerOwner = payer == null ? SystemAccountOwner.instance() : payer;
            message.setToMember(payee);
            message.setSubject(MessageProcessingHelper.processVariables(payeeSubject, localSettings, payerOwner, transfer));
            message.setBody(MessageProcessingHelper.processVariables(payeeBody, localSettings, payerOwner, transfer));
            if (sendSmsNotification) {
                message.setSms(MessageProcessingHelper.processVariables(payeeSms, localSettings, payerOwner, transfer, statusPayee));
            }
            messageService.sendFromSystem(message);
        }

        // Notify the broker
        final Member broker = payer == null ? null : payer.getBroker();
        if (authorizer == Authorizer.BROKER && broker != null) {
            final String subject = messageSettings.getNewPendingPaymentByBrokerSubject();
            final String body = messageSettings.getNewPendingPaymentByBrokerMessage();
            final String sms = messageSettings.getNewPendingPaymentByBrokerSms();

            // Send the message
            message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.BROKERING);
            message.setEntity(transfer);
            message.setToMember(broker);
            message.setSubject(MessageProcessingHelper.processVariables(subject, localSettings, payer, transfer));
            message.setBody(MessageProcessingHelper.processVariables(body, localSettings, payer, transfer));
            message.setSms(MessageProcessingHelper.processVariables(sms, localSettings, payer, transfer));
            messageService.sendFromSystem(message);
        }

        // Send a low units notification, if needed
        notifyLowUnits(transfer);
    }

    private void notifyTransactionFeedbackRequest(Payment payment) {
        // Payments from/to system, that hasn't been authorized yet or that don't use feedbacks are not notified
        payment = fetchService.fetch(payment, Transfer.Relationships.FROM, Transfer.Relationships.TO, Transfer.Relationships.TYPE);
        if (payment == null || payment.isFromSystem() || payment.isToSystem() || !payment.getType().isRequiresFeedback()) {
            return;
        }
        // Transfers without process dates are skipped too
        if (payment instanceof Transfer) {
            final Transfer transfer = (Transfer) payment;
            if (transfer.getProcessDate() == null) {
                return;
            }
        }
        final Member from = (Member) payment.getFromOwner();
        final Member to = (Member) payment.getToOwner();

        // Get the message settings
        final MessageSettings messageSettings = settingsService.getMessageSettings();
        final String subject = messageSettings.getTransactionFeedbackRequestSubject();
        final String body = messageSettings.getTransactionFeedbackRequestMessage();
        final String sms = messageSettings.getTransactionFeedbackRequestSms();

        // Process message body
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Map<String, Object> extraVariables = new HashMap<String, Object>();
        final Calendar limit = payment.getType().getFeedbackExpirationTime().add(Calendar.getInstance());
        extraVariables.put("limit", localSettings.getDateConverter().toString(limit));

        String processedSubject = MessageProcessingHelper.processVariables(subject, localSettings, to, payment);
        String processedBody = MessageProcessingHelper.processVariables(body, localSettings, to, payment);
        String processedSms = MessageProcessingHelper.processVariables(sms, localSettings, to, payment);

        processedSubject = MessageProcessingHelper.processVariables(processedSubject, extraVariables);
        processedBody = MessageProcessingHelper.processVariables(processedBody, extraVariables);
        processedSms = MessageProcessingHelper.processVariables(processedSms, extraVariables);

        // Create the DTO
        final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
        message.setType(Message.Type.TRANSACTION_FEEDBACK);
        message.setEntity(new TransactionFeedbackRequest(payment));
        message.setToMember(from);
        message.setSubject(processedSubject);
        message.setBody(processedBody);
        message.setSms(processedSms);

        // Send the message
        messageService.sendFromSystem(message);
    }

    private void sendPaymentMessages(final Transfer transfer, final List<Member> sendMessageTo, final String subject, final String body, final String sms) {
        // Process message contents
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String processedSubject = MessageProcessingHelper.processVariables(subject, transfer, localSettings);
        final String processedBody = MessageProcessingHelper.processVariables(body, transfer, localSettings);
        final String processedSms = transfer.getType().isAllowSmsNotification() ? MessageProcessingHelper.processVariables(sms, transfer, localSettings) : null;

        // Send each message
        for (final Member member : sendMessageTo) {

            // Create the DTO
            final SendMessageFromSystemDTO message = new SendMessageFromSystemDTO();
            message.setType(Message.Type.PAYMENT);
            message.setEntity(transfer);
            message.setToMember(member);
            message.setSubject(processedSubject);
            message.setBody(processedBody);
            message.setSms(processedSms);

            // Send the message
            messageService.sendFromSystem(message);
        }
    }

    private void sendRegistrationMessage(Member member) {
        MessageSettings messageSettings = settingsService.getMessageSettings();
        messageHelper().sendMemberMessage(messageSettings.getRegisteredSubject(), messageSettings.getRegisteredMessage(), messageSettings.getRegisteredSms(),
                member, Message.Type.REGISTRATION);
    }

    private void sendChangePasswordMessage(Member member) {
        MessageSettings messageSettings = settingsService.getMessageSettings();
        messageHelper().sendMemberMessage(messageSettings.getChangePasswordSubject(), messageSettings.getChangePasswordMessage(), messageSettings.getChangePasswordSms(),
                member, Message.Type.CHANGE_PASSWORD);

    }

    MessageHelper messageHelper() {
        if (messageHelper == null) {
            messageHelper = new MessageHelper(settingsService, messageService );
        }
        return messageHelper;
    }

    TokenMessages tokenMessages() {
        if (tokenMessages == null) {
            tokenMessages = new TokenMessages(settingsService, smsSender, messageService, accountService);
        }
        return tokenMessages;
    }

}