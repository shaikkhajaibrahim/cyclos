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

import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.dao.groups.GroupDAO;
import nl.strohalm.cyclos.dao.members.MemberRecordTypeDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.entities.members.records.MemberRecordTypeQuery;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

/**
 * Implementation class for member record types service
 * @author Jefferson Magno
 */
public class MemberRecordTypeServiceImpl implements MemberRecordTypeService {

    private FetchDAO            fetchDao;
    private GroupDAO            groupDao;
    private MemberRecordTypeDAO memberRecordTypeDao;

    public List<MemberRecordType> list() {
        return memberRecordTypeDao.search(new MemberRecordTypeQuery());
    }

    public MemberRecordType load(final Long id, final Relationship... fetch) {
        return memberRecordTypeDao.load(id, fetch);
    }

    public int remove(final Long... ids) throws PermissionDeniedException {
        for (final Long id : ids) {
            final MemberRecordType memberRecordType = load(id, MemberRecordType.Relationships.VIEWABLE_BY_ADMIN_GROUPS, MemberRecordType.Relationships.VIEWABLE_BY_BROKER_GROUPS);
            removeFromViewerCollections(memberRecordType);
        }
        return memberRecordTypeDao.delete(ids);
    }

    public MemberRecordType save(MemberRecordType memberRecordType) throws PermissionDeniedException {
        if (memberRecordType.isTransient()) {
            memberRecordType = memberRecordTypeDao.insert(memberRecordType);

            // Grant permissions to the admin group
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchDao.fetch(adminGroup, AdminGroup.Relationships.VIEW_MEMBER_RECORD_TYPES, AdminGroup.Relationships.VIEW_ADMIN_RECORD_TYPES);

            // Grant view and create permissions
            adminGroup.getViewMemberRecordTypes().add(memberRecordType);
            adminGroup.getCreateMemberRecordTypes().add(memberRecordType);

            // If the member record type is editable, grant modify and delete permissions
            if (memberRecordType.isEditable()) {
                adminGroup.getModifyMemberRecordTypes().add(memberRecordType);
                adminGroup.getDeleteMemberRecordTypes().add(memberRecordType);
            }
            groupDao.update(adminGroup);

            return memberRecordType;
        } else {
            return memberRecordTypeDao.update(memberRecordType);
        }
    }

    public List<MemberRecordType> search(final MemberRecordTypeQuery query) {
        return memberRecordTypeDao.search(query);
    }

    public void setFetchDao(final FetchDAO fetchDao) {
        this.fetchDao = fetchDao;
    }

    public void setGroupDao(final GroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    public void setMemberRecordTypeDao(final MemberRecordTypeDAO memberRecordTypeDao) {
        this.memberRecordTypeDao = memberRecordTypeDao;
    }

    public void validate(final MemberRecordType memberRecordType) throws ValidationException {
        getValidator().validate(memberRecordType);
    }

    private Validator getValidator() {
        final Validator validator = new Validator("memberRecordType");
        validator.property("name").required();
        validator.property("label").required();
        validator.property("layout").required();
        validator.property("editable").required();
        validator.property("showMenuItem").required();
        return validator;
    }

    private void removeFromViewerCollections(final MemberRecordType memberRecordType) {
        final Collection<AdminGroup> viewableByAdminGroups = memberRecordType.getViewableByAdminGroups();
        for (AdminGroup adminGroup : viewableByAdminGroups) {
            adminGroup = fetchDao.fetch(adminGroup, AdminGroup.Relationships.VIEW_ADMIN_RECORD_TYPES, AdminGroup.Relationships.VIEW_MEMBER_RECORD_TYPES);
            adminGroup.getViewAdminRecordTypes().remove(memberRecordType);
            adminGroup.getViewMemberRecordTypes().remove(memberRecordType);
        }

        final Collection<BrokerGroup> viewableByBrokerGroups = memberRecordType.getViewableByBrokerGroups();
        for (BrokerGroup brokerGroup : viewableByBrokerGroups) {
            brokerGroup = fetchDao.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_MEMBER_RECORD_TYPES);
            brokerGroup.getBrokerMemberRecordTypes().remove(memberRecordType);
        }
    }

}