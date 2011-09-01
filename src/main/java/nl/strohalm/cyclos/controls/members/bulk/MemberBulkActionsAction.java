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
package nl.strohalm.cyclos.controls.members.bulk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.members.SearchMembersAction;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters;

/**
 * Action used to apply bulk actions on members that match the given filter
 * @author luis
 */
public class MemberBulkActionsAction extends SearchMembersAction {

    @Override
    protected boolean allowRemovedGroups() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected QueryParameters prepareForm(final ActionContext context) {
        final FullTextMemberQuery memberQuery = (FullTextMemberQuery) super.prepareForm(context);
        final HttpServletRequest request = context.getRequest();

        // We need all enumerated custom fields
        final List<MemberCustomField> fields = new ArrayList<MemberCustomField>((List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER));
        for (final Iterator it = fields.iterator(); it.hasNext();) {
            final MemberCustomField field = (MemberCustomField) it.next();
            if (field.getType() != CustomField.Type.ENUMERATED) {
                it.remove();
            }
        }
        request.setAttribute("customFields", CustomFieldHelper.buildEntries(fields, memberQuery.getCustomValues()));

        // Prepare data for specific actions
        MemberBulkChangeGroupAction.prepare(context);

        return memberQuery;
    }

}
