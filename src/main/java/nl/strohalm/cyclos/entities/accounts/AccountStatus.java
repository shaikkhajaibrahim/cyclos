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
import java.util.Map;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.accounts.rates.RatesDTO;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.FormatObject;

/**
 * Contains the account status history on a given date. Note that accountstatus cannot be use for retrieving actual rates, as business logic is needed
 * fot that. Use RateService for this.
 * 
 * @author luis
 */
public abstract class AccountStatus extends Entity {

    /**
     * class for accessing the D-rated fields in one go.
     * @author Rinke
     */
    public static class DRateFields {
        private BigDecimal rate;
        private Calendar   date;

        private DRateFields(final BigDecimal rate, final Calendar date) {
            this.rate = rate;
            this.date = date;
        }

        public Calendar getDate() {
            return date;
        }

        public BigDecimal getRate() {
            return rate;
        }

    }

    public static enum Relationships implements Relationship {
        ACCOUNT("account"), TRANSFER("transfer"), SCHEDULED_PAYMENT("scheduledPayment"), TRANSFER_AUTHORIZATION("transferAuthorization"), CREDIT_LIMIT_CHANGED_BY("creditLimitChangedBy");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long     serialVersionUID          = -82714038288128617L;

    private Account               account;
    private Calendar              date;
    private TransactionSummaryVO  rootCredits               = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private TransactionSummaryVO  rootDebits                = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private TransactionSummaryVO  nestedCredits             = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private TransactionSummaryVO  nestedDebits              = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private TransactionSummaryVO  pendingDebits             = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private TransactionSummaryVO  reservedScheduledPayments = new TransactionSummaryVO(0, BigDecimal.ZERO);
    private BigDecimal            creditLimit               = BigDecimal.ZERO;
    private BigDecimal            upperCreditLimit          = BigDecimal.ZERO;
    private Transfer              transfer;
    private ScheduledPayment      scheduledPayment;
    private TransferAuthorization transferAuthorization;
    private Element               creditLimitChangedBy;
    private BigDecimal            dRate;
    /**
     * the date on which the dRate field was last updated.
     */
    private Calendar              lastDRateUpdate;
    /**
     * the emissiondate needed for the a-rate
     */
    private Calendar              emissionDate;

    /**
     * the correction used on balance for rate calculations. Rate calculations cannot handle negative balances, so if ever an account has gone below
     * zero, the balance must be corrected with this field before applying rate calculations.
     */
    private BigDecimal            rateBalanceCorrection;

    public AccountStatus() {
    }

    public AccountStatus(final Account account) {
        this.account = account;
        if (account.getCreditLimit() == null) {
            creditLimit = null;
        }
    }

    /**
     * copies the rate fields from one status to another.
     * @param status the status from which the fields are copied
     */
    public void copyRateFieldsFrom(final AccountStatus status) {
        dRate = status.dRate;
        rateBalanceCorrection = status.rateBalanceCorrection;
        emissionDate = status.emissionDate == null ? null : (Calendar) status.emissionDate.clone();
        lastDRateUpdate = status.lastDRateUpdate == null ? null : (Calendar) status.lastDRateUpdate.clone();

    }

    public Account getAccount() {
        return account;
    }

    /**
     * The available balance is <code>available balance without credit limit + credit limit</code>.<br>
     * Unlimited accounts have available balance = null
     */
    public BigDecimal getAvailableBalance() {
        if (creditLimit == null) {
            return null;
        }
        return getAvailableBalanceWithoutCreditLimit().add(creditLimit);
    }

    /**
     * The available balance without credit limit is <code>balance - reserved amount</code>.
     */
    public BigDecimal getAvailableBalanceWithoutCreditLimit() {
        return getBalance().subtract(getReservedAmount());
    }

