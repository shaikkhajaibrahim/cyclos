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

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeLog;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee.Status;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.ActionHelper.ByElementExtractor;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;

import org.apache.struts.action.ActionForward;

public class GuaranteeDetailsAction extends BaseFormAction {

    /**
     * 
     * @param context
     * @return the module's prefix according to the logged user
     */
    private static String getModuleTypePrefix(final ActionContext context) {
        Module.Type moduleType = null;

        if (context.isAdmin()) {
            moduleType = Module.Type.ADMIN_MEMBER;
        } else if (context.isMember()) {
            moduleType = Module.Type.MEMBER;
        } else if (context.isBroker()) {
            moduleType = Module.Type.BROKER;
        } else if (context.isOperator()) {
            moduleType = Module.Type.BROKER;
        } else {
            throw new IllegalArgumentException("Unknown logged member: " + context.getElement().getGroup().getName());
        }

        return moduleType.getPrefix();
    }

    private GuaranteeService   guaranteeService;
    private CustomFieldService customFieldService;

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
        return null;
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final GuaranteeDetailsForm form = context.getForm();
        final Long id = form.getGuaranteeId();
        Long certificationId = -1L;
        final Guarantee guarantee = guaranteeService.load(id, Guarantee.Relationships.PAYMENT_OBLIGATIONS, Guarantee.Relationships.LOGS);

        if (guarantee.getCertification() != null) {
            certificationId = guarantee.getCertification().getId();
        }
        final ByElementExtractor extractor = new ByElementExtractor() {
            public Element getByElement(final Entity entity) {
                return getFetchService().fetch(((GuaranteeLog) entity).getBy());
            }
        };

        final boolean showPaymentObligations = !context.isAdmin() || getPermissionService().checkPermission("adminMemberGuarantees", "viewPaymentObligations");

        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(guarantee.getGuaranteeType().getLoanTransferType());
        final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, guarantee.getCustomValues());
        request.setAttribute("customFields", entries);

        request.setAttribute("certificationId", certificationId);
        request.setAttribute("guarantee", guarantee);
        request.setAttribute("showPaymentObligations", showPaymentObligations);
        request.setAttribute("logsBy", ActionHelper.getByElements(context, guarantee.getLogs(), extractor));
        request.setAttribute("canAccept", guaranteeService.canChangeStatus(guarantee, Status.ACCEPTED));
        request.setAttribute("canDeny", guaranteeService.canChangeStatus(guarantee, Status.REJECTED));
        request.setAttribute("canCancel", guaranteeService.canChangeStatus(guarantee, Status.CANCELLED));
        request.setAttribute("canDelete", guaranteeService.canRemoveGuarantee(guarantee));
        request.setAttribute("isWithBuyerOnly", guarantee.getGuaranteeType().getModel() == GuaranteeType.Model.WITH_BUYER_ONLY);
        request.setAttribute("fixedFeeType", GuaranteeType.FeeType.FIXED);
        request.setAttribute("showCurrentFeeValues", showCurrentFeeValues(guarantee));
        request.setAttribute("showLoan", showLoan(context, guarantee));
    }

    private boolean showCurrentFeeValues(final Guarantee guarantee) {
        return guarantee.getLoan() != null && guarantee.getStatus() == Guarantee.Status.ACCEPTED;
    }

    private boolean showLoan(final ActionContext context, final Guarantee guarantee) {
        if (guarantee.getLoan() == null) {
            return false;
        }

        final boolean hasViewLoanPermission = getPermissionService().checkPermission(getModuleTypePrefix(context) + "Loans", "view");
        if (hasViewLoanPermission) {
            // if has view permission we must check by view authorized too
            if (context.isAdmin() && guarantee.getLoan().getStatus().isRelatedToAuthorization()) {
                return getPermissionService().checkPermission("adminMemberLoans", "viewAuthorized");
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}
