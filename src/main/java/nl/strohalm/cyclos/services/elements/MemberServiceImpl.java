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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.dao.sms.MemberSmsStatusDAO;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceSummaryDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransfersAwaitingAuthorizationQuery;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.BrokeringQuery;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.PaymentsAwaitingFeedbackQuery;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.entities.members.ReferenceQuery;
import nl.strohalm.cyclos.entities.members.messages.MessageBox;
import nl.strohalm.cyclos.entities.members.messages.MessageQuery;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.accounts.rates.ConversionSimulationDTO;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.services.transactions.TransferAuthorizationService;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewForRatesDTO;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.IteratorListImpl;
import nl.strohalm.cyclos.utils.query.PageHelper;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation for MemberService
 * @author luis
 */
public class MemberServiceImpl implements MemberService {

    private FetchService                 fetchService;
    private AccountService               accountService;
    private ElementService               elementService;
    private InvoiceService               invoiceService;
    private PermissionService            permissionService;
    private BrokeringService             brokeringService;
    private LoanService                  loanService;
    private AdService                    adService;
    private ReferenceService             referenceService;
    private PaymentService               paymentService;
    private TransferAuthorizationService transferAuthorizationService;
    private MessageService               messageService;
    private ElementDAO                   elementDao;
    private ARateService                 aRateService;
    private DRateService                 dRateService;
    private TransferTypeService          transferTypeService;
    private MemberSmsStatusDAO           memberSmsStatusDao;

    public int countActiveMembers() {
        final MemberQuery query = new MemberQuery();
        if (LoggedUser.isValid()) {
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            query.setGroups(adminGroup.getManagesGroups());
        }
        query.setEnabled(true);
        query.setPageForCount();
        return PageHelper.getTotalCount(elementService.search(query));
    }

    public int countActiveMembersWithAds() {
        final MemberQuery query = new MemberQuery();
        if (LoggedUser.isValid() && LoggedUser.isAdministrator()) {
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            query.setGroups(adminGroup.getManagesGroups());
        }
        query.setEnabled(true);
        query.setHasAds(true);
        query.setPageForCount();
        return PageHelper.getTotalCount(elementService.search(query));
    }

