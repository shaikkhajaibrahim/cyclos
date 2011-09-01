/*
   This file is part of Cyclos.

   Cyclos is free software; you can redistribute it and/or modify
   it under the terms of the GNU General License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Cyclos is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General License for more details.

   You should have received a copy of the GNU General License
   along with Cyclos; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.entities.members;

import java.util.Calendar;
import java.util.Collection;

import nl.strohalm.cyclos.entities.EntityWithCustomFields;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.MemberGroup;

/**
 * Representation for a member which have been registered in Cyclos
 * 
 * @author luis
 */
public interface RegisteredMember extends EntityWithCustomFields<MemberCustomField, MemberCustomFieldValue> {

    Member getBroker();

    Calendar getCreationDate();

    Collection<MemberCustomFieldValue> getCustomValues();

    String getEmail();

    MemberGroup getMemberGroup();

    String getName();

    String getUsername();

    boolean isHideEmail();

}