    /**
     * The balance is <code>total debits - total credits</code>
     */
    public BigDecimal getBalance() {
        return getCredits().getAmount().subtract(getDebits().getAmount());
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public Element getCreditLimitChangedBy() {
        return creditLimitChangedBy;
    }

    public TransactionSummaryVO getCredits() {
        return rootCredits.add(nestedCredits);
    }

    public Calendar getDate() {
        return date;
    }

    public TransactionSummaryVO getDebits() {
        return rootDebits.add(nestedDebits);
    }

    /**
     * getter to get both d-rated fields together in one go. Use this in stead of the single getters for the individual fields (dRate and
     * lastDRateUpdate).
     * @return A DRateFields object containing dRate and lastDRateUpdate.
     */
    public DRateFields getDRateFields() {
        if (hasNullDRate()) {
            return null;
        }
        final DRateFields dRateFields = new DRateFields(dRate, lastDRateUpdate);
        return dRateFields;
    }

    /**
     * the emissionDate is used to store the A-rate. Note that the calling method must check on null, and call RateService.initialize in order to
     * initialize rates.
     * @return the emissionDate.
     */
    public Calendar getEmissionDate() {
        return emissionDate;
    }

    public TransactionSummaryVO getNestedCredits() {
        return nestedCredits;
    }

    public TransactionSummaryVO getNestedDebits() {
        return nestedDebits;
    }

    public TransactionSummaryVO getPendingDebits() {
        return pendingDebits;
    }

    /**
     * gets the RateBalanceCorrection. Don't call directly, call RateService.readRateBalanceCorrection(AccountStatus), as this corrects for null
     * values.
     */
    public BigDecimal getRateBalanceCorrection() {
        return rateBalanceCorrection;
    }

    /**
     * The basic reserved amount is <code>pending debits + reserved scheduled payments</code>. Member account status include the account fees on this
     * calculation
     */
    public BigDecimal getReservedAmount() {
        return pendingDebits.getAmount().add(reservedScheduledPayments.getAmount());
    }

    public TransactionSummaryVO getReservedScheduledPayments() {
        return reservedScheduledPayments;
    }

    public TransactionSummaryVO getRootCredits() {
        return rootCredits;
    }

    public TransactionSummaryVO getRootDebits() {
        return rootDebits;
    }

    public ScheduledPayment getScheduledPayment() {
        return scheduledPayment;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public TransferAuthorization getTransferAuthorization() {
        return transferAuthorization;
    }

    public BigDecimal getUpperCreditLimit() {
        return upperCreditLimit;
    }

    /**
     * checks if one of the d-rate fields is null.
     * @return true if dRate or getLastDRateUpdate is null, false otherwise.
     */
    public boolean hasNullDRate() {
        if (dRate == null || lastDRateUpdate == null) {
            return true;
        }
        return false;
    }

    public AccountStatus newBasedOnThis() {
        final AccountStatus status = ClassHelper.instantiate(getClass());
        status.account = account;
        status.creditLimit = creditLimit;
        status.upperCreditLimit = upperCreditLimit;
        status.date = Calendar.getInstance();
        status.dRate = dRate;
        status.rateBalanceCorrection = rateBalanceCorrection;
        // Mutable data must be cloned
        status.rootCredits = rootCredits.clone();
        status.rootDebits = rootDebits.clone();
        status.nestedCredits = nestedCredits.clone();
        status.nestedDebits = nestedDebits.clone();
        status.pendingDebits = pendingDebits.clone();
        status.reservedScheduledPayments = reservedScheduledPayments.clone();
        status.emissionDate = emissionDate == null ? null : (Calendar) emissionDate.clone();
        status.lastDRateUpdate = lastDRateUpdate == null ? null : (Calendar) lastDRateUpdate.clone();
        return status;
    }

    public void setAccount(final Account account) {
        this.account = account;
    }

    public void setCreditLimit(final BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public void setCreditLimitChangedBy(final Element creditLimitChangedBy) {
        this.creditLimitChangedBy = creditLimitChangedBy;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setEmissionDate(final Calendar emissionDate) {
        this.emissionDate = emissionDate;
    }

    public void setNestedCredits(final TransactionSummaryVO feeCredits) {
        nestedCredits = feeCredits;
    }

    public void setNestedDebits(final TransactionSummaryVO feeDebits) {
        nestedDebits = feeDebits;
    }

    public void setPendingDebits(final TransactionSummaryVO pendingDebits) {
        this.pendingDebits = pendingDebits;
    }

    public void setRateBalanceCorrection(final BigDecimal rateBalanceCorrection) {
        this.rateBalanceCorrection = rateBalanceCorrection;
    }

    public void setReservedScheduledPayments(final TransactionSummaryVO reservedScheduledPayments) {
        this.reservedScheduledPayments = reservedScheduledPayments;
    }

    public void setRootCredits(final TransactionSummaryVO rootCredits) {
        this.rootCredits = rootCredits;
    }

    public void setRootDebits(final TransactionSummaryVO rootDebits) {
        this.rootDebits = rootDebits;
    }

    public void setScheduledPayment(final ScheduledPayment scheduledPayment) {
        this.scheduledPayment = scheduledPayment;
    }

    public void setTransfer(final Transfer transfer) {
        this.transfer = transfer;
    }

    public void setTransferAuthorization(final TransferAuthorization transferAuthorization) {
        this.transferAuthorization = transferAuthorization;
    }

    public void setUpperCreditLimit(final BigDecimal upperCreditLimit) {
        this.upperCreditLimit = upperCreditLimit;
    }

    @Override
    public String toString() {
        return getId() + " - " + account + ", balance = " + FormatObject.formatObject(getBalance());
    }

    /**
     * takes care that both the d-rate and its belonging date are updated together. Use of separate setters for d-rate and lastDRateUpdate is
     * discouraged, as they should never be used not together with the other. Updating a D-rate without updating the date is nonsense.
     * @param newDRateParams a RatesDTO containing the new D-rate and the date belonging to it.
     */
    public void updateDRate(final RatesDTO newDRateParams) {
        dRate = newDRateParams.getD();
        lastDRateUpdate = newDRateParams.getProcessDate();
    }

    @Override
    protected void appendVariableValues(final Map<String, Object> variables, final LocalSettings localSettings) {
        variables.put("balance", localSettings.getUnitsConverter(getAccount().getType().getCurrency().getPattern()).toString(getAvailableBalance()));
    }

    /**
     * gets the d-rate as it is stored. This may not be the present value updated up to today. Never call this directly; call
     * DRateServiceImpl.readRawRate(AccountStatus) in stead, as it also takes care of null rate handling. If the service is not available, use
     * AccountStatus.getDRateFields().
     * @return the d-rate as it is stored, which is most likely out of date and meaningless without the date belonging to it.
     */
    protected BigDecimal getdRate() {
        return dRate;
    }

    /**
     * gets the last update date of the dRate field. Never call this directly; call DRateServiceImpl.readRawRate(AccountStatus) in stead, as it also
     * takes care of null rate handling. If the service is not available, use AccountStatus.getDRateFields().
     */
    protected Calendar getLastDRateUpdate() {
        return lastDRateUpdate;
    }

    /**
     * don't use directly, use <code>updateDRate</code> instead. Only here because hibernate needs a public setter for the database.
     * @param dRate
     */
    protected void setdRate(final BigDecimal dRate) {
        this.dRate = dRate;
    }

    /**
     * don't use directly, use <code>updateDRate</code> instead
     * @param lastDRateUpdate
     */
    protected void setLastDRateUpdate(final Calendar lastDRateUpdate) {
        this.lastDRateUpdate = lastDRateUpdate;
    }

}
