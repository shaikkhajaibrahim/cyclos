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
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContractQuery;
import nl.strohalm.cyclos.entities.members.brokerings.Brokering;
import nl.strohalm.cyclos.entities.members.brokerings.BrokeringCommissionStatus;
import nl.strohalm.cyclos.entities.members.brokerings.DefaultBrokerCommission;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for broker commissions
 * @author Jefferson Magno
 */
@RelatedEntity(BrokerCommissionContract.class)
@PathToMember("brokering.brokered")
public interface CommissionService extends Service {

    /**
     * A member accepts a pending broker commission contract. The status of the contract is changed to "ACTIVE"
     */
    @MemberAction
    BrokerCommissionContract acceptBrokerCommissionContract(Long brokerCommissionContractId);

    /**
     * Activates broker commission contracts that were accepted and begin today
     */
    @SystemAction
    void activateAcceptedBrokerCommissionContracts(final Calendar time);

    /**
     * A broker or admin cancels a broker commission contract. The status of the contract is changed to "CANCELLED"
     */
    @AdminAction(@Permission(module = "adminMemberBrokerings", operation = "manageCommissions"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "manageContracts"))
    BrokerCommissionContract cancelBrokerCommissionContract(Long brokerCommissionContractId);

    /**
     * This method closes the brokering commission status setting an end date (= now) when the max number of transactions that generates this
     * commission is reached
     */
    BrokeringCommissionStatus closeBrokeringCommissionStatus(BrokeringCommissionStatus brokeringCommissionStatus);

    /**
     * Creates the brokering commission status objects for each broker (and his/her brokereds) of the broker groups selected in the broker commission
     */
    void createBrokeringCommissionStatus(final BrokerCommission brokerCommission);

    /**
     * Creates the brokering commission status object
     */
    BrokeringCommissionStatus createBrokeringCommissionStatus(final Brokering brokering, final BrokerCommission brokerCommission);

    /**
     * Creates a default broker commission for each broker of the broker groups selected in the broker commission
     */
    void createDefaultBrokerCommissions(final BrokerCommission brokerCommission);

    /**
     * A member denies a pending broker commission contract. The status of the contract is changed to "DENIED"
     */
    @MemberAction
    BrokerCommissionContract denyBrokerCommissionContract(Long brokerCommissionContractId);

    /**
     * Expires broker commission contracts that were not accepted until it´s beginning and close contracts that expired at the end of the previous day
     */
    void expireBrokerCommissionContracts(final Calendar time);

    /**
     * Expires brokering commission status
     */
    void expireBrokeringCommissionStatus(final Calendar date);

    /**
     * Returns the active broker commission contract or null if it doesn't exist
     */
    BrokerCommissionContract getActiveBrokerCommissionContract(Brokering brokering, BrokerCommission brokerCommission);

    /**
     * TODO: javadoc
     */
    BrokeringCommissionStatus getBrokeringCommissionStatus(final Brokering brokering, final BrokerCommission brokerCommission);

    /**
     * Returns a list of charge status for broker commissions related to a member
     */
    List<CommissionChargeStatusDTO> getCommissionChargeStatus(final Member member);

    /**
     * Returns the default broker commission
     */
    DefaultBrokerCommission getDefaultBrokerCommission(Member broker, BrokerCommission brokerCommission);

    /**
     * Returns the brokering commission status or null if it doesn't exist
     */
    BrokeringCommissionStatus getOrUpdateBrokeringCommissionStatus(final Brokering brokering, final BrokerCommission brokerCommission);

    /**
     * Loads a broker commission contraction, fetching the specified relationships
     */
    BrokerCommissionContract loadBrokerCommissionContract(Long id, Relationship... fetch);

    /**
     * Loads the default broker commissions for the given broker
     */
    List<DefaultBrokerCommission> loadDefaultBrokerCommissions(Member broker, Relationship... fetch);

    /**
     * Removes the specified broker commission contracts
     * @return The number of removed broker commission contracts
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "manageContracts"))
    int removeBrokerCommissionContracts(Long... ids);

    /**
     * Saves the broker commission contract
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "manageContracts"))
    BrokerCommissionContract saveBrokerCommissionContract(BrokerCommissionContract brokerCommissionContract);

    /**
     * Saves the default broker commissions, returning the updated instances
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "manageDefaults"))
    @IgnoreMember
    List<DefaultBrokerCommission> saveDefaultBrokerCommissions(List<DefaultBrokerCommission> commissions);

    /**
     * Searchs for broker commission contracts
     */
    List<BrokerCommissionContract> searchBrokerCommissionContracts(BrokerCommissionContractQuery query);

    /**
     * Sets the default broker commission to 0, close the broker commission contracts and close the brokering commission statuses of the broker
     */
    void stopCommissions(final BrokerCommission brokerCommission, final Member broker, final boolean removeDefaulBrokerCommission);

    /**
     * Suspend the default broker commission and suspend broker commission contracts
     */
    void suspendCommissions(final BrokerCommission brokerCommission, final Member broker);

    /**
     * Unsuspend the default broker commission and unsuspend broker commission contracts
     */
    void unsuspendCommissions(final BrokerCommission brokerCommission, final Member broker);

    /**
     * Create default broker commissions or brokering commission status applicable to the new group and suspend commissions that are not applicable to
     * new group
     */
    void updateBrokerCommissions(Member broker, MemberGroup oldGroup, MemberGroup newGroup);

    /**
     * Create brokering commission status for groups added to the commission and suspend commissions for groups removed from the commission
     */
    void updateBrokeringCommissionStatus(final BrokerCommission brokerCommission, final BrokerCommission savedBrokerCommission);

    /**
     * Saves the brokering commission status
     */
    BrokeringCommissionStatus updateBrokeringCommissionStatus(BrokeringCommissionStatus brokeringCommissionStatus);

    /**
     * Create default broker commissions for groups added to the commission and suspend commissions for groups removed from the commission
     */
    void updateDefaultBrokerCommissions(final BrokerCommission brokerCommission, final BrokerCommission savedBrokerCommission);

    /**
     * Validates the specified broker commission contract
     * @throws ValidationException if validation fails.
     */
    @DontEnforcePermission(traceable = true)
    void validateBrokerCommissionContract(BrokerCommissionContract brokerCommissionContract) throws ValidationException;

    /**
     * Validates the collection of default broker commissions before saving
     * @throws ValidationException if validation fails.
     */
    @DontEnforcePermission(traceable = true)
    void validateDefaultBrokerCommissions(List<DefaultBrokerCommission> defaultBrokerCommissions) throws ValidationException;

}