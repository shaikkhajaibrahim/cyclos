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
package nl.strohalm.cyclos.controls.accounts.details;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseQueryAction;
import nl.strohalm.cyclos.entities.EntityReference;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilterQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.GroupFilterQuery;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.OperatorQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupFilterService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transfertypes.PaymentFilterService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.TransformedIteratorList;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.conversion.AccountOwnerConverter;
import nl.strohalm.cyclos.utils.conversion.ReferenceConverter;
import nl.strohalm.cyclos.utils.conversion.Transformer;
import nl.strohalm.cyclos.utils.query.QueryParameters;

/**
 * Action used to retrieve the account history
 * @author luis
 */
public class AccountHistoryAction extends BaseQueryAction {

    /**
     * An account history entry, containing the current account and a transfer
     * @author luis
     */
    public static class Entry implements Serializable {

        private static final long serialVersionUID = -4696245469424024391L;

        /**
         * Build the entry list
         */
        public static List<Entry> build(final ElementService elementService, final Account account, final List<Transfer> transfers, final boolean fetchMember) {
            final TransformTransferInEntry transformTransferInEntry = new TransformTransferInEntry(elementService, account, fetchMember);
            return new TransformedIteratorList<Transfer, Entry>(transformTransferInEntry, transfers);
        }

        private final Account  account;
        private final Account  related;
        private final Member   relatedMember;
        private final Transfer transfer;
        private final int      signal;

        private Entry(final Account account, final Transfer transfer, final Account relatedAccount, final Member relatedMember) {
            this.transfer = transfer;
            this.account = account;
            related = relatedAccount;
            this.relatedMember = relatedMember;
            signal = account.equals(transfer.getActualFrom()) ? -1 : +1;
        }

        public Account getAccount() {
            return account;
        }

        public BigDecimal getAmount() {
            final BigDecimal amount = transfer.getAmount();
            return signal < 0 ? amount.negate() : amount;
        }

        public Account getRelated() {
            return related;
        }

        public Member getRelatedMember() {
            return relatedMember;
        }

        public int getSignal() {
            return signal;
        }

        public Transfer getTransfer() {
            return transfer;
        }

        public boolean isCredit() {
            return signal > 0;
        }

        public boolean isDebit() {
            return signal < 0;
        }
    }

    /**
     * A transformer between transfers and entries
     */
    private static class TransformTransferInEntry implements Transformer<Transfer, Entry> {

        private ElementService elementService;
        private Account        account;
        private boolean        fetchMember;

        private TransformTransferInEntry(final ElementService elementService, final Account account, final boolean fetchMember) {
            this.account = account;
            this.elementService = elementService;
            this.fetchMember = fetchMember;
        }

        public Entry transform(final Transfer transfer) {
            final Account from = transfer.getActualFrom();
            final Account to = transfer.getActualTo();
            final Account relatedAccount = account.equals(from) ? to : from;
            Member relatedMember = null;
            if (fetchMember) {
                if (relatedAccount instanceof MemberAccount) {
                    try {
                        final MemberAccount ma = (MemberAccount) relatedAccount;
                        relatedMember = (Member) elementService.load(ma.getMember().getId(), Element.Relationships.USER);
                    } catch (final PermissionDeniedException e) {
                        // Ok: The related member is in a group which is not visible by the logged member
                    }
                }
            }
            return new Entry(account, transfer, relatedAccount, relatedMember);
        }

    }

