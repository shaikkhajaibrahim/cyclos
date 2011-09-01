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
package nl.strohalm.cyclos.entities.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * A group of permissions
 * @author luis
 */
public abstract class Group extends Entity implements Comparable<Group> {

    public static enum Nature {
        ADMIN("A"), MEMBER("M"), BROKER("B"), OPERATOR("O");

        private final String discriminator;

        private Nature(final String discriminator) {
            this.discriminator = discriminator;
        }

        public String getDiscriminator() {
            return discriminator;
        }

        public Element.Nature getElementNature() {
            switch (this) {
                case ADMIN:
                    return Element.Nature.ADMIN;
                case MEMBER:
                case BROKER:
                    return Element.Nature.MEMBER;
                case OPERATOR:
                    return Element.Nature.OPERATOR;
                default:
                    return null;
            }
        }

        public Class<? extends Group> getGroupClass() {
            switch (this) {
                case ADMIN:
                    return AdminGroup.class;
                case MEMBER:
                    return MemberGroup.class;
                case BROKER:
                    return BrokerGroup.class;
                case OPERATOR:
                    return OperatorGroup.class;
                default:
                    return null;
            }
        }
    }

    public static enum Relationships implements Relationship {
        ELEMENTS("elements"), PAYMENT_FILTERS("paymentFilters"), GROUP_FILTERS("groupFilters"), PERMISSIONS("permissions"), TRANSFER_TYPES("transferTypes"), CONVERSION_SIMULATION_TTS("conversionSimulationTTs"), DOCUMENTS("documents"), CUSTOMIZED_FILES("customizedFiles"), MESSAGE_CATEGORIES("messageCategories"), GUARANTEE_TYPES("guaranteeTypes"), CHARGEBACK_TRANSFER_TYPES("chargebackTransferTypes");

        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static enum Status implements StringValuedEnum {
        NORMAL("N"), REMOVED("R");
        private final String value;

        private Status(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean isEnabled() {
            return this == NORMAL;
        }
    }

    private static final long            serialVersionUID = 3079265000327578016L;

    private String                       description;
    private Collection<Element>          elements;
    private String                       name;
    private String                       loginPageName;
    private String                       containerUrl;
    private Collection<PaymentFilter>    paymentFilters;
    private Collection<Operation>        permissions;
    private Status                       status           = Status.NORMAL;
    private BasicGroupSettings           basicSettings    = new BasicGroupSettings();
    private Collection<TransferType>     transferTypes;
    private Collection<TransferType>     conversionSimulationTTs;
    private Collection<TransferType>     chargebackTransferTypes;
    private Collection<Document>         documents;
    private Collection<CustomizedFile>   customizedFiles;
    private Collection<MessageCategory>  messageCategories;
    private Collection<GroupFilter>      groupFilters;
    private Collection<MemberRecordType> memberRecordTypes;
    private Collection<GuaranteeType>    guaranteeTypes;

    public void addDocument(final Document document) {
        if (documents == null) {
            documents = new ArrayList<Document>();
        }

        documents.add(document);
    }

    public int compareTo(final Group o) {
        return name.compareTo(o.getName());
    }

    public BasicGroupSettings getBasicSettings() {
        return basicSettings;
    }

    public Collection<TransferType> getChargebackTransferTypes() {
        return chargebackTransferTypes;
    }

    public String getContainerUrl() {
        return containerUrl;
    }

    public Collection<TransferType> getConversionSimulationTTs() {
        return conversionSimulationTTs;
    }

    public Collection<CustomizedFile> getCustomizedFiles() {
        return customizedFiles;
    }

    public String getDescription() {
        return description;
    }

    public Collection<Document> getDocuments() {
        return documents;
    }

    public Collection<? extends Element> getElements() {
        return elements;
    }

    @SuppressWarnings("unchecked")
    public Collection<GuaranteeType> getEnabledGuaranteeTypes() {
        final Collection<GuaranteeType> all = getGuaranteeTypes();

        if (all == null || all.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        final ArrayList<GuaranteeType> enabled = new ArrayList<GuaranteeType>();
        for (final GuaranteeType gt : all) {
            if (gt.isEnabled()) {
                enabled.add(gt);
            }
        }

        return enabled;
    }

    public Collection<GroupFilter> getGroupFilters() {
        return groupFilters;
    }

    public Collection<GuaranteeType> getGuaranteeTypes() {
        return guaranteeTypes;
    }

    public String getLoginPageName() {
        return loginPageName;
    }

    public Collection<MemberRecordType> getMemberRecordTypes() {
        return memberRecordTypes;
    }

    public Collection<MessageCategory> getMessageCategories() {
        return messageCategories;
    }

    public String getName() {
        return name;
    }

    public abstract Nature getNature();

    public Collection<PaymentFilter> getPaymentFilters() {
        return paymentFilters;
    }

    public Collection<Operation> getPermissions() {
        return permissions;
    }

    public Status getStatus() {
        return status;
    }

    public Collection<TransferType> getTransferTypes() {
        return transferTypes;
    }

    public boolean isRemoved() {
        return status == Status.REMOVED ? true : false;
    }

    public void setBasicSettings(BasicGroupSettings basicSettings) {
        if (basicSettings == null) {
            basicSettings = new BasicGroupSettings();
        }
        this.basicSettings = basicSettings;
    }

    public void setChargebackTransferTypes(final Collection<TransferType> chargebackTransferTypes) {
        this.chargebackTransferTypes = chargebackTransferTypes;
    }

    public void setContainerUrl(final String containerUrl) {
        this.containerUrl = containerUrl;
    }

    public void setConversionSimulationTTs(final Collection<TransferType> conversionSimulationTTs) {
        this.conversionSimulationTTs = conversionSimulationTTs;
    }

    public void setCustomizedFiles(final Collection<CustomizedFile> customizedFiles) {
        this.customizedFiles = customizedFiles;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setDocuments(final Collection<Document> documents) {
        this.documents = documents;
    }

    public void setElements(final Collection<Element> elements) {
        this.elements = elements;
    }

    public void setGroupFilters(final Collection<GroupFilter> groupFilters) {
        this.groupFilters = groupFilters;
    }

    public void setGuaranteeTypes(final Collection<GuaranteeType> guaranteeTypes) {
        this.guaranteeTypes = guaranteeTypes;
    }

    public void setLoginPageName(final String loginPageName) {
        this.loginPageName = loginPageName;
    }

    public void setMemberRecordTypes(final Collection<MemberRecordType> memberRecordTypes) {
        this.memberRecordTypes = memberRecordTypes;
    }

    public void setMessageCategories(final Collection<MessageCategory> messageCategories) {
        this.messageCategories = messageCategories;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPaymentFilters(final Collection<PaymentFilter> paymentFilters) {
        this.paymentFilters = paymentFilters;
    }

    public void setPermissions(final Collection<Operation> permissions) {
        this.permissions = permissions;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setTransferTypes(final Collection<TransferType> transferTypes) {
        this.transferTypes = transferTypes;
    }

    @Override
    public String toString() {
        return getId() + " - " + name;
    }

}