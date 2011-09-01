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
package nl.strohalm.cyclos.controls.loans;

import java.util.List;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseCsvAction;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.conversion.CollectionCountConverter;
import nl.strohalm.cyclos.utils.conversion.CustomFieldConverter;
import nl.strohalm.cyclos.utils.csv.CSVWriter;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Action used to export loans to csv
 * @author luis
 */
public class ExportLoansToCsvAction extends BaseCsvAction {

    private DataBinder<LoanQuery> dataBinder;
    private LoanService           loanService;
    private CustomFieldService    customFieldService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public DataBinder<LoanQuery> getDataBinder() {
        if (dataBinder == null) {
            dataBinder = SearchLoansAction.loanQueryDataBinder(getLocalSettings());
        }
        return dataBinder;
    }

    public LoanService getLoanService() {
        return loanService;
    }

    @Override
    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        super.onLocalSettingsUpdate(event);
        dataBinder = null;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    @Override
    protected List<?> executeQuery(final ActionContext context) {
        final SearchLoansForm form = context.getForm();
        final LoanQuery query = getDataBinder().readFromString(form.getQuery());
        query.setResultType(ResultType.ITERATOR);
        query.fetch(Loan.Relationships.PAYMENTS, RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.TYPE), RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.CUSTOM_VALUES), RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER, Element.Relationships.USER));
        return loanService.search(query);
    }

    @Override
    protected String fileName(final ActionContext context) {
        final User loggedUser = context.getUser();
        return "loans_" + loggedUser.getUsername() + ".csv";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CSVWriter initCsvWriter(final ActionContext context) {
        final LocalSettings settings = getLocalSettings();
        final CSVWriter<Loan> csv = CSVWriter.instance(Loan.class, settings);
        final SearchLoansForm form = context.getForm();
        final LoanQuery query = getDataBinder().readFromString(form.getQuery());
        final TransferType transferType = query.getTransferType();
        csv.addColumn(context.message("loan.grantDate"), "transfer.date", settings.getDateTimeConverter());
        csv.addColumn(context.message("loan.amount"), "transfer.amount", settings.getNumberConverter());
        csv.addColumn(context.message("loan.type"), "transfer.type.name");
        csv.addColumn(context.message("member.username"), "transfer.to.member.username");
        csv.addColumn(context.message("member.member"), "transfer.to.member.name");
        csv.addColumn(context.message("loan.description"), "transfer.description");
        csv.addColumn(context.message("loan.totalAmount"), "totalAmount", settings.getNumberConverter());
        csv.addColumn(context.message("loan.remainingAmount"), "remainingAmount", settings.getNumberConverter());
        csv.addColumn(context.message("loan.firstOpenPayment"), "firstOpenPayment.index");
        csv.addColumn(context.message("loan.paymentCount"), "payments", CollectionCountConverter.instance());
        csv.addColumn(context.message("loan.expirationDate"), "expirationDate", settings.getRawDateConverter());
        csv.addColumn(context.message("loan.repaymentDate"), "repaymentDate", settings.getDateConverter());
        // Get the custom fields
        if (transferType != null) {
            final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transferType);
            for (final PaymentCustomField field : customFields) {
                csv.addColumn(field.getName(), "transfer.customValues", new CustomFieldConverter(field));
            }
        }
        return csv;
    }

    @Override
    protected boolean useCsvWriterPerRequest() {
        return true;
    }
}
