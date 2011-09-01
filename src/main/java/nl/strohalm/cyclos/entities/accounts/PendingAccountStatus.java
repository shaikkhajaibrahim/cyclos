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
package nl.strohalm.cyclos.entities.accounts;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * 
 * 
 * @author luis
 */
public class PendingAccountStatus extends Entity {

    public static enum Relationships implements Relationship {
        TRANSFER("transfer"), SCHEDULED_PAYMENT("scheduledPayment"), ACCOUNT("account"), BY("by"), INVOICE("invoice"), TRANSFER_AUTHORIZATION("transferAuthorization");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static enum Type implements StringValuedEnum {
        PAYMENT("pmt"), RESERVED_SCHEDULED_PAYMENT("rsp"), LIMIT_CHANGE("lim"), ACCOUNT_FEE_DISABLED("afd"), ACCOUNT_FEE_INVOICE("afi"), AUTHORIZATION("aut"), LIBERATE_RESERVED_INSTALLMENT("lri");
        private final String value;

        private Type(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static final long     serialVersionUID = -5858316235087017163L;
    private Type                  type;
    private Calendar              date;
    private Account               account;
    private Transfer              transfer;
    private Payment.Status        transferStatus;
    private ScheduledPayment      scheduledPayment;
    private Invoice               invoice;
    private Element               by;
    private TransferAuthorization transferAuthorization;
    private BigDecimal            lowerLimit;
    private BigDecimal            upperLimit;
    private BigDecimal            subtractedAmount;

    public Account getAccount() {
        return account;
    }

    public Element getBy() {
        return by;
    }

    public Calendar getDate() {
        return date;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public BigDecimal getLowerLimit() {
        return lowerLimit;
    }

    public ScheduledPayment getScheduledPayment() {
        return scheduledPayment;
    }

    public BigDecimal getSubtractedAmount() {
        return subtractedAmount;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public TransferAuthorization getTransferAuthorization() {
        return transferAuthorization;
    }

    public Payment.Status getTransferStatus() {
        return transferStatus;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getUpperLimit() {
        return upperLimit;
    }

    public void setAccount(final Account account) {
        this.account = account;
    }

    public void setBy(final Element by) {
        this.by = by;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setInvoice(final Invoice invoice) {
        this.invoice = invoice;
    }

    public void setLowerLimit(final BigDecimal lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public void setScheduledPayment(final ScheduledPayment scheduledPayment) {
        this.scheduledPayment = scheduledPayment;
    }

    public void setSubtractedAmount(final BigDecimal subtractedAmount) {
        this.subtractedAmount = subtractedAmount;
    }

    public void setTransfer(final Transfer transfer) {
        this.transfer = transfer;
    }

    public void setTransferAuthorization(final TransferAuthorization transferAuthorization) {
        this.transferAuthorization = transferAuthorization;
    }

    public void setTransferStatus(final Payment.Status transferStatus) {
        this.transferStatus = transferStatus;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public void setUpperLimit(final BigDecimal upperLimit) {
        this.upperLimit = upperLimit;
    }

    @Override
    public String toString() {
        return transfer + "";
    }

}
