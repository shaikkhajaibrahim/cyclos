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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.members.MemberRecordDAO;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.records.FullTextMemberRecordQuery;
import nl.strohalm.cyclos.entities.members.records.MemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecordQuery;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.entities.members.records.MemberRecordTypeQuery;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

/**
 * Implementation class for member records service
 * @author Jefferson Magno
 */
public class MemberRecordServiceImpl implements MemberRecordService {

    private CustomFieldService      customFieldService;
    private FetchService            fetchService;
    private MemberRecordTypeService memberRecordTypeService;
    private MemberRecordDAO         memberRecordDao;

    private PermissionService       permissionService;

    public void addMissingEntitiesToIndex() {
        memberRecordDao.addMissingEntitiesToIndex();
    }

    public Map<MemberRecordType, Integer> countByType(final Element element) {
        // Find the types related to the given element and that the logged user has access
        final MemberRecordTypeQuery typeQuery = new MemberRecordTypeQuery();
        typeQuery.setGroup(element.getGroup());
        if (LoggedUser.isValid()) {
            final Group loggedGroup = LoggedUser.group();
            if (loggedGroup instanceof AdminGroup) {
                typeQuery.setViewableByAdminGroup((AdminGroup) loggedGroup);
            } else if (loggedGroup instanceof BrokerGroup) {
                typeQuery.setViewableByBrokerGroup((BrokerGroup) loggedGroup);
            } else {
                throw new PermissionDeniedException();
            }
        }
        // For each type, count the records for the given element
        final List<MemberRecordType> types = memberRecordTypeService.search(typeQuery);
        final Map<MemberRecordType, Integer> map = new LinkedHashMap<MemberRecordType, Integer>();
        for (final MemberRecordType type : types) {
            final MemberRecordQuery query = new MemberRecordQuery();
            query.setElement(element);
            query.setType(type);
            query.setPageForCount();
            final int count = PageHelper.getTotalCount(search(query));
            map.put(type, count);
        }
        return map;
    }

