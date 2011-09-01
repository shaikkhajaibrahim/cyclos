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
package nl.strohalm.cyclos.controls.accounts.details;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters;

import org.apache.commons.lang.StringUtils;

/**
 * Action used to print transactions
 * @author luis
 */
public class PrintAccountHistoryAction extends AccountHistoryAction {

    @Override
    protected Integer pageSize(final ActionContext context) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected QueryParameters prepareForm(final ActionContext context) {
        final TransferQuery query = (TransferQuery) super.prepareForm(context);
        final HttpServletRequest request = context.getRequest();

        // Fetch the data to show on the screen
        if (query.getPaymentFilter() != null) {
            query.setPaymentFilter(getFetchService().fetch(query.getPaymentFilter()));
        }
        if (query.getMember() != null) {
            query.setMember(getFetchService().fetch(query.getMember()));
        }
        if (query.getBy() != null) {
            query.setBy(getFetchService().fetch(query.getBy()));
        }

        // Get the period begin date
        final Period period = query.getPeriod();
        final GetTransactionsDTO dto = new GetTransactionsDTO(query.getOwner(), query.getType(), period);
        dto.setRelatedToMember(query.getMember());
        dto.setPaymentFilter(query.getPaymentFilter());
        dto.setBy(query.getBy());

        // Get additional data for the printing
        final AccountStatus status = (AccountStatus) request.getAttribute("status");
        if (period != null && period.getBegin() != null) {
            final AccountStatus initialStatus = accountService.getStatus(new GetTransactionsDTO(status.getAccount(), Period.begginingAt(period.getBegin())), false);
            request.setAttribute("initialStatus", initialStatus);
        }
        if (period != null && period.getEnd() != null) {
            final AccountStatus finalStatus = accountService.getStatus(new GetTransactionsDTO(status.getAccount(), period), false);
            request.setAttribute("finalStatus", finalStatus);
        }

        // Get the custom fields search map
        final Map<String, String> customValueFilters = new LinkedHashMap<String, String>();
        final Collection<nl.strohalm.cyclos.utils.CustomFieldHelper.Entry> entries = (Collection<nl.strohalm.cyclos.utils.CustomFieldHelper.Entry>) request.getAttribute("customFieldsForSearch");
        for (final nl.strohalm.cyclos.utils.CustomFieldHelper.Entry entry : entries) {
            final CustomField field = entry.getField();
            final CustomFieldValue fieldValue = entry.getValue();
            if (fieldValue == null) {
                continue;
            }
            String value;
            if (field.getType() == CustomField.Type.ENUMERATED) {
                // The filter may be done using a list of comma-separated identifiers. Get the value for them
                final String[] parts = StringUtils.split(fieldValue.getValue(), ",");
                if (parts == null) {
                    continue;
                }
                Collection<CustomFieldPossibleValue> possibleValues = CoercionHelper.coerceCollection(CustomFieldPossibleValue.class, parts);
                possibleValues = getFetchService().fetch(possibleValues);
                boolean first = true;
                final StringBuilder sb = new StringBuilder();
                for (final CustomFieldPossibleValue possibleValue : possibleValues) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(possibleValue.getValue());
                }
                value = sb.toString();
            } else {
                value = fieldValue.getValue();
            }
            customValueFilters.put(field.getName(), value);
        }
        request.setAttribute("customValueFilters", customValueFilters);

        return query;
    }

}