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

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.InvalidPosPinException;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.PosPinBlockedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * @author rodrigo
 */
public interface MemberPosService extends Service {

    /**
     * Block the given MemberPos
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "block"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "block"))
    @MemberAction
    @PathToMember("member")
    public MemberPos blockMemberPos(MemberPos memberPos);

    /**
     * Change MemberPOS Pin
     * @param memberPos
     * @param pin
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "changePin"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "changePin"))
    @MemberAction
    @PathToMember("member")
    public MemberPos changePin(MemberPos memberPos, final String pin);

    /**
     * Checks the member pos pin
     * @throws InvalidPosPinException Invalid pin
     * @throws PosPinBlockedException Pin blocked by exceeding wrong tries
     */
    @SystemAction
    public void checkPin(MemberPos memberPos, final String pin) throws InvalidPosPinException, PosPinBlockedException;

    /**
     * Loads a MemberPos, fetching the specified relationships
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "view"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "view"))
    @MemberAction
    @RelatedEntity(MemberPos.class)
    @PathToMember("member")
    public MemberPos load(Long id, Relationship... fetch);

    /**
     * Persist the memberPos
     * @param memberPos
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "manage"))
    @MemberAction
    @PathToMember("member")
    public void save(MemberPos memberPos);

    /**
     * Unblock given MemberPos
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "block"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "block"))
    @MemberAction
    @PathToMember("member")
    public MemberPos unblockMemberPos(MemberPos memberPos);

    /**
     * Unblock the MemberPos pin
     * @param memberPos
     */
    @AdminAction(@Permission(module = "adminMemberPos", operation = "unblockPin"))
    @BrokerAction(@Permission(module = "brokerPos", operation = "unblockPin"))
    @MemberAction
    @PathToMember("member")
    public MemberPos unblockPosPin(final MemberPos memberPos);
}
