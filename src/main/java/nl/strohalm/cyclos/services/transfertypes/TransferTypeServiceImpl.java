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
package nl.strohalm.cyclos.services.transfertypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.transactions.AuthorizationLevelDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferTypeDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.AccountType.Nature;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanParameters;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.exceptions.HasPendingPaymentsException;
import nl.strohalm.cyclos.utils.Amount;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PositiveNonZeroValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation class for transfer types
 * @author rafael
 * @author Rinke (conversion methods)
 */
public class TransferTypeServiceImpl implements TransferTypeService {

    private final class DestinationAccountTypeValidator implements PropertyValidation {

        private static final long serialVersionUID = -1068050406929695757L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final TransferType transferType = (TransferType) object;

            // Get source and destination account types
            final AccountType from = fetchService.fetch(transferType.getFrom(), AccountType.Relationships.CURRENCY);
            AccountType to = (AccountType) value;

            // Validate if the currency of the source account type is the same currency of the destination account type
            final Currency sourceAccountTypeCurrency = from.getCurrency();
            to = fetchService.fetch(to, AccountType.Relationships.CURRENCY);
            final Currency destinationAccountTypeCurrency = to.getCurrency();
            if (!sourceAccountTypeCurrency.equals(destinationAccountTypeCurrency)) {
                return new ValidationError("transferType.error.invalidDestinationType");
            }

            if (from != null && to != null) {
                if (from.equals(to) && from.getNature() == AccountType.Nature.SYSTEM) {
                    // Cannot be from and to the same account if system
                    return new InvalidError();
                }
                if (transferType.isLoanType() && ((from.getNature() == AccountType.Nature.MEMBER) || (to.getNature() == AccountType.Nature.SYSTEM))) {
                    // When is a loan, can only be from system to member
                    return new InvalidError();
                }
            }

