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
package nl.strohalm.cyclos.services.customization;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.customizations.CustomFieldDAO;
import nl.strohalm.cyclos.dao.customizations.CustomFieldPossibleValueDAO;
import nl.strohalm.cyclos.dao.customizations.CustomFieldValueDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.EntityWithCustomFields;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroup;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.imports.ImportedAd;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.LoanGroupCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomField;
import nl.strohalm.cyclos.entities.customization.fields.OperatorCustomField;
import nl.strohalm.cyclos.entities.customization.fields.OperatorCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.Validation;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Nature;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField.Access;
import nl.strohalm.cyclos.entities.customization.fields.OperatorCustomField.Visibility;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.PendingMember;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.members.imports.ImportedMember;
import nl.strohalm.cyclos.entities.members.imports.ImportedMemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.exceptions.SendingInvoiceWithMultipleTransferTypesWithCustomFields;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.StringHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.conversion.ConversionException;
import nl.strohalm.cyclos.utils.conversion.IdConverter;
import nl.strohalm.cyclos.utils.conversion.NumberConverter;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.LengthValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.UniqueError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;
import nl.strohalm.cyclos.utils.validation.Validator.PropertyRetrieveStrategy;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation for custom field service
 * @author luis
 */
public class CustomFieldServiceImpl implements CustomFieldService {

    /**
     * Retrieving strategy for validating properties
     * @author luis
     */
    public static class CustomFieldRetrievingStrategy implements PropertyRetrieveStrategy {

        private static final long serialVersionUID = 8667919404137289046L;
        private final CustomField field;

        public CustomFieldRetrievingStrategy(final CustomField field) {
            this.field = field;
        }

        public Object description(final Object object, final String name) {
            return field;
        }

        @SuppressWarnings("unchecked")
        public Object get(final Object object) {
            final Collection<? extends CustomFieldValue> values = (Collection<? extends CustomFieldValue>) PropertyHelper.get(object, "customValues");
            final CustomFieldValue fieldValue = CustomFieldHelper.findByField(field, values);
            return fieldValue == null ? null : fieldValue.getValue();
        }

        @SuppressWarnings("unchecked")
        public void set(final Object object, final Object value) {
            final Collection<? extends CustomFieldValue> values = (Collection<? extends CustomFieldValue>) PropertyHelper.get(object, "customValues");
            final CustomFieldValue fieldValue = CustomFieldHelper.findByField(field, values);
            if (fieldValue != null) {
                if (value instanceof CustomFieldPossibleValue) {
                    fieldValue.setPossibleValue((CustomFieldPossibleValue) value);
                } else {
                    fieldValue.setStringValue(ObjectUtils.toString(value, null));
                }
            }
        }
    }

