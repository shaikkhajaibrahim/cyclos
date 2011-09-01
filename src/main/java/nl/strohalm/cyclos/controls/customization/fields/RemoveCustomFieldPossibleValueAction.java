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
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to remove a custom field possible value
 * @author luis
 */
public class RemoveCustomFieldPossibleValueAction extends BaseAction {

    private CustomFieldService customFieldService;

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final RemoveCustomFieldPossibleValueForm form = context.getForm();
        final long id = form.getPossibleValueId();
        if (id <= 0) {
            throw new ValidationException();
        }
        final Map<String, Object> params = new HashMap<String, Object>();
        String key;
        try {
            final CustomFieldPossibleValue possibleValue = customFieldService.loadPossibleValue(id, CustomFieldPossibleValue.Relationships.FIELD);
            final CustomField customField = possibleValue.getField();
            switch (customField.getNature()) {
                case OPERATOR:
                    customFieldService.removeOperatorPossibleValue(id);
                    break;
                case PAYMENT:
                    final PaymentCustomField paymentField = (PaymentCustomField) customField;
                    customFieldService.removePaymentPossibleValue(id);
                    params.put("transferTypeId", paymentField.getTransferType().getId());
                    break;
                case MEMBER_RECORD:
                    final MemberRecordCustomField memberRecordField = (MemberRecordCustomField) customField;
                    customFieldService.removeMemberRecordPossibleValue(id);
                    params.put("memberRecordTypeId", memberRecordField.getMemberRecordType().getId());
                default:
                    customFieldService.removePossibleValue(id);
                    break;
            }
            key = "customField.possibleValue.removed";
        } catch (final DaoException e) {
            key = "customField.possibleValue.error.removing";
        }
        params.put("fieldId", form.getFieldId());
        context.sendMessage(key);

        return ActionHelper.redirectWithParams(context.getRequest(), context.getSuccessForward(), params);
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

}