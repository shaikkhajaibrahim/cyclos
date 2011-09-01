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

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroup;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.imports.ImportedAd;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomField;
import nl.strohalm.cyclos.entities.customization.fields.OperatorCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Nature;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.members.imports.ImportedMember;
import nl.strohalm.cyclos.entities.members.imports.ImportedMemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecord;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.exceptions.SendingInvoiceWithMultipleTransferTypesWithCustomFields;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.Validator;

/**
 * Service interface for customized fields
 * @author luis
 */
public interface CustomFieldService extends Service {

    /**
     * Clears the cache of custom fields of the given nature
     */
    @SystemAction
    void clearCache(CustomField.Nature nature);

    /**
     * Returns an admin custom field validator for the given group
     */
    @SystemAction
    Validator getAdminValueValidator(AdminGroup group);

    /**
     * Returns an ad custom field validator
     */
    @SystemAction
    Validator getAdValueValidator();

    /**
     * Returns a loan group custom field validator
     */
    Validator getLoanGroupValueValidator();

    /**
     * Returns a member custom field validator for the given group
     */
    Validator getMemberValueValidator(MemberGroup group, MemberCustomField.Access access);

    /**
     * Returns an operator custom field validator for the given group
     */
    Validator getOperatorValueValidator(Member member);

    /**
     * Returns a payment custom field validator
     */
    Validator getPaymentValueValidator(TransferType transferType);

    /**
     * Links the transfer type with the payment custom field
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    void linkPaymentCustomField(TransferType transferType, PaymentCustomField customField);

    /**
     * List all custom fields of the given nature
     */
    List<? extends CustomField> listByNature(CustomField.Nature nature);

    /**
     * Lists all member record custom fields for the member record type
     */
    List<MemberRecordCustomField> listMemberRecordFields(MemberRecordType memberRecordType);

    /**
     * Lists all operator custom fields for the member
     */
    List<OperatorCustomField> listOperatorFields(Member member);

    /**
     * Lists payment custom fields which are visible result column for the given account history. The second argument is a flag indicating whether the
     * custom fields are to be shown under loan details or normal account history
     */
    List<PaymentCustomField> listPaymentFieldForList(Account account, boolean loan);

    /**
     * Lists payment custom fields which are visible as search filters for the given account history. The second argument is a flag indicating whether
     * the custom fields are to be shown under loan details or normal account history
     */
    List<PaymentCustomField> listPaymentFieldForSearch(Account account, boolean loan);

    /**
     * Lists all payment custom fields for the transfer type
     */
    List<PaymentCustomField> listPaymentFields(TransferType transferType);

    /**
     * Lists the custom fields which may be set as parent fields of the given one
     */
    <CF extends CustomField> List<CF> listPossibleParentFields(CF field);

    /**
     * Loads the specified custom field, with the specified fetch
     */
    CustomField load(Long id, Relationship... fetch);

    /**
     * Loads the specified custom field possible value, with the specified fetch
     */
    CustomFieldPossibleValue loadPossibleValue(Long id, Relationship... fetch);

    /**
     * Changes all values from the old to new
     */
    @AdminAction(@Permission(module = "systemMemberRecordTypes", operation = "manage"))
    int moveMemberRecordValues(CustomFieldPossibleValue oldValue, CustomFieldPossibleValue newValue);

    /**
     * Changes all values from the old to new
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @RelatedEntity(OperatorCustomField.class)
    @PathToMember("field.member")
    int moveOperatorValues(CustomFieldPossibleValue oldValue, CustomFieldPossibleValue newValue);

    /**
     * Changes all values from the old to new
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    int movePaymentValues(CustomFieldPossibleValue oldValue, CustomFieldPossibleValue newValue);

    /**
     * Changes all values from the old to new
     */
    @AdminAction(@Permission(module = "systemCustomFields", operation = "manage"))
    int moveValues(CustomFieldPossibleValue oldValue, CustomFieldPossibleValue newValue);

    /**
     * Removes the specified custom fields, returning the number of removed objects
     */
    @AdminAction(@Permission(module = "systemCustomFields", operation = "manage"))
    int remove(Long... ids);

    /**
     * Removes a member record custom field
     */
    @AdminAction(@Permission(module = "systemMemberRecordTypes", operation = "manage"))
    int removeMemberRecordField(Long... ids);

    /**
     * Removes a member record custom field possible value
     */
    @AdminAction(@Permission(module = "systemMemberRecordTypes", operation = "manage"))
    int removeMemberRecordPossibleValue(Long... ids);

