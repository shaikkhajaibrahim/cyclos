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
package nl.strohalm.cyclos.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.entities.EntityWithCustomFields;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Control;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Type;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField.Access;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for custom field manipulation
 * @author luis
 */
public final class CustomFieldHelper {

    /**
     * Contains a relationship between custom field and its respective value
     * @author luis
     */
    public static class Entry implements Serializable {
        private static final long      serialVersionUID = 1629234603383130863L;
        private final CustomField      field;
        private final CustomFieldValue value;

        public Entry(final CustomField field, final CustomFieldValue value) {
            this.field = field;
            this.value = value;
        }

        public CustomField getField() {
            return field;
        }

        public CustomFieldValue getValue() {
            return value;
        }

        @Override
        public String toString() {
            String field, value;
            try {
                field = this.field.getName();
            } catch (final NullPointerException e) {
                field = "null";
            }
            try {
                value = this.value.getValue();
            } catch (final NullPointerException e) {
                value = "null";
            }
            return field + "=" + value;
        }
    }

    /**
     * Filters ad custom fields to be used on advanced search
     */
    public static List<AdCustomField> adFieldsForSearch(final List<AdCustomField> fields) {
        final List<AdCustomField> adFields = new ArrayList<AdCustomField>();
        for (final AdCustomField field : fields) {
            if (field.isShowInSearch()) {
                adFields.add(field);
            }
        }
        return adFields;
    }

    /**
     * Builds a collection using all custom fields and their respective values, if any
     */
    public static Collection<Entry> buildEntries(final Collection<? extends CustomField> fields, final Collection<? extends CustomFieldValue> values) {
        if (fields == null) {
            return null;
        }
        final Collection<Entry> entries = new ArrayList<Entry>(fields.size());
        for (final CustomField field : fields) {
            final CustomFieldValue fieldValue = findByField(field, values);
            if (fieldValue != null && StringUtils.isNotEmpty(field.getPattern())) {
                fieldValue.setValue(StringHelper.removeMask(field.getPattern(), fieldValue.getValue()));
            }
            entries.add(new Entry(field, fieldValue));
        }
        return entries;
    }

    /**
     * Builds a collection of field values given a value class, a collection of fields and a map of names/values
     */
    public static <V extends CustomFieldValue> Collection<V> buildValues(final Class<V> valueClass, final Collection<? extends CustomField> fields, final Map<String, String> values) {
        if (valueClass != null && fields != null && values != null) {
            final Collection<V> fieldValues = new ArrayList<V>();
            for (final CustomField field : fields) {
                final String value = values.get(field.getInternalName());
                final V fieldValue = ClassHelper.instantiate(valueClass);
                fieldValue.setField(field);
                if (StringUtils.isNotEmpty(value)) {
                    if (field.getType() == CustomField.Type.ENUMERATED) {
                        fieldValue.setPossibleValue(findPossibleValue(value, field.getPossibleValues()));
                    } else {
                        fieldValue.setStringValue(value);
                    }
                }
                fieldValues.add(fieldValue);
            }
            return fieldValues;
        }
        return null;
    }

    /**
     * Clones the custom field values of an entity from one instance to other
     */
    @SuppressWarnings("unchecked")
    public static <CF extends CustomField, CFV extends CustomFieldValue> void cloneFieldValues(final EntityWithCustomFields<CF, CFV> from, final EntityWithCustomFields<CF, CFV> to) {
        final Collection<CFV> customValues = from.getCustomValues();
        final List<CFV> newCustomValues = new ArrayList<CFV>();
        for (final CFV customValue : customValues) {
            final Object clone = customValue.clone();
            final CFV newCustomValue = (CFV) clone;
            newCustomValue.setOwner(to);
            newCustomValues.add(newCustomValue);
        }
        to.setCustomValues(customValues);
    }

