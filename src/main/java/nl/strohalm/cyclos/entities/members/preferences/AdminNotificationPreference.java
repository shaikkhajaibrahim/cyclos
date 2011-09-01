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
package nl.strohalm.cyclos.entities.members.preferences;

import java.util.Collection;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;

/**
 * Notification Preference for an administrator
 * @author luis
 * @author Lucas Geiss
 */
public class AdminNotificationPreference extends Entity {

    public static enum Relationships implements Relationship {
        ADMIN("admin"), TRANSFER_TYPES("transferTypes"), MESSAGE_CATEGORIES("messageCategories"), SYSTEM_ALERTS("systemAlerts"), MEMBER_ALERTS("memberAlerts"), NEW_PENDING_PAYMENTS("newPendingPayments"), NEW_MEMBERS("newMembers");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long                             serialVersionUID = -4791497274620475610L;
    private boolean                                       applicationErrors;
    private boolean                                       systemInvoices;
    private Administrator                                 admin;
    private Collection<TransferType>                      transferTypes;
    private Collection<TransferType>                      newPendingPayments;
    private Collection<GuaranteeType>                     guaranteeTypes;
    private Collection<MessageCategory>                   messageCategories;
    private Collection<SystemAlertNotificationPreference> systemAlerts;
    private Collection<MemberAlertNotificationPreference> memberAlerts;
    private Collection<MemberGroup>                       newMembers;

    public Administrator getAdmin() {
        return admin;
    }

    public Collection<GuaranteeType> getGuaranteeTypes() {
        return guaranteeTypes;
    }

    public Collection<MemberAlertNotificationPreference> getMemberAlerts() {
        return memberAlerts;
    }

    public Collection<MessageCategory> getMessageCategories() {
        return messageCategories;
    }

    public Collection<MemberGroup> getNewMembers() {
        return newMembers;
    }

    public Collection<TransferType> getNewPendingPayments() {
        return newPendingPayments;
    }

    public Collection<SystemAlertNotificationPreference> getSystemAlerts() {
        return systemAlerts;
    }

    public Collection<TransferType> getTransferTypes() {
        return transferTypes;
    }

    public boolean isApplicationErrors() {
        return applicationErrors;
    }

    public boolean isSystemInvoices() {
        return systemInvoices;
    }

    public void setAdmin(final Administrator admin) {
        this.admin = admin;
    }

    public void setApplicationErrors(final boolean applicationErrors) {
        this.applicationErrors = applicationErrors;
    }

    public void setGuaranteeTypes(final Collection<GuaranteeType> guaranteeTypes) {
        this.guaranteeTypes = guaranteeTypes;
    }

    public void setMemberAlerts(final Collection<MemberAlertNotificationPreference> memberAlerts) {
        this.memberAlerts = memberAlerts;
    }

    public void setMessageCategories(final Collection<MessageCategory> categories) {
        messageCategories = categories;
    }

    public void setNewMembers(final Collection<MemberGroup> newMembers) {
        this.newMembers = newMembers;
    }

    public void setNewPendingPayments(final Collection<TransferType> newPendingPayments) {
        this.newPendingPayments = newPendingPayments;
    }

    public void setSystemAlerts(final Collection<SystemAlertNotificationPreference> systemAlerts) {
        this.systemAlerts = systemAlerts;
    }

    public void setSystemInvoices(final boolean systemInvoices) {
        this.systemInvoices = systemInvoices;
    }

    public void setTransferTypes(final Collection<TransferType> transferTypes) {
        this.transferTypes = transferTypes;
    }

    @Override
    public String toString() {
        return getId() + " - " + admin;
    }
}
