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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseQueryAction;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.PendingMemberQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.query.QueryParameters;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.beanutils.BeanComparator;

/**
 * Action used to search for pending members
 * @author luis
 */
public class SearchPendingMembersAction extends BaseQueryAction {

    public static BeanBinder<PendingMemberQuery> createDataBinder(final LocalSettings settings) {
        final BeanBinder<PendingMemberQuery> binder = BeanBinder.instance(PendingMemberQuery.class);

        final BeanBinder<MemberCustomFieldValue> customValueBinder = BeanBinder.instance(MemberCustomFieldValue.class);
        customValueBinder.registerBinder("field", PropertyBinder.instance(MemberCustomField.class, "field"));
        customValueBinder.registerBinder("value", PropertyBinder.instance(String.class, "value"));

        binder.registerBinder("name", PropertyBinder.instance(String.class, "name"));
        binder.registerBinder("broker", PropertyBinder.instance(Member.class, "broker"));
        binder.registerBinder("creationPeriod", DataBinderHelper.periodBinder(settings, "creationPeriod"));
        binder.registerBinder("groups", SimpleCollectionBinder.instance(MemberGroup.class, "groups"));
        binder.registerBinder("customValues", BeanCollectionBinder.instance(customValueBinder, "customValues"));
        binder.registerBinder("pageParameters", DataBinderHelper.pageBinder());
        return binder;
    }

    private DataBinder<PendingMemberQuery> dataBinder;
    private CustomFieldService             customFieldService;
    private ReadWriteLock                  lock = new ReentrantReadWriteLock(true);
    private ElementService                 elementService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Override
    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        try {
            lock.writeLock().lock();
            super.onLocalSettingsUpdate(event);
            dataBinder = null;
        } finally {
            lock.writeLock().unlock();
        }
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
    protected void executeQuery(final ActionContext context, final QueryParameters queryParameters) {
        final PendingMemberQuery query = (PendingMemberQuery) queryParameters;
        final HttpServletRequest request = context.getRequest();
        request.setAttribute("pendingMembers", elementService.search(query));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final HttpServletRequest request = context.getRequest();
        final SearchPendingMembersForm form = context.getForm();
        final PendingMemberQuery query = getDataBinder().readFromString(form.getQuery());

        query.setBroker(getFetchService().fetch(query.getBroker()));

        // Retrieve the custom fields that will be used on the search
        final List<MemberCustomField> fields = CustomFieldHelper.onlyForMemberSearch((List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER));
        request.setAttribute("customFields", CustomFieldHelper.buildEntries(fields, query.getCustomValues()));

        // Get the allowed groups
        Collection<MemberGroup> allowedGroups;
        if (context.isAdmin()) {
            final AdminGroup group = getFetchService().reload((AdminGroup) context.getGroup(), AdminGroup.Relationships.MANAGES_GROUPS);
            allowedGroups = group.getManagesGroups();
        } else if (context.isBroker()) {
            final BrokerGroup group = getFetchService().reload((BrokerGroup) context.getGroup(), BrokerGroup.Relationships.POSSIBLE_INITIAL_GROUPS);
            allowedGroups = group.getPossibleInitialGroups();
        } else {
            throw new ValidationException();
        }
        final List<MemberGroup> groups = new ArrayList<MemberGroup>(allowedGroups);
        Collections.sort(groups, new BeanComparator("name"));
        request.setAttribute("groups", groups);

        return query;
    }

    @Override
    protected boolean willExecuteQuery(final ActionContext context, final QueryParameters queryParameters) throws Exception {
        return true;
    }

    private DataBinder<PendingMemberQuery> getDataBinder() {
        try {
            lock.readLock().lock();
            if (dataBinder == null) {
                final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
                dataBinder = createDataBinder(settings);
            }
            return dataBinder;
        } finally {
            lock.readLock().unlock();
        }
    }

}
