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
package mp.platform.cyclone.webservices.payments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jws.WebService;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentRequestTicket;
import nl.strohalm.cyclos.entities.accounts.transactions.Ticket;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.services.ServiceOperation;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.transactions.DoExternalPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TicketService;
import nl.strohalm.cyclos.services.transactions.TransferAuthorizationService;
import nl.strohalm.cyclos.services.transactions.exceptions.InvalidChannelException;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.WebServiceFaultsEnum;
import mp.platform.cyclone.webservices.model.AccountHistoryTransferVO;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;
import mp.platform.cyclone.webservices.utils.server.AccountHelper;
import mp.platform.cyclone.webservices.utils.server.ChannelHelper;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;
import mp.platform.cyclone.webservices.utils.server.PaymentHelper;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation for payment web service
 * @author luis
 */
@WebService(name = "payments", serviceName = "payments")
public class PaymentWebServiceImpl implements PaymentWebService {

    private static class PrepareParametersResult {
        private final PaymentStatus status;
        private final AccountOwner  from;

        private final AccountOwner  to;

        public PrepareParametersResult(final PaymentStatus status, final AccountOwner from, final AccountOwner to) {
            this.status = status;
            this.from = from;
            this.to = to;
        }

        public AccountOwner getFrom() {
            return from;
        }

        public PaymentStatus getStatus() {
            return status;
        }

        public AccountOwner getTo() {
            return to;
        }
    }

    /**
     * Common interface used by chargeback / reverse
     * @author andres
     * @param <V>
     */
    private interface TransferLoader<V> {
        Transfer load(V v);
    }

    private AccountService               accountService;
    private AccessService                accessService;
    private FetchService                 fetchService;
    private PaymentService               paymentService;
    private TicketService                ticketService;
    private TransferAuthorizationService transferAuthorizationService;
    private PaymentHelper                paymentHelper;
    private MemberHelper                 memberHelper;
    private WebServiceHelper             webServiceHelper;
    private AccountHelper                accountHelper;

    private ChannelHelper                channelHelper;

    public ChargebackResult chargeback(final Long transferId) {
        return reverse(transferId, new TransferLoader<Long>() {
            public Transfer load(final Long transferId) {
                return paymentService.load(transferId);
            }
        });
    }

    public PaymentResult confirmPayment(final ConfirmPaymentParameters params) {
        Exception errorException = null;
        AccountStatus fromMemberStatus = null;
        AccountStatus toMemberStatus = null;
        Member fromMember = null;
        Member toMember = null;

        // It's nonsense to use this if restricted to a member
        if (WebServiceContext.getMember() != null) {
            throw new PermissionDeniedException();
        }
        final Channel channel = WebServiceContext.getChannel();
        final String channelName = channel == null ? null : channel.getInternalName();

        PaymentStatus status = null;
        AccountHistoryTransferVO transferVO = null;

        // Get the ticket
        PaymentRequestTicket ticket = null;
        try {
            // Check that the ticket is valid
            final Ticket t = ticketService.load(params.getTicket());
            fromMember = t.getFrom();
            toMember = t.getTo();

            if (!(t instanceof PaymentRequestTicket) || t.getStatus() != Ticket.Status.PENDING) {
                throw new Exception("Invalid ticket and/or status: " + t.getClass().getName() + ", status: " + t.getStatus());
            }
            // Check that the channel is the expected one
            ticket = (PaymentRequestTicket) t;
            if (!ticket.getToChannel().getInternalName().equals(channelName)) {
                throw new Exception("The ticket's destination channel is not the expected one (expected=" + channelName + "): " + ticket.getToChannel().getInternalName());
            }
        } catch (final Exception e) {
            errorException = e;
            status = PaymentStatus.INVALID_PARAMETERS;
        }

        // Validate the Channel and credentials
        Member member = null;
        if (status == null) {
            member = ticket.getFrom();
            if (!accessService.isChannelEnabledForMember(channelName, member)) {
                status = PaymentStatus.INVALID_CHANNEL;
            }
            if (status == null && WebServiceContext.getClient().isCredentialsRequired()) {
                try {
                    checkCredentials(member, channel, params.getCredentials());
                } catch (final InvalidCredentialsException e) {
                    status = PaymentStatus.INVALID_CREDENTIALS;
                } catch (final BlockedCredentialsException e) {
                    status = PaymentStatus.BLOCKED_CREDENTIALS;
                }
            }
        }
        // Confirm the payment
        if (status == null) {
            try {
                final Transfer transfer = (Transfer) paymentService.confirmPayment(ticket.getTicket());
                transferVO = accountHelper.toVO(member, transfer, null);
                status = paymentHelper.toStatus(transfer);

                if (WebServiceContext.getClient().getPermissions().contains(ServiceOperation.ACCOUNT_DETAILS)) {
                    if (WebServiceContext.getMember() == null) {
                        fromMemberStatus = accountService.getStatus(new GetTransactionsDTO(fromMember, transfer.getFrom().getType()));
                        toMemberStatus = accountService.getStatus(new GetTransactionsDTO(toMember, transfer.getTo().getType()));
                    } else if (WebServiceContext.getMember().equals(fromMember)) {
                        fromMemberStatus = accountService.getStatus(new GetTransactionsDTO(fromMember, transfer.getFrom().getType()));
                    } else {
                        toMemberStatus = accountService.getStatus(new GetTransactionsDTO(toMember, transfer.getTo().getType()));
                    }
                }
            } catch (final Exception e) {
                errorException = e;
                status = paymentHelper.toStatus(e);
            }
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error(errorException != null ? errorException : new Exception("Confirm payment status: " + status));
        }
        // Build the result
        return new PaymentResult(status, transferVO, accountHelper.toVO(fromMemberStatus), accountHelper.toVO(toMemberStatus));
    }

