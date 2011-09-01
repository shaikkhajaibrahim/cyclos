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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to list custom fields
 * @author luis
 */
public class ListCustomFieldsAction extends BaseAction {

    private CustomFieldService customFieldService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ListCustomFieldsForm form = context.getForm();
        CustomField.Nature nature;
        try {
            nature = CustomField.Nature.valueOf(form.getNature());
        } catch (final Exception e) {
            throw new ValidationException();
        }
        List<? extends CustomField> fields = null;
        if (nature == CustomField.Nature.OPERATOR) {
            final Member member = (Member) context.getElement();
            fields = customFieldService.listOperatorFields(member);
        } else {
            fields = customFieldService.listByNature(nature);
        }
        request.setAttribute("customFields", fields);
        request.setAttribute("nature", nature.name());
        return context.getInputForward();
    }

}