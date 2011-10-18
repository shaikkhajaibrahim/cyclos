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
package nl.strohalm.cyclos.services.groups;

import java.util.Collection;

import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;

/**
 * Permissions for an admin group
 * @author luis
 */
public class AdminGroupPermissionsDTO extends GroupPermissionsDTO<AdminGroup> {

    private static final long             serialVersionUID = -7880633748381076158L;
    private Collection<TransferType>      grantLoanTTs;
    private Collection<TransferType>      systemChargebackTTs;
    private Collection<TransferType>      memberChargebackTTs;
    private Collection<TransferType>      asMemberToMemberTTs;
    private Collection<TransferType>      asMemberToSelfTTs;
    private Collection<TransferType>      asMemberToSystemTTs;
    private Collection<TransferType>      systemToMemberTTs;
    private Collection<TransferType>      systemToSystemTTs;
    private Collection<MemberGroup>       managesGroups;
    private Collection<AdminGroup>        managesAdminGroups;
    private Collection<SystemAccountType> viewInformationOf;
    private Collection<AdminGroup>        viewConnectedAdminsOf;
    private Collection<MessageCategory>   messageCategories;
    private Collection<MemberRecordType>  viewAdminRecordTypes;
    private Collection<MemberRecordType>  createAdminRecordTypes;
    private Collection<MemberRecordType>  modifyAdminRecordTypes;
    private Collection<MemberRecordType>  deleteAdminRecordTypes;
    private Collection<MemberRecordType>  viewMemberRecordTypes;
    private Collection<MemberRecordType>  createMemberRecordTypes;
    private Collection<MemberRecordType>  modifyMemberRecordTypes;
    private Collection<MemberRecordType>  deleteMemberRecordTypes;

    public Collection<TransferType> getAsMemberToMemberTTs() {
        return asMemberToMemberTTs;
    }

    public Collection<TransferType> getAsMemberToSelfTTs() {
        return asMemberToSelfTTs;
    }

    public Collection<TransferType> getAsMemberToSystemTTs() {
        return asMemberToSystemTTs;
    }

    public Collection<MemberRecordType> getCreateAdminRecordTypes() {
        return createAdminRecordTypes;
    }

    public Collection<MemberRecordType> getCreateMemberRecordTypes() {
        return createMemberRecordTypes;
    }

    public Collection<MemberRecordType> getDeleteAdminRecordTypes() {
        return deleteAdminRecordTypes;
    }

    public Collection<MemberRecordType> getDeleteMemberRecordTypes() {
        return deleteMemberRecordTypes;
    }

    public Collection<TransferType> getGrantLoanTTs() {
        return grantLoanTTs;
    }

    public Collection<MemberGroup> getManagesGroups() {
        return managesGroups;
    }

    public Collection<TransferType> getMemberChargebackTTs() {
        return memberChargebackTTs;
    }

    @Override
    public Collection<MessageCategory> getMessageCategories() {
        return messageCategories;
    }

    public Collection<MemberRecordType> getModifyAdminRecordTypes() {
        return modifyAdminRecordTypes;
    }

    public Collection<MemberRecordType> getModifyMemberRecordTypes() {
        return modifyMemberRecordTypes;
    }

    public Collection<TransferType> getSystemChargebackTTs() {
        return systemChargebackTTs;
    }

    public Collection<TransferType> getSystemToMemberTTs() {
        return systemToMemberTTs;
    }

    public Collection<TransferType> getSystemToSystemTTs() {
        return systemToSystemTTs;
    }

    public Collection<MemberRecordType> getViewAdminRecordTypes() {
        return viewAdminRecordTypes;
    }

    public Collection<AdminGroup> getViewConnectedAdminsOf() {
        return viewConnectedAdminsOf;
    }

    public Collection<SystemAccountType> getViewInformationOf() {
        return viewInformationOf;
    }

    public Collection<MemberRecordType> getViewMemberRecordTypes() {
        return viewMemberRecordTypes;
    }

    public void setAsMemberToMemberTTs(final Collection<TransferType> asMemberToMemberTTs) {
        this.asMemberToMemberTTs = asMemberToMemberTTs;
    }

    public void setAsMemberToSelfTTs(final Collection<TransferType> asMemberToSelfTTs) {
        this.asMemberToSelfTTs = asMemberToSelfTTs;
    }

    public void setAsMemberToSystemTTs(final Collection<TransferType> asMemberToSystemTTs) {
        this.asMemberToSystemTTs = asMemberToSystemTTs;
    }

    public void setCreateAdminRecordTypes(final Collection<MemberRecordType> createAdminRecordTypes) {
        this.createAdminRecordTypes = createAdminRecordTypes;
    }

    public void setCreateMemberRecordTypes(final Collection<MemberRecordType> createMemberRecordTypes) {
        this.createMemberRecordTypes = createMemberRecordTypes;
    }

    public void setDeleteAdminRecordTypes(final Collection<MemberRecordType> deleteAdminRecordTypes) {
        this.deleteAdminRecordTypes = deleteAdminRecordTypes;
    }

    public void setDeleteMemberRecordTypes(final Collection<MemberRecordType> deleteMemberRecordTypes) {
        this.deleteMemberRecordTypes = deleteMemberRecordTypes;
    }

    public void setGrantLoanTTs(final Collection<TransferType> grantLoanTTs) {
        this.grantLoanTTs = grantLoanTTs;
    }

    public void setManagesGroups(final Collection<MemberGroup> managesGroups) {
        this.managesGroups = managesGroups;
    }

    public void setMemberChargebackTTs(final Collection<TransferType> memberChargebackTTs) {
        this.memberChargebackTTs = memberChargebackTTs;
    }

    @Override
    public void setMessageCategories(final Collection<MessageCategory> messageCategories) {
        this.messageCategories = messageCategories;
    }

    public void setModifyAdminRecordTypes(final Collection<MemberRecordType> modifyAdminRecordTypes) {
        this.modifyAdminRecordTypes = modifyAdminRecordTypes;
    }

    public void setModifyMemberRecordTypes(final Collection<MemberRecordType> modifyMemberRecordTypes) {
        this.modifyMemberRecordTypes = modifyMemberRecordTypes;
    }

    public void setSystemChargebackTTs(final Collection<TransferType> systemChargebackTTs) {
        this.systemChargebackTTs = systemChargebackTTs;
    }

    public void setSystemToMemberTTs(final Collection<TransferType> systemToMemberTTs) {
        this.systemToMemberTTs = systemToMemberTTs;
    }

    public void setSystemToSystemTTs(final Collection<TransferType> systemToSystemTTs) {
        this.systemToSystemTTs = systemToSystemTTs;
    }

    public void setViewAdminRecordTypes(final Collection<MemberRecordType> viewAdminRecordTypes) {
        this.viewAdminRecordTypes = viewAdminRecordTypes;
    }

    public void setViewConnectedAdminsOf(final Collection<AdminGroup> viewConnectedAdminsOf) {
        this.viewConnectedAdminsOf = viewConnectedAdminsOf;
    }

    public void setViewInformationOf(final Collection<SystemAccountType> viewInformationOf) {
        this.viewInformationOf = viewInformationOf;
    }

    public void setViewMemberRecordTypes(final Collection<MemberRecordType> viewMemberRecordTypes) {
        this.viewMemberRecordTypes = viewMemberRecordTypes;
    }

    public Collection<AdminGroup> getManagesAdminGroups() {
        return managesAdminGroups;
    }

    public void setManagesAdminGroups(Collection<AdminGroup> managesAdminGroups) {
        this.managesAdminGroups = managesAdminGroups;
    }
}