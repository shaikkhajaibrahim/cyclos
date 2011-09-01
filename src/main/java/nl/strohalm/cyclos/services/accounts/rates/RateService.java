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
package nl.strohalm.cyclos.services.accounts.rates;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.GrantLoanDTO;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * General interface for all rates business logic. At the moment there is a child interface for A-rates and one for D-rates.
 * 
 * @author Rinke
 * 
 */
public interface RateService extends Service {

    public static enum NullType implements StringValuedEnum {
        A_RATE("account_status.emission_date"), D_RATE("account_status.d_rate"), RBC("account_status.rate_balance_correction");

        private String value;

        private NullType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * applies the needed changes to AccountStatus and Transfer for a Transfer. These changes are:
     * <ul>
     * <li>sets the rate to the transfer
     * <li>reads the rate from the from account, and applies this via a merge to the to account.
     * </ul>
     * Note that both parameters are changed!!!
     * @param status the AccountStatus of the to-account
     * @param transfer
     */
    public void applyTransfer(AccountStatus status, Transfer transfer);

    /**
     * gets the rate of an account. Does this by taking the last available status of the account, and recalculating the stored rate for the given
     * date. If the virtualRatedBalance = 0, then the currency's creationValue for the rate is returned.
     */
    BigDecimal getActualRate(Account account, Calendar date);

    /**
     * gets the rate of an account, with accountStatus as input. returns null if no rates enabled.
     */
    BigDecimal getActualRate(AccountStatus status);

    /**
     * gets the rate for the given date. If the virtualRatedBalance = 0, then the currency's creationValue for the rate is returned.
     * @param date the date for which the rate is calculated. If null, the present date/time is used.
     * @return the rate. returns null if the particular rate is not enabled.
     */
    BigDecimal getActualRate(AccountStatus status, Calendar date);

    /**
     * checks if there null values on RateBalanceCorrection and initializes them if necessary. If a field is null, it is initialized on the status.
     * Both accounts in the transfer are initialized on this accountstatus field.
     * @param status
     * @return The same status object (but with initialized RBC field) is returned.
     */
    AccountStatus initializeRateBalanceCorrectionOnAccounts(AccountStatus status, Transfer transfer);

    /**
     * sets the rate to the transfer dto which puts the units on the member's account. This is needed so that further rate handling is done correctly
     * when the transfer evolving from the loan is created and processed.
     * @param dto the TransferDTO which will generate the transfer for the loan, and to which the rate will be set.
     * @param params the GrantLoanDTO, containing all the details of the loan invoking this transfer.
     */
    void setRateToLoanTransfer(TransferDTO dto, GrantLoanDTO params, Currency currency);

    /**
     * changes the Rate Balance Correction on the From Account of a transaction. Does the actual changing on the status object, but also returns the
     * new RateBalanceCorrection.<br>
     * <b>Be aware</b> to call this only once - and NOT twice, once for A-rate and once for D-rate. The method handles this for A- rate and D-rate in
     * one go.<br>
     * <b>Be also aware</b> to call this BEFORE all other account changes resulting from a transfer have been applied, as the method supposes that the
     * balance is the balance BEFORE the transfer was paid.
     * 
     * @param status
     * @param amount
     * @return the new RateBalanceCorrection as it is saved to the status.
     */
    BigDecimal updateRateBalanceCorrectionOnFromAccount(AccountStatus status, BigDecimal amount);

}
