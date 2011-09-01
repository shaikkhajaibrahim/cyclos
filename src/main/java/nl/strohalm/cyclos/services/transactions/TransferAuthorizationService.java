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
package nl.strohalm.cyclos.services.transactions;

import java.util.List;

import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorizationQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransfersAwaitingAuthorizationQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.exceptions.AlreadyAuthorizedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

public interface TransferAuthorizationService extends Service {

    /**
     * Authorizes a payment made by the logged user
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "authorize"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "authorize"))
    @PathToMember("transfer.from.member")
    Transfer authorizeAsPayer(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Authorizes a payment made to the logged user
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "authorize"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "authorize"))
    @PathToMember("transfer.to.member")
    Transfer authorizeAsReceiver(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Authorizes a payment made by a member
     * @throws AlreadyAuthorizedException When the user is trying to authorize a payment, but he has already authorized a previous level
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "authorize"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "authorize"))
    @PathToMember("transfer.from.member")
    Transfer authorizeFromMember(TransferAuthorizationDTO transferAuthorizationDto) throws AlreadyAuthorizedException;

    /**
     * Authorizes a payment from a system account
     * @throws AlreadyAuthorizedException When the user is trying to authorize a payment, but he has already authorized a previous level
     */
    @AdminAction(@Permission(module = "systemPayments", operation = "authorize"))
    Transfer authorizeFromSystem(TransferAuthorizationDTO transferAuthorizationDto) throws AlreadyAuthorizedException;

    /**
     * Automatically called when a transfer is inserted to authorize instantly when the one making a payment as member is the one that authorizes
     */
    @SystemAction
    Transfer authorizeOnInsert(Transfer transfer);

    /**
     * Cancels a payment that is awaiting for authorization in behalf of the payer
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "cancelAuthorizedAsMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "cancelAuthorizedAsMember"))
    @PathToMember("transfer.from.member")
    Transfer cancelAsMember(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Cancels a payment that is awaiting for authorization belonging to the payer
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "cancelAuthorized"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "cancelAuthorized"))
    @PathToMember("transfer.from.member")
    Transfer cancelFromMember(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Cancels a payment that is awaiting for authorization belonging to the receiver
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "cancelAuthorized"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "cancelAuthorized"))
    @PathToMember("transfer.to.member")
    Transfer cancelFromMemberAsReceiver(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Cancels a payment that is awaiting for authorization belonging to the system
     */
    @AdminAction(@Permission(module = "systemPayments", operation = "cancel"))
    Transfer cancelFromSystem(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Denies a payment to the logged member
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "authorize"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "authorize"))
    @PathToMember("transfer.to.member")
    Transfer denyAsReceiver(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Denies a payment that was made by a member
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "authorize"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "authorize"))
    @PathToMember("transfer.from.member")
    Transfer denyFromMember(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Denies a payment from a system account
     */
    @AdminAction(@Permission(module = "systemPayments", operation = "authorize"))
    Transfer denyFromSystem(TransferAuthorizationDTO transferAuthorizationDto);

    /**
     * Searches for transfer authorizations
     */
    @AdminAction( { @Permission(module = "adminMemberPayments", operation = "authorize"), @Permission(module = "systemPayments", operation = "authorize") })
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "authorize"))
    @MemberAction(@Permission(module = "memberPayments", operation = "authorize"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "authorize"))
    @IgnoreMember
    List<TransferAuthorization> searchAuthorizations(TransferAuthorizationQuery query);

    /**
     * Searches for transfers awaiting authorization of the logged element
     */
    @AdminAction( { @Permission(module = "adminMemberPayments", operation = "authorize"), @Permission(module = "systemPayments", operation = "authorize") })
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "authorize"))
    @MemberAction(@Permission(module = "memberPayments", operation = "authorize"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "authorize"))
    @IgnoreMember
    List<Transfer> searchTransfersAwaitingMyAuthorization(TransfersAwaitingAuthorizationQuery query);
}