    /**
     * Returns a databinder for a transferquery
     */
    public static DataBinder<TransferQuery> transferQueryDataBinder(final LocalSettings localSettings) {
        final BeanBinder<PaymentCustomFieldValue> customValueBinder = BeanBinder.instance(PaymentCustomFieldValue.class);
        customValueBinder.registerBinder("field", PropertyBinder.instance(PaymentCustomField.class, "field"));
        customValueBinder.registerBinder("value", PropertyBinder.instance(String.class, "value"));

        final BeanBinder<TransferQuery> binder = BeanBinder.instance(TransferQuery.class);
        binder.registerBinder("owner", PropertyBinder.instance(AccountOwner.class, "owner", AccountOwnerConverter.instance()));
        binder.registerBinder("status", PropertyBinder.instance(Transfer.Status.class, "status"));
        binder.registerBinder("type", PropertyBinder.instance(AccountType.class, "type", ReferenceConverter.instance(AccountType.class)));
        binder.registerBinder("period", DataBinderHelper.rawPeriodBinder(localSettings, "period"));
        binder.registerBinder("paymentFilter", PropertyBinder.instance(PaymentFilter.class, "paymentFilter", ReferenceConverter.instance(PaymentFilter.class)));
        binder.registerBinder("description", PropertyBinder.instance(String.class, "description"));
        binder.registerBinder("transactionNumber", PropertyBinder.instance(String.class, "transactionNumber"));
        binder.registerBinder("member", PropertyBinder.instance(Member.class, "member", ReferenceConverter.instance(Member.class)));
        binder.registerBinder("by", PropertyBinder.instance(Element.class, "by", ReferenceConverter.instance(Element.class)));
        binder.registerBinder("conciliated", PropertyBinder.instance(Boolean.class, "conciliated"));
        binder.registerBinder("groups", SimpleCollectionBinder.instance(MemberGroup.class, "groups"));
        binder.registerBinder("groupFilters", SimpleCollectionBinder.instance(GroupFilter.class, "groupFilters"));
        binder.registerBinder("customValues", BeanCollectionBinder.instance(customValueBinder, "customValues"));
        binder.registerBinder("pageParameters", DataBinderHelper.pageBinder());
        return binder;
    }

    protected AccountService          accountService;
    private DataBinder<TransferQuery> dataBinder;
    private PaymentFilterService      paymentFilterService;
    private PaymentService            paymentService;
    private ElementService            elementService;
    private GroupService              groupService;
    private GroupFilterService        groupFilterService;
    private CustomFieldService        customFieldService;
    private ARateService              aRateService;
    private DRateService              dRateService;

    public AccountService getAccountService() {
        return accountService;
    }

