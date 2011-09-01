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
package nl.strohalm.cyclos.services.accounts.pos;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.pos.MemberPosDAO;
import nl.strohalm.cyclos.dao.accounts.pos.PosDAO;
import nl.strohalm.cyclos.dao.accounts.pos.PosLogDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.accounts.pos.PosLog;
import nl.strohalm.cyclos.entities.accounts.pos.PosQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.CurrentInvocationData;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

/**
 * POS Service Implementation
 * @author rodrigo
 */
public class PosServiceImpl implements PosService {

    private PosDAO            posDao;
    private PosLogDAO         posLogDao;
    private MemberPosDAO      memberPosDao;
    private PermissionService permissionService;
    private FetchService      fetchService;

    public Pos assignPos(final Member member, final Long posId) {
        final MemberPos memberPos = new MemberPos();
        final Pos pos = load(posId, RelationshipHelper.nested(Pos.Relationships.MEMBER_POS, MemberPos.Relationships.MEMBER));
        memberPos.setMember(member);
        memberPos.setStatus(MemberPos.Status.PENDING);
        memberPos.setDate(Calendar.getInstance());

        // Copy properties from member group settings
        final MemberGroupSettings mgs = new MemberGroupSettings();
        memberPos.setAllowMakePayment(mgs.isAllowMakePayment());
        memberPos.setMaxSchedulingPayments(mgs.getMaxSchedulingPayments());
        memberPos.setNumberOfCopies(mgs.getNumberOfCopies());
        memberPos.setResultPageSize(mgs.getResultPageSize());

        if (pos.getMemberPos() != null) {
            Member oldMember = pos.getMemberPos().getMember();
            if (oldMember != null) {
                if (LoggedUser.isAdministrator()) {
                    final AdminGroup admin = fetchService.fetch((AdminGroup) LoggedUser.group(), AdminGroup.Relationships.MANAGES_GROUPS);
                    if (!admin.getManagesGroups().contains(oldMember.getGroup())) {
                        throw new PermissionDeniedException();
                    }
                } else if (LoggedUser.isBroker()) {
                    oldMember = fetchService.fetch(oldMember, Member.Relationships.BROKER);
                    if (!LoggedUser.element().equals(oldMember.getBroker())) {
                        throw new PermissionDeniedException();
                    }
                } else {
                    throw new PermissionDeniedException();
                }
            } else if (!LoggedUser.isBroker() && !LoggedUser.isAdministrator()) {
                throw new PermissionDeniedException();
            }
        }

        // Associate pos to member pos and save the memberPos
        memberPos.setPos(pos);
        memberPosDao.insert(memberPos);

        // Inverse. Associates memberPos to pos and save the pos
        pos.setMemberPos(memberPos);
        pos.setStatus(Pos.Status.ASSIGNED);
        posDao.update(pos);

        // Generate log
        generateLog(pos);

        return pos;
    }

    public void deletePos(final Long... ids) {
        for (final Long id : ids) {
            final Pos pos = load(id, Pos.Relationships.MEMBER_POS);
            checkPermissions(pos, "manage");
        }

        posDao.delete(ids);
    }

    public Pos discardPos(final Long posId) {
        final Pos pos = load(posId, Pos.Relationships.MEMBER_POS);
        checkPermissions(pos, "discard");

        pos.setStatus(Pos.Status.DISCARDED);
        long memberPosId = 0;
        if (pos.getMemberPos() != null && pos.getMemberPos().getId() != null) {
            memberPosId = pos.getMemberPos().getId();
        }

        pos.setMemberPos(null);

        posDao.update(pos);
        if (memberPosId > 0L) {
            memberPosDao.delete(memberPosId);
        }
        generateLog(pos);

        return pos;
    }

    public void generateLog(final Pos pos) {
        final PosLog posLog = new PosLog();
        if (LoggedUser.isValid()) {
            posLog.setBy(LoggedUser.element());
        }
        posLog.setPos(pos);
        posLog.setDate(Calendar.getInstance());
        posLog.setPosStatus(pos.getStatus());
        if (pos.getMemberPos() != null) {
            posLog.setAssignedTo(pos.getMemberPos().getMember());
            posLog.setMemberPosStatus(pos.getMemberPos().getStatus());
        }

        posLogDao.insert(posLog);

    }

