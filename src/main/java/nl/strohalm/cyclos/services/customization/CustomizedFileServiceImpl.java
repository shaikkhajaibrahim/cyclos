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
package nl.strohalm.cyclos.services.customization;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.customizations.CustomizedFileDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFileQuery;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile.Type;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.Validator;

/**
 * Implementation for customized file service
 * @author luis
 */
public class CustomizedFileServiceImpl implements CustomizedFileService {

    private FetchService      fetchService;
    private CustomizedFileDAO customizedFileDao;

    public CustomizedFile load(final Long id, final Relationship... fetch) {
        return customizedFileDao.load(id, fetch);
    }

    public CustomizedFile load(final Type type, final String name, final Relationship... fetch) {
        return customizedFileDao.load(type, name, fetch);
    }

    public CustomizedFile save(final CustomizedFile customizedFile) {
        return doSave(customizedFile);
    }

    public CustomizedFile saveForAdminGroup(final CustomizedFile customizedFile) {
        final Group group = fetchService.fetch(customizedFile.getGroup());
        if (group == null || !(group instanceof AdminGroup)) {
            throw new UnexpectedEntityException();
        }
        customizedFile.setGroup(group);
        return doSave(customizedFile);
    }

    public CustomizedFile saveForGroupFilter(final CustomizedFile customizedFile) {
        final GroupFilter groupFilter = fetchService.fetch(customizedFile.getGroupFilter());
        if (groupFilter == null) {
            throw new UnexpectedEntityException();
        }
        customizedFile.setGroupFilter(groupFilter);
        return doSave(customizedFile);
    }

    public CustomizedFile saveForMemberGroup(final CustomizedFile customizedFile) {
        final Group group = fetchService.fetch(customizedFile.getGroup());
        if (customizedFile.getGroup() == null) {
            throw new UnexpectedEntityException();
        }
        customizedFile.setGroup(group);
        return doSave(customizedFile);
    }

    public CustomizedFile saveForOperatorGroup(final CustomizedFile customizedFile) {
        final Group group = fetchService.fetch(customizedFile.getGroup());
        customizedFile.setGroup(group);
        if (customizedFile.getGroup() == null || !(customizedFile.getGroup() instanceof OperatorGroup) || !LoggedUser.element().equals(((OperatorGroup) group).getMember())) {
            throw new UnexpectedEntityException();
        }
        return doSave(customizedFile);
    }

    public CustomizedFile saveForTheme(final CustomizedFile customizedFile) {
        return doSave(customizedFile);
    }

    public CustomizedFile saveGlobal(final CustomizedFile customizedFile) {
        if (customizedFile.getGroup() != null || customizedFile.getGroupFilter() != null) {
            throw new UnexpectedEntityException();
        }
        return doSave(customizedFile);
    }

    public List<CustomizedFile> search(final CustomizedFileQuery query) {
        return customizedFileDao.search(query);
    }

    public void setCustomizedFileDao(final CustomizedFileDAO customizedFileDAO) {
        customizedFileDao = customizedFileDAO;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void stopCustomizingForAdminGroup(final CustomizedFile customizedFile) {
        stopCustomizing(customizedFile);
    }

    public void stopCustomizingForGroupFilter(final CustomizedFile customizedFile) {
        stopCustomizing(customizedFile);
    }

    public void stopCustomizingForMemberGroup(final CustomizedFile customizedFile) {
        final Group group = customizedFile.getGroup();
        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        if (!adminGroup.getManagesGroups().contains(group)) {
            throw new PermissionDeniedException();
        }
        stopCustomizing(customizedFile);
    }

    public void stopCustomizingForOperatorGroup(final CustomizedFile customizedFile) {
        final Group group = customizedFile.getGroup();
        if (!(group instanceof OperatorGroup)) {
            throw new PermissionDeniedException();
        }

        stopCustomizing(customizedFile);
    }

    public void stopCustomizingGlobal(final CustomizedFile customizedFile) {
        stopCustomizing(customizedFile);
    }

    public void validate(final CustomizedFile customizedFile) {
        getValidator().validate(customizedFile);
    }

    private CustomizedFile doSave(final CustomizedFile customizedFile) {
        validate(customizedFile);
        if (customizedFile.isTransient() && customizedFile.getGroup() == null && customizedFile.getGroupFilter() == null) {
            // Check a file with that name and type is already customized
            try {
                final CustomizedFile current = load(customizedFile.getType(), customizedFile.getName());
                // The file exists - We shall update it's contents only
                current.setLastModified(Calendar.getInstance());
                current.setContents(customizedFile.getContents());
                current.setOriginalContents(customizedFile.getOriginalContents());
                current.setNewContents(customizedFile.getNewContents());
                return customizedFileDao.update(current);
            } catch (final EntityNotFoundException e) {
                // Ok - Not already customized
            }
        }
        if (customizedFile.isTransient()) {
            customizedFile.setLastModified(Calendar.getInstance());
            return customizedFileDao.insert(customizedFile);
        } else {
            // Load the current version to update the contents
            final CustomizedFile current = load(customizedFile.getId(), CustomizedFile.Relationships.GROUP);
            current.setLastModified(Calendar.getInstance());
            current.setContents(customizedFile.getContents());
            current.setOriginalContents(customizedFile.getOriginalContents());
            current.setNewContents(customizedFile.getNewContents());
            return customizedFileDao.update(current);
        }
    }

    private Validator getValidator() {
        final Validator validator = new Validator("customizedFile");
        validator.property("name").required().maxLength(100);
        validator.property("type").required();
        return validator;
    }

    private void stopCustomizing(CustomizedFile customizedFile) {
        customizedFile = fetchService.fetch(customizedFile, CustomizedFile.Relationships.GROUP);
        customizedFileDao.delete(customizedFile.getId());
    }

}
