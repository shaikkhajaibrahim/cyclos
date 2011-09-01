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
package nl.strohalm.cyclos.controls.groups.permissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.controls.groups.EditGroupAction;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.groups.OperatorGroupPermissionsDTO;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.conversion.ReferenceConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to edit an operator group's permissions
 * @author jefferson
 */
public class EditOperatorGroupPermissionsAction extends BaseFormAction {

    private GroupService                            groupService;
    private DataBinder<OperatorGroupPermissionsDTO> dataBinder;

    public DataBinder<OperatorGroupPermissionsDTO> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<OperatorGroupPermissionsDTO> binder = BeanBinder.instance(OperatorGroupPermissionsDTO.class);
            binder.registerBinder("group", PropertyBinder.instance(Group.class, "group", ReferenceConverter.instance(Group.class)));
            binder.registerBinder("canViewInformationOf", SimpleCollectionBinder.instance(TransferType.class, "canViewInformationOf"));
            binder.registerBinder("operations", SimpleCollectionBinder.instance(Operation.class, "operations"));
            binder.registerBinder("documents", SimpleCollectionBinder.instance(Document.class, "documents"));
            binder.registerBinder("guaranteeTypes", SimpleCollectionBinder.instance(GuaranteeType.class, "guaranteeTypes"));
            dataBinder = binder;
        }
        return dataBinder;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final EditGroupPermissionsForm form = context.getForm();
        final long groupId = form.getGroupId();
        EditGroupAction.checkPermission(context, getPermissionService(), groupService, false, groupId);
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final EditGroupPermissionsForm form = context.getForm();
        final long id = form.getGroupId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        // OperatorGroup group = (OperatorGroup) groupService.load(id);

        final OperatorGroupPermissionsDTO dto = getDataBinder().readFromString(form.getPermission());
        groupService.setOperatorPermissions(dto);

        context.sendMessage("permission.modified");
        return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "groupId", id);
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final HttpSession session = request.getSession();
        final EditGroupPermissionsForm form = context.getForm();
        final long groupId = form.getGroupId();
        if (groupId <= 0) {
            throw new ValidationException();
        }
        final Group group = groupService.load(groupId, Group.Relationships.PERMISSIONS, Group.Relationships.TRANSFER_TYPES);

        MemberGroup loggedMemberGroup = (MemberGroup) context.getGroup();
        loggedMemberGroup = getFetchService().fetch(loggedMemberGroup, Group.Relationships.TRANSFER_TYPES, MemberGroup.Relationships.ACCOUNT_SETTINGS);

        // Get the associated account types
        final Collection<MemberAccountType> memberAccountTypes = loggedMemberGroup.getAccountTypes();
        request.setAttribute("memberAccountTypes", memberAccountTypes);

        // Groups
        final GroupQuery groupQuery = new GroupQuery();
        groupQuery.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
        groupQuery.setStatus(Group.Status.NORMAL);
        groupQuery.setIgnoreManagedBy(true);
        request.setAttribute("memberGroups", groupService.search(groupQuery));

        // Guarantee types
        request.setAttribute("guaranteeTypes", loggedMemberGroup.getGuaranteeTypes());

        // Just propagate permissions that the member has
        final List<Module> modules = getPermissionService().listModules(group.getNature());
        final List<Module> allowedModules = new ArrayList<Module>();
        for (final Module module : modules) {
            final String moduleName = module.getName();
            final String parentModuleName = moduleName.replaceAll("operator", "member");
            boolean ignoreTest;
            try {
                // When the member module does not exist, don't test it ;-)
                getPermissionService().loadModule(parentModuleName);
                ignoreTest = false;
            } catch (final EntityNotFoundException e) {
                ignoreTest = true;
            }
            final Collection<Operation> allowedOperations = new ArrayList<Operation>();
            for (final Operation operation : module.getOperations()) {
                final String operationName = operation.getName();
                if (!ignoreTest) {
                    try {
                        // When the member operation does not exist, don't test it ;-)
                        getPermissionService().loadOperation(parentModuleName, operationName);
                    } catch (final EntityNotFoundException e) {
                        ignoreTest = true;
                    }
                }
                // Special operations
                if (moduleName.equals("memberAccount") && !Boolean.TRUE.equals(session.getAttribute("loggedMemberHasAccounts"))) {
                    continue;
                } else if (operationName.equals("manageMemberReferences") && !Boolean.TRUE.equals(session.getAttribute("loggedMemberHasGeneralReferences"))) {
                    continue;
                } else if (operationName.equals("manageMemberTransactionFeedbacks") && !Boolean.TRUE.equals(session.getAttribute("loggedMemberHasTransactionFeedbacks"))) {
                    continue;
                } else if (moduleName.equals("operatorInvoices") && operationName.equals("manage") && !(getPermissionService().checkPermission("memberInvoices", "sendToMember") || getPermissionService().checkPermission("memberInvoices", "sendToSystem"))) {
                    continue;
                } else if (operationName.equals("quickAccess")) {
                    continue;
                }
                // Check the member permission
                if (ignoreTest || getPermissionService().checkPermission(parentModuleName, operationName)) {
                    allowedOperations.add(operation);
                }
                ignoreTest = false;
            }
            if (!CollectionUtils.isEmpty(allowedOperations)) {
                final Module allowedModule = (Module) module.clone();
                allowedModule.setOperations(allowedOperations);
                allowedModules.add(allowedModule);
            }
        }
        request.setAttribute("group", group);
        request.setAttribute("modules", allowedModules);

    }

}