    public Pos load(final Long id, final Relationship... fetch) {
        final Pos pos = posDao.load(id, fetch);

        checkPermissions(pos, "view");
        return pos;
    }

    public Pos loadByPosId(final String posId, final Relationship... fetch) {
        final Pos pos = posDao.loadByPosId(posId, fetch);
        // this method is called from the PosWebService too, in that case we don't have a logged user
        if (!CurrentInvocationData.isSystemInvocation()) {
            checkPermissions(pos, "view");
        }
        return pos;
    }

    public Pos save(Pos pos) {
        checkPermissions(pos, "manage");

        validate(pos);

        if (pos.isTransient()) {
            pos.setStatus(Pos.Status.UNASSIGNED);
            pos = posDao.insert(pos, true);
        } else {
            pos = posDao.update(pos, true);
        }
        return pos;
    }

    public List<Pos> search(final PosQuery query) {
        if (LoggedUser.isAdministrator()) {
            query.setManagedBy((AdminGroup) LoggedUser.group());
        }
        return posDao.search(query);
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberPosDao(final MemberPosDAO memberPosDao) {
        this.memberPosDao = memberPosDao;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPosDao(final PosDAO posDao) {
        this.posDao = posDao;
    }

    public void setPosLogDao(final PosLogDAO posLogDao) {
        this.posLogDao = posLogDao;
    }

    public void unassignAllMemberPos(final Member member) {
        final List<Pos> userPos = posDao.getAllMemberPos(member);
        for (final Pos pos : userPos) {
            unassignPos(pos.getId());
        }
    }

    public Pos unassignPos(final Long posId) {
        final Pos pos = load(posId, Pos.Relationships.MEMBER_POS);
        pos.setStatus(Pos.Status.UNASSIGNED);
        final long memberPosId = pos.getMemberPos().getId();
        pos.setMemberPos(null);
        posDao.update(pos);
        memberPosDao.delete(memberPosId);
        generateLog(pos);
        return pos;
    }

    public void validate(final Pos pos) {
        getValidator(pos).validate(pos);
    }

    /**
     * Checks the permissions for admin and broker. If the POS is assigned to a member check the management relationship too.
     */
    private void checkPermissions(Pos pos, final String... permissions) {
        if (pos.isPersistent()) {
            pos = fetchService.fetch(pos, RelationshipHelper.nested(Pos.Relationships.MEMBER_POS, MemberPos.Relationships.MEMBER));
        }
        final PermissionRequestorImpl permissionRquestor = new PermissionRequestorImpl();
        permissionRquestor.adminPermissions("adminMemberPos", permissions);
        permissionRquestor.brokerPermissions("brokerPos", permissions);

        // also check related member
        if (pos.getMemberPos() != null) {
            permissionRquestor.member();
            permissionRquestor.manages(pos.getMemberPos().getMember());
        } else if (LoggedUser.isMember() && !LoggedUser.isBroker()) { // a member can't manage an unassigned POS
            throw new PermissionDeniedException();
        }

        if (!permissionService.checkPermissions(permissionRquestor)) {
            throw new PermissionDeniedException();
        }
    }

    private Validator getValidator(final Pos pos) {
        final Validator validator = new Validator("pos");
        final Property posId = validator.property("posId");
        posId.required().maxLength(64);
        if (pos.isTransient()) {
            if (pos.getPosId() != null) {
                try {
                    posDao.loadByPosId(pos.getPosId());
                    // If this point was reached, it means that there is a pos with the given posId
                    throw new ValidationException("pos.error.posIdExists", "");
                } catch (final EntityNotFoundException e) {
                    // Ok, no pos with the given posId
                }
            }
        }
        if (pos.getMemberPos() != null && pos.getMemberPos().getId() != null) {
            if ((LoggedUser.isBroker() && permissionService.checkPermission("brokerPos", "changeParameters")) || (LoggedUser.isAdministrator() && permissionService.checkPermission("adminMemberPos", "changeParameters")) || (!LoggedUser.isBroker() && LoggedUser.isMember())) {
                validator.property("memberPos.allowMakePayment").key("memberPos.allowMakePayment").required();
                validator.property("memberPos.maxSchedulingPayments").key("memberPos.maxSchedulingPayments").required();
                validator.property("memberPos.numberOfCopies").key("memberPos.numberOfCopies").required();
                validator.property("memberPos.resultPageSize").key("memberPos.resultPageSize").required();
            }
        }

        return validator;
    }
}
