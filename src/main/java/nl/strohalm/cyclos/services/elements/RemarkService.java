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

import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.remarks.BrokerRemark;
import nl.strohalm.cyclos.entities.members.remarks.GroupRemark;
import nl.strohalm.cyclos.entities.members.remarks.Remark;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for broker and group remarks
 * @author luis
 */
public interface RemarkService extends Service {

    /**
     * Lists all broker remarks for a given element
     * @param subject The element that acts as the subject of the remark
     * @return The list of remarks
     */
    @AdminAction(@Permission(module = "adminMemberBrokerings", operation = "changeBroker"))
    @PathToMember("")
    List<BrokerRemark> listBrokerRemarksFor(Element subject);

    /**
     * Lists all group remarks for a given element
     * @param subject The element that acts as the subject of the remark
     * @return The list of remarks
     */
    @DontEnforcePermission(traceable = true, value = "The implementation must carry out with the permission controls becasue it depends on the subject type")
    List<GroupRemark> listGroupRemarksFor(Element subject);

    /**
     * Saves the specified group remark
     */
    @SystemAction
    GroupRemark save(GroupRemark remark);

    /**
     * Validates the specified remark
     * @param remark The remark to be validated
     * @throws ValidationException if validation fails
     */
    @SystemAction
    void validate(Remark remark) throws ValidationException;

}