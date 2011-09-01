/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as 
published by
 the Free Software Foundation; either version 2 of the License, 
or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public 
License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
02111-1307 USA

 */

package nl.strohalm.cyclos.controls.accounts.guarantees.guarantees;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee.Status;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType.FeeType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeFeeVO;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService;
import nl.strohalm.cyclos.services.accounts.guarantees.exceptions.GuaranteeStatusChangeException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.exceptions.AuthorizedPaymentInPastException;
import nl.strohalm.cyclos.services.transactions.exceptions.CreditsException;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.MapBean;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.conversion.HtmlConverter;
import nl.strohalm.cyclos.utils.validation.DelegatingValidator;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.PeriodValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.PeriodValidation.ValidationType;

import org.apache.struts.action.ActionForward;

public class AuthorizeGuaranteeAction extends BaseFormAction {

    private class GuaranteeFeeValidation implements GeneralValidation {
        private static final long serialVersionUID = 840449718151754491L;

        public ValidationError validate(final Object object) {
            final Guarantee guarantee = (Guarantee) object;
            BigDecimal fees = null;
            fees = guarantee.getCreditFee() != null ? guarantee.getCreditFee() : new BigDecimal(0);
            fees = guarantee.getIssueFee() != null ? fees.add(guarantee.getIssueFee()) : fees;

            if (fees.compareTo(guarantee.getAmount()) == 1) {
                return new ValidationError("guarantee.error.invalidGuarantee");
            }

            return null;

        }
    }

    private GuaranteeService      guaranteeService;
    private CustomFieldService    customFieldService;
    private DataBinder<Guarantee> dataBinder;

