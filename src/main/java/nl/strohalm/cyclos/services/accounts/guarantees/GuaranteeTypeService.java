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
package nl.strohalm.cyclos.services.accounts.guarantees;

import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeTypeQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for guarantee types
 * @author Jefferson Magno
 */
public interface GuaranteeTypeService extends Service {

    /**
     * @return true if there are at least one enabled guarantee type
     */
    @DontEnforcePermission(traceable = false)
    boolean areEnabledGuaranteeTypes();

    /**
     * Loads the guarantee type, fetching the specified relationships
     */
    @AdminAction( { @Permission(module = "systemGuaranteeTypes", operation = "view"), @Permission(module = "adminMemberGuarantees", operation = "registerGuarantees") })
    @IgnoreMember
    GuaranteeType load(Long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Removes the specified guarantee types
     */
    @AdminAction(@Permission(module = "systemGuaranteeTypes", operation = "manage"))
    int remove(Long... ids);

    /**
     * Saves the specified guarantee type, returning it with the generated id if it doesn't exist yet.
     */
    @AdminAction(@Permission(module = "systemGuaranteeTypes", operation = "manage"))
    GuaranteeType save(GuaranteeType guaranteeType);

    /**
     * Search guarantee types
     */
    @AdminAction( { @Permission(module = "systemGuaranteeTypes", operation = "view"), @Permission(module = "adminMemberGroups", operation = "view"), @Permission(module = "systemAdminGroups", operation = "view") })
    @IgnoreMember
    List<GuaranteeType> search(GuaranteeTypeQuery guaranteeTypeQuery);

    /**
     * Validates the specified guarantee type
     */
    @DontEnforcePermission(traceable = true)
    void validate(GuaranteeType guaranteeType) throws ValidationException;

}