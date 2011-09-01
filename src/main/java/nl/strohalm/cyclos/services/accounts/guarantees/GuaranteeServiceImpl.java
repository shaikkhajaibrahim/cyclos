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

package nl.strohalm.cyclos.services.accounts.guarantees;

import java.math.BigDecimal;
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

import nl.strohalm.cyclos.dao.accounts.guarantees.GuaranteeDAO;
import nl.strohalm.cyclos.dao.accounts.guarantees.GuaranteeLogDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.guarantees.Certification;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeLog;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeQuery;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligation;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligationLog;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee.Status;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType.FeeType;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.accounts.guarantees.exceptions.ActiveCertificationNotFoundException;
import nl.strohalm.cyclos.services.accounts.guarantees.exceptions.CertificationAmountExceededException;
import nl.strohalm.cyclos.services.accounts.guarantees.exceptions.CertificationValidityExceededException;
import nl.strohalm.cyclos.services.accounts.guarantees.exceptions.GuaranteeStatusChangeException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.GrantSinglePaymentLoanDTO;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.conversion.NumberConverter;
import nl.strohalm.cyclos.utils.guarantees.GuaranteesHelper;
import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.DelegatingValidator;
import nl.strohalm.cyclos.utils.validation.PeriodValidation;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.PeriodValidation.ValidationType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.AndPredicate;

public class GuaranteeServiceImpl implements GuaranteeService {

    private GuaranteeDAO             guaranteeDao;
    private GuaranteeLogDAO          guaranteeLogDao;
    private PermissionService        permissionService;
    private GuaranteeTypeService     guaranteeTypeService;
    private GroupService             groupService;
    private PaymentObligationService paymentObligationService;
    private CertificationService     certificationService;
    private FetchService             fetchService;
    private LoanService              loanService;
    private PaymentService           paymentService;
    private SettingsService          settingsService;
    private CustomFieldService       customFieldService;
    private MessageResolver          messageResolver = new MessageResolver.NoOpMessageResolver();

    public Guarantee acceptGuaranteeAsMember(final Guarantee guarantee, final boolean automaticLoanAuthorization) {
        return changeStatus(guarantee, Guarantee.Status.ACCEPTED, automaticLoanAuthorization);
    }

    public BigDecimal calculateFee(final GuaranteeFeeCalculationDTO dto) {
        return GuaranteesHelper.calculateFee(dto.getValidity(), dto.getAmount(), dto.getFeeSpec());
    }

    public Guarantee cancelGuaranteeAsMember(final Guarantee guarantee) {
        return changeStatus(guarantee, Guarantee.Status.CANCELLED);
    }

    public boolean canChangeStatus(final Guarantee guarantee, final Status newStatus) {
        switch (newStatus) {
            case ACCEPTED:
            case REJECTED:
                if (isInSomeStatus(guarantee, Status.PENDING_ADMIN)) {
                    final boolean hasPermission = permissionService.checkPermission("adminMemberGuarantees", Guarantee.Status.ACCEPTED == newStatus ? "acceptGuaranteesAsMember" : "cancelGuaranteesAsMember");
                    return hasPermission;
                } else if (isInSomeStatus(guarantee, Status.PENDING_ISSUER)) {
                    return isIssuer() && guarantee.getIssuer().equals(LoggedUser.accountOwner());
                } else {
                    return false;
                }
            case CANCELLED:
                final boolean hasPermission = permissionService.checkPermission("adminMemberGuarantees", "cancelGuaranteesAsMember");
                return hasPermission && isInSomeStatus(guarantee, Status.ACCEPTED, Status.PENDING_ADMIN, Status.PENDING_ISSUER) && guarantee.getLoan() == null;
            default:
                throw new GuaranteeStatusChangeException(newStatus, "Can't change guarantee's status, unsupported target status: " + newStatus);
        }
    }

    /**
     * Only an administrator can delete a just registered (pending by administrator) guarantee registered by himself
     * @param guarantee
     */
    public boolean canRemoveGuarantee(Guarantee guarantee) {
        guarantee = fetchService.fetch(guarantee, Guarantee.Relationships.LOGS, Guarantee.Relationships.GUARANTEE_TYPE);
        final boolean currentStatusIsPendingByAdmin = guarantee.getStatus() == Status.PENDING_ADMIN;
        final GuaranteeLog log = guarantee.getLogs().iterator().next();
        final boolean isOnlyPendingByAdmin = guarantee.getLogs().size() == 1 && log.getStatus() == Status.PENDING_ADMIN;

        return LoggedUser.isAdministrator() && currentStatusIsPendingByAdmin && isOnlyPendingByAdmin && log.getBy().equals(LoggedUser.element()) && guarantee.getGuaranteeType().getModel() != GuaranteeType.Model.WITH_PAYMENT_OBLIGATION;
    }

