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
package nl.strohalm.cyclos.services.groups;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.GroupFilterQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for groups filters. This service is used to create, add and remove groups and to list groups.
 * @author jeferson
 */

public interface GroupFilterService extends Service {

    /**
     * Finds a group filter by it's login page name
     */
    GroupFilter findByLoginPageName(String loginPageName);

    /**
     * Loads the specified group filter, fetching the specified relationships
     */
    GroupFilter load(Long id, Relationship... fetch);

    /**
     * Removes the specified group filters
     * @return The number of removed objects
     */
    @AdminAction(@Permission(module = "systemGroupFilters", operation = "manage"))
    int remove(Long... ids);

    /**
     * Saves the group filter, returning the resulting object
     */
    @AdminAction(@Permission(module = "systemGroupFilters", operation = "manage"))
    GroupFilter save(GroupFilter groupFilter);

    /**
     * Searches existing group filters according to the specified query parameters
     */
    List<GroupFilter> search(GroupFilterQuery query);

    /**
     * Validate the specified group filter
     */
    void validate(GroupFilter groupFilter);

}