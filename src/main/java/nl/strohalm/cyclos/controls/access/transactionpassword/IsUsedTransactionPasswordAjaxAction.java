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

package nl.strohalm.cyclos.controls.access.transactionpassword;

import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAjaxAction;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.ResponseHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;

public class IsUsedTransactionPasswordAjaxAction extends BaseAjaxAction {

    @Override
    protected ContentType contentType() {
        return ContentType.JSON;
    }

    @Override
    protected void renderContent(final ActionContext context) throws Exception {
        final IsUsedTransactionPasswordAjaxForm form = context.getForm();
        boolean isUsed = false;

        try {
            final Long transferTypeId = getDataBinder().readFromString(form);
            // loads the TT
            final TransferType transferType = getFetchService().fetch(EntityHelper.reference(TransferType.class, transferTypeId), TransferType.Relationships.FROM);
            isUsed = context.isTransactionPasswordEnabled(transferType.getFrom());
        } catch (final Exception e) {
            // if error then nothing to, just return false.
        }

        // creates the result
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("isUsed", isUsed);

        ResponseHelper.writeStatus(context.getRequest(), context.getResponse(), ResponseHelper.Status.SUCCESS, fields);
    }

    private DataBinder<Long> getDataBinder() {
        return PropertyBinder.instance(Long.class, "transferTypeId");
    }
}
