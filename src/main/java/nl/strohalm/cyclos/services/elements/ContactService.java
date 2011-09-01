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

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.members.Contact;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for contacts (members and/or brokers)
 * @author luis
 */
public interface ContactService extends Service {

    /**
     * Lists all contacts of the given member
     * @param owner The member
     * @return The contact list
     */
    @OperatorAction(@Permission(module = "operatorContacts", operation = "manage"))
    @MemberAction
    @PathToMember("")
    List<Contact> list(Member owner);

    /**
     * Load the specified contact, fetching the specified relationships
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorContacts", operation = "manage"))
    @PathToMember("owner")
    @RelatedEntity(Contact.class)
    Contact load(Long id, Relationship... fetch);

    /**
     * Removes the specified contacts
     * @param id List of contact ids to be removed
     * @return The number of contacts removed
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorContacts", operation = "manage"))
    @PathToMember("owner")
    @RelatedEntity(Contact.class)
    int remove(Long... id);

    /**
     * Saves the given contact
     * @param contact The given contact
     * @return Saved contact
     */
    @MemberAction
    @PathToMember("owner")
    @OperatorAction(@Permission(module = "operatorContacts", operation = "manage"))
    Contact save(Contact contact);

    /**
     * Validates the given contact. Both owner and contact are required and cannot be the same. Notes are not required, and it's max length depends on
     * the underlying database column
     */
    void validate(Contact contact) throws ValidationException;
}
