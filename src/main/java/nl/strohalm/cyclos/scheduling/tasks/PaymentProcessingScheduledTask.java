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

import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Scheduled task used to process scheduled payments
 * @author luis
 */
public class PaymentProcessingScheduledTask extends BaseScheduledTask {

    private PaymentService paymentService;

    public PaymentProcessingScheduledTask() {
        super("Payment processing", false);
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    protected void doRun(final Calendar time) {
        final TransferQuery query = new TransferQuery();
        query.setResultType(ResultType.ITERATOR);
        query.setPeriod(Period.day(time));
        query.setStatus(Payment.Status.SCHEDULED);
        query.setUnordered(true);
        final List<Transfer> transfers = paymentService.search(query);
        for (final Transfer transfer : transfers) {
            paymentService.processScheduled(transfer, time);
        }
    }

}