    public List<MemberRecord> fullTextSearch(final FullTextMemberRecordQuery query) {
        final MemberRecordType type = query.getType();
        if (type == null) {
            return Collections.emptyList();
        }
        if (LoggedUser.isAdministrator()) {
            // Check if the administrator´s group has permission to view user records of the given type
            AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.VIEW_MEMBER_RECORD_TYPES, AdminGroup.Relationships.VIEW_ADMIN_RECORD_TYPES, AdminGroup.Relationships.MANAGES_GROUPS);
            if (!adminGroup.getViewMemberRecordTypes().contains(type) && !adminGroup.getViewAdminRecordTypes().contains(type)) {
                return Collections.emptyList();
            }
            // Administrators only can view user records of managed users
            final List<Group> groups = new ArrayList<Group>();
            groups.addAll(adminGroup.getManagesGroups());
            query.setGroups(groups);
        } else if (LoggedUser.isBroker()) {
            // Check if the broker´s group has permission to view user records of the given type
            final Member broker = LoggedUser.element();
            BrokerGroup brokerGroup = (BrokerGroup) LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_MEMBER_RECORD_TYPES);
            if (!brokerGroup.getMemberRecordTypes().contains(type)) {
                return Collections.emptyList();
            }
            // A broker only can view user records of his brokered members
            query.setBroker(broker);
        }
        return memberRecordDao.fullTextSearch(query);
    }

    public IndexStatus getIndexStatus() {
        return memberRecordDao.getIndexStatus(MemberRecord.class);
    }

    public MemberRecord insert(final MemberRecord memberRecord) throws PermissionDeniedException {
        final MemberRecordType type = fetchService.fetch(memberRecord.getType());
        if (LoggedUser.isAdministrator()) {
            AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.CREATE_MEMBER_RECORD_TYPES);
            if (!adminGroup.getCreateMemberRecordTypes().contains(type)) {
                throw new PermissionDeniedException();
            }
        } else {
            BrokerGroup brokerGroup = (BrokerGroup) LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_CREATE_MEMBER_RECORD_TYPES);
            if (!brokerGroup.getBrokerCreateMemberRecordTypes().contains(type)) {
                throw new PermissionDeniedException();
            }
        }
        return doSave(memberRecord);
    }

    public MemberRecord insertAdminRecord(final MemberRecord adminRecord) throws PermissionDeniedException {
        final MemberRecordType type = fetchService.fetch(adminRecord.getType());
        AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.CREATE_ADMIN_RECORD_TYPES);
        if (!adminGroup.getCreateAdminRecordTypes().contains(type)) {
            throw new PermissionDeniedException();
        }
        return doSave(adminRecord);
    }

    public MemberRecord load(final Long id, final Relationship... fetch) {
        return memberRecordDao.load(id, fetch);
    }

    public void optimizeIndex() {
        memberRecordDao.optimizeIndex(MemberRecord.class);
    }

    public void rebuildIndex() {
        memberRecordDao.rebuildIndex(MemberRecord.class);
    }

    public int remove(final Long... ids) throws PermissionDeniedException {
        if (LoggedUser.isAdministrator()) {
            AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.DELETE_MEMBER_RECORD_TYPES);
            for (final Long id : ids) {
                final MemberRecord memberRecord = load(id, MemberRecord.Relationships.TYPE);
                final MemberRecordType type = memberRecord.getType();
                if (!adminGroup.getDeleteMemberRecordTypes().contains(type)) {
                    throw new PermissionDeniedException();
                }
            }
        } else {
            BrokerGroup brokerGroup = (BrokerGroup) LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_DELETE_MEMBER_RECORD_TYPES);
            for (final Long id : ids) {
                final MemberRecord memberRecord = load(id, MemberRecord.Relationships.TYPE);
                final MemberRecordType type = memberRecord.getType();
                if (!brokerGroup.getBrokerDeleteMemberRecordTypes().contains(type)) {
                    throw new PermissionDeniedException();
                }
            }
        }
        final int count = memberRecordDao.delete(ids);
        memberRecordDao.removeFromIndex(MemberRecord.class, ids);
        return count;
    }

    public int removeAdminRecords(final Long... ids) throws PermissionDeniedException {
        AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.DELETE_ADMIN_RECORD_TYPES);
        for (final Long id : ids) {
            final MemberRecord memberRecord = load(id, MemberRecord.Relationships.TYPE);
            final MemberRecordType type = memberRecord.getType();
            if (!adminGroup.getDeleteAdminRecordTypes().contains(type)) {
                throw new PermissionDeniedException();
            }
        }
        final int count = memberRecordDao.delete(ids);
        memberRecordDao.removeFromIndex(MemberRecord.class, ids);
        return count;
    }

    public List<MemberRecord> search(final MemberRecordQuery query) {
        final MemberRecordType type = query.getType();
        if (type == null) {
            return Collections.emptyList();
        }
        final Element element = fetchService.fetch(query.getElement(), Element.Relationships.GROUP);
        if (LoggedUser.isAdministrator()) {
            // Check if the administrator´s group has permission to view user records of the given type
            AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.VIEW_MEMBER_RECORD_TYPES, AdminGroup.Relationships.VIEW_ADMIN_RECORD_TYPES, AdminGroup.Relationships.MANAGES_GROUPS);
            if (!adminGroup.getViewMemberRecordTypes().contains(type) && !adminGroup.getViewAdminRecordTypes().contains(type)) {
                return Collections.emptyList();
            }
            if (element == null) {
                // Administrators only can view user records of managed users
                final List<Group> groups = new ArrayList<Group>();
                groups.addAll(adminGroup.getManagesGroups());
                query.setGroups(groups);
            } else {
                // Ensure the admin permissions over the specific element
                final PermissionRequestorImpl permissions = new PermissionRequestorImpl();
                if (element instanceof Administrator) {
                    // For admins, there's just a global permission
                    permissions.adminPermissions("adminAdminRecords", "view");
                } else {
                    // For members, the admin must manage
                    final Member member = (Member) element;
                    permissions.manages(member);
                }
                permissionService.checkPermissions(permissions);
            }

        } else if (LoggedUser.isBroker()) {
            // Check if the broker´s group has permission to view user records of the given type
            final Member broker = LoggedUser.element();
            BrokerGroup brokerGroup = (BrokerGroup) LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_MEMBER_RECORD_TYPES);
            if (!brokerGroup.getBrokerMemberRecordTypes().contains(type)) {
                return Collections.emptyList();
            }
            // A broker only can view user records of his brokered members
            query.setBroker(broker);
        }
        return memberRecordDao.search(query);
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberRecordDao(final MemberRecordDAO memberRecordDao) {
        this.memberRecordDao = memberRecordDao;
    }

    public void setMemberRecordTypeService(final MemberRecordTypeService memberRecordTypeService) {
        this.memberRecordTypeService = memberRecordTypeService;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public MemberRecord update(final MemberRecord memberRecord) throws PermissionDeniedException {
        final MemberRecordType type = fetchService.fetch(memberRecord.getType());
        if (LoggedUser.isAdministrator()) {
            AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MODIFY_MEMBER_RECORD_TYPES);
            if (!adminGroup.getModifyMemberRecordTypes().contains(type)) {
                throw new PermissionDeniedException();
            }
        } else {
            BrokerGroup brokerGroup = (BrokerGroup) LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.BROKER_MODIFY_MEMBER_RECORD_TYPES);
            if (!brokerGroup.getBrokerModifyMemberRecordTypes().contains(type)) {
                throw new PermissionDeniedException();
            }
        }
        return doSave(memberRecord);
    }

    public MemberRecord updateAdminRecord(final MemberRecord adminRecord) throws PermissionDeniedException {
        final MemberRecordType type = fetchService.fetch(adminRecord.getType());
        AdminGroup adminGroup = (AdminGroup) LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MODIFY_ADMIN_RECORD_TYPES);
        if (!adminGroup.getModifyAdminRecordTypes().contains(type)) {
            throw new PermissionDeniedException();
        }
        return doSave(adminRecord);
    }

    public void validate(final MemberRecord memberRecord) throws ValidationException {
        getValidator().validate(memberRecord);
    }

    private MemberRecord doSave(MemberRecord memberRecord) {
        final Element by = LoggedUser.element();
        final Calendar now = Calendar.getInstance();
        final Collection<MemberRecordCustomFieldValue> customValues = memberRecord.getCustomValues();
        memberRecord.setCustomValues(null);
        if (memberRecord.isTransient()) {
            memberRecord.setDate(now);
            memberRecord.setBy(by);
            memberRecord = memberRecordDao.insert(memberRecord);
        } else {
            // Preserve the original author and date
            final MemberRecord current = load(memberRecord.getId());
            memberRecord.setBy(current.getBy());
            memberRecord.setDate(current.getDate());

            memberRecord.setModifiedBy(by);
            memberRecord.setLastModified(now);
            memberRecord = memberRecordDao.update(memberRecord);
        }
        memberRecord.setCustomValues(customValues);
        customFieldService.saveMemberRecordValues(memberRecord);

        memberRecordDao.addToIndex(memberRecord);

        return memberRecord;
    }

    private Validator getValidator() {
        final Validator validator = new Validator("memberRemark");
        validator.property("type").required();
        validator.property("member").required();
        validator.property("title").required();
        return validator;
    }

}