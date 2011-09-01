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
import java.util.Map;

import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.records.FullTextMemberRecordQuery;
import nl.strohalm.cyclos.entities.members.records.MemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecordQuery;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for member records
 * @author Jefferson Magno
 */
@RelatedEntity(MemberRecord.class)
@PathToMember("element")
public interface MemberRecordService extends Service {

    /**
     * Adds to index entities which have not yet been indexed
     */
    void addMissingEntitiesToIndex();

    /**
     * Returns a map keyed by the record types related to the given element and that the logged user has access, with the current number of records as
     * it's values
     */
    Map<MemberRecordType, Integer> countByType(Element element);

    /**
     * Searches for member records with a full-text search
     */
    List<MemberRecord> fullTextSearch(FullTextMemberRecordQuery query);

    /**
     * Returns the index status
     */
    IndexStatus getIndexStatus();

    /**
     * Inserts the given member record
     * @param memberRecord member record to be saved
     * @return The member record saved
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminMemberRecords", operation = "create"))
    @BrokerAction(@Permission(module = "brokerMemberRecords", operation = "create"))
    MemberRecord insert(MemberRecord memberRecord) throws PermissionDeniedException;

    /**
     * Inserts the given admin record
     * @param adminRecord member record to be saved
     * @return The admin record saved
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminAdminRecords", operation = "create"))
    @IgnoreMember
    MemberRecord insertAdminRecord(MemberRecord adminRecord) throws PermissionDeniedException;

    /**
     * Loads a member record fetching the specified relationships
     * @param id Id of the record to be loaded
     * @param fetch array of relationships to be fetched
     * @return The loaded reference
     */
    MemberRecord load(Long id, Relationship... fetch);

    /**
     * Optimizes the full-text indexes
     */
    void optimizeIndex();

    /**
     * Rebuilds the index for full text searches
     */
    void rebuildIndex();

    /**
     * Removes the specified member records
     * @return The number of member records removed
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminMemberRecords", operation = "delete"))
    @BrokerAction(@Permission(module = "brokerMemberRecords", operation = "delete"))
    int remove(Long... id) throws PermissionDeniedException;

    /**
     * Removes the specified admin records
     * @return The number of admin records removed
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminAdminRecords", operation = "delete"))
    int removeAdminRecords(Long... id) throws PermissionDeniedException;

    /**
     * Searches for member records
     */
    List<MemberRecord> search(MemberRecordQuery query);

    /**
     * Updates the given member record
     * @param memberRecord member record to be updated
     * @return The member record updated
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminMemberRecords", operation = "modify"))
    @BrokerAction(@Permission(module = "brokerMemberRecords", operation = "modify"))
    MemberRecord update(MemberRecord memberRecord) throws PermissionDeniedException;

    /**
     * Updates the given admin record
     * @param adminRecord admin record to be updated
     * @return The admin record saved
     * @throws PermissionDeniedException
     */
    @AdminAction(@Permission(module = "adminAdminRecords", operation = "modify"))
    @IgnoreMember
    MemberRecord updateAdminRecord(MemberRecord adminRecord) throws PermissionDeniedException;

    /**
     * Validates the specified member record
     * @param memberRecord member record to be validated
     * @throws ValidationException if validation fails
     */
    void validate(MemberRecord memberRecord) throws ValidationException;

}