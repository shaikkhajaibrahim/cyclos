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
package nl.strohalm.cyclos.scheduling.tasks;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery.Direction;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Scheduled task used to expire invoices with expired scheduled payments
 * @author Jefferson Magno
 */
public class InvoiceExpirationScheduledTask extends BaseScheduledTask {

    private InvoiceService invoiceService;

    public InvoiceExpirationScheduledTask() {
        super("Invoice expiration", false);
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    protected void doRun(final Calendar time) {
        // List the invoices with expired scheduled payments
        final InvoiceQuery query = new InvoiceQuery();
        query.fetch(RelationshipHelper.nested(Invoice.Relationships.DESTINATION_ACCOUNT_TYPE, AccountType.Relationships.CURRENCY));
        query.setPaymentPeriod(Period.endingAt(DateHelper.truncatePreviosDay(time)));
        query.setDirection(Direction.OUTGOING);
        query.setStatus(Invoice.Status.OPEN);
        query.setResultType(ResultType.ITERATOR);
        final List<Invoice> invoices = invoiceService.search(query);
        for (final Invoice invoice : invoices) {
            invoiceService.expireInvoice(invoice);
        }
    }

}
