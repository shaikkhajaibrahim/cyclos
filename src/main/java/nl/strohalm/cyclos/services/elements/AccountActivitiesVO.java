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
package nl.strohalm.cyclos.services.elements;

import java.math.BigDecimal;

import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Contains data for account activities report
 * @author luis
 * @author rafael
 */
public class AccountActivitiesVO extends DataObject {

    private static final long    serialVersionUID = 7851567897112186817L;
    private MemberAccountStatus  accountStatus;
    private TransactionSummaryVO creditsLast30Days;
    private TransactionSummaryVO debitsLast30Days;
    private TransactionSummaryVO brokerCommission;
    private TransactionSummaryVO incomingInvoices;
    private TransactionSummaryVO remainingLoans;
    private BigDecimal           aRate;
    private BigDecimal           dRate;
    private BigDecimal           totalFeePercentage;
    private boolean              showAccountInfo;
    private boolean              hasRateInfo;

    public MemberAccountStatus getAccountStatus() {
        return accountStatus;
    }

    public BigDecimal getaRate() {
        return aRate;
    }

    public TransactionSummaryVO getBrokerCommission() {
        return brokerCommission;
    }

    public TransactionSummaryVO getCreditsLast30Days() {
        return creditsLast30Days;
    }

    public TransactionSummaryVO getDebitsLast30Days() {
        return debitsLast30Days;
    }

    public BigDecimal getdRate() {
        return dRate;
    }

    public TransactionSummaryVO getIncomingInvoices() {
        return incomingInvoices;
    }

    public TransactionSummaryVO getRemainingLoans() {
        return remainingLoans;
    }

    public BigDecimal getTotalFeePercentage() {
        return totalFeePercentage;
    }

    public boolean isHasRateInfo() {
        return hasRateInfo;
    }

    public boolean isShowAccountInfo() {
        return showAccountInfo;
    }

    public void setAccountStatus(final MemberAccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public void setARate(final BigDecimal aRate) {
        this.aRate = aRate;
    }

    public void setBrokerCommission(final TransactionSummaryVO brokerCommission) {
        this.brokerCommission = brokerCommission;
    }

    public void setCreditsLast30Days(final TransactionSummaryVO creditsLast30Days) {
        this.creditsLast30Days = creditsLast30Days;
    }

    public void setDebitsLast30Days(final TransactionSummaryVO debitsLast30Days) {
        this.debitsLast30Days = debitsLast30Days;
    }

    public void setDRate(final BigDecimal rate) {
        dRate = rate;
    }

    public void setHasRateInfo(final boolean hasRateInfo) {
        this.hasRateInfo = hasRateInfo;
    }

    public void setIncomingInvoices(final TransactionSummaryVO incomingInvoices) {
        this.incomingInvoices = incomingInvoices;
    }

    public void setRemainingLoans(final TransactionSummaryVO remainingLoans) {
        this.remainingLoans = remainingLoans;
    }

    public void setShowAccountInfo(final boolean showAccountInfo) {
        this.showAccountInfo = showAccountInfo;
    }

    public void setTotalFeePercentage(final BigDecimal totalFeePercentage) {
        this.totalFeePercentage = totalFeePercentage;
    }

}