    private final class BigDecimalValidator implements PropertyValidation {
        private static final long serialVersionUID = -7933981104151866154L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String str = (String) value;
            final NumberConverter<BigDecimal> numberConverter = settingsService.getLocalSettings().getNumberConverter();
            try {
                numberConverter.valueOf(str);
                return null;
            } catch (final ConversionException e) {
                return new InvalidError();
            }
        }
    }

    private class DateValidator implements PropertyValidation {
        private static final long serialVersionUID = 5145399976834903999L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String str = (String) value;
            final CalendarConverter dateConverter = settingsService.getLocalSettings().getRawDateConverter();
            try {
                final Calendar date = dateConverter.valueOf(str);
                if (date != null) {
                    final int year = date.get(Calendar.YEAR);
                    if (year < 1900 || year > 2100) {
                        return new InvalidError();
                    }
                }
                return null;
            } catch (final ConversionException e) {
                return new InvalidError();
            }
        }
    }

    private class EnumeratedValidator implements PropertyValidation {
        private static final long serialVersionUID = 5145399976834903999L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String str = (String) value;
            if (StringUtils.isEmpty(str)) {
                return null;
            }
            final CustomField field = (CustomField) property;
            CustomFieldPossibleValue possibleValue;
            try {
                boolean byValue = true;
                if (StringUtils.isNumeric(str)) {
                    try {
                        possibleValue = customFieldPossibleValueDao.load(new Long(str));
                        if (field.equals(possibleValue.getField())) {
                            byValue = false;
                        }
                    } catch (final EntityNotFoundException e) {
                        // Not found - try by value
                    }
                }
                if (byValue) {
                    // Try by value
                    possibleValue = customFieldPossibleValueDao.load(field.getId(), str);
                }
                // Value exists - return no error
                return null;
            } catch (final EntityNotFoundException e) {
                possibleValue = null;
            }
            // Return error if not found
            if (possibleValue == null) {
                return new InvalidError();
            } else {
                return null;
            }
        }
    }

    private class IntegerValidator implements PropertyValidation {
        private static final long serialVersionUID = 5145399976834903999L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String str = (String) value;
            if (StringUtils.isNotEmpty(str) && !StringUtils.isNumeric(str)) {
                return new InvalidError();
            }
            return null;
        }
    }

    /**
     * Validates a java identifier
     * @author Jefferson Magno
     */
    private class JavaIdentifierValidation implements PropertyValidation {
        private static final long serialVersionUID = 259170291118675512L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String string = (String) value;
            if (StringUtils.isNotEmpty(string) && !StringHelper.isValidJavaIdentifier(string)) {
                return new InvalidError();
            }
            return null;
        }
    }

    /**
     * Validates the parent field
     * @author luis
     */
    private final class ParentValidator implements PropertyValidation {

        private static final long serialVersionUID = -6383825246336857857L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final CustomField field = (CustomField) object;
            final CustomField parent = (CustomField) value;
            if (parent != null) {
                final List<CustomField> possibleParents = listPossibleParentFields(field);
                if (!possibleParents.contains(parent)) {
                    return new InvalidError();
                }
            }
            return null;
        }

    }

    private class UniqueCustomFieldInternalNameValidation implements PropertyValidation {
        private static final long serialVersionUID = 1L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final CustomField field = (CustomField) object;

            if (field.getInternalName() == null || field.getInternalName().equals("")) {
                return null;
            }

            return customFieldDao.isInternalNameUsed(field) ? new UniqueError() : null;
        }
    }

    /**
     * Validates an unique field value
     * @author luis
     */
    private class UniqueFieldValueValidation implements PropertyValidation {
        private static final long serialVersionUID = 6222393116036296454L;

        public ValidationError validate(final Object object, final Object data, final Object value) {
            if (!(object instanceof EntityWithCustomFields<?, ?>)) {
                return null;
            }
            final CustomField field = (CustomField) data;
            final String string = (String) value;
            if (StringUtils.isNotEmpty(string)) {
                // Build a field value
                CustomFieldValue fieldValue;
                try {
                    fieldValue = field.getNature().getValueType().newInstance();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                fieldValue.setField(field);
                fieldValue.setOwner(object);
                fieldValue.setStringValue(string);

                // Check uniqueness
                if (customFieldValueDao.valueExists(fieldValue)) {
                    return new UniqueError();
                }
            }
            return null;
        }
    }

    /**
     * If a validator class is specified for a field, check if it exists and if it is an implementation of PropertyValidation
     * @author jefferson
     */
    private class ValidatorClassValidation implements PropertyValidation {
        private static final long serialVersionUID = -4897199868398237786L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final Validation validation = (Validation) value;
            if (validation != null && StringUtils.isNotEmpty(validation.getValidatorClass())) {
                try {
                    if (!PropertyValidation.class.isAssignableFrom(Class.forName(validation.getValidatorClass()))) {
                        return new ValidationError("customField.error.invalidValidatorClass");
                    }
                } catch (final ClassNotFoundException e) {
                    return new ValidationError("customField.error.invalidValidatorClass");
                }
            }
            return null;
        }

    }

    private static final List<String>                                  EXCLUDED_PROPERTIES_FOR_DEPENDENT_FIELDS;
    static {
        final List<String> excluded = new ArrayList<String>();
        excluded.add("class");
        excluded.add("id");
        excluded.add("name");
        excluded.add("internalName");
        excluded.add("parent");
        excluded.add("description");
        excluded.add("allSelectedLabel");
        excluded.add("type");
        excluded.add("control");
        excluded.add("size");
        excluded.add("description");
        excluded.add("possibleValues");
        excluded.add("children");
        EXCLUDED_PROPERTIES_FOR_DEPENDENT_FIELDS = Collections.unmodifiableList(excluded);
    }

    private SettingsService                                            settingsService;
    private InvoiceService                                             invoiceService;
    private CustomFieldDAO                                             customFieldDao;
    private CustomFieldValueDAO                                        customFieldValueDao;
    private CustomFieldPossibleValueDAO                                customFieldPossibleValueDao;
    private FetchService                                               fetchService;
    private final Map<CustomField.Nature, List<? extends CustomField>> cachedCustomFieldsByNature = new EnumMap<Nature, List<? extends CustomField>>(CustomField.Nature.class);

    public void clearCache(final Nature nature) {
        cachedCustomFieldsByNature.remove(nature);
    }

    @SuppressWarnings("unchecked")
    public Validator getAdminValueValidator(final AdminGroup group) {
        final List<AdminCustomField> fields = (List<AdminCustomField>) listByNature(CustomField.Nature.ADMIN);
        return getValueValidator(CustomFieldHelper.onlyForGroup(fields, group));
    }

    public Validator getAdValueValidator() {
        return getValueValidator(listByNature(CustomField.Nature.AD));
    }

    public Validator getLoanGroupValueValidator() {
        return getValueValidator(listByNature(CustomField.Nature.LOAN_GROUP));
    }

    public Validator getMemberRecordValueValidator(final MemberRecordType memberRecordType) {
        return getValueValidator(listMemberRecordFields(memberRecordType));
    }

    @SuppressWarnings("unchecked")
    public Validator getMemberValueValidator(final MemberGroup group, final MemberCustomField.Access access) {
        final List<MemberCustomField> fields = new ArrayList<MemberCustomField>((List<MemberCustomField>) listByNature(CustomField.Nature.MEMBER));
        if (access != null) {
            for (final Iterator<MemberCustomField> iterator = fields.iterator(); iterator.hasNext();) {
                final MemberCustomField field = iterator.next();
                if (access.compareTo(field.getUpdateAccess()) > 0 || access.compareTo(field.getVisibilityAccess()) > 0) {
                    iterator.remove();
                }
            }
        }
        return getValueValidator(CustomFieldHelper.onlyForGroup(fields, group));
    }

    public Validator getOperatorValueValidator(final Member member) {
        return getValueValidator(listOperatorFields(member));
    }

    public Validator getPaymentValueValidator(final TransferType transferType) {
        return getValueValidator(listPaymentFields(transferType));
    }

    public void linkPaymentCustomField(TransferType transferType, PaymentCustomField customField) {
        transferType = fetchService.fetch(transferType, TransferType.Relationships.LINKED_CUSTOM_FIELDS);
        customField = fetchService.fetch(customField);
        final Collection<PaymentCustomField> linkedCustomFields = transferType.getLinkedCustomFields();
        if (!linkedCustomFields.contains(customField)) {
            // The underlying persistence engine should persist the relationship
            linkedCustomFields.add(customField);
        }
    }

    public List<? extends CustomField> listByNature(final Nature nature) {
        if (nature == CustomField.Nature.OPERATOR || nature == CustomField.Nature.MEMBER_RECORD) {
            throw new IllegalArgumentException("Cannot list all custom fields for " + nature);
        }
        List<? extends CustomField> fields = cachedCustomFieldsByNature.get(nature);
        if (fields == null) {
            fields = customFieldDao.listByNature(nature);
            fields = (List<? extends CustomField>) fetchService.fetch(fields, CustomField.Relationships.PARENT, CustomField.Relationships.CHILDREN, CustomField.Relationships.POSSIBLE_VALUES);
            cachedCustomFieldsByNature.put(nature, fields);
        }
        return fields;
    }

    public List<MemberRecordCustomField> listMemberRecordFields(final MemberRecordType memberRecordType) {
        return customFieldDao.listMemberRecordFields(memberRecordType);
    }

    public List<OperatorCustomField> listOperatorFields(final Member member) {
        return customFieldDao.listOperatorFields(member);
    }

    public List<PaymentCustomField> listPaymentFieldForList(final Account account, final boolean loan) {
        return customFieldDao.listPaymentFieldForList(account, loan);
    }

    public List<PaymentCustomField> listPaymentFieldForSearch(final Account account, final boolean loan) {
        return customFieldDao.listPaymentFieldForSearch(account, loan);
    }

    public List<PaymentCustomField> listPaymentFields(final TransferType transferType) {
        return customFieldDao.listPaymentFields(transferType);
    }

    @SuppressWarnings("unchecked")
    public <CF extends CustomField> List<CF> listPossibleParentFields(final CF field) {
        if (field == null || (field.isPersistent() && field.getType() != CustomField.Type.ENUMERATED)) {
            return new ArrayList<CF>();
        }
        // Get other fields for the same nature / owner
        final List<CF> fields = new ArrayList<CF>();
        switch (field.getNature()) {
            case MEMBER_RECORD:
                final MemberRecordCustomField memberRecordField = (MemberRecordCustomField) field;
                fields.addAll((Collection<? extends CF>) listMemberRecordFields(memberRecordField.getMemberRecordType()));
                break;
            case OPERATOR:
                final OperatorCustomField operatorField = (OperatorCustomField) field;
                fields.addAll((Collection<? extends CF>) listOperatorFields(operatorField.getMember()));
                break;
            case PAYMENT:
                final PaymentCustomField paymentField = (PaymentCustomField) field;
                final TransferType transferType = fetchService.fetch(paymentField.getTransferType(), TransferType.Relationships.CUSTOM_FIELDS);
                fields.addAll((Collection<? extends CF>) transferType.getCustomFields());
                break;
            default:
                fields.addAll((Collection<? extends CF>) listByNature(field.getNature()));
                break;
        }
        // Remove the field itself, those which are not enumerated and those who already have a parent (don't allow multiple levels)
        for (final Iterator<CF> iterator = fields.iterator(); iterator.hasNext();) {
            final CF current = iterator.next();
            if (field.equals(current) || current.getType() != CustomField.Type.ENUMERATED || current.getControl() != CustomField.Control.SELECT || current.getParent() != null) {
                iterator.remove();
            }
        }
        return fields;
    }

    public CustomField load(final Long id, final Relationship... fetch) {
        return customFieldDao.load(id, fetch);
    }

    public CustomFieldPossibleValue loadPossibleValue(final Long id, final Relationship... fetch) {
        return customFieldPossibleValueDao.load(id, fetch);
    }

    public int moveMemberRecordValues(final CustomFieldPossibleValue oldValue, final CustomFieldPossibleValue newValue) {
        return doMoveValues(oldValue, newValue);
    }

    public int moveOperatorValues(final CustomFieldPossibleValue oldValue, final CustomFieldPossibleValue newValue) {
        return doMoveValues(oldValue, newValue);
    }

    public int movePaymentValues(final CustomFieldPossibleValue oldValue, final CustomFieldPossibleValue newValue) {
        return doMoveValues(oldValue, newValue);
    }

    public int moveValues(final CustomFieldPossibleValue oldValue, final CustomFieldPossibleValue newValue) {
        return doMoveValues(oldValue, newValue);
    }

    public int remove(final Long... ids) {
        final CustomField.Nature[] allowed = { CustomField.Nature.AD, CustomField.Nature.MEMBER, CustomField.Nature.ADMIN, CustomField.Nature.LOAN_GROUP };
        for (final Long id : ids) {
            final CustomField field = load(id);
            if (!ArrayUtils.contains(allowed, field.getNature())) {
                throw new UnexpectedEntityException();
            }
            cachedCustomFieldsByNature.remove(field.getNature());
        }
        return customFieldDao.delete(ids);
    }

    public int removeMemberRecordField(final Long... ids) {
        for (final Long id : ids) {
            final CustomField field = load(id);
            if (field.getNature() != CustomField.Nature.MEMBER_RECORD) {
                throw new UnexpectedEntityException();
            }
        }
        return customFieldDao.delete(ids);
    }

    public int removeMemberRecordPossibleValue(final Long... ids) {
        return doRemovePossibleValue(EnumSet.of(CustomField.Nature.MEMBER_RECORD), ids);
    }

    public int removeOperatorField(final Long... ids) {
        for (final Long id : ids) {
            final CustomField field = load(id);
            if (field.getNature() != CustomField.Nature.OPERATOR) {
                throw new UnexpectedEntityException();
            }
        }
        return customFieldDao.delete(ids);
    }

    public int removeOperatorPossibleValue(final Long... ids) {
        return doRemovePossibleValue(EnumSet.of(CustomField.Nature.OPERATOR), ids);
    }

    public void removePaymentCustomField(TransferType transferType, PaymentCustomField customField) {
        transferType = fetchService.fetch(transferType, TransferType.Relationships.LINKED_CUSTOM_FIELDS);
        customField = fetchService.fetch(customField);

        if (transferType.equals(customField.getTransferType())) {
            customFieldDao.delete(customField.getId());
        } else {
            final Collection<PaymentCustomField> linkedCustomFields = transferType.getLinkedCustomFields();
            // The underlying persistence engine should persist the relationship
            linkedCustomFields.remove(customField);
        }
    }

    public int removePaymentPossibleValue(final Long... ids) {
        return doRemovePossibleValue(EnumSet.of(CustomField.Nature.PAYMENT), ids);
    }

    public int removePossibleValue(final Long... ids) {
        cachedCustomFieldsByNature.clear();
        return doRemovePossibleValue(EnumSet.of(CustomField.Nature.MEMBER, CustomField.Nature.ADMIN, CustomField.Nature.AD, CustomField.Nature.LOAN_GROUP), ids);
    }

    public CustomFieldPossibleValue save(final CustomFieldPossibleValue possibleValue) {
        return doSave(possibleValue, null);
    }

    public <F extends CustomField> F save(F customField) {
        if (customField.getNature() == CustomField.Nature.OPERATOR) {
            throw new UnexpectedEntityException();
        }

        // Special handling for fields with a parent field
        CustomField parent = null;
        if (customField.getParent() != null) {
            // When the field has a parent, several settings are copied from it
            parent = fetchService.fetch(customField.getParent());
            copyParentProperties(parent, customField);
        }
        validate(customField);
        if (customField.isTransient()) {
            if (parent == null) {
                // Top level fields: set the order after other fields
                final int order = listByNature(customField.getNature()).size() + 1;
                customField.setOrder(order);
            }

            // Save the field
            customField = customFieldDao.insert(customField);

            if (parent != null) {
                // Nested fields: position the field just after his parent
                final List<? extends CustomField> allFields = listByNature(customField.getNature());
                final List<Long> order = new ArrayList<Long>();
                for (int i = 0; i < allFields.size(); i++) {
                    final Long current = allFields.get(i).getId();
                    if (current.equals(parent.getId())) {
                        // Insert the custom field just after the parent
                        order.add(parent.getId());
                        order.add(customField.getId());
                    } else if (current.equals(customField.getId())) {
                        // The custom field has already been inserted
                        continue;
                    } else {
                        // Another unrelated field
                        order.add(current);
                    }
                }
                setOrder(customField.getNature(), order.toArray(new Long[order.size()]));
            }
        } else {
            // Keep the order
            final CustomField current = customFieldDao.load(customField.getId());
            customField.setOrder(current.getOrder());

            customField = customFieldDao.update(customField);

            // Update the dependent properties for child fields
            if (customField.getType() == CustomField.Type.ENUMERATED) {
                customField = fetchService.reload(customField, CustomField.Relationships.CHILDREN);
                for (final CustomField child : customField.getChildren()) {
                    copyParentProperties(customField, child);
                }
            }
        }
        cachedCustomFieldsByNature.remove(customField.getNature());
        return customField;
    }

    public MemberRecordCustomField save(MemberRecordCustomField customField) {
        validate(customField);
        if (customField.isTransient()) {
            // Set the order after other fields
            final MemberRecordType memberRecordType = customField.getMemberRecordType();
            final int order = listMemberRecordFields(memberRecordType).size() + 1;
            customField.setOrder(order);
            customField = customFieldDao.insert(customField);
        } else {
            // Keep the order
            final CustomField current = customFieldDao.load(customField.getId());
            customField.setOrder(current.getOrder());
            customField = customFieldDao.update(customField);
        }
        return customField;
    }

    public OperatorCustomField save(OperatorCustomField customField) {
        validate(customField);
        if (customField.isTransient()) {
            // Set the order after other fields
            final Member member = customField.getMember();
            final int order = listOperatorFields(member).size() + 1;
            customField.setOrder(order);

            customField = customFieldDao.insert(customField);
        } else {
            // Keep the order
            final CustomField current = customFieldDao.load(customField.getId());
            customField.setOrder(current.getOrder());

            customField = customFieldDao.update(customField);
        }
        return customField;
    }

    public PaymentCustomField save(PaymentCustomField customField) {
        validate(customField);
        if (customField.getSearchAccess() == null) {
            customField.setSearchAccess(PaymentCustomField.Access.NONE);
        }
        if (customField.getListAccess() == null) {
            customField.setListAccess(PaymentCustomField.Access.NONE);
        }

        // Payment custom fields for loan transfer types have their search and list accesses as a boolean flag. Internally, the allowed values are
        // NONE and BOTH_ACCOUNTS. Any other value should be treated as BOTH_ACCOUNTS.
        final TransferType transferType = fetchService.fetch(customField.getTransferType());
        if (transferType.isLoanType()) {
            if (customField.getSearchAccess() != PaymentCustomField.Access.NONE) {
                customField.setSearchAccess(PaymentCustomField.Access.BOTH_ACCOUNTS);
            }
            if (customField.getListAccess() != PaymentCustomField.Access.NONE) {
                customField.setListAccess(PaymentCustomField.Access.BOTH_ACCOUNTS);
            }
        }

        if (customField.isTransient()) {
            // Set the order after other fields
            final int order = listPaymentFields(transferType).size() + 1;
            customField.setOrder(order);
            customField = customFieldDao.insert(customField);
        } else {
            // Keep the order
            final CustomField current = customFieldDao.load(customField.getId());
            customField.setOrder(current.getOrder());
            customField = customFieldDao.update(customField);
        }
        return customField;
    }

    public void saveAdminValues(final Administrator admin) {
        getAdminValueValidator(admin.getAdminGroup()).validate(admin);
        saveValues(admin);
    }

    public void saveAdValues(final Ad ad) {
        getAdValueValidator().validate(ad);
        saveValues(ad);
        final Collection<AdCustomFieldValue> customValues = ad.getCustomValues();
        if (customValues != null) {
            final Group.Nature nature = LoggedUser.isValid() ? LoggedUser.group().getNature() : Group.Nature.MEMBER;
            for (final AdCustomFieldValue fieldValue : customValues) {
                final AdCustomField field = (AdCustomField) fetchService.fetch(fieldValue.getField());
                if (!field.getVisibility().granted(nature)) {
                    // A value which is not visible. Ensure the old value will remain
                    AdCustomFieldValue current;
                    try {
                        current = (AdCustomFieldValue) customFieldValueDao.load(field, ad);
                    } catch (final EntityNotFoundException e) {
                        // There is no saved value still
                        current = new AdCustomFieldValue();
                    }
                    fieldValue.setStringValue(current.getStringValue());
                    fieldValue.setPossibleValue(current.getPossibleValue());
                }

            }
        }
    }

    public void saveImportedAdValues(final ImportedAd importedAd) {
        getAdValueValidator().validate(importedAd);
        saveValues(importedAd);
    }

    public void saveImportedMemberRecordValues(final ImportedMemberRecord record) {
        getMemberRecordValueValidator(record.getType()).validate(record);
        saveValues(record);
    }

    public void saveImportedMemberValues(final ImportedMember importedMember) {
        getMemberValueValidator(importedMember.getImport().getGroup(), MemberCustomField.Access.REGISTRATION).validate(importedMember);
        saveValues(importedMember);
    }

    public void saveLoanGroupValues(final LoanGroup loanGroup) {
        getLoanGroupValueValidator().validate(loanGroup);
        saveValues(loanGroup);
    }

    public CustomFieldPossibleValue saveMemberRecord(final CustomFieldPossibleValue possibleValue) {
        return doSave(possibleValue, CustomField.Nature.MEMBER_RECORD);
    }

    public void saveMemberRecordValues(final MemberRecord memberRecord) {
        getMemberRecordValueValidator(memberRecord.getType()).validate(memberRecord);
        saveValues(memberRecord);
    }

    public void saveMemberValues(RegisteredMember registeredMember) {
        final boolean isPublic = !LoggedUser.isValid();
        boolean byOwner = false;
        boolean byBroker = false;
        Element element = null;
        Group group = null;
        if (isPublic) {
            group = registeredMember.getMemberGroup();
            byOwner = true;
        } else {
            element = LoggedUser.element();
            group = element.getGroup();
            byOwner = element.equals(registeredMember);
            byBroker = element.equals(registeredMember.getBroker());
        }

        MemberCustomField.Access access = null;
        if (isPublic) {
            access = MemberCustomField.Access.REGISTRATION;
        } else if (byOwner) {
            access = MemberCustomField.Access.MEMBER;
        } else if (byBroker) {
            access = MemberCustomField.Access.BROKER;
        } else if (LoggedUser.isValid() && LoggedUser.isAdministrator()) {
            access = MemberCustomField.Access.ADMIN;
        }

        final Collection<MemberCustomFieldValue> customValues = registeredMember.getCustomValues();
        registeredMember = (RegisteredMember) fetchService.fetch((Entity) registeredMember, Element.Relationships.GROUP, PendingMember.Relationships.MEMBER_GROUP, Member.Relationships.BROKER);
        getMemberValueValidator(registeredMember.getMemberGroup(), access).validate(registeredMember);

        if (customValues != null) {
            for (final MemberCustomFieldValue value : customValues) {
                final MemberCustomField field = (MemberCustomField) fetchService.fetch(value.getField());
                final Access updateAccess = field.getUpdateAccess();
                if (!updateAccess.granted(group, byOwner, byBroker, isPublic)) {
                    // The member cannot change the value. Ensure the old value will remain
                    CustomFieldValue current;
                    try {
                        current = customFieldValueDao.load(field, registeredMember);
                    } catch (final EntityNotFoundException e) {
                        // There is no saved value still
                        current = new MemberCustomFieldValue();
                    }
                    value.setStringValue(current.getStringValue());
                    value.setPossibleValue(current.getPossibleValue());
                }
            }
        }
        saveValues(registeredMember);
    }

    public CustomFieldPossibleValue saveOperator(final CustomFieldPossibleValue possibleValue) {
        return doSave(possibleValue, CustomField.Nature.OPERATOR);
    }

    public void saveOperatorValues(final Operator operator) {
        getOperatorValueValidator(operator.getMember()).validate(operator);

        final Element element = LoggedUser.element();
        if (element instanceof Operator) {
            // Operator trying to save custom fields
            final Operator loggedOperator = (Operator) element;
            if (!loggedOperator.equals(operator)) {
                throw new PermissionDeniedException();
            }
            final Collection<OperatorCustomFieldValue> customValues = operator.getCustomValues();
            for (final OperatorCustomFieldValue value : customValues) {
                final OperatorCustomField field = (OperatorCustomField) fetchService.fetch(value.getField());
                final Visibility visibility = field.getVisibility();
                if (visibility != Visibility.EDITABLE) {
                    // The operator cannot change the value ... ensure the old value will remain
                    CustomFieldValue current;
                    try {
                        current = customFieldValueDao.load(field, operator);
                    } catch (final EntityNotFoundException e) {
                        // There is no saved value still
                        current = new OperatorCustomFieldValue();
                    }
                    value.setStringValue(current.getStringValue());
                    value.setPossibleValue(current.getPossibleValue());
                }
            }
        }
        saveValues(operator);
    }

    public CustomFieldPossibleValue savePayment(final CustomFieldPossibleValue possibleValue) {
        return doSave(possibleValue, CustomField.Nature.PAYMENT);
    }

    public void savePaymentValues(final Guarantee guarantee, final boolean validate) {
        final TransferType loanTransferType = guarantee.getGuaranteeType().getLoanTransferType();
        if (validate) {
            getPaymentValueValidator(loanTransferType).validate(guarantee);
        }
        saveValues(guarantee);
    }

    public void savePaymentValues(final Invoice invoice) {
        TransferType transferType = invoice.getTransferType();
        if (transferType == null) {
            // Get the possible transfer types
            final List<TransferType> transferTypes = invoiceService.getPossibleTransferTypes(invoice);
            if (transferTypes.size() == 1) {
                transferType = transferTypes.iterator().next();
                invoice.setTransferType(transferType);
            } else {
                // Custom fields only are used when there is a single possible transfer type
                for (final TransferType tt : transferTypes) {
                    if (!tt.getCustomFields().isEmpty()) {
                        throw new SendingInvoiceWithMultipleTransferTypesWithCustomFields();
                    }
                }
                // There will be no custom values
                invoice.setCustomValues(null);
                return;
            }
        }
        getPaymentValueValidator(transferType).validate(invoice);
        saveValues(invoice);
    }

    public void savePaymentValues(final Payment payment) {
        getPaymentValueValidator(payment.getType()).validate(payment);
        saveValues(payment);
    }

    public void setCustomFieldDao(final CustomFieldDAO customFieldDAO) {
        customFieldDao = customFieldDAO;
    }

    public void setCustomFieldPossibleValueDao(final CustomFieldPossibleValueDAO customFieldPossibleValueDAO) {
        customFieldPossibleValueDao = customFieldPossibleValueDAO;
    }

    public void setCustomFieldValueDao(final CustomFieldValueDAO customFieldValueDAO) {
        customFieldValueDao = customFieldValueDAO;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setOrder(final Nature nature, final Long[] fieldIds) {
        int order = 0;
        for (final Long id : fieldIds) {
            final CustomField field = load(id, CustomField.Relationships.CHILDREN);
            // We only iterate through root fields
            if (field.getParent() != null) {
                continue;
            }
            field.setOrder(++order);
            customFieldDao.update(field);
            // Then ensure children are right after their parents
            for (final CustomField child : field.getChildren()) {
                child.setOrder(++order);
                customFieldDao.update(child);
            }
        }
        if (nature != CustomField.Nature.OPERATOR) {
            // Update cached values
            final List<? extends CustomField> fields = customFieldDao.listByNature(nature);
            cachedCustomFieldsByNature.put(nature, new ArrayList<CustomField>(fields));
        }
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void validate(final CustomField field) {
        if (field instanceof MemberCustomField) {
            getMemberValidator().validate(field);
        } else if (field instanceof AdminCustomField) {
            getAdminValidator().validate(field);
        } else if (field instanceof OperatorCustomField) {
            getOperatorValidator().validate(field);
        } else if (field instanceof AdCustomField) {
            getAdValidator().validate(field);
        } else if (field instanceof PaymentCustomField) {
            getPaymentValidator().validate(field);
        } else if (field instanceof LoanGroupCustomField) {
            getLoanGroupValidator().validate(field);
        } else if (field instanceof MemberRecordCustomField) {
            getMemberRecordValidator().validate(field);
        }
    }

    public void validate(final CustomFieldPossibleValue possibleValue) {
        getPossibleValueValidator().validate(possibleValue);
    }

    @SuppressWarnings("unchecked")
    private void copyParentProperties(final CustomField parent, final CustomField child) {
        final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(parent);
        for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final String name = propertyDescriptor.getName();
            final boolean isWritable = propertyDescriptor.getWriteMethod() != null;
            final boolean isReadable = propertyDescriptor.getReadMethod() != null;
            if (isReadable && isWritable && !EXCLUDED_PROPERTIES_FOR_DEPENDENT_FIELDS.contains(name)) {
                Object value = PropertyHelper.get(parent, name);
                if (value instanceof Collection) {
                    value = new ArrayList<Object>((Collection<Object>) value);
                }
                PropertyHelper.set(child, name, value);
            }
        }
    }

    private Validator createBasicValidator() {
        final Validator validator = new Validator("customField");
        validator.property("internalName").required().maxLength(50).add(new JavaIdentifierValidation()).add(new UniqueCustomFieldInternalNameValidation());
        validator.property("name").required().maxLength(100);
        validator.property("type").required();
        validator.property("control").required();
        validator.property("size").required();
        validator.property("parent").add(new ParentValidator());
        validator.property("validation").add(new ValidatorClassValidation());

        return validator;
    }

    private int doMoveValues(CustomFieldPossibleValue oldValue, CustomFieldPossibleValue newValue) {
        oldValue = fetchService.fetch(oldValue);
        newValue = fetchService.fetch(newValue);
        if (!oldValue.getField().equals(newValue.getField())) {
            throw new ValidationException();
        }
        return customFieldValueDao.moveValues(oldValue, newValue);
    }

    private int doRemovePossibleValue(final Set<CustomField.Nature> expectedNatures, final Long... ids) {
        for (final Long id : ids) {
            final CustomFieldPossibleValue value = customFieldPossibleValueDao.load(id, CustomFieldPossibleValue.Relationships.FIELD);
            if (!expectedNatures.contains(value.getField().getNature())) {
                throw new UnexpectedEntityException();
            }
        }
        return customFieldPossibleValueDao.delete(ids);
    }

    private CustomFieldPossibleValue doSave(CustomFieldPossibleValue possibleValue, final CustomField.Nature expectedNature) {
        validate(possibleValue);
        final CustomField field = fetchService.fetch(possibleValue.getField());
        final Nature nature = field.getNature();
        if (expectedNature != null && nature != expectedNature) {
            throw new PermissionDeniedException();
        }
        try {
            if (possibleValue.isTransient()) {
                possibleValue = customFieldPossibleValueDao.insert(possibleValue);
            } else {
                possibleValue = customFieldPossibleValueDao.update(possibleValue);
            }
            customFieldPossibleValueDao.ensureDefault(possibleValue);
        } finally {
            if (nature == CustomField.Nature.MEMBER || nature == CustomField.Nature.AD || nature == CustomField.Nature.ADMIN || nature == CustomField.Nature.LOAN_GROUP) {
                cachedCustomFieldsByNature.remove(nature);
            }
        }
        return possibleValue;
    }

    private Validator getAdminValidator() {
        return createBasicValidator();
    }

    private Validator getAdValidator() {
        final Validator adValidator = createBasicValidator();
        adValidator.property("visibility").key("customField.ad.visibility").required();
        return adValidator;
    }

    private Validator getLoanGroupValidator() {
        return createBasicValidator();
    }

    private Validator getMemberRecordValidator() {
        final Validator memberRecordValidator = createBasicValidator();
        memberRecordValidator.property("memberRecordType").key("customField.memberRecord.memberRecordType");
        memberRecordValidator.property("showInSearch").key("customField.memberRecord.showInSearch");
        memberRecordValidator.property("showInList").key("customField.memberRecord.showInList");
        memberRecordValidator.property("brokerAccess").key("customField.memberRecord.brokerAccess");
        return memberRecordValidator;
    }

    private Validator getMemberValidator() {

        final Validator memberValidator = createBasicValidator();
        memberValidator.property("adSearchAccess").key("customField.member.adSearchAccess").required();
        memberValidator.property("loanSearchAccess").key("customField.member.loanSearchAccess").required();
        memberValidator.property("memberSearchAccess").key("customField.member.memberSearchAccess").required();
        memberValidator.property("indexing").key("customField.member.indexing").required();
        memberValidator.property("visibilityAccess").key("customField.member.visibilityAccess").required();
        memberValidator.property("updateAccess").key("customField.member.updateAccess").required();
        return memberValidator;
    }

    private Validator getOperatorValidator() {
        final Validator operatorValidator = createBasicValidator();
        operatorValidator.property("visibility").key("customField.operator.visibility").required();
        return operatorValidator;
    }

    private Validator getPaymentValidator() {
        final Validator paymentValidator = createBasicValidator();
        paymentValidator.property("transferType").required();
        return paymentValidator;
    }

    private Validator getPossibleValueValidator() {
        final Validator possibleValueValidator = new Validator("customField.possibleValue");
        possibleValueValidator.property("field").required();
        possibleValueValidator.property("value").required().maxLength(255);
        return possibleValueValidator;
    }

    private Validator getValueValidator(final Collection<? extends CustomField> fields) {
        final Validator validator = new Validator();

        for (CustomField field : fields) {
            field = fetchService.fetch(field);
            final Property property = validator.property(field.getInternalName(), new CustomFieldRetrievingStrategy(field));
            property.displayName(field.getName());

            switch (field.getType()) {
                case BOOLEAN:
                    property.anyOf("true", "false");
                    break;
                case INTEGER:
                    property.add(new IntegerValidator());
                    break;
                case DATE:
                    property.add(new DateValidator());
                    break;
                case ENUMERATED:
                    property.add(new EnumeratedValidator());
                    break;
                case BIG_DECIMAL:
                    property.add(new BigDecimalValidator());
                    break;
                case URL:
                    property.url();
                    break;
            }

            final Validation validation = field.getValidation();
            if (validation != null) {
                // Check required
                if (validation.isRequired()) {
                    property.required();
                }
                // Check length constraint
                final RangeConstraint lengthConstraint = validation.getLengthConstraint();
                if (lengthConstraint != null) {
                    property.add(new LengthValidation(lengthConstraint));
                }
                // Check unique
                if (validation.isUnique()) {
                    property.add(new UniqueFieldValueValidation());
                }
                // Validator class
                if (StringUtils.isNotEmpty(validation.getValidatorClass())) {
                    try {
                        final PropertyValidation validatorClass = (PropertyValidation) ClassHelper.instantiate(Class.forName(validation.getValidatorClass()));
                        property.add(validatorClass);
                    } catch (final ClassNotFoundException e) {
                        System.out.println("Validator not found!");
                    }
                }
            }
        }
        return validator;
    }

    private void saveValues(final EntityWithCustomFields<?, ?> owner) {
        final Collection<? extends CustomFieldValue> customValues = owner.getCustomValues();
        if (customValues == null || customValues.isEmpty()) {
            return;
        }

        for (final CustomFieldValue value : customValues) {
            // Retrieve the field value
            final CustomField field = fetchService.fetch(value.getField());
            value.setField(field);
            final boolean enumerated = field.getType() == CustomField.Type.ENUMERATED;
            CustomFieldPossibleValue possibleValue = null;
            String stringValue = null;
            if (enumerated) {
                // Load the possible value
                final Long possibleValueId = IdConverter.instance().valueOf(value.getValue());
                boolean tryByValue = possibleValueId == null;
                if (possibleValueId != null) {
                    // Try by id
                    try {
                        possibleValue = customFieldPossibleValueDao.load(possibleValueId);
                    } catch (final EntityNotFoundException e) {
                        tryByValue = true;
                    }
                }
                if (tryByValue && StringUtils.isNotEmpty(value.getValue())) {
                    // Try by field id + value
                    try {
                        possibleValue = customFieldPossibleValueDao.load(field.getId(), value.getValue());
                    } catch (final EntityNotFoundException e) {
                        final ValidationException vex = new ValidationException(field.getInternalName(), new InvalidError());
                        vex.setDisplayNameByProperty(Collections.singletonMap(field.getInternalName(), field.getName()));
                        throw vex;
                    }
                }
            } else {
                if ((field.getType() != CustomField.Type.STRING) || (field.getControl() != CustomField.Control.RICH_EDITOR)) {
                    stringValue = StringHelper.removeMarkupTags(value.getValue());
                } else {
                    stringValue = value.getValue();
                }

                // A String value
                stringValue = StringUtils.trimToNull(stringValue);
                if (StringUtils.isNotEmpty(field.getPattern())) {
                    stringValue = StringHelper.removeMask(field.getPattern(), stringValue);
                }
            }
            // Check if the value exists for the given owner
            try {
                final CustomFieldValue existing = customFieldValueDao.load(field, owner);
                // Exists - just update the value
                existing.setStringValue(stringValue);
                existing.setPossibleValue(possibleValue);
                if (value instanceof MemberCustomFieldValue) {
                    ((MemberCustomFieldValue) existing).setHidden(((MemberCustomFieldValue) value).isHidden());
                }
                customFieldValueDao.update(existing);
            } catch (final EntityNotFoundException e) {
                // Does not exists yet - insert a new value
                value.setOwner(owner);
                value.setStringValue(stringValue);
                value.setPossibleValue(possibleValue);
                if (value.isTransient()) {
                    customFieldValueDao.insert(value);
                } else {
                    customFieldValueDao.update(value);
                }
            }
        }
    }
}