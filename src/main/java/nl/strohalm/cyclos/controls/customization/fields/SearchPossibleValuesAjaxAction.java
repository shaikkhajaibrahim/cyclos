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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.BaseAjaxAction.ContentType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.ResponseHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used to list possible values by ajax
 * 
 * @author luis
 */
public class SearchPossibleValuesAjaxAction extends Action {

    private CustomFieldService customFieldService;
    private DataBinder<?>      possibleValueBinder;

    @Override
    public ActionForward execute(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        response.setContentType(ContentType.JSON.getContentType());
        final SearchPossibleValuesAjaxForm form = (SearchPossibleValuesAjaxForm) actionForm;
        final long fieldId = form.getFieldId();
        final long parentValueId = form.getParentValueId();
        if (fieldId <= 0L || parentValueId <= 0L) {
            throw new ValidationException();
        }
        final CustomField field = customFieldService.load(fieldId, CustomField.Relationships.POSSIBLE_VALUES);
        if (field.getType() != CustomField.Type.ENUMERATED) {
            throw new ValidationException();
        }
        final CustomFieldPossibleValue parentValue = customFieldService.loadPossibleValue(parentValueId);
        final Collection<CustomFieldPossibleValue> possibleValues = field.getPossibleValuesByParent(parentValue);
        final String json = getPossibleValueBinder().readAsString(possibleValues);
        ResponseHelper.writeJSON(response, json);
        return null;
    }

    public DataBinder<?> getPossibleValueBinder() {
        if (possibleValueBinder == null) {
            final BeanBinder<CustomFieldPossibleValue> binder = BeanBinder.instance(CustomFieldPossibleValue.class);
            binder.registerBinder("id", PropertyBinder.instance(Long.class, "id"));
            binder.registerBinder("value", PropertyBinder.instance(String.class, "value"));
            binder.registerBinder("defaultValue", PropertyBinder.instance(boolean.class, "defaultValue"));
            possibleValueBinder = BeanCollectionBinder.instance(binder);
        }
        return possibleValueBinder;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }
}