    /**
     * Removes the specified operator custom fields, returning the number of removed objects
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @RelatedEntity(OperatorCustomField.class)
    @PathToMember("member")
    int removeOperatorField(Long... ids);

    /**
     * Removes a possible operator custom field value
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @RelatedEntity(CustomFieldPossibleValue.class)
    @PathToMember("field.member")
    int removeOperatorPossibleValue(Long... id);

    /**
     * Removes or unlinks the given custom field from the given transfer type
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    void removePaymentCustomField(TransferType transferType, PaymentCustomField customField);

    /**
     * Removes a possible value from a payment custom field
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    int removePaymentPossibleValue(Long... ids);

    /**
     * Removes a possible value
     */
    @AdminAction(@Permission(module = "systemCustomFields", operation = "manage"))
    int removePossibleValue(Long... id);

    /**
     * Saves a possible value
     */
    @AdminAction(@Permission(module = "systemCustomFields", operation = "manage"))
    CustomFieldPossibleValue save(CustomFieldPossibleValue possibleValue);

    /**
     * Saves the specified custom field, returning the resulting object. The internalName and type properties cannot be modified. The control property
     * depends on the type:
     * <ul>
     * <li>Boolean can only be Checkbox</li>
     * <li>String can only be Text or TextArea</li>
     * <li>Numbers and Dates can only be Text</li>
     * <li>Enumerated can only be Select or Radio</li>
     * </ul>
     * Also, the pattern can only be applied to String fields, and allSelectedLabel to Enumerated.
     */
    @AdminAction(@Permission(module = "systemCustomFields", operation = "manage"))
    <F extends CustomField> F save(F customField);

    /**
     * Saves the specified member record custom field, returning the resulting object.
     * @see #save(CustomField)
     */
    @AdminAction(@Permission(module = "systemMemberRecordTypes", operation = "manage"))
    MemberRecordCustomField save(MemberRecordCustomField memberRecordCustomField);

    /**
     * Saves the specified operator custom field, returning the resulting object.
     * @see #save(CustomField)
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("member")
    OperatorCustomField save(OperatorCustomField operatorCustomField);

    /**
     * Saves the specified payment custom field, returning the resulting object.
     * @see #save(CustomField)
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    PaymentCustomField save(PaymentCustomField paymentCustomField);

    /**
     * Saves a collection of admin custom field values
     */
    @SystemAction
    void saveAdminValues(Administrator admin);

    /**
     * Saves a collection of ad custom field values
     */
    @SystemAction
    void saveAdValues(Ad ad);

    /**
     * Saves a collection of imported ad field values
     */
    @SystemAction
    void saveImportedAdValues(ImportedAd importedAd);

    /**
     * Saves values for an imported member record
     */
    @SystemAction
    void saveImportedMemberRecordValues(ImportedMemberRecord record);

    /**
     * Saves a collection of imported members field values
     */
    @SystemAction
    void saveImportedMemberValues(ImportedMember importedMember);

    /**
     * Saves a collection of loan group custom field values
     */
    @SystemAction
    void saveLoanGroupValues(LoanGroup loanGroup);

    /**
     * Saves a member record possible value
     */
    @AdminAction(@Permission(module = "systemMemberRecordTypes", operation = "manage"))
    CustomFieldPossibleValue saveMemberRecord(CustomFieldPossibleValue possibleValue);

    /**
     * Saves a collection of member record custom field values
     */
    @SystemAction
    void saveMemberRecordValues(final MemberRecord memberRecord);

    /**
     * Saves a collection of member custom field values
     */
    @SystemAction
    void saveMemberValues(RegisteredMember registeredMember);

    /**
     * Saves a possible operator custom field value
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("field.member")
    CustomFieldPossibleValue saveOperator(CustomFieldPossibleValue possibleValue);

    /**
     * Saves a collection of operator custom field values
     */
    @SystemAction
    void saveOperatorValues(Operator operator);

    /**
     * Saves a payment custom field possible value
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    CustomFieldPossibleValue savePayment(CustomFieldPossibleValue possibleValue);

    /**
     * Saves the custom values for a guarantee
     */
    @SystemAction
    void savePaymentValues(Guarantee guarantee, boolean validate);

    /**
     * Saves the custom values for an invoice. This method also ensures that, in invoices from member to member (with only a destination account
     * type), when multiple TTs are possible and at least one have custom fields, a {@link SendingInvoiceWithMultipleTransferTypesWithCustomFields} is
     * thrown
     * @throws SendingInvoiceWithMultipleTransferTypesWithCustomFields When there are more than one possible TTs and at least one have custom fields
     */
    @SystemAction
    void savePaymentValues(Invoice invoice) throws SendingInvoiceWithMultipleTransferTypesWithCustomFields;

    /**
     * Saves the custom values for a payment
     */
    @SystemAction
    void savePaymentValues(Payment payment);

    /**
     * Set the custom field order
     */
    void setOrder(Nature nature, Long[] fieldIds);

    /**
     * Validates the custom field
     */
    @DontEnforcePermission(traceable = true)
    void validate(CustomField field);

    /**
     * Validates a possible value
     */
    @DontEnforcePermission(traceable = true)
    void validate(CustomFieldPossibleValue possibleValue);

}