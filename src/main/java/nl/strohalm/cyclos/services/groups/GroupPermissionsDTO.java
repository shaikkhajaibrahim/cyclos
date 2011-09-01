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

import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Class used to store a group's permissions
 * @author luis
 */
public abstract class GroupPermissionsDTO<G extends Group> extends DataObject {
    private static final long           serialVersionUID = 6176708478971547330L;
    private G                           group;
    private Collection<Document>        documents;
    private Collection<Operation>       operations;
    private Collection<MessageCategory> messageCategories;
    private Collection<GuaranteeType>   guaranteeTypes;
    private Collection<TransferType>    conversionSimulationTTs;

    public Collection<TransferType> getConversionSimulationTTs() {
        return conversionSimulationTTs;
    }

    public Collection<Document> getDocuments() {
        return documents;
    }

    public G getGroup() {
        return group;
    }

    public Collection<GuaranteeType> getGuaranteeTypes() {
        return guaranteeTypes;
    }

    public Collection<MessageCategory> getMessageCategories() {
        return messageCategories;
    }

    public Collection<Operation> getOperations() {
        return operations;
    }

    public void setConversionSimulationTTs(final Collection<TransferType> conversionSimulationTTs) {
        this.conversionSimulationTTs = conversionSimulationTTs;
    }

    public void setDocuments(final Collection<Document> documents) {
        this.documents = documents;
    }

    public void setGroup(final G group) {
        this.group = group;
    }

    public void setGuaranteeTypes(final Collection<GuaranteeType> guaranteeTypes) {
        this.guaranteeTypes = guaranteeTypes;
    }

    public void setMessageCategories(final Collection<MessageCategory> messageCategories) {
        this.messageCategories = messageCategories;
    }

    public void setOperations(final Collection<Operation> operations) {
        this.operations = operations;
    }

}