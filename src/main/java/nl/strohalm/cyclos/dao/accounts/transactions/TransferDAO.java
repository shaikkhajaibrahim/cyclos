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
package nl.strohalm.cyclos.dao.accounts.transactions;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.UpdatableDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransfersAwaitingAuthorizationQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.reports.StatisticalDTO;
import nl.strohalm.cyclos.services.stats.general.KeyDevelopmentsStatsPerMonthVO;
import nl.strohalm.cyclos.utils.Pair;
import nl.strohalm.cyclos.utils.Period;

/**
 * Data access object interface for transfers
 * @author rafael
 */
public interface TransferDAO extends BaseDAO<Transfer>, InsertableDAO<Transfer>, UpdatableDAO<Transfer> {

    /**
     * List of sums of incoming transactions amounts per member. Used by Activity: all using GrossProduct. <b>Important NOTE:</b> Beware that this
     * method only gets the members who were actually trading. Members with NO incoming trades are NOT included. If you need those, you need to
     * manually add them to the list.
     * @return a List of Pair objects where the first element is the member and the second is the gross product of the member (sum of incoming
     * transactions).
     */
    List<Pair<Member, BigDecimal>> getGrossProductPerMember(StatisticalDTO dto) throws DaoException;

    /**
     * gets the gross product summed over all members in a list, where each element in the list specifies a specific month in the period.
     * @return a list with <code>KeyDevelopmentsStatsPerMonthVO</code>s, containing for each element the gross products, the year and the month.
     */
    List<KeyDevelopmentsStatsPerMonthVO> getGrossProductPerMonth(final StatisticalDTO dto);

    /**
     * List of numbers of incoming transactions per member. Used by Activity Stats > all which use number of trans, % not trading
     * @return a List of Pair objects where the first element is the member and the second is the number of transactions
     * @param dto parameters that filter the query
     */
    List<Pair<Member, Integer>> getNumberOfTransactionsPerMember(StatisticalDTO dto) throws DaoException;

    /**
     * gets the number of transactions summed over all members in a list, where each element in the list specifies a specific month in the period.
     * @return a list with <code>KeyDevelopmentsStatsPerMonthVO</code>s, containing for each element the gross products, the year and the month.
     */
    List<KeyDevelopmentsStatsPerMonthVO> getNumberOfTransactionsPerMonth(final StatisticalDTO dto);

    /**
     * List of sums of outgoing transaction amounts (payments) per member. Used by Taxes Stats. <b>Important NOTE:</b> Beware that this method only
     * gets the members who were actually trading. Members with NO incoming trades are NOT included. If you need those, you need to manually add them
     * to the list.
     * @param dto parameters that filter the query
     * @return a List of Pair objects where the first element is the member and the second is the sum of payments done by this member
     */
    List<Pair<Member, BigDecimal>> getPaymentsPerMember(StatisticalDTO dto) throws DaoException;

    /**
     * Sum of the amounts of the transactions. Used by Key Dev Stats > gross product
     * @param dto a StatisticalDTO object passing the query
     */
    BigDecimal getSumOfTransactions(StatisticalDTO dto) throws DaoException;

    /**
     * Calculates the sum of transactions there was on this SystemAccountType for any payments NOT belonging to the set of paymentFilters, during the
     * period
     */
    BigDecimal getSumOfTransactionsRest(TransferQuery query);

    /**
     * gets a list with transaction amounts and their id's. There is no separate query for the number of transactions; just use this one and the size
     * of the resulting list is the number of transactions. This is more efficient than a separate query.
     */
    List<Number> getTransactionAmounts(final StatisticalDTO dto);

    /**
     * Loads a transfer generated by the client and with the specified trace number and generated by the client id
     * @param traceNumber
     * @param clientId
     */
    Transfer loadTransferByTraceNumber(String traceNumber, Long clientId);

    /**
     * Returns simple transfers VOs for a given account and period, ordering results by date ascending
     */
    List<SimpleTransferVO> paymentVOs(Account account, Period period) throws DaoException;

    /**
     * Searches for transfers. If no entity can be found, returns an empty list. If any exception is thrown by the underlying implementation, it
     * should be wrapped by a DaoException.
     * 
     * <p>
     * The condition specified by <code>query.getMember()</code> should only be taken in account when <code>query.getOwner() != null</code> and
     * <code>query.getType() != null</code>.
     */
    List<Transfer> search(TransferQuery query) throws DaoException;

    /**
     * Searches for transfers awaiting authorization
     */
    List<Transfer> searchTransfersAwaitingAuthorization(TransfersAwaitingAuthorizationQuery query);

    /**
     * Updates the transfer with authorization data
     */
    Transfer updateAuthorizationData(Long id, Transfer.Status status, AuthorizationLevel nextLevel, Calendar processDate);

    /**
     * Updates the transfer with the chargeback
     */
    Transfer updateChargeBack(Transfer transfer, Transfer chargeback);

    /**
     * Updates the transfer with the external transfer
     */
    Transfer updateExternalTransfer(Long id, ExternalTransfer externalTransfer);

    /**
     * Updates the transfer with the status
     */
    Transfer updateStatus(Long id, Payment.Status status);

    /**
     * Updates the transfer with the generated transaction number
     */
    Transfer updateTransactionNumber(Long id, String transactionNumber);
}