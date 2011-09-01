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
package nl.strohalm.cyclos.controls.reports;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.entities.reports.AdReportVO;
import nl.strohalm.cyclos.entities.reports.CurrentStateReportVO;
import nl.strohalm.cyclos.entities.reports.InvoiceSummaryType;
import nl.strohalm.cyclos.services.accounts.AccountTypeService;
import nl.strohalm.cyclos.services.accounts.CurrencyService;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.elements.ReferenceService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;

import org.apache.struts.action.ActionForward;

@AdminAction(@Permission(module = "systemReports", operation = "current"))
public class CurrentStateReportAction extends BaseFormAction {

    private CurrencyService                   currencyService;
    private MemberService                     memberService;
    private AdService                         adService;
    private AccountTypeService                accountTypeService;
    private InvoiceService                    invoiceService;
    private LoanService                       loanService;
    private ReferenceService                  referenceService;
    private DataBinder<CurrentStateReportDTO> dataBinder;

    @Inject
    public void setAccountTypeService(final AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    @Inject
    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    @Inject
    public void setCurrencyService(final CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Inject
    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Inject
    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    @Inject
    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    @Inject
    public void setReferenceService(final ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @Override
    protected ActionForward handleDisplay(final ActionContext context) throws Exception {
        try {
            prepareForm(context);
        } catch (final Exception e) {
            return context.sendError("reports.error.formDisplayError");
        }
        return context.getInputForward();
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        getCurrentStateReport(context);
        return context.getSuccessForward();
    }

    private void getCurrentStateReport(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final CurrentStateReportForm form = context.getForm();
        final CurrentStateReportDTO dto = getDataBinder().readFromString(form.getCurrentStateReport());
        final CurrentStateReportVO report = new CurrentStateReportVO();

        final List<Currency> currencies = currencyService.listAll();

        if (dto.isMemberGroupInformation()) {
            final Integer numberActiveMembers = memberService.countActiveMembers();
            final Map<String, Integer> groupMemberCount = memberService.getGroupMemberCount();
            report.setNumberActiveMembers(numberActiveMembers);
            report.setGroupMemberCount(groupMemberCount);
        }

        if (dto.isAds()) {
            final Integer numberActiveMembersWithAds = memberService.countActiveMembersWithAds();
            final Integer numberActiveAdvertisements = adService.getNumberOfAds(Ad.Status.ACTIVE);
            final Integer numberExpiredAdvertisements = adService.getNumberOfAds(Ad.Status.EXPIRED);
            final Integer numberScheduledAdvertisements = adService.getNumberOfAds(Ad.Status.SCHEDULED);
            final Integer numberPermanentAdvertisements = adService.getNumberOfAds(Ad.Status.PERMANENT);
            final AdReportVO adReportVO = new AdReportVO();
            adReportVO.setNumberActiveMembersWithAds(numberActiveMembersWithAds);
            adReportVO.setNumberActiveAdvertisements(numberActiveAdvertisements);
            adReportVO.setNumberExpiredAdvertisements(numberExpiredAdvertisements);
            adReportVO.setNumberScheduledAdvertisements(numberScheduledAdvertisements);
            adReportVO.setNumberPermanentAdvertisements(numberPermanentAdvertisements);
            report.setAdReportVO(adReportVO);
        }

        if (dto.isSystemAccountInformation()) {
            final Map<AccountType, BigDecimal> systemAccountTypesBalance = accountTypeService.getAccountTypesBalance(AccountType.Nature.SYSTEM);
            report.setSystemAccountTypesBalance(systemAccountTypesBalance);
        }

        if (dto.isMemberAccountInformation()) {
            final Map<AccountType, BigDecimal> memberAccountTypesBalance = accountTypeService.getAccountTypesBalance(AccountType.Nature.MEMBER);
            report.setMemberAccountTypesBalance(memberAccountTypesBalance);
        }

        if (dto.isInvoices()) {
            final Map<Currency, Map<InvoiceSummaryType, TransactionSummaryVO>> invoicesSummaries = new LinkedHashMap<Currency, Map<InvoiceSummaryType, TransactionSummaryVO>>(currencies.size());

            for (final Currency currency : currencies) {
                final TransactionSummaryVO memberInvoicesSummary = invoiceService.getSummaryByType(currency, InvoiceSummaryType.MEMBER);
                final TransactionSummaryVO systemIncomingInvoicesSummary = invoiceService.getSummaryByType(currency, InvoiceSummaryType.SYSTEM_INCOMING);
                final TransactionSummaryVO systemOutgoingInvoicesSummary = invoiceService.getSummaryByType(currency, InvoiceSummaryType.SYSTEM_OUTGOING);
                final Map<InvoiceSummaryType, TransactionSummaryVO> currencyInvoices = new EnumMap<InvoiceSummaryType, TransactionSummaryVO>(InvoiceSummaryType.class);
                currencyInvoices.put(InvoiceSummaryType.MEMBER, memberInvoicesSummary);
                currencyInvoices.put(InvoiceSummaryType.SYSTEM_INCOMING, systemIncomingInvoicesSummary);
                currencyInvoices.put(InvoiceSummaryType.SYSTEM_OUTGOING, systemOutgoingInvoicesSummary);
                invoicesSummaries.put(currency, currencyInvoices);
            }

            report.setInvoicesSummaries(invoicesSummaries);
        }

        if (dto.isLoans()) {
            final Map<Currency, TransactionSummaryVO> loansSummaries = new LinkedHashMap<Currency, TransactionSummaryVO>();

            for (final Currency currency : currencies) {
                final TransactionSummaryVO openLoansSummary = loanService.getOpenLoansSummary(currency);
                loansSummaries.put(currency, openLoansSummary);
            }

            report.setOpenLoansSummary(loansSummaries);
        }

        if (dto.isReferences()) {
            final Map<Reference.Level, Integer> givenReferences = referenceService.countGivenReferencesByLevel(Reference.Nature.GENERAL);
            report.setGivenReferences(givenReferences);
        }

        request.setAttribute("dto", dto);
        request.setAttribute("singleCurrency", currencies.size() == 1);
        request.setAttribute("report", report);
    }

    private DataBinder<CurrentStateReportDTO> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<CurrentStateReportDTO> binder = BeanBinder.instance(CurrentStateReportDTO.class);
            binder.registerBinder("ads", PropertyBinder.instance(Boolean.TYPE, "ads"));
            binder.registerBinder("invoices", PropertyBinder.instance(Boolean.TYPE, "invoices"));
            binder.registerBinder("loans", PropertyBinder.instance(Boolean.TYPE, "loans"));
            binder.registerBinder("memberAccountInformation", PropertyBinder.instance(Boolean.TYPE, "memberAccountInformation"));
            binder.registerBinder("memberGroupInformation", PropertyBinder.instance(Boolean.TYPE, "memberGroupInformation"));
            binder.registerBinder("references", PropertyBinder.instance(Boolean.TYPE, "references"));
            binder.registerBinder("systemAccountInformation", PropertyBinder.instance(Boolean.TYPE, "systemAccountInformation"));
            dataBinder = binder;
        }
        return dataBinder;
    }

}