            return null;
        }
    }

    /**
     * A property validator that is only used when transaction feedback is required
     * @author luis
     */
    private class FeedbackValidator implements PropertyValidation {

        private static final long  serialVersionUID = 2435741054912450932L;
        private PropertyValidation validation;

        public FeedbackValidator(final PropertyValidation validation) {
            this.validation = validation;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final TransferType tt = (TransferType) object;
            if (tt.isRequiresFeedback()) {
                return validation.validate(object, property, value);
            }
            return null;
        }
    }

    /**
     * Validates a repayment type to be required if it's associated component is present
     * @author luis
     */
    private final class LoanWithInterestRepaymentTypeValidator implements PropertyValidation {
        private static final long serialVersionUID = -3441031471188004677L;
        private final String      property;
        private final boolean     toSystem;

        public LoanWithInterestRepaymentTypeValidator(final String property, final boolean toSystem) {
            this.property = property;
            this.toSystem = toSystem;
        }

        public ValidationError validate(final Object object, final Object name, final Object value) {
            final LoanParameters loan = (LoanParameters) object;
            final TransferType repayment = fetchService.fetch((TransferType) value, TransferType.Relationships.FROM, TransferType.Relationships.TO);
            ValidationError error = null;
            if (loan.getType() == Loan.Type.WITH_INTEREST) {
                final Object related = PropertyHelper.get(loan, property);
                boolean required = false;
                if (related instanceof Amount) {
                    final Amount amount = ((Amount) related);
                    required = amount != null && amount.getValue() != null && amount.getValue().compareTo(BigDecimal.ZERO) == 1;
                } else if (related instanceof BigDecimal) {
                    final BigDecimal f = ((BigDecimal) related);
                    required = (f != null) && (f.compareTo(BigDecimal.ZERO) == 1);
                }
                if (required) {
                    // There must be a repayment type for this value
                    error = RequiredValidation.instance().validate(object, name, value);
                    if (error == null) {
                        // Validates the TT direction
                        if (toSystem && !(!repayment.isFromSystem() && repayment.isToSystem())) {
                            // From system to member
                            error = new InvalidError();
                        } else if (!toSystem && !(repayment.isFromSystem() && !repayment.isToSystem())) {
                            // From member to system
                            error = new InvalidError();
                        }
                    }
                }
            }
            return error;
        }
    }

    private static final float    PRECISION_DELTA = 0.0001F;
    private AuthorizationLevelDAO authorizationLevelDao;
    private FetchService          fetchService;
    private TransferTypeDAO       transferTypeDao;
    private PaymentService        paymentService;
    private AccountService        accountService;
    private GroupService          groupService;
    private ChannelService        channelService;

    public List<Channel> channelsForTransferTypeEdition() {
        return channelService.list();
    }

    public List<TransferType> getConversionTTs() {
        final TransferTypeQuery ttQuery = makeConversionTransferTypeQuery();
        return search(ttQuery);
    }

    public List<TransferType> getConversionTTs(final AccountType fromAccountType) {
        final TransferTypeQuery ttQuery = makeConversionTransferTypeQuery();
        final List<AccountType> accountTypes = new ArrayList<AccountType>(1);
        accountTypes.add(fromAccountType);
        ttQuery.setFromAccountTypes(accountTypes);
        return search(ttQuery);
    }

    public List<TransferType> getConversionTTs(final Currency currency) {
        final TransferTypeQuery ttQuery = makeConversionTransferTypeQuery();
        ttQuery.setCurrency(currency);
        return search(ttQuery);
    }

    /**
     * gets transferTypes possibly being a conversion, and having an A-Rated TransferFee.
     */
    public List<TransferType> listARatedTTs() {
        final List<TransferType> conversionTTs = getConversionTTs();
        final List<TransferType> result = new ArrayList<TransferType>(conversionTTs.size());
        for (final TransferType tt : conversionTTs) {
            if (tt.isHavingAratedFees()) {
                result.add(tt);
            }
        }
        return result;
    }

    public TransferType load(final Long id, final Relationship... fetch) {
        return transferTypeDao.load(id, fetch);
    }

    public int remove(final Long... ids) {
        return transferTypeDao.delete(ids);
    }

    public TransferType save(TransferType transferType) {
        // Validates the transfer type, if validation fails,
        // a Validation exception will be thrown
        validate(transferType);
        if (transferType.getContext().isSelfPayment() && transferType.getFrom().getClass() == Nature.SYSTEM.getType() && transferType.getTo().getClass() == Nature.MEMBER.getType()) {
            final TransferType.Context context = new TransferType.Context();
            context.setPayment(true);
            transferType.setContext(context);
        } else if (transferType.getContext().isPayment() && transferType.getFrom().getClass() == Nature.SYSTEM.getType() && transferType.getTo().getClass() == Nature.SYSTEM.getType()) {
            final TransferType.Context context = new TransferType.Context();
            context.setSelfPayment(true);
            transferType.setContext(context);
        }

        // Clear all loan parameters if they are not valid
        if (!transferType.isLoanType()) {
            transferType.setLoan(null);
        }

        if (transferType.isTransient()) {
            return transferTypeDao.insert(transferType);
        } else {
            // We must keep the many-to-many relationships
            final TransferType current = load(transferType.getId(), TransferType.Relationships.PAYMENT_FILTERS, TransferType.Relationships.LINKED_CUSTOM_FIELDS);
            transferType.setPaymentFilters(new ArrayList<PaymentFilter>(current.getPaymentFilters()));
            transferType.setLinkedCustomFields(new ArrayList<PaymentCustomField>(current.getLinkedCustomFields()));

            if (current.isRequiresAuthorization() && !transferType.isRequiresAuthorization()) {
                // Authorization has been disabled. Raise an error if there are any pending payments
                final TransferQuery query = new TransferQuery();
                query.setPageForCount();
                query.setTransferType(transferType);
                query.setRequiresAuthorization(true);
                query.setStatus(Payment.Status.PENDING);
                final int payments = PageHelper.getTotalCount(paymentService.search(query));
                if (payments > 0) {
                    throw new HasPendingPaymentsException();
                }
            }

            // Update the feedbackEnabledSince
            if (current.isRequiresFeedback() && !transferType.isRequiresFeedback()) {
                // It was enabled but is no longer - remove the enabled since
                transferType.setFeedbackEnabledSince(null);
            } else if (!current.isRequiresFeedback() && transferType.isRequiresFeedback()) {
                // It was not enabled but is now - set the enabled since
                transferType.setFeedbackEnabledSince(Calendar.getInstance());
            } else {
                // Keep it as is
                transferType.setFeedbackEnabledSince(current.getFeedbackEnabledSince());
            }
            transferType = transferTypeDao.update(transferType);

            // If transfer type does not require authorization, clean authorization levels
            final Collection<AuthorizationLevel> authorizationLevels = transferType.getAuthorizationLevels();
            if (!transferType.isRequiresAuthorization() && !CollectionUtils.isEmpty(authorizationLevels)) {
                for (final AuthorizationLevel authorizationLevel : authorizationLevels) {
                    authorizationLevelDao.delete(authorizationLevel.getId());
                }
            }
            return transferType;
        }
    }

    public List<TransferType> search(final TransferTypeQuery query) {
        // If searching for an operator group permissions, the transfer types are the same as his member's. So we have to actually check by his
        // member's permissions
        final Group group = fetchService.fetch(query.getGroup(), RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
        if (group instanceof OperatorGroup) {
            final OperatorGroup operatorGroup = (OperatorGroup) group;
            query.setGroup(operatorGroup.getMember().getGroup());
        }

        final TransferTypeQuery finalQuery = query.clone();
        finalQuery.setUsePriority(false);
        if (query.isUsePriority()) {
            query.setPriority(true);
            query.setPageForCount();
            final int totalCount = PageHelper.getTotalCount(transferTypeDao.search(query));
            finalQuery.setPriority(totalCount > 0);
        }

        // When fromOwner is a member, ensure disabled accounts (when credit limit is zero) are not used
        if (query.getFromOwner() instanceof Member) {
            final Member member = fetchService.fetch((Member) query.getFromOwner(), RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));
            final List<AccountType> accountTypes = new ArrayList<AccountType>();
            final List<? extends Account> accounts = accountService.getAccounts(member);
            for (final Account account : accounts) {
                boolean hidden = false;
                try {
                    final MemberGroupAccountSettings accountSettings = groupService.loadAccountSettings(member.getGroup().getId(), account.getType().getId());
                    if (accountSettings.isHideWhenNoCreditLimit() && Math.abs(account.getCreditLimit().floatValue()) < PRECISION_DELTA) {
                        hidden = true;
                    }
                } catch (final EntityNotFoundException e) {
                    continue;
                }
                if (!hidden) {
                    accountTypes.add(account.getType());
                }
            }
            if (CollectionUtils.isNotEmpty(finalQuery.getFromAccountTypes())) {
                // When there were already from account types set, retain them only
                accountTypes.retainAll(finalQuery.getFromAccountTypes());
            }
            // Finally, ensure that only transfer types from visible accounts are used
            finalQuery.setFromAccountTypes(accountTypes);
        }
        return transferTypeDao.search(finalQuery);
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAuthorizationLevelDao(final AuthorizationLevelDAO authorizationLevelDao) {
        this.authorizationLevelDao = authorizationLevelDao;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setTransferTypeDao(final TransferTypeDAO dao) {
        transferTypeDao = dao;
    }

    public void validate(final TransferType transferType) {
        if (transferType.isLoanType()) {
            getLoanValidator().validate(transferType);
        } else {
            getValidator().validate(transferType);
        }
    }

    private Validator createValidator() {
        final Validator validator = new Validator("transferType");
        validator.property("name").required().maxLength(100);
        validator.property("description").required().maxLength(1000);
        validator.property("confirmationMessage").maxLength(4000);
        validator.property("from").required();
        validator.property("to").required().add(new DestinationAccountTypeValidator());
        validator.property("maxAmountPerDay").positiveNonZero();
        validator.property("feedbackExpirationTime.number").key("transferType.feedbackExpirationTime").add(new FeedbackValidator(RequiredValidation.instance())).add(new FeedbackValidator(PositiveNonZeroValidation.instance()));
        validator.property("feedbackExpirationTime.field").key("transferType.feedbackExpirationTime").add(new FeedbackValidator(RequiredValidation.instance()));
        validator.property("feedbackReplyExpirationTime.number").key("transferType.feedbackReplyExpirationTime").add(new FeedbackValidator(RequiredValidation.instance())).add(new FeedbackValidator(PositiveNonZeroValidation.instance()));
        validator.property("feedbackReplyExpirationTime.field").key("transferType.feedbackReplyExpirationTime").add(new FeedbackValidator(RequiredValidation.instance()));
        validator.property("defaultFeedbackComments").add(new FeedbackValidator(RequiredValidation.instance()));
        validator.property("defaultFeedbackLevel").add(new FeedbackValidator(RequiredValidation.instance()));
        return validator;
    }

    private Validator getLoanValidator() {
        final Validator loanValidator = createValidator();

        // Chain the loan parameters validator
        final Validator nestedValidator = new Validator("loan", "loan");
        loanValidator.chained(nestedValidator);

        nestedValidator.property("repaymentType").add(new PropertyValidation() {
            private static final long serialVersionUID = -3441031471188004677L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final LoanParameters lp = ((LoanParameters) object);
                ValidationError error = null;
                if (lp != null && lp.getType() != null) {
                    // There must be a repayment type on loan types
                    error = RequiredValidation.instance().validate(object, name, value);
                    if (error == null) {
                        // Validate the repayment type as being from member to system
                        final TransferType repayment = fetchService.fetch((TransferType) value, TransferType.Relationships.FROM, TransferType.Relationships.TO);
                        if (!(!repayment.isFromSystem() && repayment.isToSystem())) {
                            // Must be from member to system
                            error = new InvalidError();
                        }
                    }
                }
                return error;
            }
        });
        nestedValidator.property("repaymentDays").positiveNonZero().add(new PropertyValidation() {
            private static final long serialVersionUID = -3665200579172755969L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final LoanParameters lp = ((LoanParameters) object);
                if (lp != null && lp.getType() == Loan.Type.SINGLE_PAYMENT) {
                    // RepaymentDays is required on single payment type
                    return RequiredValidation.instance().validate(object, name, value);
                }
                return null;
            }
        });
        nestedValidator.property("grantFee").positiveNonZero();
        nestedValidator.property("grantFeeRepaymentType").add(new LoanWithInterestRepaymentTypeValidator("grantFee", true));
        nestedValidator.property("monthlyInterest").positiveNonZero();
        nestedValidator.property("monthlyInterestRepaymentType").add(new LoanWithInterestRepaymentTypeValidator("monthlyInterest", true));
        nestedValidator.property("expirationFee").positiveNonZero();
        nestedValidator.property("expirationFeeRepaymentType").add(new LoanWithInterestRepaymentTypeValidator("expirationFee", true));
        nestedValidator.property("expirationDailyInterest").positiveNonZero();
        nestedValidator.property("expirationDailyInterestRepaymentType").add(new LoanWithInterestRepaymentTypeValidator("expirationDailyInterest", true));
        return loanValidator;
    }

    private Validator getValidator() {
        return createValidator();
    }

    private TransferTypeQuery makeConversionTransferTypeQuery() {
        final TransferTypeQuery ttQuery = new TransferTypeQuery();
        ttQuery.setContext(TransactionContext.PAYMENT);
        ttQuery.setFromNature(AccountType.Nature.MEMBER);
        ttQuery.setToNature(AccountType.Nature.SYSTEM);
        ttQuery.setToLimitType(AccountType.LimitType.UNLIMITED);
        return ttQuery;
    }

}