    public Guarantee changeStatus(final Guarantee guarantee, final Status newStatus) {
        return changeStatus(guarantee, newStatus, false);
    }

    public Guarantee denyGuaranteeAsMember(final Guarantee guarantee) {
        return changeStatus(guarantee, Guarantee.Status.REJECTED);
    }

    public Collection<? extends MemberGroup> getBuyers() {
        if (LoggedUser.isAdministrator()) {
            return filterBuyers();
        } else if (isIssuer()) {
            final MemberGroup group = fetchService.fetch((MemberGroup) ((Member) LoggedUser.accountOwner()).getGroup(), MemberGroup.Relationships.CAN_ISSUE_CERTIFICATION_TO_GROUPS);
            return group.getCanIssueCertificationToGroups();
        } else { // is a seller
            return guaranteeDao.getBuyers(((Member) LoggedUser.accountOwner()).getGroup());
        }
    }

    public List<Guarantee> getGuarantees(final Certification certification, final PageParameters pageParameters, final List<Status> statusList) {
        final GuaranteeQuery guaranteeQuery = new GuaranteeQuery();
        guaranteeQuery.setResultType(ResultType.PAGE);
        guaranteeQuery.setPageParameters(pageParameters);
        guaranteeQuery.setCertification(certification);
        guaranteeQuery.setStatusList(statusList);
        guaranteeQuery.fetch(RelationshipHelper.nested(Guarantee.Relationships.LOAN, Loan.Relationships.PAYMENTS));
        return guaranteeDao.search(guaranteeQuery);
    }

    public Collection<? extends MemberGroup> getIssuers() {
        return filterIssuers();
    }

    public Collection<? extends MemberGroup> getIssuers(final GuaranteeType guaranteeType) {
        final Collection<? extends Group> groups = guaranteeDao.getIssuers(guaranteeType);

        // we must filter the list because it might contains System or removed (issuers) groups
        return filterMemberGroups(null, groups);

    }

    public MessageResolver getMessageResolver(final MessageResolver messageResolver) {
        return messageResolver;
    }

    public Collection<GuaranteeType.Model> getRelatedGuaranteeModels(final Member member) {
        return guaranteeDao.getRelatedGuaranteeModels(member);
    }

