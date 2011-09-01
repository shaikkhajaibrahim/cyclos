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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for members. This service is used to control all member operations that are directly related to the member like loans,
 * permissions and groups.
 * @author rafael
 * @author luis
 */
public interface MemberService extends Service {

    /**
     * Returns the number of active members on the system
     * @return number of active members
     */
    public int countActiveMembers();

    /**
     * Returns the number of active members with advertisements
     * @return number of active accounts with advertisements
     */
    public int countActiveMembersWithAds();

    /**
     * Returns the number of members by group The keys are the names of the groups The values are the number of members of the corresponding group
     * @return map containing the number of members by group
     */
    public Map<String, Integer> getGroupMemberCount();

    /**
     * Returns statistical data regarding the activities of the operator's member. This method is used when the user requests the "view reports"
     * feature of the system.
     */
    @OperatorAction(@Permission(module = "operatorReports", operation = "viewMember"))
    @IgnoreMember
    public ActivitiesVO getMemberActivitiesByOperator();

    /**
     * Returns statistical data regarding the activities of a member that is not the operator's member. This method is used when the user requests the
     * "view reports" feature of the system. The operation permission is not checked here but inside service's implementation because it is inherited
     * from the operator's member
     */
    @OperatorAction(@Permission(module = "memberReports", operation = "view"))
    @IgnoreMember
    public ActivitiesVO getOtherMemberActivitiesByOperator(Member member);

    /**
     * Returns the visible quick access items for the logged member or operator
     */
    @MemberAction
    @OperatorAction
    @IgnoreMember
    public QuickAccessVO getQuickAccess();

    /**
     * Returns the SMS status for the given member
     */
    public MemberSmsStatus getSmsStatus(Member member);

    /**
     * Returns the status for the logged member or operator
     */
    @MemberAction
    @OperatorAction
    @IgnoreMember
    public MemberStatusVO getStatus();

    /**
     * Returns whether the given member has value for the given field
     */
    public boolean hasValueForField(Member member, MemberCustomField field);

    /**
     * Iterates the members on the given groups, optionally ordering by name
     */
    public Iterator<Member> iterateByGroup(boolean ordered, MemberGroup... groups);

    /**
     * Iterates the members on the given groups with no expected order
     */
    public Iterator<Member> iterateByGroup(MemberGroup... groups);

    /**
     * Returns the members on the given groups
     */
    public List<Member> listByGroup(MemberGroup... groups);

    /**
     * Updates the given MemberSmsStatus
     */
    public MemberSmsStatus updateSmsStatus(MemberSmsStatus memberSmsStatus);

    /**
     * Returns the default account type for the given member
     */
    MemberAccountType getDefaultAccountType(Member member);

    /**
     * Returns statistical data regarding the activities of the specified member. This method is used when the user requests the "view reports"
     * feature of the system.
     * @param member The specified member
     * @return A view object with the statistical data
     */
    @AdminAction(@Permission(module = "adminMemberReports", operation = "view"))
    @BrokerAction(@Permission(module = "brokerReports", operation = "view"))
    @PathToMember("")
    ActivitiesVO getMemberActivities(Member member);

    /**
     * Returns statistical data regarding the activities of the specified member. This method is used when the user requests the "view reports"
     * feature of the system.
     * @param member The specified member
     * @return A view object with the statistical data
     */
    @MemberAction(@Permission(module = "memberReports", operation = "view"))
    @IgnoreMember
    ActivitiesVO getMemberActivitiesByMember(Member member);

    /**
     * Returns statistical data regarding the activities of the logged member. This method is used when the user requests the "view reports" feature
     * of the system.
     * @return A view object with the statistical data
     */
    @MemberAction
    @IgnoreMember
    ActivitiesVO getMyActivities();

}