    public List<ChargebackResult> doBulkChargeback(final List<Long> transfersIds) {
        return doBulkChargeback(transfersIds, new TransferLoader<Long>() {
            public Transfer load(final Long transfersId) {
                return paymentService.load(transfersId);
            }
        });
    }

    public List<PaymentResult> doBulkPayment(final List<PaymentParameters> params) {
        final List<PaymentResult> results = new ArrayList<PaymentResult>(params == null ? 0 : params.size());
        if (params != null) {
            // We should lock at once all from accounts for all payments, but only if all accounts are passed ok
            boolean hasError = false;
            final List<AccountDTO> allAccounts = new ArrayList<AccountDTO>();
            for (int i = 0; i < params.size(); i++) {
                final PaymentParameters param = params.get(i);
                final PrepareParametersResult result = prepareParameters(param);

                if (result.getStatus() == null) {
                    try {
                        final DoExternalPaymentDTO dto = paymentHelper.toExternalPaymentDTO(param, result.getFrom(), result.getTo());
                        if (!validateTransferType(dto)) {
                            results.add(new PaymentResult(PaymentStatus.INVALID_PARAMETERS, null));
                            webServiceHelper.error("The specified transfer type is invalid: " + dto.getTransferType());
                            hasError = true;
                        } else {
                            allAccounts.add(new AccountDTO(result.getFrom(), dto.getTransferType().getFrom()));
                            results.add(new PaymentResult(PaymentStatus.NOT_PERFORMED, null));
                        }
                    } catch (final Exception e) {
                        webServiceHelper.error(e);
                        hasError = true;
                        results.add(new PaymentResult(paymentHelper.toStatus(e), null));
                    }
                } else {
                    hasError = true;
                    results.add(new PaymentResult(result.getStatus(), null));
                    webServiceHelper.error("Bulk payment validation status [" + i + "]: " + result.getStatus());
                }
            }
            if (!hasError) {
                // No validation error. Lock all accounts and perform each payment
                try {
                    accountService.lockAccounts(true, allAccounts);
                } catch (final Exception e) {
                    WebServiceFaultsEnum.CURRENTLY_UNAVAILABLE.throwFault(e);
                }
                for (int i = 0; i < params.size(); i++) {
                    final PaymentParameters param = params.get(i);
                    PaymentResult result;
                    if (hasError) {
                        result = new PaymentResult(PaymentStatus.NOT_PERFORMED, null);
                    } else {
                        result = doPayment(param);
                        if (!result.getStatus().isSuccessful()) {
                            hasError = true;
                        }
                    }
                    results.set(i, result);
                }
            }
        }
        return results;
    }

