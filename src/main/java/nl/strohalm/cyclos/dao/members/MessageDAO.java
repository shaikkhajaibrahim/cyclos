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
package nl.strohalm.cyclos.dao.members;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.DeletableDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.UpdatableDAO;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;

/**
 * DAO interface for messages
 * @author luis
 */
public interface MessageDAO extends BaseDAO<Message>, InsertableDAO<Message>, UpdatableDAO<Message>, DeletableDAO<Message> {

    /**
     * Assign the members which have the given member as broker as pending to send
     */
    public void assignPendingToSendByBroker(Message message, Member broker);

    /**
     * Assign the members on any of the given groups as pending to send
     */
    public void assignPendingToSendByGroups(Message message, Collection<MemberGroup> groups);

    /**
     * Returns a member in the pending to send of the given message, or null if no members are left
     */
    public Member getNextMemberToSend(Message message);

    /**
     * Lists the message ids which haven't finished sending yet
     */
    public List<Long> listUnfinishedIds();

    /**
     * Removes the given member from the pendings list of the given message
     */
    public void markAsSent(Message message, Member member);

    /**
     * Removes all members pending to send from this message
     */
    public void removeAllPending(Message message);

    /**
     * Removes all messages on trash before the given date
     */
    public void removeMessagesOnTrashBefore(Calendar limit);

    /**
     * Searches for messages, ordering results by date descending
     */
    public List<Message> search(MessageQuery query);
}
