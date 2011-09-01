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
package nl.strohalm.cyclos.controls.members.messages;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseQueryAction;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.MessageBox;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;
import nl.strohalm.cyclos.entities.members.messages.Message.RootType;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.query.QueryParameters;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Action used to search messages
 * @author luis
 */
public class SearchMessagesAction extends BaseQueryAction {

    private DataBinder<MessageQuery> dataBinder;
    private MessageService           messageService;

    public DataBinder<MessageQuery> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<MessageQuery> binder = BeanBinder.instance(MessageQuery.class);
            binder.registerBinder("messageBox", PropertyBinder.instance(MessageBox.class, "messageBox"));
            binder.registerBinder("rootType", PropertyBinder.instance(RootType.class, "rootType"));
            binder.registerBinder("relatedMember", PropertyBinder.instance(Member.class, "relatedMember"));
            binder.registerBinder("category", PropertyBinder.instance(MessageCategory.class, "category"));
            binder.registerBinder("keywords", PropertyBinder.instance(String.class, "keywords"));
            binder.registerBinder("pageParameters", DataBinderHelper.pageBinder());
            dataBinder = binder;
        }
        return dataBinder;
    }

    @Inject
    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    protected void executeQuery(final ActionContext context, final QueryParameters queryParameters) {
        final List<Message> list = messageService.search((MessageQuery) queryParameters);
        context.getRequest().setAttribute("messages", list);
    }

    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final HttpServletRequest request = context.getRequest();
        final SearchMessagesForm form = context.getForm();

        // Resolve the query object
        final MessageQuery query = getDataBinder().readFromString(form.getQuery());
        final MessageBox messageBox = query.getMessageBox();
        if (messageBox == null) {
            throw new ValidationException();
        }
        if (query.getRelatedMember() != null) {
            final Member relatedMember = getFetchService().fetch(query.getRelatedMember(), Element.Relationships.USER);
            request.setAttribute("relatedMember", relatedMember);
        }

        query.fetch(Message.Relationships.FROM_MEMBER, Message.Relationships.TO_MEMBER, Message.Relationships.TO_GROUPS);

        request.setAttribute("messageBox", messageBox);

        // Store the required enums
        RequestHelper.storeEnum(request, MessageBox.class, "messageBoxes");
        RequestHelper.storeEnum(request, RootType.class, "rootTypes");

        if (context.isAdmin()) {
            // Get the categories
            AdminGroup adminGroup = context.getGroup();
            adminGroup = getFetchService().fetch(adminGroup, Group.Relationships.MESSAGE_CATEGORIES);
            request.setAttribute("categories", adminGroup.getMessageCategories());
        }

        // Check if can send a message
        boolean canSend = false;
        if (context.isMember()) {
            canSend = getPermissionService().checkPermission("memberMessages", "sendToMember") || getPermissionService().checkPermission("memberMessages", "sendToAdministration") || getPermissionService().checkPermission("brokerMessages", "sendToMembers");
        } else if (context.isOperator()) {
            canSend = getPermissionService().checkPermission("operatorMessages", "sendToMember") || getPermissionService().checkPermission("operatorMessages", "sendToAdministration");
        } else if (context.isAdmin()) {
            canSend = getPermissionService().checkPermission("adminMemberMessages", "sendToMember") || getPermissionService().checkPermission("adminMemberMessages", "sendToGroup");
        }
        request.setAttribute("canSend", canSend);

        // Check if can manage messages
        boolean canManage = false;
        if (context.isMember()) {
            canManage = getPermissionService().checkPermission("memberMessages", "manage");
        } else if (context.isOperator()) {
            canManage = getPermissionService().checkPermission("operatorMessages", "manage");
        } else if (context.isAdmin()) {
            canManage = getPermissionService().checkPermission("adminMemberMessages", "manage");
        }
        request.setAttribute("canManage", canManage);

        return query;
    }

    @Override
    protected boolean willExecuteQuery(final ActionContext context, final QueryParameters queryParameters) throws Exception {
        return true;
    }
}
