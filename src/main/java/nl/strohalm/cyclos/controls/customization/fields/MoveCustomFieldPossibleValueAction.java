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
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.ActionHelper;

import org.apache.struts.action.ActionForward;

/**
 * Action used to change all occurrences of a field value to another one
 * @author luis
 */
public class MoveCustomFieldPossibleValueAction extends BaseAction {

    private CustomFieldService customFieldService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final MoveCustomFieldPossibleValueForm form = context.getForm();
        final CustomFieldPossibleValue oldValue = customFieldService.loadPossibleValue(form.getOldValueId(), CustomFieldPossibleValue.Relationships.FIELD);
        final CustomFieldPossibleValue newValue = customFieldService.loadPossibleValue(form.getNewValueId());

        final CustomField field = oldValue.getField();
        int affected;
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("fieldId", field.getId());
        parameters.put("nature", field.getNature());
        switch (field.getNature()) {
            case PAYMENT:
                affected = customFieldService.movePaymentValues(oldValue, newValue);
                final TransferType transferType = ((PaymentCustomField) field).getTransferType();
                parameters.put("transferTypeId", transferType.getId());
                parameters.put("accountTypeId", transferType.getFrom().getId());
                break;
            case MEMBER_RECORD:
                affected = customFieldService.moveMemberRecordValues(oldValue, newValue);
                final MemberRecordType memberRecordType = ((MemberRecordCustomField) field).getMemberRecordType();
                parameters.put("memberRecordTypeId", memberRecordType.getId());
                break;
            case OPERATOR:
                affected = customFieldService.moveOperatorValues(oldValue, newValue);
                break;
            default:
                affected = customFieldService.moveValues(oldValue, newValue);
                break;
        }

        context.sendMessage("customField.valuesMoved", affected, oldValue.getValue(), newValue.getValue());

        return ActionHelper.redirectWithParams(context.getRequest(), context.getSuccessForward(), parameters);
    }

}