    public List<ChargebackResult> doBulkReverse(final List<String> traces) {
        return doBulkChargeback(traces, new TransferLoader<String>() {
            public Transfer load(final String traceNumber) {
                return paymentService.loadTransferByTraceNumber(traceNumber, WebServiceContext.getClient().getId());
            }
        });
    }

    public PaymentResult doPayment(final PaymentParameters params) {
        AccountHistoryTransferVO transferVO = null;
        PaymentStatus status;
        AccountStatus fromMemberStatus = null;
        AccountStatus toMemberStatus = null;
        try {
            final PrepareParametersResult result = prepareParameters(params);
            status = result.getStatus();

            if (status == null) {
                // Status null means no "pre-payment" errors (like validation, pin, channel...)
                // Perform the payment
                final DoExternalPaymentDTO dto = paymentHelper.toExternalPaymentDTO(params, result.getFrom(), result.getTo());

                // Validate the transfer type
                if (!validateTransferType(dto)) {
                    status = PaymentStatus.INVALID_PARAMETERS;
                    webServiceHelper.trace(status + ". Reason: The service client doesn't have permission to the specified transfer type: " + dto.getTransferType());
                } else {
                    final Transfer transfer = (Transfer) paymentService.insertExternalPayment(dto);
                    status = paymentHelper.toStatus(transfer);
                    transferVO = accountHelper.toVO(WebServiceContext.getMember(), transfer, null);

                    if (WebServiceContext.getClient().getPermissions().contains(ServiceOperation.ACCOUNT_DETAILS) && params.isReturnStatus()) {
                        if (WebServiceContext.getMember() == null) {
                            fromMemberStatus = accountService.getStatus(new GetTransactionsDTO(dto.getFrom(), dto.getTransferType().getFrom()));
                            toMemberStatus = accountService.getStatus(new GetTransactionsDTO(dto.getTo(), dto.getTransferType().getTo()));
                        } else if (WebServiceContext.getMember().equals(paymentHelper.resolveFromMember(params))) {
                            fromMemberStatus = accountService.getStatus(new GetTransactionsDTO(dto.getFrom(), dto.getTransferType().getFrom()));
                        } else {
                            toMemberStatus = accountService.getStatus(new GetTransactionsDTO(dto.getTo(), dto.getTransferType().getTo()));
                        }
                    }
                }
            }
        } catch (final Exception e) {
            webServiceHelper.error(e);
            status = paymentHelper.toStatus(e);
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error("Payment status: " + status);
        }

        return new PaymentResult(status, transferVO, accountHelper.toVO(fromMemberStatus), accountHelper.toVO(toMemberStatus));
    }

    public boolean expireTicket(final String ticketStr) {
        try {
            final PaymentRequestTicket ticket = (PaymentRequestTicket) ticketService.load(ticketStr, PaymentRequestTicket.Relationships.FROM_CHANNEL);
            // Check the member restriction
            final Member restricted = WebServiceContext.getMember();
            if (restricted != null && !restricted.equals(ticket.getTo())) {
                throw new Exception();
            }

            // Check the from channel
            final Channel resolvedChannel = WebServiceContext.getChannel();
            final Channel fromChannel = ticket.getFromChannel();
            final Channel toChannel = ticket.getToChannel();
            if ((fromChannel == null || !fromChannel.equals(resolvedChannel)) && (toChannel == null || !toChannel.equals(resolvedChannel))) {
                throw new Exception();
            }

            // Check by status
            if (ticket.getStatus() == Ticket.Status.PENDING) {
                ticketService.expirePaymentRequestTicket(ticket);
                return true;
            }
        } catch (final Exception e) {
            webServiceHelper.error(e);
            // Ignore exceptions
        }
        return false;
    }

