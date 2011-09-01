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
import nl.strohalm.cyclos.entities.members.BrokeringQuery;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.brokerings.Brokering;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.elements.exceptions.CircularBrokeringException;
import nl.strohalm.cyclos.services.elements.exceptions.MemberAlreadyInBrokeringsException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for brokering relationships (broker - registered members by broker)
 * @author luis
 */
public interface BrokeringService extends Service {

    public Brokering getActiveBrokering(Member member);

    /**
     * Changes the broker of all members returned by the query. For members that already has such broker, nothing is done
     */
    @AdminAction(@Permission(module = "adminMemberBulkActions", operation = "changeBroker"))
    @IgnoreMember
    BulkMemberActionResultVO bulkChangeMemberBroker(FullTextMemberQuery query, Member newBroker, boolean suspendCommission, String comments);

    /**
     * Changes de broker of the specified member. Should generate a BrokerRemark to keep track of this change.
     * @param dto The new brokering data
     * @return The result brokering. It may be:
     * <ul>
     * <li>The current open brokering if just suspending commission (the old and new brokers are the same and suspendCommission == true)</li>
     * <li>The new brokering if there's a new broker</li>
     * <li><code>null</code> if the new broker is null</li>
     * </ul>
     * @throws CircularBrokeringException When the member is the broker of the new broker, or recursively
     * @throws MemberAlreadyInBrokeringsException When the member is already on the active broker's member list
     */
    @AdminAction(@Permission(module = "adminMemberBrokerings", operation = "changeBroker"))
    @PathToMember( { "member", "newBroker" })
    Brokering changeBroker(ChangeBrokerDTO dto) throws MemberAlreadyInBrokeringsException, CircularBrokeringException;

    /**
     * Create a new brokering relation. Intented to be called when a broker registers a member.
     */
    Brokering create(Member broker, Member brokered);

    /**
     * Loads the brokering by id, fetching the specified relationships
     */
    Brokering load(Long id, Relationship... fetch);

    /**
     * Marks the given brokering relationship as finished
     * @throws UnexpectedEntityException When the brokered is no longer active
     */
    Brokering remove(Brokering brokering, String remark) throws UnexpectedEntityException;

    /**
     * Removes all brokering relations that are expired
     */
    List<Brokering> removeExpiredBrokerings(Calendar time);

    /**
     * Search brokerings based on specified parameters
     */
    List<Brokering> search(BrokeringQuery query);

    /**
     * Validates the specified brokering
     * @param brokering Brokering to be validated
     * @throws ValidationException if validation fails.
     */
    void validate(Brokering brokering) throws ValidationException;

    /**
     * Validates the brokering change DTO
     */
    @DontEnforcePermission(traceable = true)
    void validate(ChangeBrokerDTO dto) throws ValidationException;
}