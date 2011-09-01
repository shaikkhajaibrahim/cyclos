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
package nl.strohalm.cyclos.services.accounts.cards;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.cards.CardQuery;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.elements.BulkMemberActionResultVO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for Cards
 * @author rodrigo
 */
public interface CardService extends Service {

    /**
     * Cancel all cards from a given member. It's called when an administrator is moving a member to another group
     */

    @AdminAction(@Permission(module = "adminMembers", operation = "changeGroup"))
    @PathToMember("")
    public void cancelAllMemberCards(Member member);

    /**
     * Loads a Card, fetching the specified relationships
     */
    @AdminAction(@Permission(module = "adminMemberCards", operation = "view"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "view"))
    @MemberAction(@Permission(module = "memberCards", operation = "view"))
    @RelatedEntity(Card.class)
    @PathToMember("owner")
    public Card load(long cardId, final Relationship... fetch);

    /**
     * Unblock card security code
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "unblockSecurityCode"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "unblockSecurityCode"))
    @PathToMember("owner")
    public void unblockSecurityCode(Card card);

    /**
     * Activate the given card
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "unblock"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "unblock"))
    @MemberAction(@Permission(module = "memberCards", operation = "unblock"))
    @PathToMember("owner")
    Card activateCard(Card card, String cardCode);

    /**
     * Block given card
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "block"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "block"))
    @MemberAction(@Permission(module = "memberCards", operation = "block"))
    @PathToMember("owner")
    Card blockCard(Card card);

    /**
     * The generated cards must be in status PENDING and without cardSecurityNumber (it will be set in the card activation action)
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "generate"))
    @IgnoreMember
    BulkMemberActionResultVO bulkGenerateNewCard(FullTextMemberQuery query, boolean generateForPending, boolean generateForActive);

    /**
     * Cancel given card
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "block"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "block"))
    @PathToMember("owner")
    Card cancelCard(Card card);

    /**
     * Change card code
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "changeCardSecurityCode"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "changeCardSecurityCode"))
    @MemberAction(@Permission(module = "memberCards", operation = "changeCardSecurityCode"))
    @PathToMember("owner")
    Card changeCardCode(Card card, String code);

    /**
     * If there is an active card associated with the member it will be canceled before the new card is created.
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "generate"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "generate"))
    @PathToMember("")
    Card generateNewCard(Member member);

    /**
     * Returns the current active card for the given user, or null if there's no active card
     */
    @AdminAction(@Permission(module = "adminMemberCards", operation = "view"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "view"))
    @MemberAction(@Permission(module = "memberCards", operation = "view"))
    @PathToMember("")
    Card getActiveCard(Member member);

    /**
     * Loads a Card based on it's number
     */
    @SystemAction
    Card loadByNumber(BigInteger number, Relationship... fetch);

    /**
     * Used by the schedule task
     */
    @SystemAction
    List<Card> processCards(Calendar time);

    /**
     * Search for Cards based on the given query
     */
    @AdminAction(@Permission(module = "adminMemberCards", operation = "view"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "view"))
    @MemberAction(@Permission(module = "memberCards", operation = "view"))
    @IgnoreMember
    List<Card> search(CardQuery query);

    /**
     * Unblock given card
     */

    @AdminAction(@Permission(module = "adminMemberCards", operation = "unblock"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "unblock"))
    @MemberAction(@Permission(module = "memberCards", operation = "unblock"))
    @PathToMember("owner")
    Card unblockCard(Card card);
}
