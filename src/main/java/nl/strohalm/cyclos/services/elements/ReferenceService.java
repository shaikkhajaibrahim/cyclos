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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.GeneralReference;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.PaymentsAwaitingFeedbackQuery;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.entities.members.ReferenceQuery;
import nl.strohalm.cyclos.entities.members.TransactionFeedback;
import nl.strohalm.cyclos.entities.members.Reference.Level;
import nl.strohalm.cyclos.entities.members.Reference.Nature;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for member references
 * @author luis
 */
@RelatedEntity(Reference.class)
@PathToMember("from")
public interface ReferenceService extends Service {

    /**
     * Count the global number of given references by level (Reference.Level)
     * @return number of given references by level
     */
    @AdminAction(@Permission(module = "systemReports", operation = "current"))
    @IgnoreMember
    Map<Reference.Level, Integer> countGivenReferencesByLevel(Reference.Nature nature);

    /**
     * Count the number of references of a member by level (Reference.Level)
     * @param member the member
     * @param received true to count received references, false to count given references
     */
    Map<Level, Integer> countReferencesByLevel(Reference.Nature nature, Member member, boolean received);

    /**
     * Searches on the history for the references by member and a date.
     * @param member
     * @param received
     */
    Map<Level, Integer> countReferencesHistoryByLevel(Reference.Nature nature, Member member, Period date, boolean received);

    /**
     * Returns the reference natures a member group is related to
     */
    Collection<Nature> getNaturesByGroup(MemberGroup group);

    /**
     * Loads a reference fetching the specified relationships
     * @param id Id of the reference to be loaded
     * @param fetch array of relationships to be fetched
     * @return The loaded reference
     * @throws EntityNotFoundException When no such reference exists
     */
    @AdminAction(@Permission(module = "adminMemberReferences", operation = "view"))
    @MemberAction(@Permission(module = "memberReferences", operation = "view"))
    @OperatorAction(@Permission(module = "operatorReferences", operation = "view"))
    @BrokerAction(@Permission(module = "brokerReferences", operation = "manage"))
    Reference load(Long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads a general reference by from / to fetching the specified relationships
     * @param fetch array of relationships to be fetched
     * @return The loaded reference
     * @throws EntityNotFoundException When no such reference exists
     */
    @AdminAction(@Permission(module = "adminMemberReferences", operation = "view"))
    @MemberAction(@Permission(module = "memberReferences", operation = "view"))
    @OperatorAction(@Permission(module = "operatorReferences", operation = "view"))
    @BrokerAction(@Permission(module = "brokerReferences", operation = "manage"))
    @PathToMember("")
    GeneralReference loadGeneral(Member from, Member to, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads the transaction feedback for the given payment
     * @throws EntityNotFoundException When no such transaction feedback exists
     */
    TransactionFeedback loadTransactionFeedback(Payment payment, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Create default feedback for transactions that did not receive one after the maximum period of time.
     */
    int processExpiredFeedbacks(Calendar time);

    /**
     * Removes the specified references
     * @param id Array of references to be removed
     * @return The number of remerences removed
     * @throws PermissionDeniedException When the logged member is not the from, or his broker
     */
    @AdminAction(@Permission(module = "adminMemberReferences", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerReferences", operation = "manage"))
    int removeMemberReference(Long... id) throws PermissionDeniedException;

    /**
     * Removes the specified references
     * @param id Array of references to be removed
     * @return The number of references removed
     * @throws PermissionDeniedException When the logged member is not the from, or his broker
     */
    @MemberAction(@Permission(module = "memberReferences", operation = "give"))
    @OperatorAction(@Permission(module = "operatorReferences", operation = "give"))
    int removeMyReference(Long... id) throws PermissionDeniedException;

    /**
     * Saves the given reference
     * @param reference Reference to be saved
     * @return The reference saved
     * @throws PermissionDeniedException When an administrator is trying to insert a reference, or when a member is changing another's reference
     * without being his broker
     */
    @AdminAction(@Permission(module = "adminMemberReferences", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerReferences", operation = "manage"))
    GeneralReference saveMemberReference(GeneralReference reference) throws PermissionDeniedException;

    /**
     * Saves the given reference
     * @param reference Reference to be saved
     * @return The reference saved
     * @throws PermissionDeniedException When an administrator is trying to insert a reference, or when a member is changing another's reference
     * without being his broker
     */
    @MemberAction(@Permission(module = "memberReferences", operation = "give"))
    @OperatorAction(@Permission(module = "operatorReferences", operation = "manageMemberReferences"))
    GeneralReference saveMyReference(GeneralReference reference) throws PermissionDeniedException;

    /**
     * Sets the transaction feedback admin comments
     */
    @AdminAction(@Permission(module = "adminMemberTransactionFeedbacks", operation = "manage"))
    @PathToMember( { "from", "to" })
    TransactionFeedback saveTransactionFeedbackByAdmin(TransactionFeedback transactionFeedback);

    /**
     * Sets the transaction feedback
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorReferences", operation = "manageMemberTransactionFeedbacks"))
    @PathToMember("from")
    TransactionFeedback saveTransactionFeedbackComments(TransactionFeedback transactionFeedback);

    /**
     * Sets the transaction feedback reply
     */
    @MemberAction
    @PathToMember("to")
    TransactionFeedback saveTransactionFeedbackReplyComments(TransactionFeedback transactionFeedback);

    /**
     * Searches for references
     */
    List<? extends Reference> search(ReferenceQuery query);

    /**
     * Searches for payments awaiting buyer / seller feedback
     */
    List<Payment> searchPaymentsAwaitingFeedback(PaymentsAwaitingFeedbackQuery query);

    /**
     * Validates the specified reference
     * @param reference Reference to be validated
     * @throws ValidationException if validation fails
     */
    void validate(Reference reference) throws ValidationException;

}