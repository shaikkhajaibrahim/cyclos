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
package nl.strohalm.cyclos.services.accounts;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.dao.accounts.AccountDAO;
import nl.strohalm.cyclos.dao.accounts.AccountStatusDAO;
import nl.strohalm.cyclos.dao.accounts.MemberGroupAccountSettingsDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.transactions.GrantSinglePaymentLoanDTO;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.utils.TimePeriod;

/**
 * Handles member account creation / removal
 * @author luis
 */
public class MemberAccountHandler {
    private static final float            PRECISION_DELTA = 0.0001F;
    private ElementDAO                    elementDao;
    private AccountDAO                    accountDao;
    private AccountStatusDAO              accountStatusDao;
    private FetchDAO                      fetchDao;
    private PaymentService                paymentService;
    private LoanService                   loanService;
    private MemberGroupAccountSettingsDAO memberGroupAccountSettingsDao;
    private AccountStatusHandler          accountStatusHandler;

    /**
     * Activate the member account if it exists, or create if it doesn't
     */
    public MemberAccount activate(final Member member, final MemberAccountType type) {
        MemberAccount account;
        try {
            account = (MemberAccount) accountDao.load(member, type);
            account = activate(account);
        } catch (final EntityNotFoundException e) {
            account = create(member, type);
        }
        // Make the member active if he was not yet
        if (member.getActivationDate() == null) {
            member.setActivationDate(Calendar.getInstance());
            elementDao.update(member);
        }
        return account;
    }

    /**
     * Activate the member account
     */
    public MemberAccount activate(final MemberAccount account) {
        // The account exists, mark as active
        account.setStatus(MemberAccount.Status.ACTIVE);
        accountDao.update(account);
        return account;
    }

    /**
     * Create a member account
     */
    public MemberAccount create(Member member, final MemberAccountType type) {

        if (member == null || member.isTransient() || type == null || type.isTransient()) {
            throw new UnexpectedEntityException();
        }
        member = fetchDao.fetch(member, Element.Relationships.USER, Element.Relationships.GROUP);

        final MemberGroupAccountSettings accountSettings = memberGroupAccountSettingsDao.load(member.getMemberGroup().getId(), type.getId());

        // Create the account
        final MemberAccount account = new MemberAccount();
        account.setCreationDate(Calendar.getInstance());
        account.setCreditLimit(accountSettings.getDefaultCreditLimit());
        account.setUpperCreditLimit(accountSettings.getDefaultUpperCreditLimit());
        account.setType(type);
        account.setMember(member);
        account.setOwnerName(member.getUsername());
        accountDao.insert(account);

        // Create the initial account status
        final AccountStatus status = new MemberAccountStatus();
        status.setAccount(account);
        status.setDate(account.getCreationDate());
        status.setCreditLimit(account.getCreditLimit());
        status.setUpperCreditLimit(account.getUpperCreditLimit());
        accountStatusDao.insert(status);

        // Check for initial credit
        final BigDecimal initialCredit = accountSettings.getInitialCredit();
        final TransferType initialCreditTransferType = accountSettings.getInitialCreditTransferType();
        final BigDecimal minimumPayment = paymentService.getMinimumPayment();
        if (initialCredit != null && (initialCredit.compareTo(minimumPayment) > 0) && initialCreditTransferType != null) {
            if (initialCreditTransferType.isLoanType()) {
                Integer repaymentDays = initialCreditTransferType.getLoan().getRepaymentDays();
                if (repaymentDays == null) {
                    repaymentDays = 30;
                }
                final GrantSinglePaymentLoanDTO dto = new GrantSinglePaymentLoanDTO();
                dto.setRepaymentDate(new TimePeriod(repaymentDays, TimePeriod.Field.DAYS).add(Calendar.getInstance()));
                dto.setMember(member);
                dto.setAmount(initialCredit);
                dto.setTransferType(initialCreditTransferType);
                dto.setDescription(initialCreditTransferType.getDescription());
                loanService.grantInitialCredit(dto);
            } else {
                final TransferDTO dto = new TransferDTO();
                dto.setContext(TransactionContext.AUTOMATIC);
                dto.setAmount(initialCredit);
                dto.setTransferType(initialCreditTransferType);
                dto.setFromOwner(SystemAccountOwner.instance());
                dto.setToOwner(member);
                dto.setDescription(initialCreditTransferType.getDescription());
                paymentService.insertWithoutNotification(dto);
            }
        }

        return account;
    }

    /**
     * Deactivate a member account, or remove it if it has no transactions
     */
    public void deactivate(final Member member, final MemberAccountType type, final boolean enforceZeroBalance) {
        try {
            final MemberAccount account = (MemberAccount) accountDao.load(member, type);
            deactivate(account, enforceZeroBalance);
        } catch (final EntityNotFoundException e) {
            // Ok, the account already didn't exist
        }
    }

    /**
     * When there are no transfers, remove the account. When there are transfers and the balance is zero, set the status to inactive. Otherwise, when
     * balance is not zero, throws UnexpectedEntityException
     */
    public void deactivate(final MemberAccount account, final boolean enforceZeroBalance) {
        final AccountStatus status = accountStatusHandler.getStatus(account, null, false, false);
        final boolean hasCredits = status.getCredits().getCount() > 0;
        final boolean hasDebits = status.getDebits().getCount() > 0;
        final boolean hasCreditLimit = status.getCreditLimit() != null && Math.abs(status.getCreditLimit().floatValue()) > PRECISION_DELTA;
        if (hasCredits || hasDebits || hasCreditLimit) {
            if (enforceZeroBalance && Math.abs(status.getBalance().floatValue()) > PRECISION_DELTA) {
                // When there is non zero balance in the account, throw an error
                throw new UnexpectedEntityException();
            }
            account.setStatus(MemberAccount.Status.INACTIVE);
            accountDao.update(account);
        } else {
            accountDao.delete(account.getId());
        }
    }

    public void setAccountDao(final AccountDAO accountDao) {
        this.accountDao = accountDao;
    }

    public void setAccountSettingsDao(final MemberGroupAccountSettingsDAO accountSettingsDao) {
        memberGroupAccountSettingsDao = accountSettingsDao;
    }

    public void setAccountStatusDao(final AccountStatusDAO accountStatusDao) {
        this.accountStatusDao = accountStatusDao;
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setElementDao(final ElementDAO elementDao) {
        this.elementDao = elementDao;
    }

    public void setFetchDao(final FetchDAO fetchDao) {
        this.fetchDao = fetchDao;
    }

    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    public void setMemberGroupAccountSettingsDao(final MemberGroupAccountSettingsDAO memberGroupAccountSettingsDao) {
        this.memberGroupAccountSettingsDao = memberGroupAccountSettingsDao;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}