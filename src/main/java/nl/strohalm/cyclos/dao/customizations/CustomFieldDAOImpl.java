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
package nl.strohalm.cyclos.dao.customizations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberRecordCustomField;
import nl.strohalm.cyclos.entities.customization.fields.OperatorCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Nature;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Implementation class for custom field DAO
 * @author rafael
 */
public class CustomFieldDAOImpl extends BaseDAOImpl<CustomField> implements CustomFieldDAO {

    public CustomFieldDAOImpl() {
        super(CustomField.class);
    }

    public boolean isInternalNameUsed(final CustomField field) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("_field_", field);
        final StringBuilder hql = new StringBuilder("select count(*) from ");
        hql.append(field.getNature().getEntityType().getName());
        hql.append(" cf where " + (field.isPersistent() ? "cf <> :_field_" : "1=1"));

        switch (field.getNature()) {
            case OPERATOR:
                HibernateHelper.addParameterToQuery(hql, namedParameters, "cf.member", ((OperatorCustomField) field).getMember());
                break;
            case MEMBER_RECORD:
                HibernateHelper.addParameterToQuery(hql, namedParameters, "cf.memberRecordType", ((MemberRecordCustomField) field).getMemberRecordType());
                break;
            default:
                // global search
        }
        HibernateHelper.addParameterToQuery(hql, namedParameters, "cf.internalName", field.getInternalName());
        final Integer count = uniqueResult(hql.toString(), namedParameters);
        return count > 0;
    }

    public List<? extends CustomField> listByNature(final Nature nature) {
        final StringBuilder hql = HibernateHelper.getInitialQuery(nature.getEntityType(), "f");
        HibernateHelper.appendOrder(hql, "f.order");
        return list(ResultType.LIST, hql.toString(), null, null, CustomField.Relationships.POSSIBLE_VALUES, MemberCustomField.Relationships.GROUPS);
    }

    public List<MemberRecordCustomField> listMemberRecordFields(final MemberRecordType memberRecordType) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = HibernateHelper.getInitialQuery(MemberRecordCustomField.class, "f");
        HibernateHelper.addParameterToQuery(hql, namedParameters, "f.memberRecordType", memberRecordType);
        HibernateHelper.appendOrder(hql, "f.order");
        return list(ResultType.LIST, hql.toString(), namedParameters, null, CustomField.Relationships.POSSIBLE_VALUES);
    }

    public List<OperatorCustomField> listOperatorFields(final Member member) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = HibernateHelper.getInitialQuery(OperatorCustomField.class, "f");
        HibernateHelper.addParameterToQuery(hql, namedParameters, "f.member", member);
        HibernateHelper.appendOrder(hql, "f.order");
        return list(ResultType.LIST, hql.toString(), namedParameters, null, CustomField.Relationships.POSSIBLE_VALUES, MemberCustomField.Relationships.GROUPS);
    }

    public List<PaymentCustomField> listPaymentFieldForList(final Account account, final boolean loan) {
        return listPaymentFieldsInternal(account, "listAccess", loan);
    }

    public List<PaymentCustomField> listPaymentFieldForSearch(final Account account, final boolean loan) {
        return listPaymentFieldsInternal(account, "searchAccess", loan);
    }

    public List<PaymentCustomField> listPaymentFields(final TransferType transferType) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("transferType", transferType);
        final StringBuilder hql = HibernateHelper.getInitialQuery(PaymentCustomField.class, "f");
        hql.append(" and (f.transferType = :transferType or exists (select tt.id from TransferType tt where tt = :transferType and f in elements(tt.linkedCustomFields)))");
        // Ensure the owned fields will have lowest order. The others will have a very high order
        HibernateHelper.appendOrder(hql, "case f.transferType.id when " + transferType.getId() + " then f.order else (999999999 * f.order) end");
        return list(ResultType.LIST, hql.toString(), namedParameters, null, CustomField.Relationships.POSSIBLE_VALUES);
    }

    private List<PaymentCustomField> listPaymentFieldsInternal(final Account account, final String accessProperty, final boolean loan) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("accountType", account.getType());
        final AccountOwner owner = account.getOwner();
        if (owner instanceof Member) {
            namedParameters.put("ownerMember", owner);
        }
        for (final PaymentCustomField.Access access : PaymentCustomField.Access.values()) {
            namedParameters.put(access.name(), access);
        }
        final StringBuilder hql = HibernateHelper.getInitialQuery(PaymentCustomField.class, "f");
        hql.append(" and (");
        hql.append(" (f." + accessProperty + " = :FROM_ACCOUNT and f.transferType.from = :accountType)");
        hql.append(" or (f." + accessProperty + " = :TO_ACCOUNT and f.transferType.to = :accountType)");
        hql.append(" or (f." + accessProperty + " = :BOTH_ACCOUNTS and (f.transferType.from = :accountType or f.transferType.to = :accountType))");
        hql.append(" or (f." + accessProperty + " = :DESTINATION_MEMBER and f.transferType.fixedDestinationMember = :ownerMember)");
        hql.append(" ) ");
        hql.append(" and f.transferType.loan.type is " + (loan ? "not null" : "null"));
        HibernateHelper.appendOrder(hql, "f.transferType.name", "f.order");
        return list(ResultType.LIST, hql.toString(), namedParameters, null, CustomField.Relationships.POSSIBLE_VALUES);
    }

}
