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
package nl.strohalm.cyclos.services.transactions;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.accounts.transactions.ReverseDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.ScheduledPaymentDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.TicketDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.SimpleTransactionFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFeeQuery;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee.ChargeType;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentRequestTicket;
import nl.strohalm.cyclos.entities.accounts.transactions.Reverse;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Ticket;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType.Relationships;
import nl.strohalm.cyclos.entities.alerts.MemberAlert;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContractQuery;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.reports.StatisticalNumber;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings.TransactionNumber;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.accounts.rates.ConversionSimulationDTO;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.CommissionService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.preferences.MessageChannel;
import nl.strohalm.cyclos.services.preferences.PreferenceService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.stats.StatisticalResultDTO;
import nl.strohalm.cyclos.services.transactions.exceptions.AuthorizedPaymentInPastException;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.TransferMinimumPaymentException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.services.transfertypes.BuildTransferWithFeesDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewForRatesDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.BigDecimalHelper;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;
import nl.strohalm.cyclos.utils.statistics.GraphHelper;
import nl.strohalm.cyclos.utils.validation.CompareToValidation;
import nl.strohalm.cyclos.utils.validation.DelegatingValidator;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.UniqueError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.Marker;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation for payment service
 * @author luis
 * @author rinke (rates stuff)
 */
public class PaymentServiceImpl implements PaymentService, ApplicationContextAware {

    /**
     * A key to monitor which fees have been charged, in order to detect loops
     * @author luis
     */
    private static class ChargedFee {
        private TransactionFee fee;
        private Account        from;
        private Account        to;

