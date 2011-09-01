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
package nl.strohalm.cyclos.entities.reports;

import java.math.BigDecimal;
import java.util.Map;

import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.DataObject;

public class CurrentStateReportVO extends DataObject {

    private static final long                                            serialVersionUID = 5310819684945275392L;
    private Integer                                                      numberActiveMembers;
    private Map<String, Integer>                                         groupMemberCount;
    private AdReportVO                                                   adReportVO;
    private Map<AccountType, BigDecimal>                                 systemAccountTypesBalance;
    private Map<AccountType, BigDecimal>                                 memberAccountTypesBalance;
    private Map<Currency, Map<InvoiceSummaryType, TransactionSummaryVO>> invoicesSummaries;
    private Map<Currency, TransactionSummaryVO>                          openLoansSummary;
    private Map<Reference.Level, Integer>                                givenReferences;

    public AdReportVO getAdReportVO() {
        return adReportVO;
    }

    public Map<Reference.Level, Integer> getGivenReferences() {
        return givenReferences;
    }

    public Map<String, Integer> getGroupMemberCount() {
        return groupMemberCount;
    }

    public Map<Currency, Map<InvoiceSummaryType, TransactionSummaryVO>> getInvoicesSummaries() {
        return invoicesSummaries;
    }

    public Map<AccountType, BigDecimal> getMemberAccountTypesBalance() {
        return memberAccountTypesBalance;
    }

    public Integer getNumberActiveMembers() {
        return numberActiveMembers;
    }

    public Map<Currency, TransactionSummaryVO> getOpenLoansSummary() {
        return openLoansSummary;
    }

    public Map<AccountType, BigDecimal> getSystemAccountTypesBalance() {
        return systemAccountTypesBalance;
    }

    public void setAdReportVO(final AdReportVO adReportVO) {
        this.adReportVO = adReportVO;
    }

    public void setGivenReferences(final Map<Reference.Level, Integer> givenReferences) {
        this.givenReferences = givenReferences;
    }

    public void setGroupMemberCount(final Map<String, Integer> groupMemberCount) {
        this.groupMemberCount = groupMemberCount;
    }

    public void setInvoicesSummaries(final Map<Currency, Map<InvoiceSummaryType, TransactionSummaryVO>> invoicesSummaries) {
        this.invoicesSummaries = invoicesSummaries;
    }

    public void setMemberAccountTypesBalance(final Map<AccountType, BigDecimal> memberAccountTypesBalance) {
        this.memberAccountTypesBalance = memberAccountTypesBalance;
    }

    public void setNumberActiveMembers(final Integer numberActiveMembers) {
        this.numberActiveMembers = numberActiveMembers;
    }

    public void setOpenLoansSummary(final Map<Currency, TransactionSummaryVO> openLoansSummary) {
        this.openLoansSummary = openLoansSummary;
    }

    public void setSystemAccountTypesBalance(final Map<AccountType, BigDecimal> systemAccountTypesBalance) {
        this.systemAccountTypesBalance = systemAccountTypesBalance;
    }

}