    public MemberAccountType getDefaultAccountType(Member member) {
        if (!fetchService.isInitialized(member.getMemberGroup()) || !fetchService.isInitialized(member.getMemberGroup().getAccountSettings())) {
            member = fetchService.fetch(member, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));
        }
        final Collection<MemberGroupAccountSettings> settings = member.getMemberGroup().getAccountSettings();
        for (final MemberGroupAccountSettings setting : settings) {
            if (setting.isDefault()) {
                return setting.getAccountType();
            }
        }
        return null;
    }

    public Map<String, Integer> getGroupMemberCount() {
        final Map<String, Integer> groupMemberCount = new HashMap<String, Integer>();
        if (LoggedUser.isValid()) {
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            for (final MemberGroup memberGroup : adminGroup.getManagesGroups()) {
                final MemberQuery query = new MemberQuery();
                query.setGroups(Collections.singletonList(memberGroup));
                query.setPageForCount();
                groupMemberCount.put(memberGroup.getName(), PageHelper.getTotalCount(elementService.search(query)));
            }
        }
        return groupMemberCount;
    }

    public ActivitiesVO getMemberActivities(Member member) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        return doGetActivities(member);
    }

    public ActivitiesVO getMemberActivitiesByMember(Member member) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        return doGetActivities(member);
    }

    public ActivitiesVO getMemberActivitiesByOperator() {
        final Operator operator = LoggedUser.element();
        return doGetActivities(operator.getMember());
    }

    public ActivitiesVO getMyActivities() {
        final Member member = LoggedUser.element();
        return doGetActivities(member);
    }

    public ActivitiesVO getOtherMemberActivitiesByOperator(final Member member) {
        return doGetActivities(member);
    }

    public QuickAccessVO getQuickAccess() {
        final boolean isMember = LoggedUser.isMember();
        final String modulePrefix = isMember ? "member" : "operator";
        final Member member = (Member) LoggedUser.accountOwner();
        final Group memberGroup = member.getGroup();

        // Check if quick access is visible at all
        if (!permissionService.checkPermission(memberGroup, "basic", "quickAccess")) {
            return null;
        }

        final QuickAccessVO quickAccess = new QuickAccessVO();
        quickAccess.setUpdateProfile(true); // Update profile is always visible
        quickAccess.setAccountInformation(CollectionUtils.isNotEmpty(accountService.getAccounts(member)));
        quickAccess.setMemberPayment(permissionService.checkPermission(modulePrefix + "Payments", "paymentToMember"));
        quickAccess.setPublishAd(permissionService.checkPermission(modulePrefix + "Ads", "publish"));
        quickAccess.setSearchAds(permissionService.checkPermission(memberGroup, "memberAds", "view"));
        quickAccess.setViewMessages(permissionService.checkPermission(modulePrefix + "Messages", "view"));
        quickAccess.setViewContacts(isMember || permissionService.checkPermission("operatorContacts", "view"));
        return quickAccess;
    }

    public MemberSmsStatus getSmsStatus(final Member member) {
        final Calendar today = Calendar.getInstance();
        final Period currentMonth = TimePeriod.ONE_MONTH.currentPeriod(today);
        MemberSmsStatus status;
        try {
            // Try loading the member status
            status = memberSmsStatusDao.load(member);

            // If got to this line, the status exists
            boolean changed = false;
            if (today.after(status.getFreeSmsExpiration())) {
                // The free sms period has expired. Reset.
                status.setFreeSmsSent(0);
                status.setFreeSmsExpiration(currentMonth.getEnd());
                changed = true;
            }
            final Calendar paidSmsExpiration = status.getPaidSmsExpiration();
            if (paidSmsExpiration != null && today.after(paidSmsExpiration)) {
                // The paid sms messages have expired. Reset.
                status.setPaidSmsLeft(0);
                status.setPaidSmsExpiration(null);
                changed = true;
            }
            if (changed) {
                // Update the record if it has changed
                status = memberSmsStatusDao.update(status);
            }
        } catch (final EntityNotFoundException e) {
            // The status does not exist. Create a new one.
            status = new MemberSmsStatus();
            status.setMember(member);
            status.setFreeSmsExpiration(currentMonth.getEnd());
            status = memberSmsStatusDao.insert(status);
        }
        return fetchService.fetch(status, MemberSmsStatus.Relationships.MEMBER);
    }

    public MemberStatusVO getStatus() {
        final MemberStatusVO status = new MemberStatusVO();

        final Member member = (Member) LoggedUser.accountOwner();
        MemberGroup group = member.getMemberGroup();
        final User user = LoggedUser.user();
        final Calendar lastLogin = user.getLastLogin();
        final boolean isOperator = LoggedUser.isOperator();
        final String modulePrefix = isOperator ? "operator" : "member";

        // Count the unread messages
        if (permissionService.checkPermission(modulePrefix + "Messages", "view")) {
            final MessageQuery messages = new MessageQuery();
            messages.setGetter(member);
            messages.setMessageBox(MessageBox.INBOX);
            messages.setRead(false);
            messages.setPageForCount();
            status.setUnreadMessages(PageHelper.getTotalCount(messageService.search(messages)));
        }

        // Count the new payments since the last login
        group = fetchService.fetch(group, MemberGroup.Relationships.ACCOUNT_SETTINGS);
        final Collection<MemberAccountType> accountTypes = group.getAccountTypes();
        if (CollectionUtils.isNotEmpty(accountTypes) && !(isOperator && !permissionService.checkPermission("operatorAccount", "accountInformation"))) {
            final TransferQuery transfers = new TransferQuery();
            transfers.setRootOnly(true);
            transfers.setToAccountOwner(member);
            transfers.setLoanTransfer(false);
            if (lastLogin != null) {
                transfers.setPeriod(Period.begginingAt(lastLogin).useTime());
            }
            transfers.setPageForCount();
            status.setNewPayments(PageHelper.getTotalCount(paymentService.search(transfers)));
        }

        // Count the new references since the last login
        if (permissionService.checkPermission(modulePrefix + "References", "view")) {
            final ReferenceQuery references = new ReferenceQuery();
            references.setNature(Reference.Nature.GENERAL);
            references.setTo(member);
            if (lastLogin != null) {
                references.setPeriod(Period.begginingAt(lastLogin).useTime());
            }
            references.setPageForCount();
            status.setNewReferences(PageHelper.getTotalCount(referenceService.search(references)));
        }

        // Count the open invoices
        if (permissionService.checkPermission(modulePrefix + "Invoices", "view")) {
            final InvoiceQuery invoices = new InvoiceQuery();
            invoices.setOwner(member);
            invoices.setDirection(InvoiceQuery.Direction.INCOMING);
            invoices.setStatus(Invoice.Status.OPEN);
            invoices.setPageForCount();
            status.setOpenInvoices(PageHelper.getTotalCount(invoiceService.search(invoices)));
        }

        // Count the open loans
        if (permissionService.checkPermission(modulePrefix + "Loans", "view")) {
            final LoanQuery loans = new LoanQuery();
            loans.setMember(member);
            loans.setStatus(Loan.Status.OPEN);
            loans.setPageForCount();
            status.setOpenLoans(PageHelper.getTotalCount(loanService.search(loans)));
        }

        // Count the payments awaiting feedback
        if (!(isOperator && !permissionService.checkPermission("operatorReferences", "manageMemberTransactionFeedbacks"))) {
            final PaymentsAwaitingFeedbackQuery awaitingFeedback = new PaymentsAwaitingFeedbackQuery();
            awaitingFeedback.setPageForCount();
            awaitingFeedback.setMember(member);
            status.setPaymentsAwaitingFeedback(PageHelper.getTotalCount(referenceService.searchPaymentsAwaitingFeedback(awaitingFeedback)));
        }

        // Count the payment awaiting authorization
        if (permissionService.checkPermission(modulePrefix + "Payments", "authorize")) {
            final TransfersAwaitingAuthorizationQuery awaitingAuthorization = new TransfersAwaitingAuthorizationQuery();
            awaitingAuthorization.setPageForCount();
            status.setPaymentsToAuthorize(PageHelper.getTotalCount(transferAuthorizationService.searchTransfersAwaitingMyAuthorization(awaitingAuthorization)));
        }

        return status;
    }

    public boolean hasValueForField(final Member member, final MemberCustomField field) {
        return elementDao.hasValueForField(member, field);
    }

    public boolean isActive(final Member member) {
        return (accountService.getAccounts(member).size() > 0);
    }

    public boolean isEnabled(final Member member) {
        return permissionService.checkPermission(member.getGroup(), "basicAccess", "login");
    }

    public Iterator<Member> iterateByGroup(final boolean ordered, final MemberGroup... groups) {
        return elementDao.iterateMembers(ordered, groups);
    }

    public Iterator<Member> iterateByGroup(final MemberGroup... groups) {
        return iterateByGroup(false, groups);
    }

    public List<Member> listByGroup(final MemberGroup... groups) {
        return new IteratorListImpl<Member>(iterateByGroup(groups));
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    public void setBrokeringService(final BrokeringService brokeringService) {
        this.brokeringService = brokeringService;
    }

    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    public void setElementDao(final ElementDAO elementDao) {
        this.elementDao = elementDao;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    public void setMemberSmsStatusDao(final MemberSmsStatusDAO memberSmsStatusDao) {
        this.memberSmsStatusDao = memberSmsStatusDao;
    }

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setReferenceService(final ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    public void setTransferAuthorizationService(final TransferAuthorizationService transferAuthorizationService) {
        this.transferAuthorizationService = transferAuthorizationService;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public MemberSmsStatus updateSmsStatus(final MemberSmsStatus memberSmsStatus) {
        return memberSmsStatusDao.update(memberSmsStatus);
    }

    private boolean canViewAccountInformation(final Element element, MemberAccount memberAccount) {
        memberAccount = fetchService.fetch(memberAccount, Account.Relationships.TYPE, RelationshipHelper.nested(MemberAccount.Relationships.MEMBER, Member.Relationships.BROKER));
        if (element instanceof Administrator) {
            return true;
        } else if (element instanceof Operator) {
            final Operator operator = fetchService.fetch((Operator) element, RelationshipHelper.nested(Operator.Relationships.MEMBER, Element.Relationships.GROUP), RelationshipHelper.nested(Element.Relationships.GROUP, OperatorGroup.Relationships.CAN_VIEW_INFORMATION_OF));
            if (memberAccount.getMember().equals(operator.getMember())) {
                if (operator.getOperatorGroup().getCanViewInformationOf().contains(memberAccount.getType())) {
                    return true;
                }
            } else {
                final MemberGroup operatorsMemberGroup = fetchService.fetch(operator.getMember().getMemberGroup(), MemberGroup.Relationships.CAN_VIEW_INFORMATION_OF);
                if (operatorsMemberGroup.getCanViewInformationOf().contains(memberAccount.getType())) {
                    return true;
                }
            }
        } else if (element instanceof Member) {
            final Member member = fetchService.fetch((Member) element, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.CAN_VIEW_INFORMATION_OF), RelationshipHelper.nested(Element.Relationships.GROUP, BrokerGroup.Relationships.BROKER_CAN_VIEW_INFORMATION_OF));
            if (member.getGroup() instanceof BrokerGroup) {
                if (member.equals(memberAccount.getMember().getBroker())) {
                    final BrokerGroup brokerGroup = (BrokerGroup) member.getMemberGroup();
                    if (brokerGroup.getBrokerCanViewInformationOf().contains(memberAccount.getType())) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            final MemberGroup memberGroup = member.getMemberGroup();
            if (memberGroup.getCanViewInformationOf().contains(memberAccount.getType())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private ActivitiesVO doGetActivities(final Member member) {
        final ActivitiesVO vo = new ActivitiesVO();

        // Check if account information will be retrieved
        boolean showAccountInformation;
        final Element loggedElement = LoggedUser.element();
        if (loggedElement.equals(member)) {
            // Always show account information for the logged member
            showAccountInformation = true;
        } else {
            // For another member, check permission
            Group group;
            String module;
            String operation = "showAccountInformation";
            if (LoggedUser.isAdministrator()) {
                group = loggedElement.getGroup();
                module = "adminMemberReports";
                operation = "showAccountInformation";
            } else if (LoggedUser.isOperator()) {
                final Operator operator = fetchService.fetch((Operator) loggedElement, Operator.Relationships.MEMBER, Element.Relationships.GROUP);
                if (member.equals(operator.getMember())) {
                    group = operator.getOperatorGroup();
                    module = "operatorAccount";
                    operation = "accountInformation";
                } else {
                    group = ((Member) loggedElement.getAccountOwner()).getGroup();
                    module = "memberReports";
                    operation = "showAccountInformation";
                }
            } else {
                final Member loggedMember = (Member) loggedElement;
                group = loggedMember.getGroup();
                if (loggedMember.equals(member.getBroker())) {
                    module = "brokerReports";
                } else {
                    module = "memberReports";
                }
            }
            showAccountInformation = permissionService.checkPermission(group, module, operation);
        }

        final boolean isBroker = member.getMemberGroup().isBroker();

        // Since active
        vo.setSinceActive(member.getActivationDate());

        // Number of brokered members
        if (isBroker) {
            final BrokeringQuery query = new BrokeringQuery();
            query.setBroker(member);
            query.setStatus(BrokeringQuery.Status.ACTIVE);
            query.setPageForCount();
            vo.setNumberBrokeredMembers(PageHelper.getTotalCount(brokeringService.search(query)));
        }

        // References
        vo.setReceivedReferencesByLevel(referenceService.countReferencesByLevel(Reference.Nature.GENERAL, member, true));
        vo.setGivenReferencesByLevel(referenceService.countReferencesByLevel(Reference.Nature.GENERAL, member, false));

        // Ads
        vo.setAdsByStatus(adService.getNumberOfAds(member));

        final List<MemberAccount> accounts = (List<MemberAccount>) accountService.getAccounts(member);

        // Get invoice information
        if (showAccountInformation) {
            // Incoming invoices
            final InvoiceSummaryDTO incomingInvoicesDTO = new InvoiceSummaryDTO();
            incomingInvoicesDTO.setOwner(member);
            incomingInvoicesDTO.setDirection(InvoiceQuery.Direction.INCOMING);
            incomingInvoicesDTO.setStatus(Invoice.Status.OPEN);
            vo.setIncomingInvoices(invoiceService.getSummary(incomingInvoicesDTO));

            // Outgoing invoices
            final InvoiceSummaryDTO summaryDTO = new InvoiceSummaryDTO();
            summaryDTO.setOwner(member);
            summaryDTO.setDirection(InvoiceQuery.Direction.OUTGOING);
            summaryDTO.setStatus(Invoice.Status.OPEN);
            vo.setOutgoingInvoices(invoiceService.getSummary(summaryDTO));
        }

        // 30 days ago
        final Calendar days30 = DateHelper.truncate(Calendar.getInstance());
        days30.add(Calendar.DATE, -30);

        // Account activities; as rate info is NOT subject to permissions, always do the loop
        for (final MemberAccount account : accounts) {
            boolean hasRateInfo = false;

            final GetTransactionsDTO allTime = new GetTransactionsDTO(account);
            final GetTransactionsDTO last30Days = new GetTransactionsDTO(account, Period.begginingAt(days30));

            // Build an account activities VO
            final AccountActivitiesVO activities = new AccountActivitiesVO();

            // Get the account status
            final MemberAccountStatus accountStatus = (MemberAccountStatus) accountService.getStatus(allTime);
            activities.setAccountStatus(accountStatus);

            // get/set the rates
            final BigDecimal aRate = aRateService.getActualRate(accountStatus);
            activities.setARate(aRate);
            final BigDecimal dRate = dRateService.getActualRate(accountStatus);
            activities.setDRate(dRate);
            if (aRate != null || dRate != null) {
                hasRateInfo = true;
            }
            activities.setHasRateInfo(hasRateInfo);

            // get the conversion result
            if (hasRateInfo) {
                // get the relevant transfer type for conversions
                final Collection<TransferType> currencyConversionTTs = transferTypeService.getConversionTTs(account.getType().getCurrency());
                final Collection<TransferType> accountConversionTTs = transferTypeService.getConversionTTs(account.getType());
                TransferType conversionTT = null;
                // there must be only 1 TT available on the account. if more than one, we don't know which to choose so show nothing.
                if (accountConversionTTs.size() == 1) {
                    final Object[] ttArray = accountConversionTTs.toArray();
                    conversionTT = (TransferType) ttArray[0];
                } else if (accountConversionTTs.size() == 0 && currencyConversionTTs.size() == 1) {
                    // OR in case there is none on the account, we will take the only one available on the currency.
                    final Object[] ttArray = currencyConversionTTs.toArray();
                    conversionTT = (TransferType) ttArray[0];
                }
                // if no balance or no TT, there's nothing to convert.
                final BigDecimal balance = accountStatus.getBalance();
                if (balance.compareTo(BigDecimal.ZERO) > 0 && conversionTT != null) {
                    final ConversionSimulationDTO dto = new ConversionSimulationDTO();
                    dto.setTransferType(conversionTT);
                    dto.setAccount(account);
                    dto.setAmount(balance);
                    dto.setUseActualRates(true);
                    dto.setDate(Calendar.getInstance());
                    final TransactionFeePreviewForRatesDTO result = paymentService.simulateConversionInternal(dto);
                    activities.setTotalFeePercentage(result.getRatesAsFeePercentage());
                }
            }

            // rest of the activities info is subject to permissions
            if (showAccountInformation) {
                // Check if user has permission to view information of that account
                if (account.getMember().equals(loggedElement) || canViewAccountInformation(loggedElement, account)) {

                    activities.setShowAccountInfo(true);

                    // get last 30 days info
                    final MemberAccountStatus statusLast30Days = (MemberAccountStatus) accountService.getStatus(last30Days, false).newBasedOnThis();
                    activities.setCreditsLast30Days(accountStatus.getRootCredits().subtract(statusLast30Days.getRootCredits()));
                    activities.setDebitsLast30Days(accountStatus.getRootDebits().subtract(statusLast30Days.getRootDebits()));

                    // Get the broker commission
                    if (isBroker) {
                        activities.setBrokerCommission(accountService.getBrokerCommissions(allTime));
                    }

                    // Calculate the total of remaining loans
                    final LoanQuery loanQuery = new LoanQuery();
                    loanQuery.setMember(member);
                    loanQuery.setStatus(Loan.Status.OPEN);
                    loanQuery.setAccountType(account.getType());
                    int remainingLoans = 0;
                    BigDecimal remainingLoanAmount = BigDecimal.ZERO;
                    final List<Loan> loans = loanService.search(loanQuery);
                    for (final Loan loan : loans) {
                        remainingLoans++;
                        remainingLoanAmount = remainingLoanAmount.add(loan.getRemainingAmount());
                    }
                    activities.setRemainingLoans(new TransactionSummaryVO(remainingLoans, remainingLoanAmount));

                }
            }
            // Store this one, but only if there is info
            if (activities.isShowAccountInfo() || activities.isHasRateInfo()) {
                vo.addAccountActivities(account.getType().getName(), activities);
            }
        }

        return vo;
    }

}