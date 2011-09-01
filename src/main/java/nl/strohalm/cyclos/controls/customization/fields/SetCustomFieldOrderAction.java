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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.MemberRecordTypeService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to set the custom field order
 * @author luis
 */
public class SetCustomFieldOrderAction extends BaseFormAction {

    private CustomFieldService      customFieldService;
    private MemberRecordTypeService memberRecordTypeService;
    private TransferTypeService     transferTypeService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setMemberRecordTypeService(final MemberRecordTypeService memberRecordTypeService) {
        this.memberRecordTypeService = memberRecordTypeService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final SetCustomFieldOrderForm form = context.getForm();
        CustomField.Nature nature;
        try {
            nature = CustomField.Nature.valueOf(form.getNature());
        } catch (final Exception e) {
            throw new ValidationException();
        }
        customFieldService.setOrder(nature, form.getFieldIds());
        final Map<String, Object> params = new HashMap<String, Object>();
        ActionForward forward;
        switch (nature) {
            case MEMBER_RECORD:
                params.put("memberRecordTypeId", form.getMemberRecordTypeId());
                forward = context.findForward("editMemberRecordType");
                break;
            case PAYMENT:
                final TransferType transferType = transferTypeService.load(form.getTransferTypeId());
                params.put("transferTypeId", transferType.getId());
                params.put("accountTypeId", transferType.getFrom().getId());
                forward = context.findForward("editTransferType");
                break;
            default:
                params.put("nature", nature);
                forward = context.getSuccessForward();
                break;
        }
        context.sendMessage("customField.orderModified");
        return ActionHelper.redirectWithParams(context.getRequest(), forward, params);
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final SetCustomFieldOrderForm form = context.getForm();
        CustomField.Nature nature;
        try {
            nature = CustomField.Nature.valueOf(form.getNature());
        } catch (final Exception e) {
            throw new ValidationException();
        }
        List<? extends CustomField> fields;
        switch (nature) {
            case OPERATOR:
                final Member member = (Member) context.getElement();
                fields = customFieldService.listOperatorFields(member);
                break;
            case MEMBER_RECORD:
                final MemberRecordType memberRecordType = memberRecordTypeService.load(form.getMemberRecordTypeId());
                fields = customFieldService.listMemberRecordFields(memberRecordType);
                request.setAttribute("memberRecordType", memberRecordType);
                break;
            case PAYMENT:
                final TransferType transferType = transferTypeService.load(form.getTransferTypeId());
                fields = (List<? extends CustomField>) transferType.getCustomFields();
                request.setAttribute("transferType", transferType);
                break;
            default:
                fields = customFieldService.listByNature(nature);
                break;
        }
        // Remove all fields with a parent one
        fields = new ArrayList<CustomField>(fields);
        for (final Iterator<? extends CustomField> it = fields.iterator(); it.hasNext();) {
            final CustomField customField = it.next();
            if (customField.getParent() != null) {
                it.remove();
            }
        }
        request.setAttribute("customFields", fields);
        request.setAttribute("nature", nature.name());
        super.prepareForm(context);
    }
}