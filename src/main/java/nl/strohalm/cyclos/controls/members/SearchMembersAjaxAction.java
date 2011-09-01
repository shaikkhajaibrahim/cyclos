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
package nl.strohalm.cyclos.controls.members;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAjaxAction;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.elements.BrokerQuery;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.ResponseHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;

import org.apache.commons.collections.CollectionUtils;

/**
 * Searches members and returns the list as an JSON
 * @author luis
 */
public class SearchMembersAjaxAction extends BaseAjaxAction {

    private ElementService elementService;
    private GroupService   groupService;
    private DataBinder<?>  dataBinder;
    private DataBinder<?>  dataBinderWithMaxScheduledPayments;

    public DataBinder<?> getDataBinder() {
        if (dataBinder == null) {
            dataBinder = BeanCollectionBinder.instance(DataBinderHelper.simpleElementBinder());
        }
        return dataBinder;
    }

    public DataBinder<?> getDataBinderWithMaxScheduledPayments() {
        if (dataBinderWithMaxScheduledPayments == null) {
            final BeanBinder<Map<String, Object>> elementBinder = DataBinderHelper.simpleElementBinder();
            elementBinder.registerBinder("maxScheduledPayments", PropertyBinder.instance(int.class, "group.memberSettings.maxSchedulingPayments"));
            dataBinderWithMaxScheduledPayments = BeanCollectionBinder.instance(elementBinder);
        }
        return dataBinderWithMaxScheduledPayments;
    }

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected ContentType contentType() {
        return ContentType.JSON;
    }

    @Override
    protected void renderContent(final ActionContext context) throws Exception {
        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
        final SearchMembersAjaxForm form = context.getForm();
        final MemberQuery memberQuery = form.isBrokers() ? new BrokerQuery() : new MemberQuery();
        memberQuery.setViewableGroup(form.getViewableGroup());
        if (form.isEnabled()) {
            memberQuery.setEnabled(true);
        }
        memberQuery.limitResults(localSettings.getMaxAjaxResults());
        memberQuery.setName(form.getName());
        memberQuery.setUsername(form.getUsername());
        // Search only brokered users
        if (context.isBroker()) {
            if (form.isBrokered()) {
                final Member broker = (Member) context.getElement();
                memberQuery.setBroker(broker);
            }
        }
        Element exclude;
        if (form.getExclude() != null) {
            // When specifying a member to exclude from search, apply it...
            exclude = EntityHelper.reference(Element.class, form.getExclude());
        } else {
            // ... otherwise, exclude the logged member himself
            if (context.isOperator()) {
                exclude = (Element) context.getAccountOwner();
            } else {
                exclude = context.getElement();
            }
        }
        memberQuery.setExcludeElements(Collections.singleton(exclude));
        memberQuery.fetch(Element.Relationships.USER);
        final Collection<MemberGroup> groups = resolveGroups(context);
        List<? extends Element> members;
        if (groups.isEmpty()) {
            // If no groups can be viewed, ensure no member will be returned
            members = Collections.emptyList();
        } else {
            memberQuery.setGroups(groups);
            members = elementService.search(memberQuery);
        }
        final String json = (form.isMaxScheduledPayments() ? getDataBinderWithMaxScheduledPayments() : getDataBinder()).readAsString(members);
        ResponseHelper.writeJSON(context.getResponse(), json);
    }

    /**
     * Resolves the possible groups for the logged user to see
     */
    @SuppressWarnings("unchecked")
    private Collection<MemberGroup> resolveGroups(final ActionContext context) {
        Collection<MemberGroup> possibleGroups;
        if (context.isAdmin()) {
            AdminGroup adminGroup = context.getGroup();
            adminGroup = getFetchService().fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            possibleGroups = adminGroup.getManagesGroups();
        } else {
            Member member = (Member) context.getAccountOwner();
            member = getFetchService().fetch(member, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS));
            possibleGroups = member.getMemberGroup().getCanViewProfileOfGroups();
        }
        // Ensure that only normal groups (not removed) are used
        final Collection<MemberGroup> groups = new HashSet<MemberGroup>();
        for (final MemberGroup group : possibleGroups) {
            if (group.getStatus().isEnabled()) {
                groups.add(group);
            }
        }
        final SearchMembersAjaxForm form = context.getForm();
        if (form.getGroupIds() != null && form.getGroupIds().length > 0) {
            final Collection<MemberGroup> selectedGroups = new HashSet<MemberGroup>();
            for (final Long id : form.getGroupIds()) {
                if (id == 0) {
                    continue;
                }
                selectedGroups.add((MemberGroup) groupService.load(id));
            }
            return CollectionUtils.intersection(groups, selectedGroups);
        } else {
            return groups;
        }
    }
}