    public Collection<? extends MemberGroup> getSellers() {
        if (LoggedUser.isAdministrator()) {
            return filterSellers();
        } else {
            if (isBuyer()) {
                final MemberGroup group = fetchService.fetch((MemberGroup) ((Member) LoggedUser.accountOwner()).getGroup(), MemberGroup.Relationships.CAN_BUY_WITH_PAYMENT_OBLIGATIONS_FROM_GROUPS);
                return group.getCanBuyWithPaymentObligationsFromGroups();
            } else { // is an issuer
                return guaranteeDao.getSellers(((Member) LoggedUser.accountOwner()).getGroup());
            }
        }
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public List<Guarantee> guaranteesToProcess(Calendar time) {
        time = DateHelper.truncate(time);
        final GuaranteeQuery query = new GuaranteeQuery();
        query.setResultType(ResultType.ITERATOR);
        final Set<Relationship> fetch = new HashSet<Relationship>();
        fetch.add(Guarantee.Relationships.GUARANTEE_TYPE);
        fetch.add(Guarantee.Relationships.LOGS);
        query.setFetch(fetch);
        query.setStatusList(Arrays.asList(Guarantee.Status.PENDING_ADMIN, Guarantee.Status.PENDING_ISSUER));

        final List<Guarantee> result = new ArrayList<Guarantee>();

        final List<Guarantee> guarantees = guaranteeDao.search(query);
        for (final Guarantee guarantee : guarantees) {
            final TimePeriod period = guarantee.getGuaranteeType().getPendingGuaranteeExpiration();
            final Calendar lowerBound = period.remove(time);
            final Calendar registrationDate = DateHelper.truncate(guarantee.getRegistrationDate());
            if (registrationDate.before(lowerBound)) {
                result.add(guarantee);
            }
        }
        return result;
    }

    public boolean isBuyer() {
        return LoggedUser.isValid() && permissionService.checkPermission(LoggedUser.group(), getModuleName(LoggedUser.group()), "buyWithPaymentObligations");
    }

    public boolean isIssuer() {
        return LoggedUser.isValid() && permissionService.checkPermission(LoggedUser.group(), getModuleName(LoggedUser.group()), "issueGuarantees");
    }

    public boolean isSeller() {
        return LoggedUser.isValid() && permissionService.checkPermission(LoggedUser.group(), getModuleName(LoggedUser.group()), "sellWithPaymentObligations");
    }

    public Guarantee load(final Long id, final Relationship... fetch) {
        Guarantee guarantee = guaranteeDao.load(id, fetch);
        guarantee = fetchService.fetch(guarantee, Guarantee.Relationships.GUARANTEE_TYPE, Guarantee.Relationships.ISSUER, RelationshipHelper.nested(Guarantee.Relationships.BUYER, Element.Relationships.GROUP), RelationshipHelper.nested(Guarantee.Relationships.SELLER, Element.Relationships.GROUP));

        final PermissionRequestorImpl permissionRequestor = new PermissionRequestorImpl();
        permissionRequestor.adminPermissions("adminMemberGuarantees", "viewGuarantees").member().operator();

        // we have a permission to define a member as a guarantee's issuer then
        // if the logged user is the guarantee's issuer and he doesn't have permission we must reject him
        if (guarantee.getIssuer().equals(LoggedUser.element()) && !permissionService.checkPermission("memberGuarantees", "issueGuarantees")) {
            throw new PermissionDeniedException();
        } else if (guarantee.getIssuer().equals(LoggedUser.element())) {
            return guarantee;
        }

        if (guarantee.getGuaranteeType().getModel() == GuaranteeType.Model.WITH_BUYER_ONLY) {
            permissionRequestor.manages(guarantee.getBuyer());
        } else {
            permissionRequestor.manages(guarantee.getBuyer(), guarantee.getSeller());
        }

        if (!permissionService.checkPermissions(permissionRequestor)) {
            throw new PermissionDeniedException();
        }

        return guarantee;
    }

    public Guarantee processGuarantee(final Guarantee guarantee, final Calendar time) {
        guarantee.setStatus(Guarantee.Status.WITHOUT_ACTION);
        final GuaranteeLog log = guarantee.changeStatus(Guarantee.Status.WITHOUT_ACTION, null);
        saveLog(log);
        save(guarantee, true);

        return guarantee;
    }

    /**
     * Generates a new loan for each guarantee if its status is ACCEPTED and the begin period is before or equals to the time parameter
     * @param time the times used as the current time
     */
    public void processGuaranteeLoans(final Calendar time) {
        final GuaranteeQuery query = new GuaranteeQuery();
        query.fetch(Guarantee.Relationships.GUARANTEE_TYPE, Guarantee.Relationships.SELLER, Guarantee.Relationships.BUYER);
        query.setStatusList(Collections.singletonList(Guarantee.Status.ACCEPTED));
        query.setStartIn(Period.endingAt(time));
        query.setLoanFilter(GuaranteeQuery.LoanFilter.WITHOUT_LOAN);

        final List<Guarantee> guarantees = guaranteeDao.search(query);

        for (final Guarantee guarantee : guarantees) {
            grantLoan(time, guarantee, false);
            save(guarantee, true);
        }
    }

    public Guarantee registerGuarantee(Guarantee guarantee) {
        validate(guarantee);
        guarantee = save(guarantee, true);
        grantLoan(Calendar.getInstance(), guarantee, false);

        return guarantee;
    }

    public int remove(final Long guaranteeId) {
        final Guarantee guarantee = load(guaranteeId);
        if (canRemoveGuarantee(guarantee)) {
            return guaranteeDao.delete(guaranteeId);
        } else {
            throw new PermissionDeniedException();
        }
    }

    public Guarantee requestGuarantee(final PaymentObligationPackDTO pack) {
        final Long[] poIds = pack.getPaymentObligations();
        // verifies the pack validity
        if (pack.getIssuer() == null) {
            throw new IllegalArgumentException("Invalid guarantee request: Issuer is null");
        } else if (poIds == null || poIds.length == 0) {
            throw new IllegalArgumentException("Invalid guarantee request: payment obligations pack is empty");
        }

        // take the first payment obligation to get the currency and buyer (all belong to the same buyer and have the same currency)
        final PaymentObligation firstPaymentObligation = paymentObligationService.load(poIds[0]);
        final Member buyer = firstPaymentObligation.getBuyer();
        final Member seller = firstPaymentObligation.getSeller();
        final Member issuer = pack.getIssuer();
        final AccountOwner accOwner = LoggedUser.accountOwner();

        if (!accOwner.equals(seller)) {
            throw new PermissionDeniedException();
        }

        final Certification certification = certificationService.getActiveCertification(firstPaymentObligation.getCurrency(), buyer, issuer);

        // verifies if there is an active certification
        if (certification == null) {
            throw new ActiveCertificationNotFoundException(buyer, issuer, firstPaymentObligation.getCurrency());
        }

        // calculates the guarantee's amount and expirationDate
        BigDecimal amount = firstPaymentObligation.getAmount();
        final ArrayList<PaymentObligation> paymentObligations = new ArrayList<PaymentObligation>();
        paymentObligations.add(firstPaymentObligation);
        Calendar lastExpirationdate = firstPaymentObligation.getExpirationDate();
        for (int i = 1; i < poIds.length; i++) {
            final PaymentObligation po = paymentObligationService.load(poIds[i]);
            if (!accOwner.equals(po.getSeller())) {
                throw new PermissionDeniedException();
            }
            amount = amount.add(po.getAmount());
            if (po.getExpirationDate().after(lastExpirationdate)) {
                lastExpirationdate = po.getExpirationDate();
            }

            paymentObligations.add(po);
        }

        // verify that the certificatin's amount is not exceeded
        final BigDecimal usedCertificationAmount = certificationService.getUsedAmount(certification, true);
        final BigDecimal remainingAmount = certification.getAmount().subtract(usedCertificationAmount);
        if (amount.compareTo(remainingAmount) > 0) {
            throw new CertificationAmountExceededException(certification, remainingAmount, amount);
        }

        // verify that the certificatin's validity is not exceeded
        if (lastExpirationdate.after(certification.getValidity().getEnd())) {
            throw new CertificationValidityExceededException(certification);
        }

        final GuaranteeType guaranteeType = certification.getGuaranteeType();
        Guarantee guarantee = new Guarantee();

        guarantee.setBuyer(buyer);
        guarantee.setSeller(seller);
        guarantee.setIssuer(pack.getIssuer());
        guarantee.setCertification(certification);
        guarantee.setGuaranteeType(guaranteeType);
        guarantee.setAmount(amount);
        guarantee.setValidity(new Period(null, lastExpirationdate));
        guarantee.setPaymentObligations(paymentObligations);
        guarantee.setCreditFeeSpec((GuaranteeFeeVO) guaranteeType.getCreditFee().clone());
        guarantee.setIssueFeeSpec((GuaranteeFeeVO) guaranteeType.getIssueFee().clone());

        guarantee = save(guarantee, false);

        for (int i = 0; i < poIds.length; i++) {
            final PaymentObligation po = paymentObligations.get(i);
            po.setGuarantee(guarantee);
            paymentObligationService.changeStatus(po.getId(), PaymentObligation.Status.ACCEPTED);
        }

        return guarantee;
    }

    public GuaranteeLog saveLog(final GuaranteeLog guaranteeLog) {
        if (guaranteeLog.isTransient()) {
            return guaranteeLogDao.insert(guaranteeLog);
        } else {
            return guaranteeLogDao.update(guaranteeLog);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Guarantee> search(final GuaranteeQuery queryParameters) {
        final boolean isIssuer = isIssuer();
        final boolean isBuyer = isBuyer();
        final boolean isSeller = isSeller();

        if (isIssuer) {
            final Group group = fetchService.fetch(LoggedUser.group(), Group.Relationships.GUARANTEE_TYPES);
            if (group.getGuaranteeTypes().isEmpty()) { // has no permission to any guarantee type
                return Collections.EMPTY_LIST;
            } else { // check valid guarantee type selection
                if (queryParameters.getGuaranteeType() == null) { // all allowed GT
                    queryParameters.setAllowedGuaranteeTypes(group.getGuaranteeTypes());
                } else if (!group.getGuaranteeTypes().contains(queryParameters.getGuaranteeType())) {
                    throw new IllegalArgumentException("Guarantee type not allowed to filter: " + queryParameters.getGuaranteeType());
                }
            }
            queryParameters.setIssuer((Member) LoggedUser.accountOwner());
        }

        if (isBuyer && isSeller) {
            queryParameters.setLoggedMember((Member) LoggedUser.accountOwner());
        } else if (isBuyer) {
            queryParameters.setBuyer((Member) LoggedUser.accountOwner());
        } else if (isSeller) {
            queryParameters.setLoggedMember((Member) LoggedUser.accountOwner());
        }

        // if hasn't got any role then we must set the logged user as the buyer (to get guarantees whose model is with buyer only)
        final boolean hasNoRole = !isBuyer && !isIssuer && !isSeller && !hasAdminViewPermission();
        if (hasNoRole) {
            queryParameters.setBuyer((Member) LoggedUser.accountOwner());
        }

        return guaranteeDao.search(queryParameters);
    }

    public void setCertificationService(final CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setGuaranteeDao(final GuaranteeDAO guaranteeDao) {
        this.guaranteeDao = guaranteeDao;
    }

    public void setGuaranteeLogDao(final GuaranteeLogDAO guaranteeLogDao) {
        this.guaranteeLogDao = guaranteeLogDao;
    }

    public void setGuaranteeTypeService(final GuaranteeTypeService guaranteeTypeService) {
        this.guaranteeTypeService = guaranteeTypeService;
    }

    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setPaymentObligationService(final PaymentObligationService paymentObligationService) {
        this.paymentObligationService = paymentObligationService;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void validate(final Guarantee guarantee) {
        getValidator(guarantee).validate(guarantee);
    }

    /**
     * Adds the fee's amount to the loan's amount only if the fee payer is the buyer and the model is different from WITH_BUYER_ONLY
     * @param loanAmount
     * @param feePayer
     * @param guarantee
     */
    private BigDecimal addFeeToLoanAmount(BigDecimal loanAmount, final AccountOwner feePayer, final BigDecimal fee, final Guarantee guarantee) {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final boolean hasFee = BigDecimal.ZERO.compareTo(localSettings.round(fee)) == -1;
        final GuaranteeType.Model model = guarantee.getGuaranteeType().getModel();

        if (model != GuaranteeType.Model.WITH_BUYER_ONLY && feePayer == guarantee.getBuyer().getAccountOwner() && hasFee) {
            loanAmount = loanAmount.add(localSettings.round(fee));
        }
        return loanAmount;
    }

    /**
     * Calculates the initial guarantee's status according to the guarantee's type
     * @param guarantee
     */
    private Status calcInitialStatus(final Guarantee guarantee) {
        final GuaranteeType guaranteeType = guaranteeTypeService.load(guarantee.getGuaranteeType().getId());
        if (guaranteeType.getModel() == GuaranteeType.Model.WITH_PAYMENT_OBLIGATION) {
            return Status.PENDING_ISSUER;
        } else {
            switch (guaranteeType.getAuthorizedBy()) {
                case ISSUER:
                case BOTH:
                    return Status.PENDING_ISSUER;
                case ADMIN:
                    return Status.PENDING_ADMIN;
                case NONE:
                    return Status.ACCEPTED;
                default:
                    throw new IllegalArgumentException("Unsupported authorizer value: " + guaranteeType.getAuthorizedBy());
            }
        }
    }

    private Guarantee changeStatus(Guarantee guarantee, Status newStatus, final boolean automaticLoanAuthorization) {
        // this is necessary to ensure an instance of (the entity) Guarantee
        guarantee = fetchService.fetch(guarantee, Guarantee.Relationships.PAYMENT_OBLIGATIONS);

        final boolean changeAllowed = canChangeStatus(guarantee, newStatus);

        if (!changeAllowed) {
            throw new GuaranteeStatusChangeException(newStatus);
        } else {
            // Force the status to PENDING_ADMIN if the condition is true
            if (newStatus == Guarantee.Status.ACCEPTED && isInSomeStatus(guarantee, Status.PENDING_ISSUER) && guarantee.getGuaranteeType().getAuthorizedBy() == GuaranteeType.AuthorizedBy.BOTH) {
                newStatus = Status.PENDING_ADMIN;
            }

            // Create log of the status changing
            final GuaranteeLog log = guarantee.changeStatus(newStatus, LoggedUser.user().getElement());
            saveLog(log);

            // Generates a new loan if the status of guarantee is ACCEPTED and the begin period is now or before.
            grantLoan(Calendar.getInstance(), guarantee, automaticLoanAuthorization);

            // Save guarantee
            save(guarantee, true);

            // If the guarantee was cancelled, change the status of associated payment obligations
            if (newStatus == Status.CANCELLED) {
                updateAssociatedPaymentObligations(guarantee);
            }
        }
        return guarantee; // the return is used in the aspects
    }

    private String convertFee(final boolean isCreditFee, final Guarantee guarantee) {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        NumberConverter<BigDecimal> numberConverter;

        final GuaranteeType guaranteeType = guarantee.getGuaranteeType();
        final GuaranteeFeeVO feeSpec = isCreditFee ? guarantee.getCreditFeeSpec() : guarantee.getIssueFeeSpec();

        if (feeSpec.getType() == FeeType.FIXED) {
            numberConverter = localSettings.getUnitsConverter(guaranteeType.getCurrency().getPattern());
            return numberConverter.toString(feeSpec.getFee());
        } else {
            numberConverter = localSettings.getNumberConverter();
            return numberConverter.toString(feeSpec.getFee()) + " " + messageResolver.message("guaranteeType.feeType." + feeSpec.getType());
        }
    }

    private Collection<? extends MemberGroup> filterBuyers() {
        return filterMemberGroups(new Predicate() {
            public boolean evaluate(final Object object) {
                return isBuyerMember((Group) object);
            }
        });
    }

    private Collection<? extends MemberGroup> filterIssuers() {
        return filterMemberGroups(new Predicate() {
            public boolean evaluate(final Object object) {
                return isIssuerMember((Group) object);
            }
        });
    }

    private Collection<? extends MemberGroup> filterMemberGroups(final Predicate predicate) {
        return filterMemberGroups(predicate, null);
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends MemberGroup> filterMemberGroups(final Predicate predicate, Collection<? extends Group> groups) {
        Predicate predicateToApply = predicate;

        if (groups == null) { // search for not removed member and broker groups
            final GroupQuery query = new GroupQuery();
            query.setStatus(Group.Status.NORMAL);
            query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);

            groups = groupService.search(query);
        } else if (groups.isEmpty()) { // if the group list is empty then return the same (empty) list
            return (Collection<? extends MemberGroup>) groups;
        } else { // it creates a predicate to filter not removed member and broker groups
            final Predicate memberGroupPredicate = new Predicate() {
                public boolean evaluate(final Object object) {
                    final Group grp = (Group) object;
                    return Group.Status.NORMAL == grp.getStatus() && (Group.Nature.MEMBER == grp.getNature() || Group.Nature.BROKER == grp.getNature());
                }
            };

            predicateToApply = predicate == null ? memberGroupPredicate : new AndPredicate(memberGroupPredicate, predicate);
        }

        CollectionUtils.filter(groups, predicateToApply);
        return (Collection<? extends MemberGroup>) groups;
    }

    private Collection<? extends MemberGroup> filterSellers() {
        return filterMemberGroups(new Predicate() {
            public boolean evaluate(final Object object) {
                return isSellerMember((Group) object);
            }
        });
    }

    /**
     * 
     * @param isCreditFee if false it is issue fee payer
     * @param guaranteeType
     * @param guarantee
     * @return the guarantee's fee payer according to GuaranteeType
     */
    private AccountOwner getFeePayer(final boolean isCreditFee, final Guarantee guarantee) {
        Member payer = null;
        final GuaranteeType guaranteeType = guarantee.getGuaranteeType();
        if (isCreditFee) {
            payer = guaranteeType.getCreditFeePayer() == GuaranteeType.FeePayer.BUYER ? guarantee.getBuyer() : guarantee.getSeller();
        } else {
            payer = guaranteeType.getIssueFeePayer() == GuaranteeType.FeePayer.BUYER ? guarantee.getBuyer() : guarantee.getSeller();
        }

        return payer.getAccountOwner();
    }

    private String getModuleName(final Group group) {
        return Group.Nature.OPERATOR == group.getNature() ? "operatorGuarantees" : "memberGuarantees";
    }

    private Validator getValidator(final Guarantee guarantee) {

        final GuaranteeType guaranteeType = fetchService.fetch(guarantee.getGuaranteeType());
        if (guaranteeType == null) {
            return null;
        }

        final Validator validator = new Validator("guarantee");
        if (guaranteeType.getModel() != GuaranteeType.Model.WITH_BUYER_ONLY) {
            validator.property("seller").required().key("guarantee.sellerUsername");
        }
        validator.property("buyer").required().key("guarantee.buyerUsername");
        validator.property("issuer").required().key("guarantee.issuerUsername");
        validator.property("amount").required().positiveNonZero();
        validator.property("validity").add(new PeriodValidation(ValidationType.BOTH_REQUIRED_AND_NOT_EXPIRED)).key("certification.validity");

        // Custom fields
        validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
            public Validator getValidator() {
                final TransferType transferType = guaranteeType.getLoanTransferType();
                return customFieldService.getPaymentValueValidator(transferType);
            }
        }));

        return validator;
    }

    /**
     * Generates a new loan only if the guarantee' status is ACCEPTED and the begin period is now or before.
     * @param guarantee
     * @param time the times used as the current time
     */
    private void grantLoan(final Calendar time, final Guarantee guarantee, final boolean automaticLoanAuthorization) {
        if (guarantee.getStatus() != Guarantee.Status.ACCEPTED || guarantee.getValidity().getBegin().after(time)) {
            return;
        }

        final GuaranteeType guaranteeType = guarantee.getGuaranteeType();
        final LocalSettings localSettings = settingsService.getLocalSettings();

        final BigDecimal creditFee = guarantee.getCreditFee();
        final BigDecimal issueFee = guarantee.getIssueFee();

        AccountOwner creditFeePayer = null;
        AccountOwner issueFeePayer = null;

        /* obtains the fees payers */
        if (guaranteeType.getModel() != GuaranteeType.Model.WITH_BUYER_ONLY) {
            creditFeePayer = getFeePayer(true, guarantee);
            issueFeePayer = getFeePayer(false, guarantee);
        } else { /* in this case we only have set the buyer in the guarantee */
            creditFeePayer = guarantee.getBuyer().getAccountOwner();
            issueFeePayer = creditFeePayer;
        }
        /* add the credit fee amount to the loan's amount if the fee payer is the buyer */
        BigDecimal loanAmount = addFeeToLoanAmount(guarantee.getAmount(), creditFeePayer, creditFee, guarantee);

        /* add the issue fee amount to the loan's amount if the fee payer is the buyer */
        loanAmount = addFeeToLoanAmount(loanAmount, issueFeePayer, issueFee, guarantee);

        /* grants loan to buyer */
        final GrantSinglePaymentLoanDTO loanDto = new GrantSinglePaymentLoanDTO();
        loanDto.setMember(guarantee.getBuyer());
        loanDto.setAmount(loanAmount);
        loanDto.setDescription(guaranteeType.getLoanTransferType().getDescription());
        loanDto.setTransferType(guaranteeType.getLoanTransferType());
        loanDto.setRepaymentDate(guarantee.getValidity().getEnd());
        loanDto.setCustomValues(new ArrayList<PaymentCustomFieldValue>(guarantee.getCustomValues()));
        final Loan loan = loanService.grantForGuarantee(loanDto, automaticLoanAuthorization);

        TransferDTO transferDto = null;
        /* only in this case there is a seller to forward to the loan */
        if (guaranteeType.getModel() != GuaranteeType.Model.WITH_BUYER_ONLY) {
            /* forwards loan's amount from Buyer to Seller */
            transferDto = new TransferDTO();
            transferDto.setForced(true);
            transferDto.setContext(TransactionContext.AUTOMATIC);
            transferDto.setFromOwner(guarantee.getBuyer().getAccountOwner());
            transferDto.setToOwner(guarantee.getSeller().getAccountOwner());
            transferDto.setTransferType(guaranteeType.getForwardTransferType());
            transferDto.setAmount(guarantee.getAmount());
            transferDto.setDescription(guaranteeType.getForwardTransferType().getDescription());
            transferDto.setParent(loan.getTransfer());
            paymentService.insertWithoutNotification(transferDto);
        }

        /* credit fee payment from Buyer/Seller (according to GuaranteeType) to system account */
        if (BigDecimal.ZERO.compareTo(localSettings.round(creditFee)) == -1) {
            final Map<String, String> valuesMap = new HashMap<String, String>();
            valuesMap.put("creditFee", convertFee(true, guarantee));

            transferDto = new TransferDTO();
            transferDto.setForced(true);
            transferDto.setFromOwner(creditFeePayer);
            transferDto.setToOwner(SystemAccountOwner.instance());
            transferDto.setTransferType(guaranteeType.getCreditFeeTransferType());
            transferDto.setAmount(creditFee);

            transferDto.setContext(TransactionContext.AUTOMATIC);
            transferDto.setDescription(MessageProcessingHelper.processVariables(guaranteeType.getCreditFeeTransferType().getDescription(), valuesMap));
            transferDto.setParent(loan.getTransfer());
            paymentService.insertWithoutNotification(transferDto);
        }

        /* issue fee payment from Buyer/Seller (according to GuaranteeType) to Issuer */
        if (BigDecimal.ZERO.compareTo(localSettings.round(issueFee)) == -1) {
            final Map<String, String> valuesMap = new HashMap<String, String>();
            valuesMap.put("emissionFee", convertFee(false, guarantee));

            transferDto = new TransferDTO();
            transferDto.setForced(true);
            transferDto.setFromOwner(issueFeePayer);
            transferDto.setToOwner(guarantee.getIssuer().getAccountOwner());
            transferDto.setTransferType(guaranteeType.getIssueFeeTransferType());
            transferDto.setAmount(issueFee);

            transferDto.setContext(TransactionContext.AUTOMATIC);
            transferDto.setDescription(MessageProcessingHelper.processVariables(guaranteeType.getIssueFeeTransferType().getDescription(), valuesMap));
            transferDto.setParent(loan.getTransfer());
            paymentService.insertWithoutNotification(transferDto);
        }

        /* update guarantee with the generated loan */
        guarantee.setLoan(loan);
    }

    private boolean hasAdminViewPermission() {
        return permissionService.checkPermission("adminMemberGuarantees", "viewGuarantees");
    }

    /**
     * Calculates the initial Guarantee Status according to the associated GuaranteeType
     * @param guarantee
     */
    private void initialize(final Guarantee guarantee) {
        final Status status = calcInitialStatus(guarantee);

        guarantee.setStatus(status);
        guarantee.setRegistrationDate(Calendar.getInstance());

        final GuaranteeType guaranteeType = fetchService.fetch(guarantee.getGuaranteeType());

        if (guaranteeType.getCreditFee().isReadonly()) {
            guarantee.setCreditFeeSpec((GuaranteeFeeVO) guaranteeType.getCreditFee().clone());
        }
        if (guaranteeType.getIssueFee().isReadonly()) {
            guarantee.setIssueFeeSpec((GuaranteeFeeVO) guaranteeType.getIssueFee().clone());
        }
    }

    private boolean isBuyerMember(final Group group) {
        return permissionService.checkPermission(group, "memberGuarantees", "buyWithPaymentObligations");
    }

    private boolean isInSomeStatus(final Guarantee guarantee, final Status... status) {
        for (final Status s : status) {
            if (guarantee.getStatus() == s) {
                return true;
            }
        }
        return false;
    }

    private boolean isIssuerMember(final Group group) {
        return permissionService.checkPermission(group, "memberGuarantees", "issueGuarantees");
    }

    private boolean isSellerMember(final Group group) {
        return permissionService.checkPermission(group, "memberGuarantees", "sellWithPaymentObligations");
    }

    /**
     * It saves the Guarantee taking into account if it is new or an already created guarantee (update)
     */
    private Guarantee save(Guarantee guarantee, final boolean validateCustomFields) {
        final Collection<PaymentCustomFieldValue> customValues = guarantee.getCustomValues();
        if (guarantee.isTransient()) {
            initialize(guarantee);
            guarantee = guaranteeDao.insert(guarantee);
            final GuaranteeLog log = guarantee.getNewLog(guarantee.getStatus(), LoggedUser.user().getElement());
            saveLog(log);
        } else {
            guarantee = guaranteeDao.update(guarantee);
        }
        guarantee.setCustomValues(customValues);
        customFieldService.savePaymentValues(guarantee, validateCustomFields);
        return guarantee;
    }

    private void updateAssociatedPaymentObligations(final Guarantee guarantee) {
        final Calendar today = DateHelper.truncateNextDay(Calendar.getInstance());
        for (final PaymentObligation paymentObligation : guarantee.getPaymentObligations()) {
            final Calendar expirationDate = paymentObligation.getExpirationDate();
            final Calendar maxPublishDate = paymentObligation.getMaxPublishDate();
            final Element by = LoggedUser.element();
            PaymentObligationLog log = null;
            if (today.after(expirationDate)) {
                log = paymentObligation.changeStatus(PaymentObligation.Status.EXPIRED, by);
            } else if (today.after(maxPublishDate)) {
                log = paymentObligation.changeStatus(PaymentObligation.Status.REGISTERED, by);
                paymentObligation.setMaxPublishDate(null);
            } else {
                log = paymentObligation.changeStatus(PaymentObligation.Status.PUBLISHED, by);
            }
            paymentObligation.setGuarantee(null);
            paymentObligationService.saveLog(log);
            paymentObligationService.save(paymentObligation, false);
        }
    }
}
