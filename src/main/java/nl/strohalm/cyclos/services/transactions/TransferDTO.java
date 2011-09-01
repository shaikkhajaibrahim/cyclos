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
package nl.strohalm.cyclos.services.transactions;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.loans.LoanPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Ticket;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Parameters for a loan repayment
 * @author luis
 */
public class TransferDTO extends DataObject {

    private static final long                   serialVersionUID = 4615832057413419970L;

    private AccountFeeLog                       accountFeeLog;
    private BigDecimal                          amount;
    private boolean                             automatic;
    private String                              channel;
    private TransactionContext                  context;
    private Calendar                            date;
    private String                              description;
    private Currency                            currency;
    private boolean                             forced;
    private Element                             by;
    private Account                             from;
    private AccountOwner                        fromOwner;
    private LoanPayment                         loanPayment;
    private Account                             to;
    private AccountOwner                        toOwner;
    private TransferType                        transferType;
    private Ticket                              ticket;
    private Transfer                            parent;
    private Element                             receiver;
    private ExternalTransfer                    externalTransfer;
    private List<ScheduledPaymentDTO>           payments;
    private ScheduledPayment                    scheduledPayment;
    private Collection<PaymentCustomFieldValue> customValues;
    private BigDecimal                          dRate;
    /**
     * the internal storage field for the A-rate.
     */
    private Calendar                            emissionDate;

    private String                              traceNumber;

    /**
     * The client id wich generated the trace number
     */
    private Long                                clientId;

    public AccountFeeLog getAccountFeeLog() {
        return accountFeeLog;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Element getBy() {
        return by;
    }

    public String getChannel() {
        return channel;
    }

    public Long getClientId() {
        return clientId;
    }

    public TransactionContext getContext() {
        return context;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Collection<PaymentCustomFieldValue> getCustomValues() {
        return customValues;
    }

    public Calendar getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDRate() {
        return dRate;
    }

    public Calendar getEmissionDate() {
        return emissionDate;
    }

    public ExternalTransfer getExternalTransfer() {
        return externalTransfer;
    }

    public Account getFrom() {
        return from;
    }

    public AccountOwner getFromOwner() {
        return fromOwner;
    }

    public LoanPayment getLoanPayment() {
        return loanPayment;
    }

    public Transfer getParent() {
        return parent;
    }

    public List<ScheduledPaymentDTO> getPayments() {
        return payments;
    }

    public Element getReceiver() {
        return receiver;
    }

    public ScheduledPayment getScheduledPayment() {
        return scheduledPayment;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Account getTo() {
        return to;
    }

    public AccountOwner getToOwner() {
        return toOwner;
    }

    public String getTraceNumber() {
        return traceNumber;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public boolean isForced() {
        return forced;
    }

    public void setAccountFeeLog(final AccountFeeLog accountFeeLog) {
        this.accountFeeLog = accountFeeLog;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setAutomatic(final boolean automatic) {
        this.automatic = automatic;
    }

    public void setBy(final Element by) {
        this.by = by;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public void setClientId(final Long clientId) {
        this.clientId = clientId;
    }

    public void setContext(final TransactionContext context) {
        this.context = context;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setCustomValues(final Collection<PaymentCustomFieldValue> customValues) {
        this.customValues = customValues;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setDRate(final BigDecimal dRate) {
        this.dRate = dRate;
    }

    public void setEmissionDate(final Calendar emissionDate) {
        this.emissionDate = emissionDate;
    }

    public void setExternalTransfer(final ExternalTransfer externalTransfer) {
        this.externalTransfer = externalTransfer;
    }

    public void setForced(final boolean forced) {
        this.forced = forced;
    }

    public void setFrom(final Account from) {
        this.from = from;
    }

    public void setFromOwner(final AccountOwner fromOwner) {
        this.fromOwner = fromOwner;
    }

    public void setLoanPayment(final LoanPayment loanPayment) {
        this.loanPayment = loanPayment;
    }

    public void setParent(final Transfer parent) {
        this.parent = parent;
    }

    public void setPayments(final List<ScheduledPaymentDTO> payments) {
        this.payments = payments;
    }

    public void setReceiver(final Element receiver) {
        this.receiver = receiver;
    }

    public void setScheduledPayment(final ScheduledPayment scheduledPayment) {
        this.scheduledPayment = scheduledPayment;
    }

    public void setTicket(final Ticket ticket) {
        this.ticket = ticket;
    }

    public void setTo(final Account to) {
        this.to = to;
    }

    public void setToOwner(final AccountOwner toOwner) {
        this.toOwner = toOwner;
    }

    public void setTraceNumber(final String traceNumber) {
        this.traceNumber = traceNumber;

    }

    public void setTransferType(final TransferType transferType) {
        this.transferType = transferType;
    }

    public DoPaymentDTO toPaymentDTO() {
        final DoPaymentDTO dto = new DoPaymentDTO();
        dto.setTransferType(transferType);
        dto.setTo(to.getOwner());
        dto.setAmount(amount);
        dto.setContext(context);
        dto.setDate(date);
        dto.setDescription(description);
        dto.setTicket(ticket);
        return dto;
    }

}