    public DataBinder<TransferQuery> getDataBinder() {
        if (dataBinder == null) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            dataBinder = transferQueryDataBinder(localSettings);
        }
        return dataBinder;
    }

    @Override
    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        super.onLocalSettingsUpdate(event);
        dataBinder = null;
    }

    @Inject
    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    @Inject
    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupFilterService(final GroupFilterService groupFilterService) {
        this.groupFilterService = groupFilterService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Inject
    public void setPaymentFilterService(final PaymentFilterService paymentFilterService) {
        this.paymentFilterService = paymentFilterService;
    }

    @Inject
    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    protected void executeQuery(final ActionContext context, final QueryParameters queryParameters) {
        final HttpServletRequest request = context.getRequest();
        final TransferQuery query = (TransferQuery) queryParameters;
        final Account account = (Account) request.getAttribute("account");
        final List<Transfer> transfers = paymentService.search(query);
        request.setAttribute("transfers", transfers);
        request.setAttribute("accountHistory", Entry.build(elementService, account, transfers, fetchMember()));
    }

    /**
     * Used to determine if the each transfer's related member will be fetch
     */
    protected boolean fetchMember() {
        return true;
    }

    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final HttpServletRequest request = context.getRequest();
        final AccountHistoryForm form = context.getForm();
        final LocalSettings localSettings = SettingsHelper.getLocalSettings(request);

        // Set the owner and the account type on the first request
        boolean firstTime = false;
        if (RequestHelper.isGet(request)) {
            form.setQuery("owner", form.getMemberId());
            form.setQuery("type", form.getTypeId());
            firstTime = true;
        }

        // Retrieve the query parameters
        final TransferQuery query = getDataBinder().readFromString(form.getQuery());
        query.fetch(Transfer.Relationships.CUSTOM_VALUES, Transfer.Relationships.FROM, Transfer.Relationships.TO, Transfer.Relationships.TYPE);
        query.setReverseOrder(true);

        // Fetch the account type, and add relationship externalAccounts
        final AccountType type = getFetchService().fetch(query.getType(), SystemAccountType.Relationships.EXTERNAL_ACCOUNTS);

        // Set the default status to PROCESSED
        if (query.getStatus() == null) {
            query.setStatus(Transfer.Status.PROCESSED);
            form.setQuery("status", query.getStatus().name());
        }
        if (firstTime) {
            if (type instanceof SystemAccountType) {
                // Ensure the initial period filter will start from the 1st day of the previous month, to avoid potentially huge DB scans
                final Period lastMonthPeriod = TimePeriod.ONE_MONTH.previousPeriod(Calendar.getInstance());
                query.setPeriod(Period.begginingAt(lastMonthPeriod.getBegin()));
                final String formattedDate = localSettings.getDateConverter().toString(lastMonthPeriod.getBegin());
                PropertyHelper.set(form.getQuery("period"), "begin", formattedDate);
            }
        }

        // Fetch the owner if is a member
        AccountOwner owner = query.getOwner();
        if (owner == null) {
            owner = SystemAccountOwner.instance();
        } else if (owner instanceof EntityReference) {
            owner = getFetchService().fetch((Member) owner);
        }

        // Check if authorization status will be shown
        boolean showStatus = false;
        if (owner instanceof SystemAccountOwner) {
            showStatus = getPermissionService().checkPermission("systemAccounts", "authorizedInformation");
        } else if (context.isAdmin()) {
            showStatus = getPermissionService().checkPermission("adminMemberAccounts", "authorizedInformation");
        } else if (context.getAccountOwner().equals(owner)) {
            showStatus = getPermissionService().checkPermission("memberAccount", "authorizedInformation");
        } else if (owner instanceof Member && context.isBrokerOf((Member) owner)) {
            showStatus = getPermissionService().checkPermission("brokerAccounts", "authorizedInformation");
        }
        if (showStatus) {
            request.setAttribute("paymentStatus", EnumSet.of(Transfer.Status.PROCESSED, Transfer.Status.PENDING, Transfer.Status.DENIED, Transfer.Status.CANCELED));
        }

        // Retrieve the account
        final Account account = resolveAccount(type, owner);

        // Fetch the member on filter
        if (query.getMember() instanceof EntityReference) {
            query.setMember(getFetchService().fetch(query.getMember(), Element.Relationships.USER));
        }

        // When a member, get it's operators
        if (context.isMember() && form.isAdvanced()) {
            final OperatorQuery oq = new OperatorQuery();
            oq.setMember((Member) context.getElement());
            final List<? extends Element> operators = elementService.search(oq);
            request.setAttribute("operators", operators);
        }

        // When a system account, get groups / group filters
        if (type instanceof SystemAccountType) {
            final AdminGroup adminGroup = context.getGroup();

            final GroupQuery groups = new GroupQuery();
            groups.setManagedBy(adminGroup);
            groups.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
            groups.setStatus(Group.Status.NORMAL);
            request.setAttribute("memberGroups", groupService.search(groups));

            final GroupFilterQuery groupFilters = new GroupFilterQuery();
            groupFilters.setAdminGroup(adminGroup);
            request.setAttribute("groupFilters", groupFilterService.search(groupFilters));
        }

        // Get the account status
        final GetTransactionsDTO params = new GetTransactionsDTO(query.getOwner(), query.getType());
        AccountStatus status = accountService.getStatus(params);
        if (status instanceof MemberAccountStatus) {
            MemberAccountStatus memberStatus = (MemberAccountStatus) status;
            // Ensure a transient instance is used to avoid changing persistent data
            if (memberStatus.isPersistent()) {
                // 2 assignments here: the outer status and the memberStatus
                status = memberStatus = memberStatus.newBasedOnThis();
            }
            // Round the volume account fees
            final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            memberStatus.setVolumeAccountFees(settings.round(memberStatus.getVolumeAccountFees()));
        }

        // Get the credit limit
        final BigDecimal min = paymentService.getMinimumPayment();
        final BigDecimal creditLimit = accountService.getCreditLimit(params);
        // Don't show if zero
        if (creditLimit != null && creditLimit.abs().compareTo(min) == 1) {
            request.setAttribute("creditLimit", creditLimit.negate());
        }

        if (type.getNature() == AccountType.Nature.MEMBER && context.isMember()) {
            Member member = (Member) elementService.load(context.getElement().getId());

            request.setAttribute("accountSettings", groupService.loadAccountSettings(member.getGroup().getId(), account.getType().getId()));
        }

        // Retrieve the payment filters
        final PaymentFilterQuery pfQuery = new PaymentFilterQuery();
        pfQuery.setAccountType(query.getType());
        pfQuery.setContext(PaymentFilterQuery.Context.ACCOUNT_HISTORY);
        pfQuery.setElement(owner instanceof SystemAccountOwner ? context.getElement() : (Member) owner);
        final List<PaymentFilter> paymentFilters = paymentFilterService.search(pfQuery);

        // Set the required request attributes
        request.setAttribute("owner", owner instanceof SystemAccountOwner ? null : owner);
        request.setAttribute("type", type);
        request.setAttribute("paymentFilters", paymentFilters);
        request.setAttribute("myAccount", context.getAccountOwner().equals(owner));
        request.setAttribute("status", status);
        request.setAttribute("unitsPattern", type.getCurrency().getPattern());
        request.setAttribute("account", account);
        request.setAttribute("aRate", aRateService.getActualRate(status));
        request.setAttribute("dRate", dRateService.getActualRate(status));

        if (type instanceof SystemAccountType) {
            final SystemAccountType systemType = (SystemAccountType) type;
            request.setAttribute("showConciliated", !systemType.getExternalAccounts().isEmpty());
        } else {
            request.setAttribute("showConciliated", Boolean.FALSE);
        }

        // Get the custom fields
        final List<PaymentCustomField> customFieldsForSearch = customFieldService.listPaymentFieldForSearch(account, false);
        final List<PaymentCustomField> customFieldsForList = customFieldService.listPaymentFieldForList(account, false);
        request.setAttribute("customFieldsForSearch", CustomFieldHelper.buildEntries(customFieldsForSearch, query.getCustomValues()));
        request.setAttribute("customFieldsForList", customFieldsForList);

        // Determine where to go back
        String backTo = null;
        if (type instanceof SystemAccountType) {
            if (!form.isSingleAccount()) {
                // On system accounts, only go back when there's an account overview
                backTo = context.getPathPrefix() + "/accountOverview";
            }
        } else {
            // A member account, go back to either overview or profile
            final Member member = (Member) owner;
            if (form.isSingleAccount()) {
                backTo = context.getPathPrefix() + "/profile?memberId=" + member.getId();
            } else {
                backTo = context.getPathPrefix() + "/accountOverview?memberId=" + member.getId();
            }
        }
        request.setAttribute("backTo", backTo);

        return query;
    }

    @Override
    protected boolean willExecuteQuery(final ActionContext context, final QueryParameters queryParameters) throws Exception {
        // The query is always executed
        return true;
    }

    private Account resolveAccount(final AccountType type, final AccountOwner owner) {
        final Collection<? extends AccountType> allowedTypes = accountService.getAllowedTypes(owner);
        if (allowedTypes != null && !allowedTypes.contains(type)) {
            throw new PermissionDeniedException();
        }
        return accountService.getAccount(new AccountDTO(owner, type));
    }

}