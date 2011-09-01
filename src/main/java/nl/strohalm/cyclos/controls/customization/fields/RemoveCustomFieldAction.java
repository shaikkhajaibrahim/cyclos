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
package nl.strohalm.cyclos.controls.customization.fields;

import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to remove a custom field
 * @author luis
 */
public class RemoveCustomFieldAction extends BaseAction {

    private TransferTypeService transferTypeService;
    private CustomFieldService  customFieldService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final RemoveCustomFieldForm form = context.getForm();
        final long id = form.getFieldId();
        if (id <= 0) {
            throw new ValidationException();
        }
        final CustomField field = customFieldService.load(id);
        ActionForward forward;
        final Map<String, Object> params = new HashMap<String, Object>();
        try {
            switch (field.getNature()) {
                case OPERATOR:
                    customFieldService.removeOperatorField(id);
                    forward = context.getSuccessForward();
                    params.put("nature", field.getNature());
                    break;
                case PAYMENT:
                    final TransferType transferType = transferTypeService.load(form.getTransferTypeId());
                    customFieldService.removePaymentCustomField(transferType, (PaymentCustomField) field);
                    forward = context.findForward("editTransferType");
                    params.put("transferTypeId", transferType.getId());
                    params.put("accountTypeId", transferType.getFrom().getId());
                    break;
                case MEMBER_RECORD:
                    customFieldService.removeMemberRecordField(id);
                    forward = context.findForward("editMemberRecordType");
                    params.put("memberRecordTypeId", form.getMemberRecordTypeId());
                    break;
                default:
                    customFieldService.remove(id);
                    forward = context.getSuccessForward();
                    params.put("nature", field.getNature());
                    break;
            }
            context.sendMessage("customField.removed");
            return ActionHelper.redirectWithParams(context.getRequest(), forward, params);
        } catch (final DaoException e) {
            return context.sendError("customField.error.removing");
        }
    }

}