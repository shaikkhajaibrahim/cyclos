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

import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;

/**
 * Class used to store a member group's permissions
 * @author luis
 */
public class BrokerGroupPermissionsDTO extends MemberGroupPermissionsDTO<BrokerGroup> {

    private static final long            serialVersionUID = 8264674596578541889L;
    private Collection<Document>         brokerDocuments;
    private Collection<TransferType>     asMemberToMemberTTs;
    private Collection<TransferType>     asMemberToSelfTTs;
    private Collection<TransferType>     asMemberToSystemTTs;
    private Collection<TransferType>     brokerConversionSimulationTTs;
    private Collection<AccountType>      brokerCanViewInformationOf;
    private Collection<MemberRecordType> brokerMemberRecordTypes;
    private Collection<MemberRecordType> brokerCreateMemberRecordTypes;
    private Collection<MemberRecordType> brokerModifyMemberRecordTypes;
    private Collection<MemberRecordType> brokerDeleteMemberRecordTypes;

    public Collection<TransferType> getAsMemberToMemberTTs() {
        return asMemberToMemberTTs;
    }

    public Collection<TransferType> getAsMemberToSelfTTs() {
        return asMemberToSelfTTs;
    }

    public Collection<TransferType> getAsMemberToSystemTTs() {
        return asMemberToSystemTTs;
    }

    public Collection<AccountType> getBrokerCanViewInformationOf() {
        return brokerCanViewInformationOf;
    }

    public Collection<TransferType> getBrokerConversionSimulationTTs() {
        return brokerConversionSimulationTTs;
    }

    public Collection<MemberRecordType> getBrokerCreateMemberRecordTypes() {
        return brokerCreateMemberRecordTypes;
    }

    public Collection<MemberRecordType> getBrokerDeleteMemberRecordTypes() {
        return brokerDeleteMemberRecordTypes;
    }

    public Collection<Document> getBrokerDocuments() {
        return brokerDocuments;
    }

    public Collection<MemberRecordType> getBrokerMemberRecordTypes() {
        return brokerMemberRecordTypes;
    }

    public Collection<MemberRecordType> getBrokerModifyMemberRecordTypes() {
        return brokerModifyMemberRecordTypes;
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

    public void setBrokerCanViewInformationOf(final Collection<AccountType> brokerCanViewInformationOf) {
        this.brokerCanViewInformationOf = brokerCanViewInformationOf;
    }

    public void setBrokerConversionSimulationTTs(final Collection<TransferType> brokerConversionSimulationTTs) {
        this.brokerConversionSimulationTTs = brokerConversionSimulationTTs;
    }

    public void setBrokerCreateMemberRecordTypes(final Collection<MemberRecordType> brokerCreateMemberRecordTypes) {
        this.brokerCreateMemberRecordTypes = brokerCreateMemberRecordTypes;
    }

    public void setBrokerDeleteMemberRecordTypes(final Collection<MemberRecordType> brokerDeleteMemberRecordTypes) {
        this.brokerDeleteMemberRecordTypes = brokerDeleteMemberRecordTypes;
    }

    public void setBrokerDocuments(final Collection<Document> brokerDocuments) {
        this.brokerDocuments = brokerDocuments;
    }

    public void setBrokerMemberRecordTypes(final Collection<MemberRecordType> brokerMemberRecordTypes) {
        this.brokerMemberRecordTypes = brokerMemberRecordTypes;
    }

    public void setBrokerModifyMemberRecordTypes(final Collection<MemberRecordType> brokerModifyMemberRecordTypes) {
        this.brokerModifyMemberRecordTypes = brokerModifyMemberRecordTypes;
    }

}