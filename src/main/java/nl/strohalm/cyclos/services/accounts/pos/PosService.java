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
/**
 * 
 */
package nl.strohalm.cyclos.services.accounts.pos;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.accounts.pos.PosQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for POS
 * @author rodrigo
 */
public interface PosService extends Service {

    /**
     * Assign the POS to a given member
     * @param posId
     * @param member
     */

    @AdminAction(@Permission(module = "adminMemberPos", operation = "assign"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "assign"))
    @PathToMember("")
    public Pos assignPos(final Member member, Long posId);

    /**
     * Delete the POS
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public void deletePos(Long... ids);

    /**
     * Discard the POS - cannot be used again
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public Pos discardPos(Long posId);

    /**
     * Generate a PosLog
     */
    @SystemAction
    public void generateLog(final Pos pos);

    /**
     * Loads a POS, fetching the specified relationships
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public Pos load(Long id, Relationship... fetch);

    /**
     * Loads a POS, fetching the specified relationships
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public Pos loadByPosId(String posId, Relationship... fetch);

    /**
     * Persist the POS
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public Pos save(Pos pos);

    /**
     * Search for Pos based on the given query
     * @param query
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "view"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "view"))
    @IgnoreMember
    public List<Pos> search(PosQuery query);

    /**
     * Unassign all POS from a member
     * @param member
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "assign"))
    @PathToMember("")
    public void unassignAllMemberPos(Member member);

    /**
     * Unassign the POS from the member it was assigned
     */

    @AdminAction(@Permission(module = "adminMemberPos", operation = "assign"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "assign"))
    @RelatedEntity(Pos.class)
    @PathToMember("memberPos.member")
    public Pos unassignPos(Long posId);

    /**
     * Validate the POS been persisted
     */
    @DontEnforcePermission(traceable = true)
    public void validate(final Pos pos);

}
