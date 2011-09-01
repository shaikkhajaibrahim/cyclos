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
package nl.strohalm.cyclos.setup;

import java.util.ArrayList;
import java.util.List;

import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;

/**
 * Defines all modules on the system
 * @author luis
 */
public class Permissions {

    /**
     * Returns a list with all modules, along with their operations
     */
    public static List<Module> all() {
        final List<Module> modules = new ArrayList<Module>();

        /* COMMON PERMISSIONS */
        buildModule(modules, Module.Type.BASIC, "basic", "login", "inviteMember", "quickAccess");

        /* ADMINISTRATOR PERMISSIONS */
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemCurrencies", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemAccounts", "manage", "view", "information", "authorizedInformation", "scheduledInformation");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemSettings", "manageLocal", "manageAlert", "manageAccess", "manageMail", "manageLog", "view", "file");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemCustomizedFiles", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemCustomImages", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemCustomFields", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemTranslation", "manage", "view", "file", "manageMailTranslation", "manageNotification");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemThemes", "select", "remove", "import", "export");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemPayments", "payment", "authorize", "cancel", "chargeback");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemAccountFees", "view", "charge");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemAdCategories", "manage", "view", "file");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemMessageCategories", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemAlerts", "manageMemberAlerts", "manageSystemAlerts", "viewMemberAlerts", "viewSystemAlerts");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemErrorLog", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemGroups", "manageMember", "manageBroker", "manageAdmin");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemRegistrationAgreements", "view", "manage");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemAdminGroups", "view", "manageAdminCustomizedFiles");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemGroupFilters", "manage", "view", "manageCustomizedFiles");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemLoanGroups", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemReports", "current", "memberList", "smsLogs", "statistics", "simulations"
                //
        );
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemTasks", "onlineState", "manageIndexes");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemStatus", "view", "viewConnectedAdmins", "viewConnectedBrokers", "viewConnectedMembers", "viewConnectedOperators");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemExternalAccounts", "manage", "view", "details", "processPayment", "checkPayment", "managePayment");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemMemberRecordTypes", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemServiceClients", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemChannels", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemGuaranteeTypes", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemCardTypes", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_SYSTEM, "systemInfoTexts", "manage", "view");

        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMembers", "view", "register", "managePending", "changeProfile", "changeName", "changeUsername", "remove", "changeGroup", "import");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberAccess", "changePassword", "resetPassword", "transactionPassword", "disconnect", "disconnectOperator", "enableLogin", "changePin", "unblockPin", "changeChannelsAccess");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberBrokerings", "changeBroker", "viewMembers", "viewLoans", "manageCommissions");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberAccounts", "information", "authorizedInformation", "scheduledInformation"
                //
                , "creditLimit");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberGroups", "view", "manageAccountSettings", "manageMemberCustomizedFiles");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberReports", "view", "showAccountInformation");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberPayments", "payment", "directPayment", "paymentWithDate", "paymentAsMemberToMember", "paymentAsMemberToSelf", "paymentAsMemberToSystem", "authorize", "cancelAuthorizedAsMember", "cancelScheduledAsMember", "blockScheduledAsMember", "chargeback");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberInvoices", "send", "directSend", "view", "accept", "cancel", "deny", "sendAsMemberToMember", "sendAsMemberToSystem", "acceptAsMemberFromMember", "acceptAsMemberFromSystem", "denyAsMember", "cancelAsMember");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberAds", "view", "manage", "import");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberReferences", "view", "manage");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberTransactionFeedbacks", "view", "manage");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberLoans", "view", "viewAuthorized", "grant", "grantWithDate", "discard", "repay", "repayWithDate", "manageExpiredStatus");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberLoanGroups", "manage", "view");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberMessages", "view", "sendToMember", "sendToGroup", "manage");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberDocuments", "details", "manageDynamic", "manageStatic", "manageMember");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberRecords", "view", "create", "modify", "delete");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberBulkActions", "changeGroup", "changeBroker", "generateCard");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberSms", "view");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberSmsMailings", "view", "freeSmsMailings", "paidSmsMailings");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberGuarantees", "viewPaymentObligations", "viewCertifications", "viewGuarantees", "registerGuarantees", "cancelCertificationsAsMember", "cancelGuaranteesAsMember", "acceptGuaranteesAsMember");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberCards", "view", "generate", "cancel", "block", "unblock", "changeCardSecurityCode", "unblockSecurityCode");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberPos", "view", "manage", "assign", "block", "discard", "unblockPin", "changePin", "changeParameters");
        buildModule(modules, Module.Type.ADMIN_MEMBER, "adminMemberPreferences", "manageNotifications");

        buildModule(modules, Module.Type.ADMIN_ADMIN, "adminAdmins", "view", "register", "changeProfile", "changeGroup", "remove");
        buildModule(modules, Module.Type.ADMIN_ADMIN, "adminAdminAccess", "changePassword", "transactionPassword", "disconnect", "enableLogin");
        buildModule(modules, Module.Type.ADMIN_ADMIN, "adminAdminRecords", "view", "create", "modify", "delete");

        /* MEMBER PERMISSIONS */
        buildModule(modules, Module.Type.MEMBER, "memberProfile", "view", "changeUsername", "changeName");
        buildModule(modules, Module.Type.MEMBER, "memberAccess", "unblockPin");
        buildModule(modules, Module.Type.MEMBER, "memberAccount", "authorizedInformation", "scheduledInformation"
                //
        );
        buildModule(modules, Module.Type.MEMBER, "memberPayments", "paymentToSelf", "paymentToMember", "directPaymentToMember", "paymentToSystem", "ticket", "authorize", "cancelAuthorized", "cancelScheduled", "blockScheduled", "request", "chargeback");
        buildModule(modules, Module.Type.MEMBER, "memberInvoices", "view", "sendToMember", "directSendToMember", "sendToSystem");
        buildModule(modules, Module.Type.MEMBER, "memberReferences", "view", "give");
        buildModule(modules, Module.Type.MEMBER, "memberDocuments", "view");
        buildModule(modules, Module.Type.MEMBER, "memberLoans", "view", "repay");
        buildModule(modules, Module.Type.MEMBER, "memberAds", "view", "publish");
        buildModule(modules, Module.Type.MEMBER, "memberPreferences", "manageNotifications", "manageAdInterests");
        buildModule(modules, Module.Type.MEMBER, "memberReports", "view", "showAccountInformation");
        buildModule(modules, Module.Type.MEMBER, "memberMessages", "view", "sendToMember", "sendToAdministration", "manage");
        buildModule(modules, Module.Type.MEMBER, "memberOperators", "manage");
        buildModule(modules, Module.Type.MEMBER, "memberCommissions", "view"); // this permission is only used to show/hide the menu
        buildModule(modules, Module.Type.MEMBER, "memberSms", "view");
        buildModule(modules, Module.Type.MEMBER, "memberGuarantees", "issueGuarantees", "issueCertifications", "buyWithPaymentObligations", "sellWithPaymentObligations");
        buildModule(modules, Module.Type.MEMBER, "memberCards", "view", "block", "unblock", "changeCardSecurityCode");

        /* BROKER PERMISSIONS */
        buildModule(modules, Module.Type.BROKER, "brokerMembers", "register", "managePending", "changeProfile", "changeName", "changeUsername", "manageDefaults", "manageContracts");
        buildModule(modules, Module.Type.BROKER, "brokerAccounts", "information", "authorizedInformation", "scheduledInformation"
                //
        );
        buildModule(modules, Module.Type.BROKER, "brokerReports", "view", "showAccountInformation");
        buildModule(modules, Module.Type.BROKER, "brokerAds", "view", "manage");
        buildModule(modules, Module.Type.BROKER, "brokerReferences", "manage");
        buildModule(modules, Module.Type.BROKER, "brokerInvoices", "view", "sendAsMemberToMember", "sendAsMemberToSystem", "acceptAsMemberFromMember", "acceptAsMemberFromSystem", "denyAsMember", "cancelAsMember");
        buildModule(modules, Module.Type.BROKER, "brokerLoans", "view");
        buildModule(modules, Module.Type.BROKER, "brokerLoanGroups", "view");
        buildModule(modules, Module.Type.BROKER, "brokerDocuments", "view", "viewMember", "manageMember");
        buildModule(modules, Module.Type.BROKER, "brokerMessages", "sendToMembers");
        buildModule(modules, Module.Type.BROKER, "brokerMemberAccess", "changePassword", "resetPassword", "transactionPassword", "changePin", "unblockPin", "changeChannelsAccess");
        buildModule(modules, Module.Type.BROKER, "brokerMemberPayments", "paymentAsMemberToMember", "paymentAsMemberToSelf", "paymentAsMemberToSystem", "authorize", "cancelAuthorizedAsMember", "cancelScheduledAsMember", "blockScheduledAsMember");
        buildModule(modules, Module.Type.BROKER, "brokerMemberRecords", "view", "create", "modify", "delete");
        buildModule(modules, Module.Type.BROKER, "brokerMemberSms", "view");
        buildModule(modules, Module.Type.BROKER, "brokerCards", "view", "generate", "cancel", "block", "unblock", "changeCardSecurityCode", "unblockSecurityCode");
        buildModule(modules, Module.Type.BROKER, "brokerPos", "view", "manage", "assign", "block", "discard", "unblockPin", "changePin", "changeParameters");
        buildModule(modules, Module.Type.BROKER, "brokerSmsMailings", "freeSmsMailings", "paidSmsMailings");
        buildModule(modules, Module.Type.BROKER, "brokerPreferences", "manageNotifications");

        /* OPERATOR PERMISSIONS */
        buildModule(modules, Module.Type.OPERATOR, "operatorAccount", "authorizedInformation", "scheduledInformation", "accountInformation"
                //
        );
        buildModule(modules, Module.Type.OPERATOR, "operatorPayments", "paymentToSelf", "paymentToMember", "directPaymentToMember", "paymentToSystem", "externalMakePayment", "externalReceivePayment", "authorize", "cancelAuthorized", "cancelScheduled", "blockScheduled", "request");
        buildModule(modules, Module.Type.OPERATOR, "operatorInvoices", "view", "sendToMember", "directSendToMember", "sendToSystem", "manage");
        buildModule(modules, Module.Type.OPERATOR, "operatorReferences", "view", "manageMemberReferences", "manageMemberTransactionFeedbacks");
        buildModule(modules, Module.Type.OPERATOR, "operatorLoans", "view", "repay");
        buildModule(modules, Module.Type.OPERATOR, "operatorAds", "publish");
        buildModule(modules, Module.Type.OPERATOR, "operatorReports", "viewMember");
        buildModule(modules, Module.Type.OPERATOR, "operatorContacts", "manage", "view");
        buildModule(modules, Module.Type.OPERATOR, "operatorGuarantees", "issueGuarantees", "issueCertifications", "buyWithPaymentObligations", "sellWithPaymentObligations");
        buildModule(modules, Module.Type.OPERATOR, "operatorMessages", "view", "sendToMember", "sendToAdministration", "manage");

        return modules;
    }

    private static Module buildModule(final List<Module> modules, final Module.Type type, final String name, final String... operations) {
        final Module module = new Module();
        module.setType(type);
        module.setName(name);
        module.setMessageKey("permission." + name);
        modules.add(module);
        module.setOperations(new ArrayList<Operation>());
        if (operations != null && operations.length > 0) {
            for (final String opName : operations) {
                final Operation operation = new Operation();
                operation.setModule(module);
                operation.setName(opName);
                operation.setMessageKey("permission." + name + "." + opName);
                module.getOperations().add(operation);
            }
        }
        return module;
    }

}