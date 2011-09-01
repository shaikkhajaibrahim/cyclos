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
package nl.strohalm.cyclos.services.loangroups;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroup;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroupQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.loangroups.exceptions.MemberAlreadyInListException;
import nl.strohalm.cyclos.services.loangroups.exceptions.MemberNotInListException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;

/**
 * Service interface for (micro finance) loan groups.
 * @author luis
 */
@RelatedEntity(LoanGroup.class)
public interface LoanGroupService extends Service {

    /**
     * Adds a member to the loan group
     * @throws MemberAlreadyInListException When the member is already on the given loan group
     */
    @AdminAction(@Permission(module = "adminMemberLoanGroups", operation = "manage"))
    @PathToMember("")
    void addMember(Member member, LoanGroup loanGroup) throws MemberAlreadyInListException;

    /**
     * Loads a loan group
     */
    @AdminAction(@Permission(module = "systemLoanGroups", operation = "view"))
    @BrokerAction(@Permission(module = "brokerLoanGroups", operation = "view"))
    @MemberAction
    @IgnoreMember
    LoanGroup load(Long id, Relationship... fetch);

    /**
     * Removes the loan groups, returning the number of removed objects
     */
    @AdminAction(@Permission(module = "systemLoanGroups", operation = "manage"))
    int remove(Long... ids);

    /**
     * Removes a member to the loan group
     * @throws MemberNotInListException When the member isn't already on the given loan group
     */
    @AdminAction(@Permission(module = "adminMemberLoanGroups", operation = "manage"))
    @PathToMember("")
    void removeMember(Member member, LoanGroup loanGroup) throws MemberNotInListException;

    /**
     * Saves the loan group, returning the resulting object
     */
    @AdminAction(@Permission(module = "systemLoanGroups", operation = "manage"))
    LoanGroup save(LoanGroup loanGroup);

    /**
     * Searches the loan groups defined in the system
     */
    @AdminAction( { @Permission(module = "systemLoanGroups", operation = "view") })
    List<LoanGroup> search(LoanGroupQuery query);

    /**
     * Searches the loan groups for a specific member
     */
    @AdminAction( { @Permission(module = "adminMemberLoanGroups", operation = "view") })
    @BrokerAction(@Permission(module = "brokerLoanGroups", operation = "view"))
    @MemberAction
    @PathToMember("member")
    List<LoanGroup> searchForMember(LoanGroupQuery query);

    /**
     * Validates the specified loan group
     */
    void validate(LoanGroup loanGroup);

}