    public RequestPaymentResult requestPaymentConfirmation(final RequestPaymentParameters params) {
        Exception errorException = null;
        PaymentRequestStatus status = null;
        // Get the to member
        Member toMember = null;
        final Member restricted = WebServiceContext.getMember();
        if (restricted != null) {
            // When restricted to a member, he is always the to
            toMember = restricted;
        } else {
            try {
                toMember = paymentHelper.resolveToMember(params);
            } catch (final EntityNotFoundException e) {
                status = PaymentRequestStatus.TO_NOT_FOUND;
            }
            // When not restricted to a member, check the channel access of the payment receiver
            if (status == null && !memberHelper.isChannelEnabledForMember(toMember)) {
                status = PaymentRequestStatus.TO_INVALID_CHANNEL;
            }
        }
        // Get the from member
        Member fromMember = null;
        if (status == null) {
            try {
                fromMember = paymentHelper.resolveFromMember(params);
            } catch (final EntityNotFoundException e) {
                status = PaymentRequestStatus.FROM_NOT_FOUND;
            }
        }

        // Generate the ticket if no error so far
        PaymentRequestTicket ticket = null;
        if (status == null) {
            try {
                ticket = paymentHelper.toTicket(params, null);
                ticket.setFrom(fromMember);
                ticket.setTo(toMember);
                ticket = ticketService.generate(ticket);
                status = PaymentRequestStatus.REQUEST_RECEIVED;
            } catch (final InvalidChannelException e) {
                status = PaymentRequestStatus.FROM_INVALID_CHANNEL;
            } catch (final Exception e) {
                errorException = e;
                final PaymentStatus paymentStatus = paymentHelper.toStatus(e);
                try {
                    // Probably it's a payment status also present on payment request status
                    status = PaymentRequestStatus.valueOf(paymentStatus.name());
                } catch (final Exception e1) {
                    e1.initCause(e);
                    errorException = e1;
                    status = PaymentRequestStatus.UNKNOWN_ERROR;
                }
            }
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error(errorException != null ? errorException : new Exception("Request payment confirmation status: " + status));
        }

        // Build a result
        final RequestPaymentResult result = new RequestPaymentResult();
        result.setStatus(status);
        if (ticket != null) {
            result.setTicket(ticket.getTicket());
        }
        return result;
    }

