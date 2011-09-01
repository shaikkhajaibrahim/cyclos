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
package nl.strohalm.cyclos.controls.groups.customizedFiles;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.services.customization.CustomizedFileService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.CoercionConverter;
import nl.strohalm.cyclos.utils.conversion.IdConverter;
import nl.strohalm.cyclos.utils.conversion.ReferenceConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to edit a customized file for a group
 * @author luis
 */
public class EditGroupCustomizedFileAction extends BaseFormAction {

    private CustomizedFileService      customizedFileService;
    private GroupService               groupService;
    private DataBinder<CustomizedFile> dataBinder;

    public CustomizedFileService getCustomizedFileService() {
        return customizedFileService;
    }

    public DataBinder<CustomizedFile> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<CustomizedFile> binder = BeanBinder.instance(CustomizedFile.class);
            binder.registerBinder("id", PropertyBinder.instance(Long.class, "id", IdConverter.instance()));
            binder.registerBinder("type", PropertyBinder.instance(CustomizedFile.Type.class, "type"));
            binder.registerBinder("group", PropertyBinder.instance(Group.class, "group", ReferenceConverter.instance(Group.class)));
            binder.registerBinder("name", PropertyBinder.instance(String.class, "name"));
            binder.registerBinder("contents", PropertyBinder.instance(String.class, "contents", CoercionConverter.instance(String.class)));
            dataBinder = binder;
        }
        return dataBinder;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    @Inject
    public void setCustomizedFileService(final CustomizedFileService customizedFileService) {
        this.customizedFileService = customizedFileService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final EditGroupCustomizedFileForm form = context.getForm();
        final HttpServletRequest request = context.getRequest();
        CustomizedFile file = getDataBinder().readFromString(form.getFile());
        Group group = file.getGroup();
        // Ensure the file has a group
        if (group == null) {
            throw new ValidationException();
        } else {
            group = getFetchService().fetch(group);
            file.setGroup(group);
        }

        final boolean isInsert = file.isTransient();
        if (group instanceof MemberGroup) {
            file = customizedFileService.saveForMemberGroup(file);
        } else if (group instanceof OperatorGroup) {
            file.setType(CustomizedFile.Type.STATIC_FILE);
            file = customizedFileService.saveForOperatorGroup(file);
        } else {
            file = customizedFileService.saveForAdminGroup(file);
        }

        // Physically update the file
        final ServletContext servletContext = getServlet().getServletContext();
        final File physicalFile = CustomizationHelper.customizedFileOf(servletContext, file);
        CustomizationHelper.updateFile(servletContext, physicalFile, file);

        context.sendMessage(isInsert ? "group.customizedFiles.customized" : "group.customizedFiles.modified");
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("fileId", file.getId());
        params.put("groupId", group.getId());
        return ActionHelper.redirectWithParams(request, context.getSuccessForward(), params);
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final EditGroupCustomizedFileForm form = context.getForm();
        final HttpServletRequest request = context.getRequest();

        boolean editable = false;

        // Retrieve the group
        final long groupId = form.getGroupId();
        if (groupId <= 0L) {
            throw new ValidationException();
        }
        final Group group = groupService.load(groupId);

        final long id = form.getFileId();
        final boolean isInsert = id <= 0L;
        CustomizedFile file;
        if (isInsert) {
            file = new CustomizedFile();
            file.setGroup(group);
            // Prepare the possible types
            request.setAttribute("types", Arrays.asList(CustomizedFile.Type.STATIC_FILE, CustomizedFile.Type.STYLE));
            editable = true;
        } else {
            // Retrieve the file
            file = customizedFileService.load(id);
            if (file.getGroup() == null || !file.getGroup().equals(group)) {
                // Wrong group passed
                throw new ValidationException();
            }
            if (context.isAdmin()) {
                AdminGroup adminGroup = context.getGroup();
                adminGroup = getFetchService().fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                if (group instanceof AdminGroup) {
                    if (getPermissionService().checkPermission("systemAdminGroups", "manageAdminCustomizedFiles")) {
                        editable = true;
                    }
                } else {
                    if (getPermissionService().checkPermission("adminMemberGroups", "manageMemberCustomizedFiles") && adminGroup.getManagesGroups().contains(group)) {
                        editable = true;
                    }
                }
            } else {
                // Member editing an operator group customized file
                editable = true;
            }

        }
        request.setAttribute("file", file);
        getDataBinder().writeAsString(form.getFile(), file);
        request.setAttribute("group", group);
        request.setAttribute("isInsert", isInsert);
        request.setAttribute("editable", editable);
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final EditGroupCustomizedFileForm form = context.getForm();
        final CustomizedFile file = getDataBinder().readFromString(form.getFile());
        if (context.isMember()) {
            file.setType(CustomizedFile.Type.STATIC_FILE);
        }
        customizedFileService.validate(file);
    }
}
