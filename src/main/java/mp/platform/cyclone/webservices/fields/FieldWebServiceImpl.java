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
package mp.platform.cyclone.webservices.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.jws.WebService;

import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField.Access;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.model.FieldVO;
import mp.platform.cyclone.webservices.model.PossibleValueVO;
import mp.platform.cyclone.webservices.utils.server.FieldHelper;

/**
 * Web service implementation
 * @author luis
 */
@WebService(name = "fields", serviceName = "fields")
public class FieldWebServiceImpl implements FieldWebService {

    private FieldHelper        fieldHelper;
    private CustomFieldService customFieldService;

    private FetchService       fetchService;

    @SuppressWarnings("unchecked")
    public List<FieldVO> adFieldsForAdSearch() {
        final List<FieldVO> vos = new ArrayList<FieldVO>();
        final List<AdCustomField> customFields = (List<AdCustomField>) customFieldService.listByNature(CustomField.Nature.AD);
        for (final AdCustomField field : customFields) {
            if (field.isShowInSearch()) {
                vos.add(fieldHelper.toVO(field));
            }
        }
        return vos;
    }

    public List<FieldVO> allAdFields() {
        return fieldHelper.toVOs(customFieldService.listByNature(CustomField.Nature.AD));
    }

    public List<FieldVO> allMemberFields() {
        return fieldHelper.toVOs(customFieldService.listByNature(CustomField.Nature.MEMBER));
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public FieldHelper getFieldHelper() {
        return fieldHelper;
    }

    @SuppressWarnings("unchecked")
    public List<FieldVO> memberFields(final Long groupId) {
        final ServiceClient client = fetchService.fetch(WebServiceContext.getClient(), ServiceClient.Relationships.MANAGE_GROUPS);
        final Set<MemberGroup> groups = client.getManageGroups();
        if (groups.isEmpty()) {
            throw new PermissionDeniedException();
        }
        // Find the group
        MemberGroup group = null;
        if (groupId == null || groupId.intValue() <= 0) {
            group = groups.iterator().next();
        } else {
            for (final MemberGroup memberGroup : groups) {
                if (memberGroup.getId().equals(groupId)) {
                    group = memberGroup;
                    break;
                }
            }
            if (group == null) {
                throw new PermissionDeniedException();
            }
        }
        final List<MemberCustomField> allCustomFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        final List<MemberCustomField> fields = CustomFieldHelper.onlyForGroup(allCustomFields, group);
        return fieldHelper.toVOs(fields);
    }

    @SuppressWarnings("unchecked")
    public List<FieldVO> memberFieldsForAdSearch() {
        final List<FieldVO> vos = new ArrayList<FieldVO>();
        final List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        for (final MemberCustomField field : customFields) {
            final Access adSearchAccess = field.getAdSearchAccess();
            if (adSearchAccess != null && adSearchAccess == MemberCustomField.Access.MEMBER) {
                vos.add(fieldHelper.toVO(field));
            }
        }
        return vos;
    }

    @SuppressWarnings("unchecked")
    public List<FieldVO> memberFieldsForMemberSearch() {
        final List<FieldVO> vos = new ArrayList<FieldVO>();
        final List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        for (final MemberCustomField field : customFields) {
            final Access memberSearchAccess = field.getMemberSearchAccess();
            if (memberSearchAccess != null && memberSearchAccess == MemberCustomField.Access.MEMBER) {
                vos.add(fieldHelper.toVO(field));
            }
        }
        return vos;
    }

    public List<FieldVO> paymentFields(final Long transferTypeId) {
        final TransferType transferType = EntityHelper.reference(TransferType.class, transferTypeId);
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transferType);
        return fieldHelper.toVOs(customFields);
    }

    public List<PossibleValueVO> possibleValuesForAdField(final String name) {
        return possibleValuesForAdFieldGivenParent(name, null);
    }

    public List<PossibleValueVO> possibleValuesForAdFieldGivenParent(final String name, final Long parentValueId) {
        final List<? extends CustomField> fields = customFieldService.listByNature(CustomField.Nature.AD);
        return possibleValues(name, fields, parentValueId);
    }

    public List<PossibleValueVO> possibleValuesForMemberField(final String name) {
        return possibleValuesForMemberFieldGivenParent(name, null);
    }

    public List<PossibleValueVO> possibleValuesForMemberFieldGivenParent(final String name, final Long parentValueId) {
        final List<? extends CustomField> fields = customFieldService.listByNature(CustomField.Nature.MEMBER);
        return possibleValues(name, fields, parentValueId);
    }

    public List<PossibleValueVO> possibleValuesForPaymentFields(final Long transferTypeId, final String name) {
        final TransferType transferType = EntityHelper.reference(TransferType.class, transferTypeId);
        final List<PaymentCustomField> fields = customFieldService.listPaymentFields(transferType);
        return possibleValues(name, fields, null);
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setFieldHelper(final FieldHelper fieldHelper) {
        this.fieldHelper = fieldHelper;
    }

    private List<PossibleValueVO> possibleValues(final String internalName, final List<? extends CustomField> fields, final Long parentValueId) {
        if (internalName == null) {
            return null;
        }

        final CustomField field = CustomFieldHelper.findByInternalName(fields, internalName);
        if (field == null || field.getType() != CustomField.Type.ENUMERATED) {
            return null;
        }
        final List<PossibleValueVO> values = new ArrayList<PossibleValueVO>();
        Collection<CustomFieldPossibleValue> possibleValues;
        if (parentValueId == null) {
            possibleValues = field.getPossibleValues();
        } else {
            possibleValues = field.getPossibleValuesByParent(customFieldService.loadPossibleValue(parentValueId));
        }
        for (final CustomFieldPossibleValue possibleValue : possibleValues) {
            values.add(fieldHelper.toVO(possibleValue));
        }
        return values;
    }
}
