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
package mp.platform.cyclone.webservices.accounts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import mp.platform.cyclone.webservices.PrincipalParameters;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.WebServiceFaultsEnum;
import mp.platform.cyclone.webservices.model.AccountHistoryTransferVO;
import mp.platform.cyclone.webservices.model.MemberAccountVO;
import mp.platform.cyclone.webservices.model.TransferTypeVO;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;
import mp.platform.cyclone.webservices.utils.server.AccountHelper;
import mp.platform.cyclone.webservices.utils.server.CurrencyHelper;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation for account web service
 * @author luis
 */
@WebService(name = "accounts", serviceName = "accounts")
public class AccountWebServiceImpl implements AccountWebService {

    private AccountService      accountService;
    private AccessService       accessService;
    private PaymentService      paymentService;
    private TransferTypeService transferTypeService;
    private AccountHelper       accountHelper;
    private MemberHelper        memberHelper;
    private WebServiceHelper    webServiceHelper;
    private CustomFieldService  customFieldService;

    private CurrencyHelper      currencyHelper;
    private SettingsService     settingsService;

    @SuppressWarnings("unchecked")
    public List<MemberAccountVO> getMemberAccounts(final PrincipalParameters params) {
        final Member member = memberHelper.resolveMember(params);
        if (member == null) {
            return Collections.emptyList();
        }
        final List<MemberAccount> accounts = (List<MemberAccount>) accountService.getAccounts(member, Account.Relationships.TYPE);
        final List<MemberAccountVO> vos = new ArrayList<MemberAccountVO>(accounts.size());
        for (final MemberAccount account : accounts) {
            vos.add(accountHelper.toVO(account));
        }
        return vos;
    }

    public AccountHistoryTransferVO loadTransfer(final LoadTransferParameters params) {
        // Load the transfer
        Transfer transfer;
        try {
            transfer = paymentService.load(params.getTransferId(), Transfer.Relationships.FROM, Transfer.Relationships.TO, Transfer.Relationships.TYPE, Transfer.Relationships.CUSTOM_VALUES);
        } catch (final RuntimeException e) {
            webServiceHelper.error(e);
            throw e;
        }
        // Get and validate the member
        final Member member = memberHelper.resolveMember(params);
        if (member != null) {
            if (!member.equals(transfer.getFromOwner()) && !member.equals(transfer.getToOwner())) {
                throw new PermissionDeniedException();
            }
            // Ensure the channel is enabled for the given member
            memberHelper.checkChannelEnabledForMember(member);

            // Check credentials if required
            final String credentials = params.getCredentials();
            checkCredentialsIfNeeded(member, credentials);
        }
        // Convert to VO
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transfer.getType());
        final AccountOwner owner = member == null ? SystemAccountOwner.instance() : member;
        return accountHelper.toVO(owner, transfer, customFields);
    }

    public AccountHistoryResultPage searchAccountHistory(final AccountHistorySearchParameters params) {
        // Get the member and ensure it is valid
        final Member member = memberHelper.resolveMember(params.getPrincipalType(), params.getPrincipal());
        if (member == null) {
            WebServiceFaultsEnum.INVALID_PARAMETERS.throwFault("The member is null");
        }
        // Ensure the channel is enabled for the given member
        memberHelper.checkChannelEnabledForMember(member);

        // Check credentials if required
        final String credentials = params.getCredentials();
        checkCredentialsIfNeeded(member, credentials);

        // Get the query and account type
        final TransferQuery query = accountHelper.toQuery(params);
        AccountType type = query.getType();
        if (type == null) {
            // No account type id was passed. Try by currency first
            final Currency currency = currencyHelper.resolve(params.getCurrency());
            if (currency == null) {
                // No currency was passed: get the default account
                type = resolveDefaultAccountType(member);
            } else {
                // Get the first account with the given currency
                for (final Account account : accountService.getAccounts(member, Account.Relationships.TYPE)) {
                    if (currency.equals(account.getType().getCurrency())) {
                        type = account.getType();
                        break;
                    }
                }
                if (type == null) {
                    // No account of the given currency
                    WebServiceFaultsEnum.INVALID_PARAMETERS.throwFault("No account of the given currency: " + currency);
                }
            }
            query.setType(type);
        }

        final boolean isSmsChannel = WebServiceContext.getChannel().getInternalName().equals(settingsService.getLocalSettings().getSmsChannelName());
        if (isSmsChannel) {
            query.setExcludeTransferType(((MemberGroup) member.getGroup()).getMemberSettings().getSmsChargeTransferType());
        }

        // Perform the search and get the status
        try {
            final List<Transfer> transfers = paymentService.search(query);
            final AccountHistoryResultPage page = accountHelper.toResultPage(member, transfers);
            final AccountStatus status = accountService.getStatus(new GetTransactionsDTO(member, type));
            page.setAccountStatus(accountHelper.toVO(status));
            return page;
        } catch (final Exception e) {
            webServiceHelper.error(e);
            return null;
        }
    }

    public List<TransferTypeVO> searchTransferTypes(final TransferTypeSearchParameters params) {
        final ServiceClient client = WebServiceContext.getClient();
        final Collection<TransferType> transferTypePermissions = new ArrayList<TransferType>();
        transferTypePermissions.addAll(client.getDoPaymentTypes());
        transferTypePermissions.addAll(client.getReceivePaymentTypes());

        final TransferTypeQuery query = accountHelper.toQuery(params);
        final List<TransferType> transferTypes = transferTypeService.search(query);
        final List<TransferTypeVO> vos = new ArrayList<TransferTypeVO>(transferTypes.size());
        for (final TransferType transferType : transferTypes) {
            if (transferTypePermissions.contains(transferType)) {
                vos.add(accountHelper.toVO(transferType));
            }
        }
        return vos;
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAccountHelper(final AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setCurrencyHelper(final CurrencyHelper currencyHelper) {
        this.currencyHelper = currencyHelper;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void setWebServiceHelper(final WebServiceHelper webServiceHelper) {
        this.webServiceHelper = webServiceHelper;
    }

    private void checkCredentialsIfNeeded(final Member member, final String credentials) {
        final ServiceClient client = WebServiceContext.getClient();
        if (client.isCredentialsRequired()) {
            if (StringUtils.isEmpty(credentials)) {
                WebServiceFaultsEnum.INVALID_CREDENTIALS.throwFault();
            }
            accessService.checkCredentials(WebServiceContext.getChannel(), member.getMemberUser(), credentials, WebServiceContext.getRequest().getRemoteAddr(), WebServiceContext.getMember());
        }
    }

    @SuppressWarnings("unchecked")
    private AccountType resolveDefaultAccountType(final Member member) {
        final List<Account> allAccounts = (List<Account>) accountService.getAccounts(member);
        final Account defaultAccount = accountService.getDefaultAccountFromList(member, allAccounts);
        return defaultAccount == null ? null : defaultAccount.getType();
    }
}
