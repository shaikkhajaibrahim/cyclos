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
package nl.strohalm.cyclos.services.stats.activity;

import java.util.ArrayList;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.reports.StatisticalActivityQuery;
import nl.strohalm.cyclos.entities.reports.StatisticalDTO;
import nl.strohalm.cyclos.services.stats.general.MemberStats;
import nl.strohalm.cyclos.utils.Pair;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.statistics.ListOperations;

/**
 * Class with common helper functions on retrieving statistics about transactions per member, for activity statistics.
 * @author Rinke
 */
public class TransactionCountPerMemberStats extends MemberStats {

    /**
     * gets a list with number of transfers, where each element is the personal number of transactions of a member
     * @param countByMember a list with <code>Pair</code>s, where the first element is the member, and the second is his number of transfers.
     */
    private static List<Number> getTransfers(final List<Pair<Member, Integer>> countByMember) {
        return ListOperations.getSecondNumberFromPairCollection(countByMember);
    }

    private final PaymentFilter paymentFilter;
    private final TransferDAO   transferDao;

    private List<Number>        transfersForTraders;

    public TransactionCountPerMemberStats(final StatisticalActivityQuery queryParameters, final Period period, final TransferDAO transferDao, final ElementDAO elementDao) {
        super(queryParameters, period, elementDao);
        paymentFilter = queryParameters.getPaymentFilter();
        this.transferDao = transferDao;
    }

    /**
     * For each member, the number of transactions is counted. All these results are put in a list, which is returned by this method.
     * @return a List with Numbers representing the number of transactions each member performed
     */
    public synchronized List<Number> getTransactionCountPerAllMembers() {
        if (transfersForTraders == null) {
            getTransactionCountPerTradingMember();
        }
        final List<Number> completeList = new ArrayList<Number>();
        final int extra = numberOfMembersForPeriod - transfersForTraders.size();
        for (int i = 0; i < extra; i++) {
            completeList.add(new Integer(0));
        }
        completeList.addAll(transfersForTraders);
        return completeList;
    }

    /**
     * For each trading member, the number of transactions is counted. All these results are put in a list, which is returned by this method.
     * @return a List with Numbers representing the number of transactions each trading member performed
     */
    public synchronized List<Number> getTransactionCountPerTradingMember() {
        if (transfersForTraders == null) {
            final List<Pair<Member, Integer>> countByMember = getTransfersPerTrader();
            transfersForTraders = getTransfers(countByMember);
        }
        return transfersForTraders;
    }

    /**
     * @return a list with pairs, where the first element is the member, and the second is it's number of transfers. Only trading members are
     * included.
     */
    public List<Pair<Member, Integer>> getTransfersPerTrader() {
        final StatisticalDTO dto = new StatisticalDTO(period, paymentFilter, groups);
        return transferDao.getNumberOfTransactionsPerMember(dto);
    }

}