    public ChargebackResult reverse(final String traceNumber) {
        return reverse(traceNumber, new TransferLoader<String>() {
            public Transfer load(final String traceNumber) {
                return paymentService.loadTransferByTraceNumber(traceNumber, WebServiceContext.getClient().getId());
            }
        });
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAccountHelper(final AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setChannelHelper(final ChannelHelper channelHelper) {
        this.channelHelper = channelHelper;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setPaymentHelper(final PaymentHelper paymentHelper) {
        this.paymentHelper = paymentHelper;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setTicketService(final TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void setTransferAuthorizationService(final TransferAuthorizationService transferAuthorizationService) {
        this.transferAuthorizationService = transferAuthorizationService;
    }

    public void setWebServiceHelper(final WebServiceHelper webServiceHelper) {
        this.webServiceHelper = webServiceHelper;
    }

    public PaymentSimulationResult simulatePayment(final PaymentParameters params) {

        PaymentStatus status;
        AccountHistoryTransferVO transferVO = null;


        try {
            final PrepareParametersResult result = prepareParameters(params);
            status = result.getStatus();

            if (status == null) {
                final DoExternalPaymentDTO dto = paymentHelper.toExternalPaymentDTO(params, result.getFrom(), result.getTo());
                if (!validateTransferType(dto)) {
                    webServiceHelper.trace(PaymentStatus.INVALID_PARAMETERS + ". Reason: The service client doesn't have permission to the specified transfer type: " + dto.getTransferType());
                    status = PaymentStatus.INVALID_PARAMETERS;
                } else {
                    // Simulate the payment
                    final Transfer transfer = (Transfer) paymentService.simulatePayment(dto);
                    transferVO = accountHelper.toVO(WebServiceContext.getMember(), transfer, null);
                    status = paymentHelper.toStatus(transfer);
                }
            }
        } catch (final Exception e) {
            webServiceHelper.error(e);
            status = paymentHelper.toStatus(e);
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error("Simulate payment status: " + status);
        }
        return new PaymentSimulationResult(status, transferVO);
    }

    private List<ChargebackResult> bulkReverse(final List<Transfer> transfers, final List<AccountDTO> accountsToLock) {
        final List<ChargebackResult> results = new LinkedList<ChargebackResult>();
        boolean failed = false;

        try {
            accountService.lockAccounts(true, accountsToLock);
        } catch (final Exception e) {
            WebServiceFaultsEnum.CURRENTLY_UNAVAILABLE.throwFault(e);
        }

        for (final Transfer transfer : transfers) {
            try {
                if (failed) {
                    results.add(new ChargebackResult(ChargebackStatus.NOT_PERFORMED, null, null));
                } else {
                    final ChargebackResult result = doChargeback(transfer);
                    results.add(result);
                    if (!result.getStatus().isSuccessful()) {
                        failed = true;
                    }
                }
            } catch (final Exception e) {
                failed = true;
                webServiceHelper.error(e);
                results.add(new ChargebackResult(ChargebackStatus.TRANSFER_CANNOT_BE_CHARGEDBACK, null, null));
            }
        }

        return results;
    }

    /**
     * Checks the given member's pin
     */
    private void checkCredentials(Member member, final Channel channel, final String credentials) {
        if (member == null) {
            return;
        }
        final ServiceClient client = WebServiceContext.getClient();
        final Member restrictedMember = client.getMember();
        if (restrictedMember == null) {
            // Non-restricted clients use the flag credentials required
            if (!client.isCredentialsRequired()) {
                // No credentials should be checked
                throw new InvalidCredentialsException();
            }
        } else {
            // Restricted clients don't need check if is the same member
            if (restrictedMember.equals(member)) {
                throw new InvalidCredentialsException();
            }
        }
        if (StringUtils.isEmpty(credentials)) {
            throw new InvalidCredentialsException();
        }
        member = fetchService.fetch(member, Element.Relationships.USER);
        accessService.checkCredentials(channel, member.getMemberUser(), credentials, WebServiceContext.getRequest().getRemoteAddr(), WebServiceContext.getMember());
    }

    /**
     * Performs some static checks and loads the transfers to be chargedback by the bulk reverse operation.
     * @param ids: Could be trace numbers or transfer ids.
     * @param loader: The object responsible for loading the transfer by trace number or transferId.
     */
    private <V> List<ChargebackResult> doBulkChargeback(final List<V> ids, final TransferLoader<V> loader) {
        final List<Transfer> transfers = new LinkedList<Transfer>();
        final List<AccountDTO> allAccounts = new LinkedList<AccountDTO>();
        final Member member = WebServiceContext.getMember();
        final List<ChargebackResult> result = new LinkedList<ChargebackResult>();
        boolean failed = false;

        for (final V id : ids) {
            Transfer transfer = null;
            try {
                transfer = loader.load(id);
                transfers.add(transfer);
                if (member != null && !transfer.getToOwner().equals(member)) {
                    throw new EntityNotFoundException();
                }
                result.add(new ChargebackResult(ChargebackStatus.NOT_PERFORMED, null, null));
                allAccounts.add(new AccountDTO(transfer.getFrom()));
                allAccounts.add(new AccountDTO(transfer.getTo()));
            } catch (final EntityNotFoundException e) {
                failed = true;
                result.add(new ChargebackResult(ChargebackStatus.TRANSFER_NOT_FOUND, null, null));
                webServiceHelper.error(new Exception("Bulk status [Id=" + id + "]: " + ChargebackStatus.TRANSFER_NOT_FOUND, e));
            }
        }

        if (failed) {
            return result;
        }

        return bulkReverse(transfers, allAccounts);
    }

    private ChargebackResult doChargeback(final Transfer transfer) {
        ChargebackStatus status = null;
        Transfer chargebackTransfer = null;

        // Check if the transfer can be charged back
        if (!paymentService.canChargeback(transfer, false)) {
            if (transfer.getChargedBackBy() != null) {
                chargebackTransfer = transfer.getChargedBackBy();
                status = ChargebackStatus.TRANSFER_ALREADY_CHARGEDBACK;
            } else {
                if (transfer.getStatus() == Payment.Status.PENDING) {
                    final TransferAuthorizationDTO transferAuthorizationDto = new TransferAuthorizationDTO();
                    transferAuthorizationDto.setTransfer(transfer);
                    transferAuthorizationDto.setShowToMember(false);
                    chargebackTransfer = transferAuthorizationService.cancelFromMemberAsReceiver(transferAuthorizationDto);
                    status = ChargebackStatus.SUCCESS;
                } else {
                    status = ChargebackStatus.TRANSFER_CANNOT_BE_CHARGEDBACK;
                }
            }
        }

        // Do the chargeback
        if (status == null) {
            chargebackTransfer = paymentService.chargeback(transfer, WebServiceContext.getClient().getId());
            status = ChargebackStatus.SUCCESS;
        }

        if (!status.isSuccessful()) {
            webServiceHelper.error("Chargeback result: " + status);
        }

        final Member member = WebServiceContext.getMember();
        // Build the result
        if (status == ChargebackStatus.SUCCESS || status == ChargebackStatus.TRANSFER_ALREADY_CHARGEDBACK) {
            final AccountOwner owner = member == null ? transfer.getToOwner() : member;
            final AccountHistoryTransferVO originalVO = accountHelper.toVO(owner, transfer, null);
            final AccountHistoryTransferVO chargebackVO = accountHelper.toVO(owner, chargebackTransfer, null);
            return new ChargebackResult(status, originalVO, chargebackVO);
        } else {
            return new ChargebackResult(status, null, null);
        }
    }

    /**
     * Prepares the parameters for a payment. The resulting status is null when no problem found
     */
    private PrepareParametersResult prepareParameters(final PaymentParameters params) {

        final Member restricted = WebServiceContext.getMember();
        final boolean fromSystem = params.isFromSystem();
        final boolean toSystem = params.isToSystem();
        PaymentStatus status = null;
        Member fromMember = null;
        Member toMember = null;
        // Load the from member
        if (!fromSystem) {
            try {
                fromMember = paymentHelper.resolveFromMember(params);
            } catch (final EntityNotFoundException e) {
                webServiceHelper.error(e);
                status = PaymentStatus.FROM_NOT_FOUND;
            }
        }
        // Load the to member
        if (!toSystem) {
            try {
                toMember = paymentHelper.resolveToMember(params);
            } catch (final EntityNotFoundException e) {
                webServiceHelper.error(e);
                status = PaymentStatus.TO_NOT_FOUND;
            }
        }

        if (status == null) {
            if (restricted == null) {
                // Ensure has the do payment permission
                if (!WebServiceContext.hasPermission(ServiceOperation.DO_PAYMENT)) {
                    throw new PermissionDeniedException("The service client doesn't have the following permission: " + ServiceOperation.DO_PAYMENT);
                }
                // Check the channel immediately, as needed by SMS controller
                if (fromMember != null && !accessService.isChannelEnabledForMember(channelHelper.restricted(), fromMember)) {
                    status = PaymentStatus.INVALID_CHANNEL;
                }
            } else {
                // Enforce the restricted to member parameters
                if (fromSystem) {
                    // Restricted to member can't perform payment from system
                    status = PaymentStatus.FROM_NOT_FOUND;
                } else {
                    if (fromMember == null) {
                        fromMember = restricted;
                    } else if (toMember == null && !toSystem) {
                        toMember = restricted;
                    }
                }
                if (status == null) {
                    // Check make / receive payment permissions
                    if (fromMember.equals(restricted)) {
                        if (!WebServiceContext.hasPermission(ServiceOperation.DO_PAYMENT)) {
                            throw new PermissionDeniedException("The service client doesn't have the following permission: " + ServiceOperation.DO_PAYMENT);
                        }
                    } else {
                        if (!WebServiceContext.hasPermission(ServiceOperation.RECEIVE_PAYMENT)) {
                            throw new PermissionDeniedException("The service client doesn't have the following permission: " + ServiceOperation.RECEIVE_PAYMENT);
                        }
                    }
                    // Ensure that either from or to member is the restricted one
                    if (!fromMember.equals(restricted) && !toMember.equals(restricted)) {
                        status = PaymentStatus.INVALID_PARAMETERS;
                        webServiceHelper.trace(status + ". Reason: Neither the origin nor the destination members are equal to the restricted: " + restricted);
                    }
                }
                if (status == null) {
                    // Enforce the permissions
                    if (restricted.equals(fromMember) && !WebServiceContext.hasPermission(ServiceOperation.DO_PAYMENT)) {
                        throw new PermissionDeniedException("The service client doesn't have the following permission: " + ServiceOperation.DO_PAYMENT);
                    } else if (restricted.equals(toMember) && !WebServiceContext.hasPermission(ServiceOperation.RECEIVE_PAYMENT)) {
                        throw new PermissionDeniedException("The service client doesn't have the following permission: " + ServiceOperation.RECEIVE_PAYMENT);
                    }
                }
            }
        }

        // Ensure both from and to member are present
        if (status == null) {
            if (fromMember == null && !fromSystem) {
                status = PaymentStatus.FROM_NOT_FOUND;
            } else if (toMember == null && !toSystem) {
                status = PaymentStatus.TO_NOT_FOUND;
            }
        }

        if (status == null) {
            // Check the channel
            if (fromMember != null && !accessService.isChannelEnabledForMember(channelHelper.restricted(), fromMember)) {
                status = PaymentStatus.INVALID_CHANNEL;
            }
        }
        if (status == null) {
            // Check the credentials
            boolean checkCredentials;
            if (restricted != null) {
                checkCredentials = !fromMember.equals(restricted);
            } else {
                checkCredentials = !fromSystem && WebServiceContext.getClient().isCredentialsRequired();
            }
            if (checkCredentials) {
                try {
                    checkCredentials(fromMember, WebServiceContext.getChannel(), params.getCredentials());
                } catch (final InvalidCredentialsException e) {
                    status = PaymentStatus.INVALID_CREDENTIALS;
                } catch (final BlockedCredentialsException e) {
                    status = PaymentStatus.BLOCKED_CREDENTIALS;
                }
            }
        }

        // No error
        final AccountOwner fromOwner = fromSystem ? SystemAccountOwner.instance() : fromMember;
        final AccountOwner toOwner = toSystem ? SystemAccountOwner.instance() : toMember;
        return new PrepareParametersResult(status, fromOwner, toOwner);
    }

    private <V> ChargebackResult reverse(final V transferId, final TransferLoader<V> loader) {
        Exception errorException = null;
        ChargebackStatus status = null;
        Transfer transfer = null;

        try {
            transfer = loader.load(transferId);
            // Ensure the member is the one who received the payment
            final Member member = WebServiceContext.getMember();
            if (member != null && !transfer.getToOwner().equals(member)) {
                throw new EntityNotFoundException();
            } else {
                final Collection<TransferType> possibleTypes = fetchService.fetch(WebServiceContext.getClient(), ServiceClient.Relationships.CHARGEBACK_PAYMENT_TYPES).getChargebackPaymentTypes();
                if (!possibleTypes.contains(transfer.getType())) {
                    throw new EntityNotFoundException();
                }
            }
        } catch (final EntityNotFoundException e) {
            errorException = e;
            status = ChargebackStatus.TRANSFER_NOT_FOUND;
        }

        if (status == null) {
            try {
                return doChargeback(transfer);
            } catch (final Exception e) {
                webServiceHelper.error(e);
                return new ChargebackResult(ChargebackStatus.TRANSFER_CANNOT_BE_CHARGEDBACK, null, null);
            }
        } else {
            if (!status.isSuccessful()) {
                webServiceHelper.error(errorException != null ? errorException : new Exception("Chargeback status: " + status));
            }
            return new ChargebackResult(status, null, null);
        }
    }

    private boolean validateTransferType(final DoExternalPaymentDTO dto) {
        final Collection<TransferType> possibleTypes = paymentHelper.listPossibleTypes(dto);
        return possibleTypes != null && possibleTypes.contains(dto.getTransferType());
    }
}
