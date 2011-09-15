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
package mp.platform.cyclone.webservices.utils.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.StringHelper;
import mp.platform.cyclone.webservices.model.FieldVO;
import mp.platform.cyclone.webservices.model.FieldValueVO;
import mp.platform.cyclone.webservices.model.PossibleValueVO;
import mp.platform.cyclone.webservices.model.RegistrationFieldValueVO;
import mp.platform.cyclone.webservices.model.FieldVO.FieldVOType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Helper methods for
 * @author luis
 */
public class FieldHelper {

    private CustomFieldService customFieldService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public List<FieldValueVO> toList(final Collection<? extends CustomField> fields, final Collection<? extends CustomFieldValue> customValues) {
        if (CollectionUtils.isEmpty(fields) || CollectionUtils.isEmpty(customValues)) {
            return Collections.emptyList();
        }
        final List<FieldValueVO> values = new ArrayList<FieldValueVO>();
        for (final CustomField field : fields) {
            final String valueAsString = getFieldValue(field, customValues);
            if (valueAsString != null) {
                values.add(new FieldValueVO(field.getInternalName(), valueAsString));
            }
        }
        return values;
    }

    /**
     * Builds a map with field values
     */
    public Map<String, String> toMap(final Collection<? extends CustomField> fields, final Collection<? extends CustomFieldValue> customValues) {
        if (CollectionUtils.isEmpty(fields) || CollectionUtils.isEmpty(customValues)) {
            return Collections.emptyMap();
        }
        final Map<String, String> values = new LinkedHashMap<String, String>();
        for (final CustomField field : fields) {
            final String valueAsString = getFieldValue(field, customValues);
            if (valueAsString != null) {
                values.put(field.getInternalName(), valueAsString);
            }
        }
        return values;
    }

    /**
     * Convert an array of FieldValue instances to a collection of CustomFieldValue
     */
    @SuppressWarnings("unchecked")
    public <T extends CustomFieldValue> Collection<T> toValueCollection(final Collection<? extends CustomField> fields, final List<? extends FieldValueVO> fieldValues) {
        if (CollectionUtils.isEmpty(fields) || CollectionUtils.isEmpty(fieldValues)) {
            return Collections.emptySet();
        }
        final List<T> customValues = new ArrayList<T>();
        for (final FieldValueVO fieldValue : fieldValues) {
            final String key = fieldValue.getField();
            CustomField field;
            // Check if key is the id or internal name
            if (StringUtils.isNumeric(key)) {
                // By id
                field = CustomFieldHelper.findById(fields, Long.parseLong(key));
            } else {
                field = CustomFieldHelper.findByInternalName(fields, key);
            }
            if (field == null) {
                throw new IllegalArgumentException("Custom field " + key + " not found");
            }
            final T value = (T) ClassHelper.instantiate(field.getNature().getValueType());
            value.setField(field);
            value.setValue(fieldValue.getValue());
            if (StringUtils.isNotEmpty(field.getPattern())) {
                value.setValue(StringHelper.removeMask(field.getPattern(), value.getValue()));
            }
            if (fieldValue instanceof RegistrationFieldValueVO && value instanceof MemberCustomFieldValue) {
                final RegistrationFieldValueVO reg = (RegistrationFieldValueVO) fieldValue;
                final MemberCustomFieldValue memberValue = (MemberCustomFieldValue) value;
                memberValue.setHidden(reg.isHidden());
            }
            customValues.add(value);
        }
        return customValues;
    }

    /**
     * Convert an array of FieldValue instances to a collection of CustomFieldValue
     */
    public <T extends CustomFieldValue> Collection<T> toValueCollection(final CustomField.Nature nature, final List<FieldValueVO> fieldValues) {
        return toValueCollection(customFieldService.listByNature(nature), fieldValues);
    }

    /**
     * Convert a field to VO
     */
    public FieldVO toVO(final CustomField field) {
        if (field == null) {
            return null;
        }
        final FieldVO vo = new FieldVO();
        vo.setId(field.getId());
        vo.setDisplayName(field.getName());
        vo.setInternalName(field.getInternalName());
        vo.setType(FieldVOType.valueOf(field.getType().name()));
        return vo;
    }

    /**
     * Convert a possible value to VO
     */
    public PossibleValueVO toVO(final CustomFieldPossibleValue possibleValue) {
        if (possibleValue == null) {
            return null;
        }
        final PossibleValueVO vo = new PossibleValueVO();
        vo.setId(possibleValue.getId());
        vo.setValue(possibleValue.getValue());
        return vo;
    }

    /**
     * Converts a collection of custom fields into VOs
     */
    public List<FieldVO> toVOs(final Collection<? extends CustomField> customFields) {
        final List<FieldVO> vos = new ArrayList<FieldVO>(customFields.size());
        for (final CustomField field : customFields) {
            vos.add(toVO(field));
        }
        return vos;
    }

    protected String getFieldValue(final CustomField field, final Collection<? extends CustomFieldValue> customValues) {
        final CustomFieldValue value = CustomFieldHelper.findByField(field, customValues);
        if (value instanceof MemberCustomFieldValue) {
            final MemberCustomFieldValue memberValue = (MemberCustomFieldValue) value;
            if (memberValue.isHidden()) {
                // Skip hidden value
                return null;
            }
        }
        if (value != null) {
            String valueAsString = value.getValue();
            if (StringUtils.isNotEmpty(field.getPattern())) {
                valueAsString = StringHelper.applyMask(field.getPattern(), valueAsString);
            }
            return valueAsString;
        }
        return null;
    }
}