        private ChargedFee(final TransactionFee fee, final Account from, final Account to) {
            this.fee = fee;
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof ChargedFee)) {
                return false;
            }
            final ChargedFee f = (ChargedFee) obj;
            return new EqualsBuilder().append(fee, f.fee).append(from, f.from).append(to, f.to).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(fee).append(from).append(to).toHashCode();
        }
    }

    /**
     * validator always returning a validationError. To be called if the final amount of a payment (after applying all fees) is negative
     * @author rinke
     */
    private final class FinalAmountValidator implements GeneralValidation {
        private static final long serialVersionUID = -2789145696000017181L;

        public ValidationError validate(final Object object) {
            return new ValidationError("payment.error.negativeFinalAmount");
        }
    }

    private class InsertReverseThread extends Thread {
        private String traceNumber;
        private Long   clientId;
        Throwable      error;

        private InsertReverseThread(final String traceNumber, final Long clientId) {
            super(InsertReverseThread.class.getName());
            this.traceNumber = traceNumber;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        final Reverse reverse = new Reverse();
                        reverse.setDate(Calendar.getInstance());
                        reverse.setClientId(clientId);
                        reverse.setTraceNumber(traceNumber);
                        reverseDao.insert(reverse);
                    } catch (final Throwable t) {
                        if (ExceptionUtils.indexOfThrowable(t, DataIntegrityViolationException.class) != -1) {
                            // the unique constraint was violated
                            error = null;
                        } else {
                            error = t;
                        }
                        status.setRollbackOnly();
                    }
                }
            });
        }

        public void throwErrorIfAny() {
            if (error != null) {
                if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                }
                throw new IllegalStateException(error);
            }
        }
    }

    /**
     * validator which always returns a validationError. To be called if a past date on a transfer is combined with rates.
     * @author Rinke
     * 
     */
    private static final class NoPastDateWithRatesValidator implements GeneralValidation {

        private static final long serialVersionUID = -6914314732478889087L;

        public ValidationError validate(final Object object) {
            return new ValidationError("payment.error.pastDateWithRates");
        }
    }

    private final class PendingContractValidator implements GeneralValidation {

        private static final long serialVersionUID = 5608258953479316287L;

        @SuppressWarnings("unchecked")
        public ValidationError validate(final Object object) {
            // Validate the scheduled payments
            final DoPaymentDTO payment = (DoPaymentDTO) object;

            Member fromMember = null;
            if (payment.getFrom() instanceof Member) {
                fromMember = fetchService.fetch((Member) payment.getFrom(), Element.Relationships.GROUP);

                // Validate if there is a fee (broker commission) with a pending contract
                if (payment.getTo() != null && payment.getTo() instanceof Member && payment.getTransferType() != null) {
                    final TransferType transferType = fetchService.fetch(payment.getTransferType(), TransferType.Relationships.TRANSACTION_FEES);
                    final Collection<TransactionFee> transactionFees = (Collection<TransactionFee>) fetchService.fetch(transferType.getTransactionFees(), TransactionFee.Relationships.GENERATED_TRANSFER_TYPE);
                    for (final TransactionFee transactionFee : transactionFees) {
                        if (transactionFee instanceof BrokerCommission && transactionFee.isFromMember()) {
                            final BrokerCommission brokerCommission = (BrokerCommission) transactionFee;
                            final BrokerCommissionContractQuery contractsQuery = new BrokerCommissionContractQuery();
                            contractsQuery.setBrokerCommission(brokerCommission);
                            contractsQuery.setStatus(BrokerCommissionContract.Status.PENDING);
                            switch (brokerCommission.getWhichBroker()) {
                                case SOURCE:
                                    contractsQuery.setMember(fromMember);
                                    break;
                                case DESTINATION:
                                    contractsQuery.setMember((Member) payment.getTo());
                                    break;
                            }
                            final List<BrokerCommissionContract> commissionContracts = commissionService.searchBrokerCommissionContracts(contractsQuery);
                            if (CollectionUtils.isNotEmpty(commissionContracts)) {
                                return new ValidationError("payment.error.pendingCommissionContract", brokerCommission.getName());
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    private final class SchedulingValidator implements GeneralValidation {

        private static final long serialVersionUID = 4085922259108191939L;

        @SuppressWarnings("unchecked")
        public ValidationError validate(final Object object) {
            // Validate the scheduled payments
            final DoPaymentDTO payment = (DoPaymentDTO) object;
            final List<ScheduledPaymentDTO> payments = payment.getPayments();
            if (CollectionUtils.isEmpty(payments)) {
                return null;
            }

            final TransferType transferType = fetchService.fetch(payment.getTransferType(), TransferType.Relationships.TRANSACTION_FEES);

            // Validate the from member
            Member fromMember = null;
            if (payment.getFrom() instanceof Member) {
                fromMember = fetchService.fetch((Member) payment.getFrom(), Element.Relationships.GROUP);
            } else if (LoggedUser.isValid() && LoggedUser.isMember()) {
                fromMember = LoggedUser.element();
            }
            Calendar maxPaymentDate = null;
            if (fromMember != null) {
                final MemberGroup group = fromMember.getMemberGroup();

                // Validate the max payments
                final int maxSchedulingPayments = transferType.isAllowsScheduledPayments() ? group.getMemberSettings().getMaxSchedulingPayments() : 0;
                if (payments.size() > maxSchedulingPayments) {
                    return new ValidationError("errors.greaterEquals", messageResolver.message("transfer.paymentCount"), maxSchedulingPayments);
                }

                // Get the maximum payment date
                final TimePeriod maxSchedulingPeriod = group.getMemberSettings().getMaxSchedulingPeriod();
                if (maxSchedulingPeriod != null) {
                    maxPaymentDate = maxSchedulingPeriod.add(DateHelper.truncate(Calendar.getInstance()));
                }

                // Validate if there is a fee with a pending contract
                if (payment.getTo() != null && payment.getTo() instanceof Member) {
                    final Collection<TransactionFee> transactionFees = (Collection<TransactionFee>) fetchService.fetch(transferType.getTransactionFees(), TransactionFee.Relationships.GENERATED_TRANSFER_TYPE);
                    for (final TransactionFee transactionFee : transactionFees) {
                        if (transactionFee instanceof BrokerCommission && transactionFee.isFromMember()) {
                            final BrokerCommission brokerCommission = (BrokerCommission) transactionFee;
                            final BrokerCommissionContractQuery contractsQuery = new BrokerCommissionContractQuery();
                            contractsQuery.setBrokerCommission(brokerCommission);
                            contractsQuery.setStatus(BrokerCommissionContract.Status.PENDING);
                            switch (brokerCommission.getWhichBroker()) {
                                case SOURCE:
                                    contractsQuery.setMember(fromMember);
                                    break;
                                case DESTINATION:
                                    contractsQuery.setMember((Member) payment.getTo());
                                    break;
                            }
                            final List<BrokerCommissionContract> commissionContracts = commissionService.searchBrokerCommissionContracts(contractsQuery);
                            if (CollectionUtils.isNotEmpty(commissionContracts)) {
                                return new ValidationError("payment.error.pendingCommissionContract", brokerCommission.getName());
                            }
                        }
                    }
                }

            }

            // Validate the total payment amount and dates
            final BigDecimal paymentAmount = payment.getAmount();
            final BigDecimal minimumPayment = getMinimumPayment();
            BigDecimal totalAmount = BigDecimal.ZERO;
            Calendar lastDate = DateHelper.truncatePreviosDay(Calendar.getInstance());
            for (final ScheduledPaymentDTO dto : payments) {
                final Calendar date = dto.getDate();
                // Validate the max payment date
                if (maxPaymentDate != null && date.after(maxPaymentDate)) {
                    final LocalSettings localSettings = settingsService.getLocalSettings();
                    final CalendarConverter dateConverter = localSettings.getRawDateConverter();
                    return new ValidationError("payment.invalid.schedulingDate", dateConverter.toString(maxPaymentDate));
                }

                final BigDecimal amount = dto.getAmount();

                if (amount == null || amount.compareTo(minimumPayment) < 0) {
                    return new RequiredError(messageResolver.message("transfer.amount"));
                }
                if (date == null) {
                    return new RequiredError(messageResolver.message("transfer.date"));
                } else if (date.before(lastDate) || DateUtils.isSameDay(date, lastDate)) {
                    return new ValidationError("payment.invalid.paymentDates");
                }
                totalAmount = totalAmount.add(amount);
                lastDate = date;
            }
            // Validate the total payment amount
            if (paymentAmount != null && totalAmount.compareTo(paymentAmount) != 0) {
                return new ValidationError("payment.invalid.paymentAmount");
            }
            return null;
        }
    }

    /**
     * Class used to simulate a payment. It actually performs the payment in a new transaction an runs a rollback
     * @author luis
     */
    private class SimulatePaymentThread extends Thread {
        private DoExternalPaymentDTO dto;
        private Payment              payment;
        private Throwable            error;

        public SimulatePaymentThread(final DoExternalPaymentDTO dto) {
            this.dto = dto;
        }

        public Throwable getError() {
            return error;
        }

        public Payment getPayment() {
            return payment;
        }

        @Override
        public void run() {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        payment = doPayment(dto);
                    } catch (final Throwable t) {
                        error = t;
                    }
                    status.setRollbackOnly();
                }
            });
            CurrentTransactionData.cleanup();
        }

    }

    private class TraceNumberValidation implements PropertyValidation {

        private static final long serialVersionUID = 2424106851078796317L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final TransferDTO dto = (TransferDTO) object;
            final Long clientId = dto.getClientId();
            final String traceNumber = dto.getTraceNumber();
            if (clientId == null || StringUtils.isEmpty(traceNumber)) {
                return null;
            }
            try {
                transferDao.loadTransferByTraceNumber(traceNumber, clientId);
                reverseDao.load(clientId, traceNumber);
                // Invalid, as if it reaches here, there is at least one other transfer with the given trace number
                return new UniqueError();
            } catch (final EntityNotFoundException e) {
                // Is valid, as there are no other transfer using that trace number for that client id
                return null;
            }
        }
    }

    private static final float                     PRECISION_DELTA            = 0.0001F;

    private static final Relationship[]            CONCILIATION_FETCH         = { Transfer.Relationships.EXTERNAL_TRANSFER, RelationshipHelper.nested(Transfer.Relationships.FROM, MemberAccount.Relationships.MEMBER) };

    /** Contains the last payment's date. Used to clear the MAX_AMOUNT_MAP when the day has changed */
    private Calendar                               lastPaymentDate            = Calendar.getInstance();

    /** The key is the account id. The value is another Map where the key is the transfer type id and the value is the accumulated amount on the day */
    private final Map<Long, Map<Long, BigDecimal>> maxAmountPerDayMap         = Collections.synchronizedMap(new HashMap<Long, Map<Long, BigDecimal>>());
    /** The key is the operator id. The value is another Map where the key is the transfer type id and the value is the accumulated amount on the day */
    private final Map<Long, Map<Long, BigDecimal>> operatorMaxAmountPerDayMap = Collections.synchronizedMap(new HashMap<Long, Map<Long, BigDecimal>>());
    private AccountService                         accountService;
    private CommissionService                      commissionService;
    private SettingsService                        settingsService;
    private TransferAuthorizationService           transferAuthorizationService;
    private TicketDAO                              ticketDao;
    private TransactionFeeService                  transactionFeeService;
    private TransferDAO                            transferDao;
    private ReverseDAO                             reverseDao;
    private ScheduledPaymentDAO                    scheduledPaymentDao;
    private TransferTypeService                    transferTypeService;
    private FetchService                           fetchService;
    private LoggingHandler                         loggingHandler;
    private PermissionService                      permissionService;
    private AlertService                           alertService;
    private MessageResolver                        messageResolver;
    private ApplicationContext                     applicationContext;
    private TransactionTemplate                    transactionTemplate;
    private CustomFieldService                     customFieldService;
    private AccountStatusHandler                   accountStatusHandler;
    private ARateService                           aRateService;
    private DRateService                           dRateService;

    private PreferenceService                      preferenceService;

    public List<ScheduledPaymentDTO> calculatePaymentProjection(final ProjectionDTO params) {
        getProjectionValidator().validate(params);

        final LocalSettings localSettings = settingsService.getLocalSettings();

        final int paymentCount = params.getPaymentCount();
        final TimePeriod recurrence = params.getRecurrence();
        final BigDecimal totalAmount = params.getAmount();
        final BigDecimal paymentAmount = localSettings.round(totalAmount.divide(CoercionHelper.coerce(BigDecimal.class, paymentCount), localSettings.getMathContext()));
        BigDecimal accumulatedAmount = BigDecimal.ZERO;
        Calendar currentDate = DateHelper.truncate(params.getFirstExpirationDate());
        final List<ScheduledPaymentDTO> payments = new ArrayList<ScheduledPaymentDTO>(paymentCount);
        for (int i = 0; i < paymentCount; i++) {
            final ScheduledPaymentDTO dto = new ScheduledPaymentDTO();
            dto.setDate(currentDate);
            dto.setAmount(i == paymentCount - 1 ? totalAmount.subtract(accumulatedAmount) : paymentAmount);
            payments.add(dto);
            accumulatedAmount = accumulatedAmount.add(dto.getAmount(), localSettings.getMathContext());
            currentDate = recurrence.add(currentDate);
        }
        return payments;
    }

    public boolean canChargeback(Transfer transfer, final boolean ignorePendingPayment) {
        transfer = fetchService.fetch(transfer, Transfer.Relationships.FROM, Transfer.Relationships.TO, Transfer.Relationships.PARENT, Transfer.Relationships.CHARGEBACK_OF, Transfer.Relationships.CHILDREN);
        if (transfer == null) {
            return false;
        }
        // Pending payments cannot be charged back
        final Calendar processDate = transfer.getProcessDate();
        if (!ignorePendingPayment && processDate == null) {
            return false;
        }

        // Check the max chargeback time
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final TimePeriod maxChargebackTime = localSettings.getMaxChargebackTime();
        final Calendar maxDate = maxChargebackTime.add(processDate);
        if (Calendar.getInstance().after(maxDate)) {
            return false;
        }

        // Nested transfers cannot be charged back
        if (transfer.getParent() != null) {
            return false;
        }
        // Payments which has already been charged back cannot be charged back again
        if (transfer.getChargedBackBy() != null) {
            return false;
        }
        // Payments which are chargebacks cannot be charged back
        if (transfer.getChargebackOf() != null) {
            return false;
        }
        // Cannot chargeback if from owner is removed
        if (!transfer.isFromSystem()) {
            final Member fromOwner = (Member) transfer.getFromOwner();
            if (fromOwner.getGroup().getStatus() == Group.Status.REMOVED) {
                return false;
            }
        }
        // Cannot chargeback if to owner is removed
        if (!transfer.isToSystem()) {
            final Member toOwner = (Member) transfer.getToOwner();
            if (toOwner.getGroup().getStatus() == Group.Status.REMOVED) {
                return false;
            }
        }
        return true;
    }

    public Transfer chargeback(final Transfer transfer, final Long clientId) throws UnexpectedEntityException {
        if (clientId == null) {
            throw new UnexpectedEntityException();
        }
        return doChargeback(transfer, null, false, clientId);
    }

    public Transfer chargebackMemberPayment(final Transfer transfer) throws UnexpectedEntityException {
        return doChargeback(transfer, AccountType.Nature.MEMBER, true, null);
    }

    public Transfer chargebackReceivedPayment(final Transfer transfer) throws UnexpectedEntityException {
        return doChargeback(transfer, null, true, null);
    }

    public void chargebackSmsCharge(final Transfer transfer) {
        // All checks are bypassed because this is only invoked internally
        insertChargeback(transfer, null, null);
    }

    public Transfer chargebackSystemPayment(final Transfer transfer) throws UnexpectedEntityException {
        return doChargeback(transfer, AccountType.Nature.SYSTEM, true, null);
    }

    public void checkView(Transfer transfer) throws PermissionDeniedException {
        final Relationship[] fetch = { RelationshipHelper.nested(Transfer.Relationships.FROM, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP), RelationshipHelper.nested(Transfer.Relationships.FROM, Account.Relationships.TYPE), RelationshipHelper.nested(Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP), RelationshipHelper.nested(Transfer.Relationships.TO, Account.Relationships.TYPE) };
        transfer = fetchService.fetch(transfer, fetch);
        final AccountOwner fromAccountOwner = transfer.getFromOwner();
        final AccountOwner toAccountOwner = transfer.getToOwner();
        if (LoggedUser.isOperator()) {
            final Member member = (Member) LoggedUser.accountOwner();
            if (member.equals(fromAccountOwner) || member.equals(toAccountOwner)) {
                if (permissionService.checkPermission("operatorAccount", "accountInformation") || permissionService.checkPermission("operatorPayments", null)) {
                    return;
                }
            }
        } else if (LoggedUser.isMember()) {
            final Member member = LoggedUser.element();
            if (member.equals(fromAccountOwner) || member.equals(toAccountOwner)) {
                return;
            }
            if (LoggedUser.isBroker() && permissionService.checkPermission("brokerAccounts", "information")) {
                if (fromAccountOwner instanceof Member) {
                    final Member fromMember = fetchService.fetch((Member) fromAccountOwner, Member.Relationships.BROKER);
                    if (member.equals(fromMember.getBroker())) {
                        return;
                    }
                }
                if (toAccountOwner instanceof Member) {
                    final Member toMember = fetchService.fetch((Member) toAccountOwner, Member.Relationships.BROKER);
                    if (member.equals(toMember.getBroker())) {
                        return;
                    }
                }
            }
        } else if (LoggedUser.isAdministrator()) {
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.VIEW_INFORMATION_OF, AdminGroup.Relationships.MANAGES_GROUPS);
            if (fromAccountOwner instanceof SystemAccountOwner) {
                final SystemAccountType fromAccountType = (SystemAccountType) transfer.getFrom().getType();
                if (adminGroup.getViewInformationOf().contains(fromAccountType)) {
                    return;
                }
            } else {
                final Member fromMember = (Member) transfer.getFromOwner();
                final MemberGroup fromMemberGroup = fromMember.getMemberGroup();
                if (permissionService.checkPermission("adminMemberAccounts", "information") && adminGroup.getManagesGroups().contains(fromMemberGroup)) {
                    return;
                }
            }
            if (toAccountOwner instanceof SystemAccountOwner) {
                final SystemAccountType toAccountType = (SystemAccountType) transfer.getTo().getType();
                if (adminGroup.getViewInformationOf().contains(toAccountType)) {
                    return;
                }
            } else {
                final Member toMember = (Member) transfer.getToOwner();
                final MemberGroup toMemberGroup = toMember.getMemberGroup();
                if (permissionService.checkPermission("adminMemberAccounts", "information") && adminGroup.getManagesGroups().contains(toMemberGroup)) {
                    return;
                }
            }
        }
        throw new PermissionDeniedException();
    }

    public Transfer conciliate(Transfer transfer, final ExternalTransfer externalTransfer) {
        transfer = fetchService.fetch(transfer, CONCILIATION_FETCH);
        if (transfer != null && transfer.getExternalTransfer() != null) {
            // If the transfer is already conciliated, ignore it
            transfer = null;
        }
        if (transfer != null) {
            final Account from = transfer.getFrom();
            final AccountOwner owner = from.getOwner();
            if (!owner.equals(externalTransfer.getMember())) {
                // The account does not belong to the expected member, ignore it
                transfer = null;
            }
        }
        if (transfer == null) {
            throw new UnexpectedEntityException();
        }
        return transferDao.updateExternalTransfer(transfer.getId(), externalTransfer);
    }

    public Payment confirmPayment(final String ticketStr) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, EntityNotFoundException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException {
        // Get and validate the ticket
        final Ticket ticket = ticketDao.load(ticketStr);
        if (!(ticket instanceof PaymentRequestTicket) || ticket.getStatus() != Ticket.Status.PENDING) {
            throw new EntityNotFoundException(PaymentRequestTicket.class);
        }
        final PaymentRequestTicket prTicket = (PaymentRequestTicket) ticket;
        final Member fromMember = prTicket.getFrom();
        final Member toMember = prTicket.getTo();
        final String channel = prTicket.getToChannel().getInternalName();
        final String description = prTicket.getDescription();

        // Create the payment
        final TransferDTO dto = new TransferDTO();
        dto.setFromOwner(fromMember);
        dto.setToOwner(toMember);
        dto.setTransferType(prTicket.getTransferType());
        dto.setAmount(prTicket.getAmount());
        dto.setChannel(channel);
        dto.setTicket(prTicket);
        dto.setDescription(description);
        final Payment payment = insert(dto);

        // Update the ticket
        ticket.setStatus(Ticket.Status.OK);
        ticketDao.update(ticket);

        return payment;
    }

    public Transfer doExternalPaymentByOperator(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return (Transfer) doPaymentFromMemberToMember(params);
    }

    public Payment doPaymentAsMemberToMember(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        if (params.getFrom() == null || !(params.getFrom() instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        if (params.getTo() == null || !(params.getTo() instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        return doPayment(params);
    }

    public Payment doPaymentAsMemberToSelf(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException {
        return doPaymentAsMemberToMember(params);
    }

    public Payment doPaymentAsMemberToSystem(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        if (params.getFrom() == null || !(params.getFrom() instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        params.setTo(SystemAccountOwner.instance());
        return doPayment(params);
    }

    public Payment doPaymentFromMemberToMember(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        preprocessDto(params);
        if (!(params.getFrom() instanceof Member) || !(params.getTo() instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        return doPayment(params);
    }

    public Transfer doPaymentFromMemberToSelf(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException {
        return doSelfPayment(params);
    }

    public Payment doPaymentFromMemberToSystem(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        preprocessDto(params);
        if (!(params.getFrom() instanceof Member) || !(params.getTo() instanceof SystemAccountOwner)) {
            throw new UnexpectedEntityException();
        }
        return doPayment(params);
    }

    public Payment doPaymentFromSystemToMember(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        preprocessDto(params);
        if (!(params.getFrom() instanceof SystemAccountOwner) || !(params.getTo() instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        return doPayment(params);
    }

    public Transfer doPaymentFromSystemToSystem(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException {
        return doSelfPayment(params);
    }

    public ConversionSimulationDTO getDefaultConversionDTOForMember(final MemberAccount account, final List<TransferType> transferTypes) {
        return getDefaultConversionDTOForSelf(account, transferTypes);
    }

    public ConversionSimulationDTO getDefaultConversionDTOForSelf(MemberAccount account, final List<TransferType> transferTypes) {
        account = fetchService.fetch(account, Account.Relationships.TYPE, MemberAccount.Relationships.MEMBER);
        // Get the current account status
        final AccountStatus status = accountService.getStatus(new GetTransactionsDTO(account));

        final ConversionSimulationDTO dto = new ConversionSimulationDTO();
        dto.setAccount(account);

        // Find the default amount: the balance of the current account
        BigDecimal defaultAmount = status.getAvailableBalanceWithoutCreditLimit();
        if (BigDecimal.ZERO.compareTo(defaultAmount) > 0) {
            defaultAmount = BigDecimal.ZERO;
        }
        dto.setAmount(defaultAmount);

        // find the first rated TT, and choose this.
        for (final TransferType currentTT : transferTypes) {
            if (currentTT.isHavingRatedFees()) {
                dto.setTransferType(currentTT);
                break;
            }
        }
        // If not any rated TT, just choose the first
        if (dto.getTransferType() == null) {
            dto.setTransferType(transferTypes.get(0));
        }

        dto.setDate(Calendar.getInstance());
        // erase any present content
        dto.setArate(null);
        dto.setDrate(null);
        // rates on a zero balance are meaningless, so...
        if (dto.getTransferType().isHavingRatedFees() && BigDecimal.ZERO.compareTo(defaultAmount) < 0) {
            if (dto.getTransferType().isHavingDratedFees()) {
                final BigDecimal dRate = dRateService.getActualRate(status);
                dto.setDrate(dRate);
            }
            if (dto.getTransferType().isHavingAratedFees()) {
                final BigDecimal aRate = aRateService.getActualRate(status);
                dto.setArate(aRate);
            }
        }

        return dto;
    }

    public BigDecimal getMinimumPayment() {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final int precision = localSettings.getPrecision().getValue();
        final BigDecimal minimumPayment = new BigDecimal(new BigInteger("1"), precision);
        return minimumPayment;
    }

    public StatisticalResultDTO getSimulateConversionGraph(final ConversionSimulationDTO inputParameters) {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final byte precision = (byte) localSettings.getPrecision().getValue();

        // get series
        final TransactionFeePreviewDTO temp = simulateConversionForMember(inputParameters);
        final int series = temp.getFees().size();
        // get range of points, but without values for A < 0
        BigDecimal initialARate = null;
        if (inputParameters.isUseActualRates()) {
            final MemberAccount account = fetchService.fetch(inputParameters.getAccount(), Account.Relationships.TYPE);
            final AccountStatus status = accountService.getStatus(new GetTransactionsDTO(account));
            initialARate = aRateService.getActualRate(status, inputParameters.getDate());
        } else {
            initialARate = inputParameters.getArate();
        }
        // lowerlimit takes care that values for A < 0 are left out of the graph
        final Double lowerLimit = (initialARate == null) ? null : initialARate.negate().doubleValue();
        final Number[] xRange = GraphHelper.getOptimalRangeAround(0, 33, 0, 0.8, lowerLimit);

        // Data structure to build the table
        final Number[][] tableCells = new Number[xRange.length][series];
        // initialize series names and x labels
        final String[] seriesNames = new String[series];
        final byte[] seriesOrder = new byte[series];
        final Calendar[] xPointDates = new Calendar[xRange.length];
        final Calendar now = Calendar.getInstance();
        // assign data
        for (int i = 0; i < xRange.length; i++) {
            final ConversionSimulationDTO inputPoint = inputParameters.clone();
            final Calendar date = (Calendar) ((inputParameters.isUseActualRates()) ? inputParameters.getDate().clone() : now.clone());
            date.add(Calendar.DAY_OF_YEAR, xRange[i].intValue());
            xPointDates[i] = date;
            if (inputParameters.isUseActualRates()) {
                inputPoint.setDate(date);
            } else {
                final BigDecimal dRate = inputPoint.getDrate();
                if (dRate != null) {
                    inputPoint.setDrate(dRate.subtract(new BigDecimal(xRange[i].doubleValue())));
                }
                final BigDecimal aRate = inputPoint.getArate();
                if (aRate != null) {
                    inputPoint.setArate(aRate.add(new BigDecimal(xRange[i].doubleValue())));
                }
            }

            final TransactionFeePreviewDTO tempResult = simulateConversionForMember(inputPoint);
            int j = 0;
            for (final TransactionFee fee : tempResult.getFees().keySet()) {
                tableCells[i][j] = new StatisticalNumber(tempResult.getFees().get(fee).doubleValue(), precision);
                byte index;
                switch (fee.getChargeType()) {
                    case D_RATE:
                        index = 2;
                        break;
                    case A_RATE:
                    case MIXED_A_D_RATES:
                        index = 3;
                        break;
                    default:
                        index = 1;
                        break;
                }
                seriesOrder[j] = index;
                seriesNames[j++] = fee.getName();
            }
        }

        // create the graph object
        final StatisticalResultDTO result = new StatisticalResultDTO(tableCells);
        result.setBaseKey("conversionSimulation.result.graph");
        result.setHelpFile("account_management");
        // date labels along x-axis
        final String[] rowKeys = new String[xRange.length];
        Arrays.fill(rowKeys, "");
        result.setRowKeys(rowKeys);
        for (int i = 0; i < rowKeys.length; i++) {
            final String rowHeader = localSettings.getDateConverterForGraphs().toString(xPointDates[i]);
            result.setRowHeader(rowHeader, i);
        }
        // mark the actual date upon which the x-axis is based as a vertical line
        final Calendar baseDate = (inputParameters.isUseActualRates()) ? (Calendar) inputParameters.getDate().clone() : now;
        final String baseDateString = localSettings.getDateConverterForGraphs().toString(baseDate);
        final Marker[] markers = new Marker[1];
        markers[0] = new CategoryMarker(baseDateString);
        markers[0].setPaint(Color.ORANGE);
        final String todayString = localSettings.getDateConverterForGraphs().toString(now);
        if (todayString.equals(baseDateString)) {
            markers[0].setLabel("global.today");
        }
        result.setDomainMarkers(markers);

        // Series labels indicate fee names
        final String[] columnKeys = new String[series];
        Arrays.fill(columnKeys, "");
        result.setColumnKeys(columnKeys);
        for (int j = 0; j < columnKeys.length; j++) {
            result.setColumnHeader(seriesNames[j], j);
        }

        // order the series
        result.orderSeries(seriesOrder);

        final TransferType tt = fetchService.fetch(inputParameters.getTransferType(), RelationshipHelper.nested(TransferType.Relationships.FROM, AccountType.Relationships.CURRENCY));
        result.setYAxisUnits(tt.getCurrency().getSymbol());
        result.setShowTable(false);
        result.setGraphType(StatisticalResultDTO.GraphType.STACKED_AREA);
        return result;
    }

    public Payment insertExternalPayment(final DoExternalPaymentDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return doPayment(dto);
    }

    public Payment insertWithNotification(final TransferDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return insert(dto);
    }

    public Payment insertWithoutNotification(final TransferDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        return insert(dto);
    }

    public Transfer load(final Long id, final Relationship... fetch) {
        return transferDao.<Transfer> load(id, fetch);
    }

    public Transfer loadTransferByTraceNumber(final String traceNumber, final Long clientId) {
        if (traceNumber == null || traceNumber.trim().equals("") || clientId == null) {
            throw new EntityNotFoundException();
        }

        final Transfer transfer = transferDao.loadTransferByTraceNumber(traceNumber, clientId);
        if (transfer == null) {
            insertReverse(traceNumber, clientId);
            throw new EntityNotFoundException();
        }
        return transfer;
    }

    public Transfer pay(final Transfer transfer, final Calendar processedDate) {
        return processScheduledTransfer(transfer, processedDate == null ? Calendar.getInstance() : processedDate, false);
    }

    public Transfer processScheduled(final Transfer transfer, final Calendar time) {
        return processScheduledTransfer(transfer, time, true);
    }

    public List<Transfer> search(final TransferQuery query) {
        return transferDao.search(query);
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    public void setCommissionService(final CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPreferenceService(final PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    public void setReverseDao(final ReverseDAO reverseDao) {
        this.reverseDao = reverseDao;
    }

    public void setScheduledPaymentDao(final ScheduledPaymentDAO scheduledPaymentDao) {
        this.scheduledPaymentDao = scheduledPaymentDao;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTicketDao(final TicketDAO ticketDao) {
        this.ticketDao = ticketDao;
    }

    public void setTransactionFeeService(final TransactionFeeService transactionFeeService) {
        this.transactionFeeService = transactionFeeService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setTransferAuthorizationService(final TransferAuthorizationService transferAuthorizationService) {
        this.transferAuthorizationService = transferAuthorizationService;
    }

    public void setTransferDao(final TransferDAO transferDao) {
        this.transferDao = transferDao;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public TransactionFeePreviewForRatesDTO simulateConversionForMember(final ConversionSimulationDTO params) {
        TransferType transferType = params.getTransferType();
        transferType = fetchService.fetch(transferType, TransferType.Relationships.TO, TransferType.Relationships.TRANSACTION_FEES, RelationshipHelper.nested(SystemAccountType.Relationships.ACCOUNT));

        AccountStatus status = null;
        BigDecimal usedARate = null;
        if (transferType.isHavingAratedFees()) {
            if (params.isUseActualRates()) {
                final MemberAccount account = fetchService.fetch(params.getAccount(), Account.Relationships.TYPE, MemberAccount.Relationships.MEMBER);
                status = accountService.getStatus(new GetTransactionsDTO(account));
                usedARate = aRateService.getActualRate(status, params.getDate());
            } else {
                usedARate = params.getArate();
            }
        }
        BigDecimal usedDRate = null;
        if (transferType.isHavingDratedFees()) {
            if (params.isUseActualRates()) {
                final MemberAccount account = fetchService.fetch(params.getAccount(), Account.Relationships.TYPE);
                status = (status == null) ? accountService.getStatus(new GetTransactionsDTO(account)) : status;
                usedDRate = dRateService.getActualRate(status, params.getDate());
            } else {
                usedDRate = params.getDrate();
            }
        }

        final MemberAccount account = fetchService.fetch(params.getAccount(), MemberAccount.Relationships.MEMBER);
        final Member from = account.getMember();
        final BigDecimal amount = params.getAmount();
        final SystemAccountOwner to = SystemAccountOwner.instance();
        final TransactionFeePreviewForRatesDTO preview = (TransactionFeePreviewForRatesDTO) transactionFeeService.preview(from, to, transferType, amount, usedARate, usedDRate);
        return preview;
    }

    public TransactionFeePreviewForRatesDTO simulateConversionForSelf(final ConversionSimulationDTO params) {
        return simulateConversionForMember(params);
    }

    public TransactionFeePreviewForRatesDTO simulateConversionInternal(final ConversionSimulationDTO params) {
        return simulateConversionForMember(params);
    }

    public Payment simulatePayment(final DoExternalPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException {
        final SimulatePaymentThread thread = new SimulatePaymentThread(params);
        thread.start();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        final Payment payment = thread.getPayment();
        if (payment != null) {
            return payment;
        }
        final Throwable error = thread.getError();
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        }
        throw new IllegalStateException();
    }

    public void validate(final ConversionSimulationDTO dto) {
        final Validator validator = new Validator("");
        validator.property("amount").key("conversionSimulation.amount").required().positiveNonZero();
        if (dto.isUseActualRates()) {
            validator.property("date").key("conversionSimulation.date").required();
        } else {
            final Account account = fetchService.fetch(dto.getAccount(), RelationshipHelper.nested(Account.Relationships.TYPE, AccountType.Relationships.CURRENCY));
            final TransferType transferType = fetchService.fetch(dto.getTransferType(), TransferType.Relationships.TRANSACTION_FEES);
            final Currency currency = account.getType().getCurrency();
            if (currency.isEnableARate() && transferType.isHavingAratedFees()) {
                validator.property("arate").key("conversionSimulation.aRate.targeted").required().positive();
            }
            if (currency.isEnableDRate() && transferType.isHavingDratedFees()) {
                validator.property("drate").key("conversionSimulation.dRate.targeted").required();
            }
        }
        validator.validate(dto);
    }

    public void validate(final DoPaymentDTO payment) {
        getPaymentValidator(payment).validate(payment);
    }

    public TransferDTO verify(final DoPaymentDTO params) {
        // Build and verify the DTO
        final TransferDTO dto = new TransferDTO();
        dto.setAmount(params.getAmount());
        dto.setCurrency(params.getCurrency());
        dto.setChannel(params.getChannel());
        dto.setContext(params.getContext());
        if (params.getDate() != null) {
            dto.setDate(params.getDate());
        }
        dto.setDescription(params.getDescription());
        dto.setFromOwner(params.getFrom() == null ? LoggedUser.accountOwner() : params.getFrom());

        if (!(params instanceof DoExternalPaymentDTO)) {
            dto.setBy(LoggedUser.element());
        }

        dto.setToOwner(params.getTo());
        dto.setTicket(params.getTicket());
        dto.setTransferType(params.getTransferType());
        dto.setReceiver(params.getReceiver());
        dto.setPayments(params.getPayments());
        dto.setCustomValues(params.getCustomValues());

        if (params instanceof DoExternalPaymentDTO) {
            dto.setTraceNumber(((DoExternalPaymentDTO) params).getTraceNumber());
            dto.setClientId(((DoExternalPaymentDTO) params).getClientId());
        }

        verify(dto);

        return dto;
    }

    public boolean wouldRequireAuthorization(final DoPaymentDTO dto) {
        return firstAuthorizationLevel(dto) != null;
    }

    public boolean wouldRequireAuthorization(final Transfer transfer) {
        return firstAuthorizationLevel(transfer.getType(), transfer.getAmount(), transfer.getFromOwner()) != null;
    }

    private Transfer doChargeback(Transfer transfer, final AccountType.Nature expectedFromNature, final boolean checkPermission, final Long clientId) {
        if (!canChargeback(transfer, false)) {
            throw new UnexpectedEntityException();
        }
        transfer = fetchService.fetch(transfer, Transfer.Relationships.TYPE);

        if (LoggedUser.isValid()) {
            if (checkPermission) {
                final Group group = fetchService.fetch((LoggedUser.group()), Group.Relationships.CHARGEBACK_TRANSFER_TYPES);
                if (!group.getChargebackTransferTypes().contains(transfer.getType())) {
                    throw new PermissionDeniedException();
                }
            }
            if (LoggedUser.isMember()) {
                // A member can only chargeback payments he had received
                if (!transfer.getToOwner().equals(LoggedUser.element())) {
                    throw new PermissionDeniedException();
                }
            } else if (LoggedUser.isAdministrator()) {
                // Check the expected from nature
                if (transfer.getFrom().getNature() != expectedFromNature) {
                    throw new PermissionDeniedException();
                }
            }
        }

        // We need to lock both accounts on a chargeback
        accountService.lock(true, Arrays.asList(transfer.getFrom(), transfer.getTo()));

        // Insert the chargeback
        final Transfer chargeback = insertChargeback(transfer, null, clientId);

        // If the transfer type uses a maximum amount per day and the original payment was performed today, return the max amount per day amount
        final BigDecimal maxAmountPerDay = transfer.getType().getMaxAmountPerDay();
        if (maxAmountPerDay != null && DateHelper.daysBetween(transfer.getProcessDate(), Calendar.getInstance()) == 0) {
            synchronized (maxAmountPerDayMap) {
                final Map<Long, BigDecimal> amountsPerTT = maxAmountPerDayMap.get(transfer.getFrom().getId());
                final Long ttId = transfer.getType().getId();
                BigDecimal amountToday = BigDecimalHelper.nvl(amountsPerTT.get(ttId));
                final BigDecimal transferAmount = transfer.getActualAmount();
                if (amountToday.compareTo(transferAmount) <= 0) {
                    amountToday = BigDecimal.ZERO;
                } else {
                    amountToday = amountToday.subtract(transferAmount);
                }
                amountsPerTT.put(ttId, amountToday);
            }
        }
        return chargeback;
    }

    /**
     * Insert the dto and it's generated fees. The verify method must have been called on this dto before invoking this method.
     */
    private Payment doInsert(final TransferDTO dto, final AuthorizationLevel firstAuthorizationLevel) {
        final TransferType transferType = dto.getTransferType();
        final Account fromAccount = fetchService.fetch(dto.getFrom(), RelationshipHelper.nested(Account.Relationships.TYPE, AccountType.Relationships.CURRENCY));
        final Account toAccount = fetchService.fetch(dto.getTo(), MemberAccount.Relationships.MEMBER);

        final BigDecimal amount = dto.getAmount();

        // Update the max amount per day map
        updateMaxAmountPerDay(transferType, fromAccount, amount);

        Payment payment;
        // Check scheduling
        final Calendar today = Calendar.getInstance();
        if (CollectionUtils.isEmpty(dto.getPayments())) {
            // Not scheduled - build a transfer
            final String traceNumber = dto.getTraceNumber();
            final Long clientId = dto.getClientId();

            Transfer transfer = new Transfer();
            transfer.setFrom(fromAccount);
            transfer.setTo(toAccount);
            transfer.setBy(dto.getBy());
            transfer.setDate(today);
            transfer.setAmount(dto.getAmount());
            transfer.setType(transferType);
            transfer.setDescription(dto.getDescription());
            transfer.setAccountFeeLog(dto.getAccountFeeLog());
            transfer.setLoanPayment(dto.getLoanPayment());
            transfer.setParent(dto.getParent());
            transfer.setReceiver(dto.getReceiver());
            transfer.setExternalTransfer(dto.getExternalTransfer());
            transfer.setCustomValues(dto.getCustomValues());
            transfer.setdRate(dto.getDRate());
            transfer.setEmissionDate(dto.getEmissionDate());
            transfer.setTraceNumber(traceNumber);
            transfer.setClientId(clientId);
            if (firstAuthorizationLevel == null) {
                transfer.setProcessDate(dto.getDate() == null ? today : dto.getDate());
                transfer.setStatus(Transfer.Status.PROCESSED);
            } else {
                transfer.setStatus(Transfer.Status.PENDING);
                transfer.setNextAuthorizationLevel(firstAuthorizationLevel);
            }
            // Ensure the from account is locked, to prevent concurrent access to available balance (which could allow an account pass the limit)
            final List<Account> toLock = new ArrayList<Account>();
            toLock.add(fromAccount);
            if (toAccount instanceof MemberAccount && transferType.isAllowSmsNotification()) {
                // If paying to a member, and he will receive notifications via SMS, we should lock atomically the to account, as it will need the
                // lock anyway to get the account balance for the notification
                final MemberAccount memberAccount = (MemberAccount) toAccount;
                final Member to = memberAccount.getMember();
                if (preferenceService.receivedChannels(to, Message.Type.PAYMENT).contains(MessageChannel.SMS)) {
                    toLock.add(memberAccount);
                }
            }
            accountService.lock(true, toLock);
            // Within the critical session, we must check the trace number again, as another thread could have inserted a reverse on it
            if (clientId != null && StringUtils.isNotEmpty(traceNumber)) {
                try {
                    reverseDao.load(clientId, traceNumber);
                    // If didn't throw an EntityNotFoundException, this payment was already reversed, and should fail
                    throw new ValidationException("traceNumber", "transfer.traceNumber", new UniqueError());
                } catch (final EntityNotFoundException e) {
                    // Ok, there are no reverses for it
                }
            }

            // Insert the transfer and pay fees
            transfer = insertTransferAndPayFees(transfer, dto.isForced(), new HashSet<ChargedFee>());
            // Process the authorization automatically when the authorizer is performing a payment as member
            payment = transferAuthorizationService.authorizeOnInsert(transfer);
        } else {
            // Scheduled payment

            final boolean reserveTotalAmount = transferType.isReserveTotalAmountOnScheduling();
            if (reserveTotalAmount && !dto.isForced()) {
                // Ensure the from account is locked, to prevent concurrent access to available balance (which could allow an account pass the limit)
                accountService.lock(true, Arrays.asList(fromAccount));

                // Validate the account has balance for the total amount
                validateAmount(dto.getAmount(), fromAccount, null, null);
            }

            final Collection<PaymentCustomFieldValue> customValues = dto.getCustomValues();
            ScheduledPayment scheduledPayment = new ScheduledPayment();
            scheduledPayment.setFrom(fromAccount);
            scheduledPayment.setTo(toAccount);
            scheduledPayment.setBy(dto.getBy());
            scheduledPayment.setDate(today);
            scheduledPayment.setAmount(dto.getAmount());
            scheduledPayment.setType(transferType);
            scheduledPayment.setDescription(dto.getDescription());
            scheduledPayment.setStatus(Payment.Status.SCHEDULED);
            scheduledPayment.setReserveAmount(reserveTotalAmount);

            scheduledPayment = scheduledPaymentDao.insert(scheduledPayment);
            scheduledPayment.setCustomValues(new ArrayList<PaymentCustomFieldValue>(customValues));
            customFieldService.savePaymentValues(scheduledPayment);

            final List<Transfer> scheduledTransfers = new ArrayList<Transfer>();
            Transfer transferToProcess = null;
            for (final ScheduledPaymentDTO current : dto.getPayments()) {
                final TransferDTO currentDTO = dto.clone();
                currentDTO.setDate(current.getDate());
                currentDTO.setAmount(current.getAmount());
                currentDTO.setScheduledPayment(scheduledPayment);
                Transfer transfer = new Transfer();
                transfer.setFrom(fromAccount);
                transfer.setTo(dto.getTo());
                transfer.setBy(dto.getBy());
                transfer.setDate(current.getDate());
                transfer.setAmount(current.getAmount());
                transfer.setType(transferType);
                transfer.setDescription(dto.getDescription());
                transfer.setStatus(Transfer.Status.SCHEDULED);
                transfer.setScheduledPayment(scheduledPayment);
                transfer.setEmissionDate(today);
                // When the payment is scheduled for today, process it now
                if (DateUtils.isSameDay(today, transfer.getDate())) {
                    transferToProcess = transfer;
                    transfer.setDate(today);
                }
                transfer = transferDao.insert(transfer);
                transfer.setCustomValues(new ArrayList<PaymentCustomFieldValue>());
                if (customValues != null) {
                    for (final PaymentCustomFieldValue fieldValue : customValues) {
                        final PaymentCustomFieldValue newValue = new PaymentCustomFieldValue();
                        newValue.setField(fieldValue.getField());
                        newValue.setStringValue(fieldValue.getStringValue());
                        newValue.setPossibleValue(fieldValue.getPossibleValue());
                        transfer.getCustomValues().add(newValue);
                    }
                }
                customFieldService.savePaymentValues(transfer);
                scheduledTransfers.add(transfer);
            }
            scheduledPayment.setTransfers(scheduledTransfers);

            // When the scheduled payment is set to reserve the amount, process it on a pending account status
            if (scheduledPayment.isReserveAmount()) {
                accountStatusHandler.processReservedScheduledPayment(scheduledPayment);
            }

            if (transferToProcess != null) {
                // This is ugly, I know. Since our AOP is based on proxy, calling a method from the proxied class invokes the "unproxied" method.
                // There's an aspect that sends a message on payNow, so, we must retrieve the proxied instance and invoke the method on it.
                final PaymentService me = (PaymentService) applicationContext.getBean("paymentService");
                me.pay(transferToProcess, today);
            }
            payment = scheduledPayment;
        }

        // Return the transfer object
        return payment;
    }

    private Payment doPayment(final DoPaymentDTO params) {

        // Check permission to pay with date
        if (params.getDate() != null && !permissionService.checkPermission("adminMemberPayments", "paymentWithDate")) {
            throw new PermissionDeniedException();
        }

        // Validate dto
        validate(params);

        // Tests whether there is a valid ticket to be used
        final Ticket ticket = fetchService.fetch(params.getTicket());
        if (ticket != null) {
            if (params.getChannel() != Channel.WEBSHOP || params.getContext() != TransactionContext.PAYMENT || ticket.getStatus() != Ticket.Status.PENDING) {
                throw new EntityNotFoundException(Ticket.class);
            }
            // Force the ticket parameters on the payment
            if (ticket.getAmount() != null && ticket.getAmount().compareTo(getMinimumPayment()) == 1) {
                params.setAmount(ticket.getAmount());
            }
            if (StringUtils.isNotEmpty(ticket.getDescription())) {
                params.setDescription(ticket.getDescription());
            }
            params.setTo(ticket.getTo());
            ticket.setFrom((Member) params.getFrom());
        }

        // Check the authorization
        final AuthorizationLevel firstAuthorizationLevel = firstAuthorizationLevel(params);
        if (firstAuthorizationLevel != null) {
            if (params.getDate() != null && !DateUtils.isSameDay(params.getDate(), Calendar.getInstance())) {
                // Authorized payments are not allowed in past date
                throw new AuthorizedPaymentInPastException();
            }
        }

        // Insert the transfer
        final TransferDTO dto = verify(params);

        final Payment payment = doInsert(dto, firstAuthorizationLevel);

        // Complete the ticket, if it exists
        if (ticket != null) {
            ticket.setAmount(payment.getAmount());
            ticket.setDescription(payment.getDescription());
            if (payment.getFrom().getOwner() instanceof Member) {
                ticket.setFrom((Member) payment.getFrom().getOwner());
            } else {
                ticket.setFrom(null);
            }
            ticket.setTo((Member) payment.getTo().getOwner());
            ticket.setStatus(Ticket.Status.OK);
            ticket.setTransfer((Transfer) payment);
            ticketDao.update(ticket);
        }

        return payment;
    }

    private Transfer doSelfPayment(final DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException {
        preprocessDto(params);
        if (!LoggedUser.accountOwner().equals(params.getTo())) {
            throw new UnexpectedEntityException();
        }
        return (Transfer) doPayment(params);
    }

    /**
     * Resolve the first authorization level for the given payment, if any. When the payment wouldn't be authorizable, return null
     */
    private AuthorizationLevel firstAuthorizationLevel(final DoPaymentDTO params) {
        // Scheduled payments shouldn't be authorized, only it's payments
        if (CollectionUtils.isNotEmpty(params.getPayments())) {
            return null;
        }
        return firstAuthorizationLevel(params.getTransferType(), params.getAmount(), params.getFrom());
    }

    /**
     * Resolve the first authorization level for the given payment, if any. When the payment wouldn't be authorizable, return null
     */
    private AuthorizationLevel firstAuthorizationLevel(TransferType transferType, final BigDecimal amount, AccountOwner from) {
        transferType = fetchService.fetch(transferType, TransferType.Relationships.AUTHORIZATION_LEVELS);
        if (transferType.isRequiresAuthorization() && CollectionUtils.isNotEmpty(transferType.getAuthorizationLevels())) {
            if (from == null) {
                from = LoggedUser.accountOwner();
            }
            final Account account = accountService.getAccount(new AccountDTO(from, transferType.getFrom()));
            final Map<Long, BigDecimal> amountByTransferType = maxAmountPerDayMap.get(account.getId());
            BigDecimal amountSoFarToday = BigDecimal.ZERO;
            if (amountByTransferType != null) {
                final BigDecimal today = amountByTransferType.get(transferType.getId());
                if (today != null) {
                    amountSoFarToday = amountSoFarToday.add(today);
                }
            }
            final AuthorizationLevel authorization = transferType.getAuthorizationLevels().iterator().next();

            // When the amount is greater than the authorization, return true
            final BigDecimal amountToTest = amountSoFarToday.add(amount);
            if (amountToTest.compareTo(authorization.getAmount()) >= 0) {
                return transferType.getAuthorizationLevels().iterator().next();
            }
        }
        return null;
    }

    private Validator getPaymentValidator(final DoPaymentDTO payment) {
        final Validator validator = new Validator("transfer");
        if (payment instanceof DoExternalPaymentDTO) {
            validator.property("context").required().anyOf(TransactionContext.PAYMENT, TransactionContext.AUTOMATIC, TransactionContext.SELF_PAYMENT);
        } else {
            validator.property("context").required().anyOf(TransactionContext.PAYMENT, TransactionContext.SELF_PAYMENT);
        }
        validator.property("to").required().key("payment.recipient");
        // as currency is maybe not set on the DTO, we get it from the TT in stead of directly from the DTO
        final TransferType tt = fetchService.fetch(payment.getTransferType(), Relationships.TRANSACTION_FEES, RelationshipHelper.nested(TransferType.Relationships.FROM, TransferType.Relationships.TO, AccountType.Relationships.CURRENCY, Currency.Relatonships.A_RATE_PARAMETERS), RelationshipHelper.nested(TransferType.Relationships.FROM, TransferType.Relationships.TO, AccountType.Relationships.CURRENCY, Currency.Relatonships.D_RATE_PARAMETERS));
        final Currency currency = tt == null ? null : tt.getCurrency();
        if (currency != null && (currency.isEnableARate() || currency.isEnableDRate())) {
            // if the date is not null at this moment, it is in the past, which is not allowed with rates.
            if (payment.getDate() != null) {
                validator.general(new NoPastDateWithRatesValidator());
            }
        } else {
            validator.property("date").key("payment.manualDate").past();
        }
        validator.property("amount").required().positiveNonZero();
        validator.property("transferType").key("transfer.type").required();
        validator.property("description").maxLength(1000);
        validator.general(new SchedulingValidator());
        validator.general(new PendingContractValidator());
        if (payment.getTransferType() != null && payment.getTo() != null && payment.getAmount() != null) {

            /*
             * For user validation, we need to check if the transaction amount is high enough to cover all fees. This depends on all fees, but only in
             * case of fixed fees it makes sense to increase the transaction amount. The formula for this is: given transactionamount > (sum of fixed
             * fees )/ (1 minus sum of percentage fees expressed as fractions). This of course only applies for fees with deductAmount; fees which are
             * not deducted are excluded from this calculation.
             */
            final TransactionFeePreviewDTO preview = transactionFeeService.preview(payment.getFrom(), payment.getTo(), tt, payment.getAmount());
            final Property amount = validator.property("amount");
            final Collection<? extends TransactionFee> fees = preview.getFees().keySet();
            BigDecimal sumOfFixedFees = BigDecimal.ZERO;
            BigDecimal sumOfPercentageFees = BigDecimal.ZERO;
            for (final TransactionFee fee : fees) {
                if (fee.isDeductAmount()) {
                    if (fee.getChargeType() == ChargeType.FIXED) {
                        sumOfFixedFees = sumOfFixedFees.add(preview.getFees().get(fee));
                    } else {
                        sumOfPercentageFees = sumOfPercentageFees.add(preview.getFees().get(fee));
                    }
                }
            }
            // Show a warning if there are fixed fees and if the amount is not enough to cover them
            if (sumOfFixedFees.signum() == 1) {
                final int scale = LocalSettings.MAX_PRECISION;
                final MathContext mc = new MathContext(scale);
                final BigDecimal sumOfPercentages = sumOfPercentageFees.divide(payment.getAmount(), mc);
                final BigDecimal minimalAmount = sumOfFixedFees.divide((BigDecimal.ONE.subtract(sumOfPercentages)), mc);
                amount.comparable(minimalAmount, ">", new ValidationError("errors.greaterThan", messageResolver.message("transactionFee.invalidChargeValue", minimalAmount)));
            } else if (preview.getFinalAmount().signum() == -1) {
                validator.general(new FinalAmountValidator());
            }

            // Custom fields
            validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
                public Validator getValidator() {
                    return customFieldService.getPaymentValueValidator(payment.getTransferType());
                }
            }));
        }
        return validator;
    }

    private Validator getProjectionValidator() {
        final Validator projectionValidator = new Validator("transfer");
        projectionValidator.property("paymentCount").required().positiveNonZero().add(new PropertyValidation() {
            private static final long serialVersionUID = 5022911381764849941L;

            public ValidationError validate(final Object object, final Object property, final Object value) {
                final Integer paymentCount = (Integer) value;
                if (paymentCount == null) {
                    return null;
                }
                final ProjectionDTO dto = (ProjectionDTO) object;
                final AccountOwner from = dto.getFrom();
                if (from instanceof Member) {
                    final Member member = fetchService.fetch((Member) from, Element.Relationships.GROUP);
                    final int maxSchedulingPayments = member.getMemberGroup().getMemberSettings().getMaxSchedulingPayments();
                    return CompareToValidation.lessEquals(maxSchedulingPayments).validate(object, property, value);
                }
                return null;
            }
        });
        projectionValidator.property("amount").required().positiveNonZero();
        projectionValidator.property("firstExpirationDate").key("transfer.firstPaymentDate").required().add(new PropertyValidation() {
            private static final long serialVersionUID = -3612786027250751763L;

            public ValidationError validate(final Object object, final Object property, final Object value) {
                final Calendar firstDate = CoercionHelper.coerce(Calendar.class, value);
                if (firstDate == null) {
                    return null;
                }
                if (firstDate.before(DateHelper.truncate(Calendar.getInstance()))) {
                    return new InvalidError();
                }
                return null;
            }
        });
        projectionValidator.property("recurrence.number").key("transfer.paymentEvery").required().between(1, 100);
        projectionValidator.property("recurrence.field").key("transfer.paymentEvery").required().anyOf(TimePeriod.Field.DAYS, TimePeriod.Field.WEEKS, TimePeriod.Field.MONTHS);
        return projectionValidator;
    }

    private Validator getTransferValidator(final TransferDTO transfer) {
        final Validator validator = new Validator("transfer");
        // as currency is sometimes not set on the DTO, we get it from the TT in stead of directly from the DTO
        final TransferType tt = fetchService.fetch(transfer.getTransferType(), RelationshipHelper.nested(TransferType.Relationships.FROM, AccountType.Relationships.CURRENCY, Currency.Relatonships.A_RATE_PARAMETERS, Currency.Relatonships.D_RATE_PARAMETERS));
        final Currency currency = tt.getCurrency();
        // if rates enabled, it is not allowed to have a date in the past.
        if (currency.isEnableARate() || currency.isEnableDRate()) {
            final Calendar now = Calendar.getInstance();
            // make a few minutes earlier, because if the transfer's date has just before been set to Calendar.getInstance(), it may already be a
            // few milliseconds or even seconds later.
            now.add(Calendar.MINUTE, -4);
            final Calendar date = transfer.getDate();
            if (date != null && date.before(now)) {
                validator.general(new NoPastDateWithRatesValidator());
            }
        } else {
            validator.property("date").key("payment.manualDate").pastOrToday();
        }
        validator.property("fromOwner").required();
        validator.property("toOwner").required();
        validator.property("amount").required().positiveNonZero();
        validator.property("transferType").key("transfer.type").required();
        validator.property("description").maxLength(1000);
        validator.property("traceNumber").add(new TraceNumberValidation());

        if (transfer.getTransferType() != null) {
            // Custom fields
            validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
                public Validator getValidator() {
                    return customFieldService.getPaymentValueValidator(transfer.getTransferType());
                }
            }));
        }
        return validator;
    }

    private Payment insert(final TransferDTO dto) {
        // Verify the parameters
        verify(dto);
        AuthorizationLevel firstAuthorizationLevel;
        final Transfer parent = fetchService.fetch(dto.getParent(), Transfer.Relationships.NEXT_AUTHORIZATION_LEVEL);
        if (parent != null && parent.getNextAuthorizationLevel() != null) {
            firstAuthorizationLevel = parent.getNextAuthorizationLevel();
        } else {
            firstAuthorizationLevel = firstAuthorizationLevel(dto.getTransferType(), dto.getAmount(), dto.getFromOwner());
        }
        return doInsert(dto, firstAuthorizationLevel);
    }

    private Transfer insertChargeback(Transfer transfer, final Transfer parentChargeback, final Long clientId) {
        transfer = fetchService.fetch(transfer, Transfer.Relationships.CHILDREN);
        // Chargeback a pending payment is a nonsense
        if (transfer.getProcessDate() == null) {
            throw new UnexpectedEntityException();
        }

        // Validate the amount
        validateAmount(transfer.getAmount(), transfer.getTo(), transfer.getFrom(), transfer);

        // Duplicate the transfer, setting relevant properties on the charge-back
        Transfer chargeback = transferDao.duplicate(transfer);
        chargeback.setTraceNumber(null);
        chargeback.setClientId(clientId);
        chargeback.setChargebackOf(transfer);
        chargeback.setParent(parentChargeback);
        chargeback.setAmount(chargeback.getAmount().negate());
        final Calendar today = Calendar.getInstance();
        chargeback.setDate(today);
        chargeback.setProcessDate(today);
        chargeback.setStatus(Payment.Status.PROCESSED);
        if (LoggedUser.isValid()) {
            chargeback.setBy(LoggedUser.element());
        }
        chargeback.setReceiver(null);
        chargeback.setScheduledPayment(null);

        // Build the description according to settings
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("description", transfer.getDescription());
        variables.put("date", localSettings.getDateConverter().toString(transfer.getDate()));
        chargeback.setDescription(MessageProcessingHelper.processVariables(localSettings.getChargebackDescription(), variables));

        // Insert the chargeback
        chargeback = transferDao.insert(chargeback, false);

        // Copy the custom values from the original transfer
        if (CollectionUtils.isNotEmpty(transfer.getCustomValues())) {
            final Collection<PaymentCustomFieldValue> customValues = new ArrayList<PaymentCustomFieldValue>();
            if (transfer.getCustomValues() != null) {
                for (final PaymentCustomFieldValue original : transfer.getCustomValues()) {
                    final PaymentCustomFieldValue newValue = new PaymentCustomFieldValue();
                    newValue.setTransfer(chargeback);
                    newValue.setField(original.getField());
                    newValue.setStringValue(original.getStringValue());
                    newValue.setPossibleValue(original.getPossibleValue());
                    customValues.add(newValue);
                }
            }
            chargeback.setCustomValues(customValues);
            customFieldService.savePaymentValues(chargeback);
        }

        // Update the original transfer
        transfer = transferDao.updateChargeBack(transfer, chargeback);

        // Assign the transaction number
        final TransactionNumber transactionNumber = settingsService.getLocalSettings().getTransactionNumber();
        if (transactionNumber != null && transactionNumber.isValid()) {
            final String generated = transactionNumber.generate(chargeback.getId(), chargeback.getDate());
            transferDao.updateTransactionNumber(chargeback.getId(), generated);
        }
        accountStatusHandler.processTransfer(chargeback);

        // Insert children chargebacks
        for (final Transfer child : transfer.getChildren()) {
            insertChargeback(child, chargeback, clientId);
        }

        return chargeback;
    }

    private void insertFees(final Transfer transfer, final boolean forced, final BigDecimal originalAmount, final Set<ChargedFee> chargedFees) {
        final TransferType transferType = transfer.getType();
        final Account from = transfer.getFrom();
        final Account to = transfer.getTo();
        final TransactionFeeQuery query = new TransactionFeeQuery();
        query.setTransferType(transferType);
        final List<? extends TransactionFee> fees = transactionFeeService.search(query);
        BigDecimal totalPercentage = BigDecimal.ZERO;
        BigDecimal feeTotalAmount = BigDecimal.ZERO;
        Transfer topMost = transfer;
        while (topMost.getParent() != null) {
            topMost = topMost.getParent();
        }
        final Calendar date = topMost.getDate();
        for (final TransactionFee fee : fees) {
            final Account fromAccount = fetchService.fetch(from, Account.Relationships.TYPE, MemberAccount.Relationships.MEMBER);
            final Account toAccount = fetchService.fetch(to, Account.Relationships.TYPE, MemberAccount.Relationships.MEMBER);

            final ChargedFee key = new ChargedFee(fee, fromAccount, toAccount);
            if (chargedFees.contains(key)) {
                throw new ValidationException("payment.error.circularFees");
            }
            chargedFees.add(key);

            // Build the fee transfer
            final BuildTransferWithFeesDTO params = new BuildTransferWithFeesDTO(date, fromAccount, toAccount, originalAmount, fee, false);
            final Transfer feeTransfer = transactionFeeService.buildTransfer(params);

            // If the fee transfer is null, the fee should not be applied
            if (feeTransfer == null) {
                continue;
            }
            // Ensure the last fee when 100% will be the exact amount left
            if (fee instanceof SimpleTransactionFee && fee.getAmount().isPercentage()) {
                final BigDecimal feeValue = fee.getAmount().getValue();
                // Only when it's not a single fee
                if (!(totalPercentage.equals(BigDecimal.ZERO) && feeValue.doubleValue() == 100.0)) {
                    totalPercentage = totalPercentage.add(feeValue);
                    // TODO: shouldn't this be >= 0 in stead of == 0 (Rinke) ?
                    if (totalPercentage.compareTo(new BigDecimal(100)) == 0 && feeTransfer != null) {
                        feeTransfer.setAmount(originalAmount.subtract(feeTotalAmount));
                    }
                }
            }

            // Insert the fee transfer
            if (feeTransfer != null && feeTransfer.getAmount().floatValue() > PRECISION_DELTA) {
                feeTotalAmount = feeTotalAmount.add(feeTransfer.getAmount());
                feeTransfer.setParent(transfer);
                feeTransfer.setDate(transfer.getDate());
                feeTransfer.setStatus(transfer.getStatus());
                feeTransfer.setNextAuthorizationLevel(transfer.getNextAuthorizationLevel());
                feeTransfer.setProcessDate(transfer.getProcessDate());
                feeTransfer.setExternalTransfer(transfer.getExternalTransfer());
                feeTransfer.setBy(transfer.getBy());

                // Copy custom values of common custom fields from the parent to the fee transfer
                final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(feeTransfer.getType());
                if (!CollectionUtils.isEmpty(transfer.getCustomValues())) {
                    final Collection<PaymentCustomFieldValue> feeTransferCustomValues = new ArrayList<PaymentCustomFieldValue>();
                    for (final PaymentCustomFieldValue fieldValue : transfer.getCustomValues()) {
                        final CustomField field = fieldValue.getField();
                        if (customFields.contains(field)) {
                            final PaymentCustomFieldValue newFieldValue = new PaymentCustomFieldValue();
                            newFieldValue.setField(field);
                            newFieldValue.setValue(fieldValue.getValue());
                            feeTransferCustomValues.add(newFieldValue);
                        }
                    }
                    feeTransfer.setCustomValues(feeTransferCustomValues);
                }

                insertTransferAndPayFees(feeTransfer, forced, chargedFees);
            }
        }
    }

    /**
     * Insert a reverse for a transfer with the specified TN and clientId
     * @param traceNumber the traceNumber of the transfer to be reversed
     * @param clientId the clientId of the transfer to be reversed
     */
    private void insertReverse(final String traceNumber, final Long clientId) {
        final InsertReverseThread thread = new InsertReverseThread(traceNumber, clientId);
        thread.start();

        try {
            thread.join();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        thread.throwErrorIfAny();
    }

    /**
     * Insert a transfer and it's generated fees
     */
    private Transfer insertTransferAndPayFees(Transfer transfer, final boolean forced, final Set<ChargedFee> chargedFees) {
        final TransferType transferType = transfer.getType();
        final Collection<PaymentCustomFieldValue> customValues = transfer.getCustomValues();

        final Account fromAccount = transfer.getFrom();
        final Account toAccount = transfer.getTo();
        if (fromAccount.equals(toAccount)) {
            throw new ValidationException("payment.error.sameFromAntToInFee");
        }
        final AccountOwner from = fromAccount.getOwner();
        final AccountOwner to = toAccount.getOwner();

        // Preview fees to determine the deducted amount
        final BigDecimal originalAmount = transfer.getAmount();
        final TransactionFeePreviewDTO preview = transactionFeeService.preview(from, to, transferType, transfer.getAmount());
        transfer.setAmount(preview.getFinalAmount());

        // Insert the parent amount
        if (!forced) {
            validateAmount(transfer.getAmount(), fromAccount, toAccount, transfer);
        }
        transfer.setCustomValues(null);
        transfer = transferDao.insert(transfer);
        final TransactionNumber transactionNumber = settingsService.getLocalSettings().getTransactionNumber();
        if (transactionNumber != null && transactionNumber.isValid()) {
            final String generated = transactionNumber.generate(transfer.getId(), transfer.getDate());
            transferDao.updateTransactionNumber(transfer.getId(), generated);
        }
        transfer.setCustomValues(customValues);
        customFieldService.savePaymentValues(transfer);

        // Insert the account status for both from and to accounts
        accountStatusHandler.processTransfer(transfer);

        // Log this transfer
        loggingHandler.logTransfer(transfer);

        insertFees(transfer, forced, originalAmount, chargedFees);
        return transfer;
    }

    private void preprocessDto(final DoPaymentDTO params) {
        if (params.getFrom() == null && LoggedUser.isValid()) {
            params.setFrom(LoggedUser.accountOwner());
        }
    }

    private Transfer processScheduledTransfer(Transfer transfer, final Calendar time, final boolean failOnError) {
        transfer = fetchService.fetch(transfer, Transfer.Relationships.SCHEDULED_PAYMENT);
        final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
        if (scheduledPayment == null || !transfer.getStatus().canPayNow()) {
            throw new UnexpectedEntityException();
        }

        final Account from = transfer.getFrom();
        final AccountOwner fromOwner = transfer.getFromOwner();
        final BigDecimal amount = transfer.getAmount();
        final Account to = transfer.getTo();
        final TransferType transferType = transfer.getType();
        final AuthorizationLevel firstAuthorizationLevel = firstAuthorizationLevel(transferType, amount, fromOwner);

        try {
            Account fromAccountToValidate = from;
            if (scheduledPayment.isReserveAmount()) {
                // When the scheduled payment has reserved the amount, we don't need to validate the from amount, because it's guaranteed to have as
                // reserved amount, so, we pass fromAccount = null
                fromAccountToValidate = null;
            }
            validateAmount(amount, fromAccountToValidate, to, transfer);
            validateMaxAmountToday(from, transferType, amount);
            final TransactionFeePreviewDTO preview = transactionFeeService.preview(from.getOwner(), to.getOwner(), transferType, amount);
            transfer.setAmount(preview.getFinalAmount());
            if (LoggedUser.isValid()) {
                transfer.setBy(LoggedUser.element());
            }
            boolean shouldLiberateAmount = false;
            if (firstAuthorizationLevel != null) {
                transfer.setStatus(Transfer.Status.PENDING);
                transfer.setNextAuthorizationLevel(firstAuthorizationLevel);
            } else {
                transfer.setStatus(Transfer.Status.PROCESSED);
                transfer.setProcessDate(time);
                shouldLiberateAmount = scheduledPayment.isReserveAmount();

                // Generate the transaction number
                final TransactionNumber transactionNumber = settingsService.getLocalSettings().getTransactionNumber();
                if (transactionNumber != null && transactionNumber.isValid()) {
                    final String generated = transactionNumber.generate(transfer.getId(), transfer.getProcessDate());
                    transfer.setTransactionNumber(generated);
                }
            }

            transferDao.update(transfer);
            insertFees(transfer, false, amount, new HashSet<ChargedFee>());

            // Update the account status
            accountStatusHandler.processTransfer(transfer);
            if (shouldLiberateAmount) {
                accountStatusHandler.liberateReservedAmountForInstallment(transfer);
            }

            // Update scheduled payment status
            updateScheduledPaymentStatus(scheduledPayment);
        } catch (final RuntimeException e) {
            if (failOnError) {
                transferDao.updateStatus(transfer.getId(), Payment.Status.FAILED);
                updateScheduledPaymentStatus(scheduledPayment);

                // Ensure the amount is liberated
                accountStatusHandler.liberateReservedAmountForInstallment(transfer);

                // Generate an alert when it's from system
                if (transfer.isFromSystem()) {
                    final Member member = (Member) transfer.getToOwner();
                    final LocalSettings settings = settingsService.getLocalSettings();
                    final Object[] arguments = { settings.getUnitsConverter(transfer.getType().getFrom().getCurrency().getPattern()).toString(transfer.getAmount()), transfer.getType().getName() };
                    alertService.create(member, MemberAlert.Alerts.SCHEDULED_PAYMENT_FAILED, arguments);
                }
            } else {
                throw e;
            }
        }
        return transfer;
    }

    private void updateMaxAmountPerDay(final TransferType transferType, final Account account, final BigDecimal amount) {
        // Update the amount today
        Map<Long, BigDecimal> accountTransactions;
        synchronized (maxAmountPerDayMap) {
            accountTransactions = maxAmountPerDayMap.get(account.getId());
            if (accountTransactions == null) {
                accountTransactions = new HashMap<Long, BigDecimal>();
                maxAmountPerDayMap.put(account.getId(), accountTransactions);
            }
        }
        BigDecimal amountToday = accountTransactions.get(transferType.getId());
        if (amountToday == null) {
            amountToday = amount;
        } else {
            amountToday = amountToday.add(amount);
        }
        accountTransactions.put(transferType.getId(), amountToday);

        if (LoggedUser.isValid() && LoggedUser.isOperator()) {
            final Operator operator = LoggedUser.element();
            OperatorGroup group = operator.getOperatorGroup();
            group = fetchService.fetch(group, OperatorGroup.Relationships.MAX_AMOUNT_PER_DAY_BY_TRANSFER_TYPE);
            final BigDecimal maxAmount = group.getMaxAmountPerDayByTransferType().get(transferType);
            if (maxAmount != null && maxAmount.compareTo(getMinimumPayment()) > 0) {
                Map<Long, BigDecimal> operatorTransactions;
                synchronized (operatorMaxAmountPerDayMap) {
                    operatorTransactions = operatorMaxAmountPerDayMap.get(operator.getId());
                    if (operatorTransactions == null) {
                        operatorTransactions = new HashMap<Long, BigDecimal>();
                        operatorMaxAmountPerDayMap.put(operator.getId(), operatorTransactions);
                    }
                }
                BigDecimal amountOnDay = operatorTransactions.get(transferType.getId());
                if (amountOnDay == null) {
                    amountOnDay = amount;
                } else {
                    amountOnDay = amountOnDay.add(amount);
                }
                operatorTransactions.put(transferType.getId(), amountOnDay);
            }
        }
    }

    private ScheduledPayment updateScheduledPaymentStatus(ScheduledPayment scheduledPayment) {
        scheduledPayment = fetchService.fetch(scheduledPayment, ScheduledPayment.Relationships.TRANSFERS);
        scheduledPayment.setStatus(Payment.Status.PROCESSED);
        for (final Transfer transfer : scheduledPayment.getTransfers()) {
            if (transfer.getProcessDate() == null) {
                scheduledPayment.setStatus(transfer.getStatus());
                break;
            }
        }
        return scheduledPaymentDao.update(scheduledPayment);
    }

    private void validate(final TransferDTO params) {
        getTransferValidator(params).validate(params);
    }

    /**
     * Validates the given amount
     */
    private void validateAmount(final BigDecimal amount, final Account fromAccount, final Account toAccount, final Transfer transfer) {
        // Validate the from account credit limit ...
        final LocalSettings localSettings = settingsService.getLocalSettings();
        if (fromAccount != null) {
            final BigDecimal creditLimit = fromAccount.getCreditLimit();
            if (creditLimit != null) {
                // ... only if not unlimited
                final AccountStatus fromStatus = accountService.getStatus(new GetTransactionsDTO(fromAccount));
                if (creditLimit.floatValue() > -PRECISION_DELTA) {
                    final BigDecimal available = localSettings.round(fromStatus.getAvailableBalance());
                    if (available.subtract(amount).floatValue() < -PRECISION_DELTA) {
                        final boolean isOriginalAccount = transfer == null ? true : fromAccount.equals(transfer.getRootTransfer().getFrom());
                        throw new NotEnoughCreditsException(fromAccount, amount, isOriginalAccount);
                    }
                }
            }
        }

        // Validate the to account upper credit limit
        if (toAccount != null) {
            final BigDecimal upperCreditLimit = toAccount.getUpperCreditLimit();
            if (upperCreditLimit != null && upperCreditLimit.floatValue() > PRECISION_DELTA) {
                final AccountStatus toStatus = accountService.getStatus(new GetTransactionsDTO(toAccount));
                final BigDecimal balance = toStatus.getBalance();
                if (upperCreditLimit.subtract(balance).subtract(amount).floatValue() < -PRECISION_DELTA) {
                    throw new UpperCreditLimitReachedException(localSettings.getUnitsConverter(toAccount.getType().getCurrency().getPattern()).toString(toAccount.getUpperCreditLimit()), toAccount, amount);
                }
            }
        }
    }

    /**
     * Validates the max amount per day
     */
    private void validateMaxAmountToday(final Account account, final TransferType transferType, final BigDecimal amount) {
        // Check if the day has changed
        final Calendar now = Calendar.getInstance();
        if (!DateUtils.isSameDay(now, lastPaymentDate)) {
            // Update the last payment date
            lastPaymentDate = now;
            // Clear the max amount per day control map...
            synchronized (maxAmountPerDayMap) {
                maxAmountPerDayMap.clear();
            }
            // Clear the operators max amount per day
            synchronized (operatorMaxAmountPerDayMap) {
                operatorMaxAmountPerDayMap.clear();
            }
        }

        // Test the max amount per day
        final BigDecimal maxAmountPerDay = transferType.getMaxAmountPerDay();
        if (maxAmountPerDay != null && maxAmountPerDay.floatValue() > PRECISION_DELTA) {
            // Lookup the transfer type / amount map
            Map<Long, BigDecimal> accountTransactions;
            synchronized (maxAmountPerDayMap) {
                accountTransactions = maxAmountPerDayMap.get(account.getId());
                if (accountTransactions == null) {
                    accountTransactions = new HashMap<Long, BigDecimal>();
                    maxAmountPerDayMap.put(account.getId(), accountTransactions);
                }
            }
            // Get the amount on today
            BigDecimal amountOnDay = accountTransactions.get(transferType.getId());
            if (amountOnDay == null) {
                amountOnDay = BigDecimal.ZERO;
            }
            // Validate
            if (amountOnDay.add(amount).compareTo(maxAmountPerDay) > 0) {
                throw new MaxAmountPerDayExceededException(transferType, account, amount);
            }
        }

        // Test the operator max amount per day
        if (LoggedUser.isValid() && LoggedUser.isOperator()) {
            final Operator operator = LoggedUser.element();
            OperatorGroup group = operator.getOperatorGroup();
            group = fetchService.fetch(group, OperatorGroup.Relationships.MAX_AMOUNT_PER_DAY_BY_TRANSFER_TYPE);
            final BigDecimal maxAmount = group.getMaxAmountPerDayByTransferType().get(transferType);
            if (maxAmount != null && maxAmount.floatValue() > PRECISION_DELTA) {
                // Lookup the transfer type / amount map
                Map<Long, BigDecimal> operatorTransactions;
                synchronized (operatorMaxAmountPerDayMap) {
                    operatorTransactions = operatorMaxAmountPerDayMap.get(operator.getId());
                    if (operatorTransactions == null) {
                        operatorTransactions = new HashMap<Long, BigDecimal>();
                        operatorMaxAmountPerDayMap.put(operator.getId(), operatorTransactions);
                    }
                }
                // Get the amount on today
                BigDecimal amountOnDay = operatorTransactions.get(transferType.getId());
                if (amountOnDay == null) {
                    amountOnDay = BigDecimal.ZERO;
                }
                // Validate
                if (amountOnDay.add(amount).compareTo(maxAmount) == 1) {
                    throw new MaxAmountPerDayExceededException(transferType, account, amount);
                }
            }
        }
    }

    /**
     * Validates if a given transfer type is valid
     */
    private TransferType validateTransferType(final TransferDTO params) {
        final TransferType transferType = transferTypeService.load(params.getTransferType().getId(), TransferType.Relationships.FROM, TransferType.Relationships.TO);
        final TransferTypeQuery ttQuery = new TransferTypeQuery();
        ttQuery.setChannel(params.getChannel());
        if (params.isAutomatic()) {
            ttQuery.setContext(TransactionContext.AUTOMATIC);
        } else {
            ttQuery.setContext(params.getContext());
        }
        final TransactionContext context = ttQuery.getContext();
        if (context != TransactionContext.AUTOMATIC && context != TransactionContext.AUTOMATIC_LOAN) {
            ttQuery.setUsePriority(true);
        }
        ttQuery.setCurrency(params.getCurrency());
        ttQuery.setFromAccountType(transferType.getFrom());
        ttQuery.setToAccountType(transferType.getTo());
        final AccountOwner fromOwner = params.getFromOwner();
        if (params.getBy() != null && fromOwner != null && !params.getBy().getAccountOwner().equals(fromOwner)) {
            // Set by when performing a payment in behalf of someone
            ttQuery.setBy(params.getBy());
        } else if (context != TransactionContext.AUTOMATIC && context != TransactionContext.AUTOMATIC_LOAN) {
            // Test the permission for the payment
            if (fromOwner instanceof Member) {
                ttQuery.setGroup(((Member) fromOwner).getGroup());
            } else if (LoggedUser.isValid()) {
                ttQuery.setGroup(LoggedUser.group());
            }
        }
        ttQuery.setFromOwner(fromOwner);
        ttQuery.setToOwner(params.getToOwner());
        final List<TransferType> possibleTypes = transferTypeService.search(ttQuery);
        if (possibleTypes == null || !possibleTypes.contains(transferType)) {
            throw new UnexpectedEntityException("Transfer type not found for query");
        }
        return transferType;
    }

    private void verify(final TransferDTO params) {

        if (params.getFrom() != null) {
            final Account from = fetchService.fetch(params.getFrom(), MemberAccount.Relationships.MEMBER);
            params.setFromOwner(from.getOwner());
        }
        if (params.getTo() != null) {
            final Account to = fetchService.fetch(params.getTo(), MemberAccount.Relationships.MEMBER);
            params.setToOwner(to.getOwner());
        }

        validate(params);

        final AccountOwner fromOwner = params.getFromOwner();
        final AccountOwner toOwner = params.getToOwner();

        // Validate the transfer type
        final TransferType transferType = validateTransferType(params);

        // Retrieve the from and to accounts
        final Account fromAccount = accountService.getAccount(new AccountDTO(fromOwner, transferType.getFrom()));
        final Account toAccount = accountService.getAccount(new AccountDTO(toOwner, transferType.getTo()));

        if (fromAccount.equals(toAccount)) {
            throw new ValidationException(new ValidationError("payment.error.sameAccount"));
        }

        // Retrieve the amount
        final BigDecimal amount = params.getAmount();

        // Check the minimum payment
        if (amount.compareTo(getMinimumPayment()) == -1) {
            final LocalSettings localSettings = settingsService.getLocalSettings();

            throw new TransferMinimumPaymentException(localSettings.getUnitsConverter(fromAccount.getType().getCurrency().getPattern()).toString(getMinimumPayment()), fromAccount, amount);
        }

        // Validate the max amount today
        validateMaxAmountToday(fromAccount, transferType, amount);

        // Update some retrieved parameters on the DTO
        params.setTransferType(transferType);
        params.setFrom(fromAccount);
        params.setTo(toAccount);
        if (StringUtils.isEmpty(params.getDescription())) {
            params.setDescription(transferType.getDescription());
        }
    }

}