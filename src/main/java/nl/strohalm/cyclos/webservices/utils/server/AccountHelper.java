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
package nl.strohalm.cyclos.webservices.utils.server;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;
import nl.strohalm.cyclos.utils.query.Page;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.webservices.WebServiceContext;
import nl.strohalm.cyclos.webservices.WebServiceFaultsEnum;
import nl.strohalm.cyclos.webservices.accounts.AccountHistoryResultPage;
import nl.strohalm.cyclos.webservices.accounts.AccountHistorySearchParameters;
import nl.strohalm.cyclos.webservices.accounts.TransferTypeSearchParameters;
import nl.strohalm.cyclos.webservices.model.AccountHistoryTransferVO;
import nl.strohalm.cyclos.webservices.model.AccountStatusVO;
import nl.strohalm.cyclos.webservices.model.AccountTypeVO;
import nl.strohalm.cyclos.webservices.model.CurrencyVO;
import nl.strohalm.cyclos.webservices.model.DetailedAccountTypeVO;
import nl.strohalm.cyclos.webservices.model.FieldValueVO;
import nl.strohalm.cyclos.webservices.model.MemberAccountVO;
import nl.strohalm.cyclos.webservices.model.TransferTypeVO;
import nl.strohalm.cyclos.webservices.utils.WebServiceHelper;

/**
 * Utility class for accounts
 * @author luis
 */
public class AccountHelper {

    private CustomFieldService  customFieldService;
    private SettingsService     settingsService;
    private TransferTypeService transferTypeService;
    private FetchService        fetchService;
    private QueryHelper         queryHelper;
    private MemberHelper        memberHelper;
    private FieldHelper         fieldHelper;
    private ChannelHelper       channelHelper;
    private CurrencyHelper      currencyHelper;

    public CurrencyHelper getCurrencyHelper() {
        return currencyHelper;
    }

    public void setChannelHelper(final ChannelHelper channelHelper) {
        this.channelHelper = channelHelper;
    }