    private DataBinder<Guarantee> readDataBinder;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setGuaranteeService(final GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final AuthorizeGuaranteeForm form = context.getForm();
        final Guarantee guarantee = guaranteeService.load(form.getGuaranteeId(), Guarantee.Relationships.GUARANTEE_TYPE);
        updateGuarantee(context, form, guarantee);
        try {
            if (context.isAdmin()) {
                final boolean authorizeGuarantee = form.isAutomaticLoanAuthorization() && getPermissionService().checkPermission(context.getGroup(), "systemPayments", "authorize");
                guaranteeService.acceptGuaranteeAsMember(guarantee, authorizeGuarantee);
            } else {
                guaranteeService.changeStatus(guarantee, Status.ACCEPTED);
            }
            return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "guaranteeId", guarantee.getId());
        } catch (final GuaranteeStatusChangeException e) {
            context.sendMessage("guarantee.error.changeStatus", context.message("guarantee.status." + e.getNewstatus()));
            return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "guaranteeId", guarantee.getId());
        } catch (final CreditsException e) {
            return context.sendError(ActionHelper.resolveErrorKey(e), ActionHelper.resolveParameters(e));
        } catch (final UnexpectedEntityException e) {
            return context.sendError("payment.error.invalidTransferType");
        } catch (final AuthorizedPaymentInPastException e) {
            return context.sendError("payment.error.authorizedInPast");
        }
    }

    /**
     * Method use to prepare a form for being displayed
     */
    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final AuthorizeGuaranteeForm form = context.getForm();
        final Long id = form.getGuaranteeId();
        final Guarantee guarantee = guaranteeService.load(id, Guarantee.Relationships.GUARANTEE_TYPE);
        final boolean canAcceptLoan = getPermissionService().checkPermission(context.getGroup(), "systemPayments", "authorize");
        getReadDataBinder().writeAsString(form.getGuarantee(), guarantee);

        // suggest the validity begin as the current date
        if (guarantee.getValidity() == null || guarantee.getValidity().getBegin() == null) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            final CalendarConverter calendarConverter = localSettings.getRawDateConverter();

            ((MapBean) form.getGuarantee("validity")).set("begin", calendarConverter.toString(Calendar.getInstance()));
        }

        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(guarantee.getGuaranteeType().getLoanTransferType());
        request.setAttribute("customFields", CustomFieldHelper.buildEntries(customFields, guarantee.getCustomValues()));
        request.setAttribute("canAcceptLoan", canAcceptLoan);

        request.setAttribute("guarantee", guarantee);
        RequestHelper.storeEnum(request, GuaranteeType.FeeType.class, "feeTypes");
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final AuthorizeGuaranteeForm form = context.getForm();
        final Guarantee guarantee = getDataBinder().readFromString(form.getGuarantee());
        final Guarantee loaded = guaranteeService.load(form.getGuaranteeId(), Guarantee.Relationships.GUARANTEE_TYPE);

        final Validator validator = new Validator("guarantee");
        final GuaranteeFeeValidation guaranteeFeValidation = new GuaranteeFeeValidation();
        validator.general(guaranteeFeValidation);
        validator.property("validity").add(new PeriodValidation(ValidationType.BOTH_REQUIRED_AND_NOT_EXPIRED)).key("guarantee.validity");

        // Custom fields
        validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
            public Validator getValidator() {
                final TransferType transferType = loaded.getGuaranteeType().getLoanTransferType();
                return customFieldService.getPaymentValueValidator(transferType);
            }
        }));

        validator.validate(guarantee);
    }

    private DataBinder<Guarantee> getDataBinder() {
        if (dataBinder == null) {

            final BeanBinder<Guarantee> binder = BeanBinder.instance(Guarantee.class);
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            binder.registerBinder("validity", DataBinderHelper.rawPeriodBinder(localSettings, "validity"));
            binder.registerBinder("amount", PropertyBinder.instance(BigDecimal.class, "amount"));

            final BeanBinder<? extends CustomFieldValue> customValueBinder = BeanBinder.instance(PaymentCustomFieldValue.class);
            customValueBinder.registerBinder("field", PropertyBinder.instance(PaymentCustomField.class, "field"));
            customValueBinder.registerBinder("value", PropertyBinder.instance(String.class, "value", HtmlConverter.instance()));
            binder.registerBinder("customValues", BeanCollectionBinder.instance(customValueBinder, "customValues"));

            final BeanBinder<GuaranteeFeeVO> issueFeeBinder = BeanBinder.instance(GuaranteeFeeVO.class, "issueFeeSpec");
            issueFeeBinder.registerBinder("type", PropertyBinder.instance(FeeType.class, "type"));
            issueFeeBinder.registerBinder("fee", PropertyBinder.instance(BigDecimal.class, "fee", localSettings.getNumberConverter()));
            binder.registerBinder("issueFeeSpec", issueFeeBinder);

            final BeanBinder<GuaranteeFeeVO> creditFeeBinder = BeanBinder.instance(GuaranteeFeeVO.class, "creditFeeSpec");
            creditFeeBinder.registerBinder("type", PropertyBinder.instance(FeeType.class, "type"));
            creditFeeBinder.registerBinder("fee", PropertyBinder.instance(BigDecimal.class, "fee", localSettings.getNumberConverter()));
            binder.registerBinder("creditFeeSpec", creditFeeBinder);

            dataBinder = binder;
        }
        return dataBinder;
    }

    private DataBinder<Guarantee> getReadDataBinder() {
        if (readDataBinder == null) {
            readDataBinder = getDataBinder();
            dataBinder = null;
            final BeanBinder<Guarantee> beanBinder = (BeanBinder<Guarantee>) readDataBinder;
            beanBinder.getMappings().remove("customValues");
        }
        return readDataBinder;
    }

    private void updateGuarantee(final ActionContext context, final AuthorizeGuaranteeForm form, final Guarantee guarantee) {
        final Guarantee updatedGuarantee = getDataBinder().readFromString(form.getGuarantee());

        guarantee.setValidity(updatedGuarantee.getValidity());
        guarantee.setCustomValues(updatedGuarantee.getCustomValues());
        if (context.isAdmin() && !guarantee.getGuaranteeType().getCreditFee().isReadonly()) { // only the admin can change the credit fee
            guarantee.setCreditFeeSpec(updatedGuarantee.getCreditFeeSpec());
        }
        if (!guarantee.getGuaranteeType().getIssueFee().isReadonly()) {
            guarantee.setIssueFeeSpec(updatedGuarantee.getIssueFeeSpec());
        }
    }
}