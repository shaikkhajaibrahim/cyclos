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

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee.Status;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

public class CancelGuaranteeAction extends ChangeStatusGuaranteeAction {

    @Override
    protected void changeStatus(final ActionContext context) {
        if (context.isAdmin()) {
            final ChangeStatusGuaranteeForm form = context.getForm();
            guaranteeService.cancelGuaranteeAsMember(CoercionHelper.coerce(Guarantee.class, form.getGuaranteeId()));
        } else {
            super.changeStatus(context);
        }
    }

    @Override
    protected Status getNewStatus() {
        return Status.CANCELLED;
    }

}
