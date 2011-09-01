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
package nl.strohalm.cyclos.controls.members.pending;

import java.util.List;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseCsvAction;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.members.PendingMember;
import nl.strohalm.cyclos.entities.members.PendingMemberQuery;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.AccessSettings.UsernameGeneration;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.conversion.CustomFieldConverter;
import nl.strohalm.cyclos.utils.csv.CSVWriter;

/**
 * Action used to export a member search result as csv
 * @author luis
 */
public class ExportPendingMembersToCsvAction extends BaseCsvAction {

    private DataBinder<PendingMemberQuery> dataBinder;
    private CustomFieldService             customFieldService;
    private ElementService                 elementService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Override
    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        super.onLocalSettingsUpdate(event);
        dataBinder = null;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected List<?> executeQuery(final ActionContext context) {
        final SearchPendingMembersForm form = context.getForm();
        final PendingMemberQuery query = getDataBinder().readFromString(form.getQuery());
        query.fetch(PendingMember.Relationships.CUSTOM_VALUES, PendingMember.Relationships.MEMBER, RelationshipHelper.nested(PendingMember.Relationships.BROKER));
        return elementService.search(query);
    }

    @Override
    protected String fileName(final ActionContext context) {
        final User loggedUser = context.getUser();
        return "pending_members_" + loggedUser.getUsername() + ".csv";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CSVWriter initCsvWriter(final ActionContext context) {
        final LocalSettings settings = getLocalSettings();
        final AccessSettings accessSettings = SettingsHelper.getAccessSettings(getServlet().getServletContext());

        final CSVWriter<PendingMember> csv = CSVWriter.instance(PendingMember.class, settings);
        if (accessSettings.getUsernameGeneration() == UsernameGeneration.NONE) {
            csv.addColumn(context.message("member.username"), "username");
        }
        csv.addColumn(context.message("member.name"), "name");
        csv.addColumn(context.message("member.email"), "email");
        csv.addColumn(context.message("member.creationDate"), "creationDate", settings.getDateConverter());
        csv.addColumn(context.message("member.group"), "memberGroup.name");
        final List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        for (final MemberCustomField field : customFields) {
            csv.addColumn(field.getName(), "customValues", new CustomFieldConverter(field));
        }
        if (context.isAdmin()) {
            csv.addColumn(context.message("member.brokerUsername"), "broker.username");
            csv.addColumn(context.message("member.brokerName"), "broker.name");
        }
        return csv;
    }

    private DataBinder<PendingMemberQuery> getDataBinder() {
        if (dataBinder == null) {
            final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            dataBinder = SearchPendingMembersAction.createDataBinder(settings);
        }
        return dataBinder;
    }
}