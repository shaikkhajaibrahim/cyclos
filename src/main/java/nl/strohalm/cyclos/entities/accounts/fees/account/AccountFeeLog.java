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
package nl.strohalm.cyclos.entities.accounts.fees.account;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.utils.Amount;
import nl.strohalm.cyclos.utils.FormatObject;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.StringValuedEnum;
import nl.strohalm.cyclos.utils.TimePeriod;

/**
 * A tax log records a tax execution
 * @author luis
 */
public class AccountFeeLog extends Entity {

    public static enum Relationships implements Relationship {
        ACCOUNT_FEE("accountFee"), TRANSFERS("transfers"), INVOICES("invoices");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Status implements StringValuedEnum {
        SCHEDULED("S", false), FINISHED("F", false), RUNNING("R", false), PARTIALLY_FAILED("X", true), CANCELED("C", true), NEVER_RAN("N", true);

        private final String  value;
        private final boolean canRecharge;

        private Status(final String value, final boolean canRecharge) {
            this.value = value;
            this.canRecharge = canRecharge;
        }

        public String getValue() {
            return value;
        }

        public boolean isCanRecharge() {
            return canRecharge;
        }
    }

    private static final long    serialVersionUID = -1715437658356438694L;

    private AccountFee           accountFee;
    private Calendar             date;
    private BigDecimal           freeBase;
    private TimePeriod           tolerance;
    private Period               period;
    private Status               status;
    private BigDecimal           amount;
    private Collection<Transfer> transfers;
    private Collection<Invoice>  invoices;

    public AccountFeeLog() {
    }

    public AccountFee getAccountFee() {
        return accountFee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Amount getAmountValue() {
        final Amount amount = new Amount();
        amount.setType(accountFee.getChargeMode().getAmountType());
        amount.setValue(this.amount);
        return amount;
    }

    public Calendar getDate() {
        return date;
    }

    public BigDecimal getFreeBase() {
        return freeBase;
    }

    public Collection<Invoice> getInvoices() {
        return invoices;
    }

    public Period getPeriod() {
        return period;
    }

    public Status getStatus() {
        return status;
    }

    public TimePeriod getTolerance() {
        return tolerance;
    }

    public Collection<Transfer> getTransfers() {
        return transfers;
    }

    public void setAccountFee(final AccountFee accountFee) {
        this.accountFee = accountFee;
    }

    public void setAmount(final BigDecimal value) {
        amount = value;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setFreeBase(final BigDecimal freeBase) {
        this.freeBase = freeBase;
    }

    public void setInvoices(final Collection<Invoice> invoices) {
        this.invoices = invoices;
    }

    public void setPeriod(final Period period) {
        this.period = period;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setTolerance(final TimePeriod tolerance) {
        this.tolerance = tolerance;
    }

    public void setTransfers(final Collection<Transfer> transfers) {
        this.transfers = transfers;
    }

    @Override
    public String toString() {
        return getId() + " - " + accountFee + " at " + FormatObject.formatObject(date) + ": " + status;
    }

}