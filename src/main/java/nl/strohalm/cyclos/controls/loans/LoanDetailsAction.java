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
package nl.strohalm.cyclos.controls.loans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanParameters;
import nl.strohalm.cyclos.entities.accounts.loans.LoanPayment;
import nl.strohalm.cyclos.entities.accounts.loans.LoanRepaymentAmountsDTO;
import nl.strohalm.cyclos.entities.accounts.loans.LoanPayment.Status;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.LoanDateDTO;
import nl.strohalm.cyclos.services.transactions.LoanPaymentDTO;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.Amount.Type;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Action used to retrieve details about a loan
 * @author luis
 */
public class LoanDetailsAction extends BaseFormAction implements LocalSettingsChangeListener {

    protected LoanService                     loanService;
    protected PaymentService                  paymentService;
    private CustomFieldService                customFieldService;
    private DataBinder<? extends LoanDateDTO> dataBinder;

    public DataBinder<? extends LoanDateDTO> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<? extends LoanDateDTO> binder = BeanBinder.instance(getDtoClass());
            initDataBinder(binder);
            dataBinder = binder;
        }
        return dataBinder;
    }

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        dataBinder = null;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    @Inject
    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    protected Class<? extends LoanDateDTO> getDtoClass() {
        return LoanDateDTO.class;
    }

    protected void initDataBinder(final BeanBinder<? extends LoanDateDTO> binder) {
        binder.registerBinder("loan", PropertyBinder.instance(Loan.class, "loanId"));
        binder.registerBinder("loanPayment", PropertyBinder.instance(LoanPayment.class, "loanPaymentId"));
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final LoanDateDTO loanDTO = resolveLoanDTO(context);
        final Loan loan = loanDTO.getLoan();
        final LoanPayment firstOpenPayment = loan.getFirstOpenPayment();
        final boolean closed = loan.getStatus().isClosed();
        final String currencyPattern = loan.getTransferType().getCurrency().getPattern();
        request.setAttribute("loan", loan);
        try {
            paymentService.checkView(loan.getTransfer());
            request.setAttribute("showRelatedTransfer", true);
        } catch (final PermissionDeniedException e) {
            request.setAttribute("showRelatedTransfer", false);
        }
        request.setAttribute("currencyPattern", currencyPattern);
        if (loan.getParameters().getType() == Loan.Type.WITH_INTEREST) {
            final LoanParameters params = loan.getParameters();
            if (params.getMonthlyInterestAmount() != null) {
                request.setAttribute("monthlyInterestPattern", params.getMonthlyInterestAmount().getType() == Type.FIXED ? currencyPattern : "");
            }
            if (params.getExpirationDailyInterestAmount() != null) {
                request.setAttribute("expirationDailyInterestPattern", params.getExpirationDailyInterestAmount().getType() == Type.FIXED ? currencyPattern : "");
            }
            if (params.getExpirationFee() != null) {
                request.setAttribute("expirationFeePattern", params.getExpirationFee().getType() == Type.FIXED ? currencyPattern : "");
            }
            if (params.getGrantFee() != null) {
                request.setAttribute("grantFeePattern", params.getGrantFee().getType() == Type.FIXED ? currencyPattern : "");
            }
        }

        final Member member = getFetchService().fetch(loan.getMember(), Element.Relationships.GROUP);
        // Get the custom values
        final Transfer transfer = loan.getTransfer();
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transfer.getType());
        final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, transfer.getCustomValues());
        request.setAttribute("customFields", entries);

        if (CollectionUtils.isNotEmpty(loan.getToMembers())) {
            // Ensure the responsible is the first member shown and the list will be sorted
            final List<Member> membersInGroup = new ArrayList<Member>(loan.getToMembers());
            final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            Collections.sort(membersInGroup, settings.getMemberComparator());
            membersInGroup.remove(member);
            membersInGroup.add(0, member);
            request.setAttribute("membersInLoan", membersInGroup);
        }

        boolean canRepay = false;
        boolean canDiscard = false;
        boolean canMarkAsInProcess = false;
        boolean canMarkAsUnrecoverable = false;
        boolean canMarkAsRecovered = false;
        if (!closed && transfer.getProcessDate() != null) {
            final Status firstOpenPaymentStatus = firstOpenPayment.getStatus();
            if ((firstOpenPaymentStatus == LoanPayment.Status.IN_PROCESS)) {
                // Admins can mark in-process loans as unrecoverable or recovered
                if (getPermissionService().checkPermission("adminMemberLoans", "manageExpiredStatus")) {
                    canMarkAsRecovered = true;
                    canMarkAsUnrecoverable = true;
                }
            } else {
                if (context.isMember()) {
                    final Member loggedElement = context.getElement();
                    final boolean repayByGroup = loggedElement.getMemberGroup().getMemberSettings().isRepayLoanByGroup();
                    if (loggedElement.equals(member) || (repayByGroup && loan.getToMembers().contains(loggedElement))) {
                        canRepay = getPermissionService().checkPermission("memberLoans", "repay");
                    }
                } else if (context.isOperator()) {
                    final Operator operator = context.getElement();
                    if (operator.getMember().equals(member)) {
                        canRepay = getPermissionService().checkPermission("operatorLoans", "repay");
                    }
                } else if (context.isAdmin()) {
                    canRepay = getPermissionService().checkPermission("adminMemberLoans", "repay");
                    canDiscard = getPermissionService().checkPermission("adminMemberLoans", "discard");
                    canMarkAsInProcess = firstOpenPaymentStatus == LoanPayment.Status.EXPIRED && getPermissionService().checkPermission("adminMemberLoans", "manageExpiredStatus");
                }
            }
        }

        final boolean canPerformExpiredAction = canMarkAsInProcess || canMarkAsUnrecoverable || canMarkAsRecovered;
        request.setAttribute("canRepay", canRepay);
        request.setAttribute("canDiscard", canDiscard);
        request.setAttribute("canMarkAsInProcess", canMarkAsInProcess);
        request.setAttribute("canMarkAsRecovered", canMarkAsRecovered);
        request.setAttribute("canMarkAsUnrecoverable", canMarkAsUnrecoverable);
        request.setAttribute("canPerformExpiredAction", canPerformExpiredAction);

        if (canRepay || canDiscard || canPerformExpiredAction) {
            final LoanRepaymentAmountsDTO dto = loanService.getLoanPaymentAmount(loanDTO);
            request.setAttribute("repaymentAmounts", dto);

            final boolean requestTransactionPassword = shouldValidateTransactionPassword(context, loan);
            if (requestTransactionPassword) {
                context.validateTransactionPassword();
            }
            request.setAttribute("requestTransactionPassword", requestTransactionPassword);
        }
    }

    protected LoanDateDTO resolveLoanDTO(final ActionContext context) {
        final LoanDetailsForm form = context.getForm();
        final LoanDateDTO dto = getDataBinder().readFromString(form);
        // because it comes from a data binder it's an entity reference
        // then we can use the load method
        Loan loan = dto.getLoan();
        if (loan == null) {
            throw new ValidationException();
        }
        final Relationship[] relationships = new Relationship[] { RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.CUSTOM_VALUES), RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.TYPE), RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER, Element.Relationships.USER), Loan.Relationships.PAYMENTS, RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.CUSTOM_VALUES), Loan.Relationships.LOAN_GROUP, Loan.Relationships.TO_MEMBERS };
        if ((context.isMember() && !context.isBroker()) || context.isOperator()) {
            loan = loanService.loadAsMember(loan.getId(), relationships);
        } else {
            loan = loanService.load(loan.getId(), relationships);
        }

        dto.setLoan(loan);
        return dto;
    }

    protected boolean shouldValidateTransactionPassword(final ActionContext context, final Loan loan) {
        if (context.getAccountOwner().equals(loan.getMember())) {
            // When a logged member performing an operation over a loan to himself
            return context.isTransactionPasswordEnabled(loan.getTransferType().getTo());
        } else {
            return context.isTransactionPasswordEnabled();
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final LoanPaymentDTO loanDTO = resolveLoanDTO(context);
        if (shouldValidateTransactionPassword(context, loanDTO.getLoan())) {
            final BaseLoanActionForm form = context.getForm();
            if (StringUtils.isEmpty(form.getTransactionPassword())) {
                throw new ValidationException("_transactionPassword", "login.transactionPassword", new RequiredError());
            }
        }
    }

}
