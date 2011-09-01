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

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFileQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for managing customized files (save, load, edit)
 * @author luis
 */
public interface CustomizedFileService extends Service {

    /**
     * Loads a system-wide customized file by type and name
     */
    CustomizedFile load(CustomizedFile.Type type, String name, Relationship... fetch);

    /**
     * Loads a customized file by id
     */
    CustomizedFile load(Long id, Relationship... fetch);

    /**
     * Saves a customized file with no permission check. Should be used on internal procedures
     */
    @SystemAction
    CustomizedFile save(CustomizedFile customizedFile);

    /**
     * Saves a customized file for an admin group
     */
    @AdminAction(@Permission(module = "systemAdminGroups", operation = "manageAdminCustomizedFiles"))
    CustomizedFile saveForAdminGroup(CustomizedFile customizedFile);

    /**
     * Saves a customized file for a group filter
     */
    @AdminAction(@Permission(module = "systemGroupFilters", operation = "manageCustomizedFiles"))
    CustomizedFile saveForGroupFilter(CustomizedFile file);

    /**
     * Saves a customized file for a member group
     */
    @AdminAction(@Permission(module = "adminMemberGroups", operation = "manageMemberCustomizedFiles"))
    @IgnoreMember
    CustomizedFile saveForMemberGroup(CustomizedFile customizedFile);

    /**
     * Saves a customized file for an operator group
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("group.member")
    CustomizedFile saveForOperatorGroup(CustomizedFile customizedFile);

    /**
     * Saves a customized file.
     */
    @AdminAction(@Permission(module = "systemThemes", operation = "select"))
    CustomizedFile saveForTheme(CustomizedFile customizedFile);

    /**
     * Saves a system-wide customized file
     */
    @AdminAction(@Permission(module = "systemCustomizedFiles", operation = "manage"))
    CustomizedFile saveGlobal(CustomizedFile customizedFile);

    /**
     * Searches for customized files, according to the given parameters
     */
    List<CustomizedFile> search(CustomizedFileQuery query);

    /**
     * Stops customizing the file for an admin group, leaving the file as it is on the file system.
     */
    @AdminAction(@Permission(module = "systemAdminGroups", operation = "manageAdminCustomizedFiles"))
    void stopCustomizingForAdminGroup(CustomizedFile customizedFile);

    /**
     * Stops customizing the file for a group filter, leaving the file as it is on the file system.
     */
    @AdminAction(@Permission(module = "systemGroupFilters", operation = "manageCustomizedFiles"))
    void stopCustomizingForGroupFilter(CustomizedFile customizedFile);

    /**
     * Stops customizing the file for a member group, leaving the file as it is on the file system.
     */
    @AdminAction(@Permission(module = "adminMemberGroups", operation = "manageMemberCustomizedFiles"))
    @IgnoreMember
    void stopCustomizingForMemberGroup(CustomizedFile customizedFile);

    /**
     * Stops customizing the file for an operator group, leaving the file as it is on the file system.
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("group.member")
    void stopCustomizingForOperatorGroup(CustomizedFile customizedFile);

    /**
     * Stops customizing a system-wide file, leaving the file as it is on the file system.
     */
    @AdminAction(@Permission(module = "systemCustomizedFiles", operation = "manage"))
    void stopCustomizingGlobal(CustomizedFile customizedFile);

    /**
     * Validates a customized file
     */
    @DontEnforcePermission(traceable = true)
    void validate(CustomizedFile customizedFile) throws ValidationException;

}