    /**
     * Finds the value of the given field inside the collection
     */
    public static <V extends CustomFieldValue> V findByField(final CustomField field, final Collection<V> values) {
        if (values != null && field != null) {
            for (final V value : values) {
                if (field.equals(value.getField())) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Finds the value of the given field inside the collection
     */
    public static <V extends CustomFieldValue> V findByFieldId(final Long fieldId, final Collection<V> values) {
        if (values != null && fieldId != null) {
            for (final V value : values) {
                if (value.getField().getId().equals(fieldId)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Finds the value of the given field inside the collection
     */
    public static <V extends CustomFieldValue> V findByFieldName(final String fieldName, final Collection<V> values) {
        if (values != null && StringUtils.isNotEmpty(fieldName)) {
            for (final V value : values) {
                if (value.getField().getInternalName().equals(fieldName)) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Finds a custom field in a collection by it's identifier
     */
    public static <F extends CustomField> F findById(final Collection<F> fields, final Long id) {
        if (fields != null && id != null) {
            for (final F f : fields) {
                if (ObjectUtils.equals(f.getId(), id)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Finds a custom field in a collection by it's internal name
     */
    public static <F extends CustomField> F findByInternalName(final Collection<F> fields, final String internalName) {
        if (fields != null && internalName != null) {
            for (final F f : fields) {
                if (ObjectUtils.equals(f.getInternalName(), internalName)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Finds a possible value reference on the collection
     */
    public static CustomFieldPossibleValue findPossibleValue(final String value, final Collection<CustomFieldPossibleValue> possibleValues) {
        if (StringUtils.isNotEmpty(value)) {
            for (final CustomFieldPossibleValue possibleValue : possibleValues) {
                if (value.equals(possibleValue.getValue())) {
                    return possibleValue;
                }
            }
        }
        return null;
    }

    /**
     * Finds a possible value label by id
     */
    public static String findPossibleValueById(final Object value, final Collection<CustomFieldPossibleValue> possibleValues) {
        long id;
        try {
            id = CoercionHelper.coerce(Long.TYPE, value);
            for (final CustomFieldPossibleValue possibleValue : possibleValues) {
                if (id == possibleValue.getId()) {
                    return possibleValue.getValue();
                }
            }
        } catch (final Exception e) {
            // Keep on
        }
        return null;
    }

    /**
     * Returns the basic fields only, that is, strings with control = text box, or integers or enums
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomField> List<T> onlyBasic(final List<T> customFields) {
        final List<T> result = new ArrayList<T>(customFields.size());
        for (final CustomField field : customFields) {
            final Type type = field.getType();
            final Control control = field.getControl();
            boolean useField = false;
            if (type == Type.STRING && control == Control.TEXT) {
                useField = true;
            } else if (type == Type.ENUMERATED || type == Type.INTEGER) {
                useField = true;
            }
            if (useField) {
                result.add((T) field);
            }
        }
        return result;
    }

    /**
     * Filters the member custom field list, returning only those for ad search
     */
    public static List<MemberCustomField> onlyForAdSearch(final List<MemberCustomField> fields) {
        final List<MemberCustomField> memberFields = new ArrayList<MemberCustomField>();
        final Group group = LoggedUser.group();
        for (final MemberCustomField field : fields) {
            final Access access = field.getAdSearchAccess();
            if (access != null && access.granted(group)) {
                memberFields.add(field);
            }
        }
        return memberFields;
    }

    /**
     * Filters the admin custom field list, returning only those used for the given group
     */
    public static List<AdminCustomField> onlyForGroup(final List<AdminCustomField> fields, final AdminGroup group) {
        final List<AdminCustomField> adminFields = new ArrayList<AdminCustomField>();
        for (final AdminCustomField field : fields) {
            if (field.getGroups().contains(group)) {
                adminFields.add(field);
            }
        }
        return adminFields;
    }

    /**
     * Filters the member custom field list, returning only those used for the given group
     */
    public static List<MemberCustomField> onlyForGroup(final List<MemberCustomField> fields, final MemberGroup group) {
        final List<MemberCustomField> memberFields = new ArrayList<MemberCustomField>();
        for (final MemberCustomField field : fields) {
            if (field.getGroups().contains(group)) {
                memberFields.add(field);
            }
        }
        return memberFields;
    }

    public static List<MemberCustomField> onlyForGroups(final List<MemberCustomField> fields, final Collection<MemberGroup> groups) {
        final Set<MemberCustomField> memberFields = new HashSet<MemberCustomField>();
        for (final MemberGroup group : groups) {
            memberFields.addAll(onlyForGroup(fields, group));
        }
        return new ArrayList<MemberCustomField>(memberFields);
    }

    /**
     * Filters the member custom field list, returning only those for loan search
     */
    public static List<MemberCustomField> onlyForLoanSearch(final List<MemberCustomField> fields) {
        final List<MemberCustomField> memberFields = new ArrayList<MemberCustomField>();
        final Group group = LoggedUser.group();
        for (final MemberCustomField field : fields) {
            final Access access = field.getLoanSearchAccess();
            if (access != null && access.granted(group)) {
                memberFields.add(field);
            }
        }
        return memberFields;
    }

    /**
     * Filters the member custom field list, returning only those for member search
     */
    public static List<MemberCustomField> onlyForMemberSearch(final List<MemberCustomField> fields) {
        final List<MemberCustomField> memberFields = new ArrayList<MemberCustomField>();
        final Group group = LoggedUser.group();
        for (final MemberCustomField field : fields) {
            final Access access = field.getMemberSearchAccess();
            if (access != null && access.granted(group)) {
                memberFields.add(field);
            }
        }
        return memberFields;
    }

    /**
     * Filters the ad custom field list, returning only those for ad search
     */
    public static List<AdCustomField> onlyVisible(final List<AdCustomField> fields) {
        final Group.Nature nature = LoggedUser.isValid() ? LoggedUser.group().getNature() : Group.Nature.MEMBER;
        final List<AdCustomField> adFields = new ArrayList<AdCustomField>();
        for (final AdCustomField field : fields) {
            final AdCustomField.Visibility visibility = field.getVisibility();
            if (visibility.granted(nature)) {
                adFields.add(field);
            }
        }
        return adFields;
    }

}