    public void setCurrencyHelper(final CurrencyHelper currencyHelper) {
        this.currencyHelper = currencyHelper;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setFieldHelper(final FieldHelper fieldHelper) {
        this.fieldHelper = fieldHelper;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setQueryHelper(final QueryHelper queryHelper) {
        this.queryHelper = queryHelper;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    /**
     * Transforms a MemberAccount into a {@link DetailedAccountTypeVO}
     */
    public DetailedAccountTypeVO toDetailedTypeVO(final String channel, MemberAccount memberAccount) {
        if (memberAccount == null) {
            return null;
        }
        if (memberAccount == null) {
            return null;
        }
        memberAccount = fetchService.fetch(memberAccount, Account.Relationships.TYPE, RelationshipHelper.nested(MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));

        // Find the default account type for the member's group
        final AccountType type = memberAccount.getType();
        final Member member = memberAccount.getOwner();
        final MemberGroup memberGroup = member.getMemberGroup();
        final boolean isDefault = isDefault(type, memberGroup);

        // Build the VO
        final DetailedAccountTypeVO vo = new DetailedAccountTypeVO();
        vo.setId(type.getId());
        vo.setName(type.getName());
        vo.setDefault(isDefault);
        vo.setCurrency(toVO(type.getCurrency()));

        // Search the possible transfer types
        final TransferTypeQuery query = new TransferTypeQuery();
        query.setChannel(channel);
        query.setContext(TransactionContext.PAYMENT);
        query.setFromOwner(member);
        query.setFromAccountType(type);
        final List<TransferTypeVO> transferTypes = new ArrayList<TransferTypeVO>();
        for (final TransferType tt : transferTypeService.search(query)) {
            transferTypes.add(toVO(tt));
        }
        vo.setTransferTypes(transferTypes);
        return vo;
    }

    public TransferQuery toQuery(final AccountHistorySearchParameters params) {
        if (params == null) {
            return null;
        }
        final TransferQuery query = new TransferQuery();
        queryHelper.fill(params, query);
        query.setReverseOrder(params.isReverseOrder());
        query.setType(CoercionHelper.coerce(AccountType.class, params.getAccountTypeId()));
        // Use the client member when restricted
        final Member ownerMember = memberHelper.resolveMember(params.getPrincipalType(), params.getPrincipal());
        query.setTransactionNumber(params.getTransactionNumber());
        query.setOwner(ownerMember == null ? SystemAccountOwner.instance() : ownerMember);
        query.setMember(memberHelper.loadByPrincipal(params.getRelatedMemberPrincipalType(), params.getRelatedMember()));
        final List<FieldValueVO> fields = params.getFields();
        if (fields != null && fields.size() > 0) {
            final Collection<PaymentCustomFieldValue> customFields = fieldHelper.toValueCollection(CustomField.Nature.PAYMENT, fields);
            query.setCustomValues(customFields);
        }
        if (params.getBeginDate() != null || params.getEndDate() != null) {
            final Period period = new Period(params.getBeginDate(), params.getEndDate());
            query.setPeriod(period);
        }
        return query;
    }

    public TransferTypeQuery toQuery(final TransferTypeSearchParameters params) {
        if (params == null) {
            return null;
        }

        final TransferTypeQuery query = new TransferTypeQuery();
        query.setResultType(ResultType.LIST);
        query.setCurrency(currencyHelper.resolve(params.getCurrency()));
        query.setFromAccountType(CoercionHelper.coerce(AccountType.class, params.getFromAccountTypeId()));
        query.setToAccountType(CoercionHelper.coerce(AccountType.class, params.getToAccountTypeId()));
        query.setChannel(channelHelper.restricted());

        final Member restrictedMember = WebServiceContext.getMember();

        if (restrictedMember != null) {
            if (params.getToMember() != null) {
                query.setToOwner(memberHelper.loadByPrincipal(params.getToMemberPrincipalType(), params.getToMember()));
            } else if (params.isToSystem()) {
                query.setToOwner(SystemAccountOwner.instance());
            } else {
                query.setToOwner(restrictedMember);
            }

            if (params.getFromMember() != null) {
                final Member member = memberHelper.loadByPrincipal(params.getFromMemberPrincipalType(), params.getFromMember());
                query.setFromOwner(member);
                query.setGroup(member == null ? null : member.getGroup()); // this is to take into account the group permissions in the search TT
            } else if (params.isFromSystem()) {
                query.setFromOwner(SystemAccountOwner.instance());
            }

            if (!restrictedMember.equals(query.getFromOwner()) && !restrictedMember.equals(query.getToOwner())) {
                WebServiceFaultsEnum.UNAUTHORIZED_ACCESS.throwFault();
            }
        } else {
            if (params.isFromSystem()) {
                query.setFromNature(AccountType.Nature.SYSTEM);
            } else {
                final Member member = memberHelper.loadByPrincipal(params.getFromMemberPrincipalType(), params.getFromMember());
                query.setFromOwner(member);
                query.setGroup(member == null ? null : member.getGroup()); // this is to take into account the group permissions in the search TT
            }

            if (params.isToSystem()) {
                query.setToNature(AccountType.Nature.SYSTEM);
            } else {
                query.setToOwner(memberHelper.loadByPrincipal(params.getToMemberPrincipalType(), params.getToMember()));
            }
        }

        return query;
    }

    /**
     * Transform a list of transfers into an account history result page
     */
    public AccountHistoryResultPage toResultPage(final AccountOwner owner, final List<Transfer> transfers) {
        final AccountHistoryResultPage page = new AccountHistoryResultPage();
        if (transfers instanceof Page<?>) {
            final Page<Transfer> transfersPage = (Page<Transfer>) transfers;
            page.setCurrentPage(transfersPage.getCurrentPage());
            page.setTotalCount(transfersPage.getTotalCount());
        }
        final List<AccountHistoryTransferVO> vos = new ArrayList<AccountHistoryTransferVO>();

        final Map<TransferType, Collection<PaymentCustomField>> customFieldsByTransferType = new HashMap<TransferType, Collection<PaymentCustomField>>();

        for (int i = 0; i < transfers.size(); i++) {
            final Transfer transfer = transfers.get(i);
            final TransferType transferType = transfer.getType();
            Collection<PaymentCustomField> customFields = customFieldsByTransferType.get(transferType);
            if (customFields == null) {
                customFields = customFieldService.listPaymentFields(transferType);
                customFieldsByTransferType.put(transferType, customFields);
            }

            vos.add(toVO(owner, transfer, customFields));
        }

        page.setTransfers(vos);
        return page;
    }

    /**
     * Transforms the given transfer to a VO, according to the member viewing it
     */
    public AccountHistoryTransferVO toVO(final AccountOwner viewingOwner, final Transfer transfer, Collection<PaymentCustomField> customFields) {
        if (transfer == null) {
            return null;
        }

        final LocalSettings localSettings = settingsService.getLocalSettings();
        final BigDecimal amount = transfer.getActualAmount();
        final AccountHistoryTransferVO vo = new AccountHistoryTransferVO();
        vo.setId(transfer.getId());
        vo.setDate(transfer.getDate());
        vo.setFormattedDate(localSettings.getDateConverter().toString(vo.getDate()));
        vo.setProcessDate(transfer.getProcessDate());
        vo.setFormattedProcessDate(localSettings.getDateConverter().toString(vo.getProcessDate()));
        vo.setTransactionNumber(transfer.getTransactionNumber());
        // only for the client which originate the transfer sets the trace number
        if (WebServiceHelper.isCurrentClient(transfer.getClientId())) {
            vo.setTraceNumber(transfer.getTraceNumber());
        }
        vo.setTransferType(toVO(transfer.getType()));

        final Member restrictedMember = WebServiceContext.getMember();
        if (restrictedMember == null && viewingOwner == null) { // in this case is a nonsense have a viewing member
            vo.setAmount(amount);
            setRelatedAccount(vo, fetchService.fetch(transfer.getActualFrom()), false);
            setRelatedAccount(vo, fetchService.fetch(transfer.getActualTo()), true);
        } else {
            final boolean isDebit = viewingOwner.equals(transfer.getActualFrom().getOwner());
            final Account relatedAccount = fetchService.fetch(isDebit ? transfer.getActualTo() : transfer.getActualFrom());
            vo.setAmount(isDebit ? amount.negate() : amount);
            setRelatedAccount(vo, relatedAccount, true);
        }
        vo.setFormattedAmount(localSettings.getUnitsConverter(transfer.getType().getFrom().getCurrency().getPattern()).toString(vo.getAmount()));
        if (customFields == null) {
            customFields = customFieldService.listPaymentFields(transfer.getType());
        }
        vo.setFields(fieldHelper.toList(customFields, transfer.getCustomValues()));
        return vo;
    }

    /**
     * Transforms the given account status to a VO
     */
    public AccountStatusVO toVO(final AccountStatus status) {
        if (status == null) {
            return null;
        }
        final UnitsConverter unitsConverter = settingsService.getLocalSettings().getUnitsConverter(status.getAccount().getType().getCurrency().getPattern());
        final AccountStatusVO vo = new AccountStatusVO();
        vo.setId(status.getId());
        vo.setBalance(status.getBalance());
        vo.setFormattedBalance(unitsConverter.toString(vo.getBalance()));
        vo.setAvailableBalance(status.getAvailableBalance());
        vo.setFormattedAvailableBalance(unitsConverter.toString(vo.getAvailableBalance()));
        vo.setReservedAmount(status.getReservedAmount());
        vo.setFormattedReservedAmount(unitsConverter.toString(vo.getReservedAmount()));
        vo.setCreditLimit(status.getCreditLimit());
        vo.setFormattedCreditLimit(unitsConverter.toString(vo.getCreditLimit()));
        vo.setUpperCreditLimit(status.getUpperCreditLimit());
        vo.setFormattedUpperCreditLimit(unitsConverter.toString(vo.getUpperCreditLimit()));
        return vo;
    }

    /**
     * Transforms the given account type to a VO
     */
    public AccountTypeVO toVO(final AccountType type) {
        if (type == null) {
            return null;
        }
        final AccountTypeVO vo = new AccountTypeVO();
        vo.setId(type.getId());
        vo.setCurrency(toVO(type.getCurrency()));
        vo.setName(type.getName());
        return vo;
    }

    /**
     * Transforms the given currency to a VO
     */
    public CurrencyVO toVO(final Currency currency) {
        if (currency == null) {
            return null;
        }
        final CurrencyVO vo = new CurrencyVO();
        vo.setId(currency.getId());
        vo.setName(currency.getName());
        vo.setSymbol(currency.getSymbol());
        vo.setPattern(currency.getPattern());
        return vo;
    }

    /**
     * Transforms the given account status to a VO
     */
    public MemberAccountVO toVO(MemberAccount memberAccount) {
        if (memberAccount == null) {
            return null;
        }
        memberAccount = fetchService.fetch(memberAccount, Account.Relationships.TYPE, RelationshipHelper.nested(MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));

        // Find the default account type for the member's group
        final AccountType type = memberAccount.getType();
        final MemberGroup memberGroup = memberAccount.getOwner().getMemberGroup();
        final boolean isDefault = isDefault(type, memberGroup);

        final MemberAccountVO vo = new MemberAccountVO();
        vo.setId(memberAccount.getId());
        vo.setDefault(isDefault);
        vo.setType(toVO(type));
        return vo;
    }

    /**
     * Transforms the given transfer type to a VO
     */
    public TransferTypeVO toVO(final TransferType tt) {
        if (tt == null) {
            return null;
        }
        final TransferTypeVO vo = new TransferTypeVO();
        vo.setId(tt.getId());
        vo.setName(tt.getName());
        vo.setFrom(toVO(tt.getFrom()));
        vo.setTo(toVO(tt.getTo()));
        return vo;
    }

    /**
     * Checks whether the given account type is the default for the given group
     */
    private boolean isDefault(final AccountType accountType, final MemberGroup memberGroup) {
        boolean isDefault = false;
        for (final MemberGroupAccountSettings accountSettings : memberGroup.getAccountSettings()) {
            if (accountSettings.getAccountType().equals(accountType) && accountSettings.isDefault()) {
                isDefault = true;
                break;
            }
        }
        return isDefault;
    }

    private void setRelatedAccount(final AccountHistoryTransferVO vo, final Account account, final boolean isRelated) {
        if (account instanceof MemberAccount) {
            if (isRelated) {
                vo.setMember(memberHelper.toVO((Member) account.getOwner()));
            } else {
                vo.setFromMember(memberHelper.toVO((Member) account.getOwner()));
            }
        } else {
            if (isRelated) {
                vo.setSystemAccountName(account.getOwnerName());
            } else {
                vo.setFromSystemAccountName(account.getOwnerName());
            }
        }
    }
}
