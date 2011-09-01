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
package nl.strohalm.cyclos.services.stats.taxes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.reports.StatisticalQuery;
import nl.strohalm.cyclos.services.stats.general.MemberStats;
import nl.strohalm.cyclos.utils.Pair;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.statistics.ListOperations;

/**
 * This class defines common helper methods for retrieving stats on transfer volume per member. These may be incoming transfers (Gross Product for
 * Activity Stats) or outgoing transfers (Payments, for Taxes Stats). Subsequent subclasses divide between those two.
 * 
 * @author rinke
 * 
 */
public abstract class TransferVolumePerMemberStats extends MemberStats {

    /**
     * gets a list with personal transfer volume, one element for each member
     * @param sumByMember a list with pairs, where the first element is the member, and the second its personal transfer volume
     * @return a list with personal transfer volumes.
     */
    protected static List<Number> getSumOfTransfers(final List<Pair<Member, BigDecimal>> sumByMember) {
        return ListOperations.getSecondNumberFromPairCollection(sumByMember);
    }

    protected final PaymentFilter paymentFilter;
    protected final TransferDAO   transferDao;
    protected List<Number>        sumOfTransfersForTraders;

    public TransferVolumePerMemberStats(final StatisticalQuery queryParameters, final Period period, final ElementDAO elementDao, final TransferDAO transferDao) {
        super(queryParameters, period, elementDao);
        paymentFilter = queryParameters.getPaymentFilter();
        this.transferDao = transferDao;
    }

    /**
     * gets the List of transfer volumes for all members as a List.
     * @return a List containing personal transfer volumes for each member.
     */
    public List<Number> getListVolumePerAllMembers() {
        if (sumOfTransfersForTraders == null) {
            getListVolumePerTradingMember();
        }
        final List<Number> completeList = new ArrayList<Number>();
        final int extra = numberOfMembersForPeriod - sumOfTransfersForTraders.size();
        for (int i = 0; i < extra; i++) {
            completeList.add(new Double(0.0));
        }
        completeList.addAll(sumOfTransfersForTraders);
        return completeList;
    }

    /**
     * gets the List of transfer volumes only for trading members as a List. Currently not used outside this class.
     * @return a List containing personal transfer volumes for each member.
     */
    public List<Number> getListVolumePerTradingMember() {
        if (sumOfTransfersForTraders == null) {
            final List<Pair<Member, BigDecimal>> sumByMember = getSumOfTransfersPerTrader();
            sumOfTransfersForTraders = getSumOfTransfers(sumByMember);
        }
        return sumOfTransfersForTraders;
    }

    /**
     * gets the basic list with pairs, where the first element is the <code>Member</code> and the second element is the sum of transfers for this
     * member in this period.
     * @return a List with Members and there results (the sum of their transfers).
     */
    abstract public List<Pair<Member, BigDecimal>> getSumOfTransfersPerTrader();

    /**
     * gets the Volume for all members as an array.
     * @return a double[] containing personal transfer volumes for each member
     */
    public double[] getVolumePerAllMembers() {
        final List<Number> listGrossProductPerAllMembers = getListVolumePerAllMembers();
        return ListOperations.listToArray(listGrossProductPerAllMembers);
    }

}
