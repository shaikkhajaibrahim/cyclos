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
package nl.strohalm.cyclos.services.elements;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.members.adInterests.AdInterest;
import nl.strohalm.cyclos.entities.members.adInterests.AdInterestQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for ad interests
 * @author jefferson
 */
@RelatedEntity(AdInterest.class)
@PathToMember("owner")
public interface AdInterestService extends Service {

    /**
     * Loads the ad interest, fetching the specified relationships.
     * @return The ads loaded
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageAdInterests"))
    AdInterest load(Long id, Relationship... fetch);

    /**
     * This method notifies members interested on the given ad on a separate thread
     */
    @SystemAction
    void notifyInterestedMembers(Ad ad);

    /**
     * This method notifies members interested on all ads that became active on the given day and that have not yet been notified
     */
    @SystemAction
    void notifyInterestedMembers(Calendar time);

    /**
     * Remove the given ad interests.
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageAdInterests"))
    int remove(Long[] ids);

    /**
     * Saves the specified ad interest
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageAdInterests"))
    AdInterest save(AdInterest adInterest);

    /**
     * Search the existing ad interests based on the AdInterestQuery object
     * @return a list of ad interests
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageAdInterests"))
    List<AdInterest> search(AdInterestQuery query);

    /**
     * Validates the specified ad interest
     * @throws ValidationException if validation fails.
     */
    @MemberAction(@Permission(module = "memberPreferences", operation = "manageAdInterests"))
    void validate(AdInterest adInterest) throws ValidationException;

}