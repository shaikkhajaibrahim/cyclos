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
package nl.strohalm.cyclos.controls.elements;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

public abstract class RemoveElementAction extends BaseAction {

    private ElementService elementService;

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    /**
     * Should be overriden to remove the element
     */
    protected abstract ActionForward doRemove(long id, ActionContext context);

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final RemoveElementForm form = context.getForm();
        final long id = form.getElementId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        try {
            return doRemove(id, context);
        } catch (final DaoException e) {
            return context.sendError("changeGroup.error.remove");
        }
    }

}
