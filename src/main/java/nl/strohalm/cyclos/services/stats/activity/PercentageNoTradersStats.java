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

import javastat.inference.onesample.OneSampProp;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.reports.StatisticalActivityQuery;
import nl.strohalm.cyclos.entities.reports.StatisticalNumber;
import nl.strohalm.cyclos.services.stats.StatisticalService;
import nl.strohalm.cyclos.utils.Period;

/**
 * Class with common helper functions on retrieving statistics about the percentage of members not trading, for activity statistics.
 * @author Rinke
 */
public class PercentageNoTradersStats extends TransactionCountPerMemberStats {

    public PercentageNoTradersStats(final StatisticalActivityQuery queryParameters, final Period period, final TransferDAO transferDao, final ElementDAO elementDao) {
        super(queryParameters, period, transferDao, elementDao);
    }

    /**
     * calculates the percentage of members not trading, including a confidence interval
     * 
     * @param npart an int indicating the number of members not trading
     * @param nfull an int indicating the total number of members
     * @return a <code>StatisticalNumber</code> indicating the percentage of members not trading.
     */
    public StatisticalNumber getPercentageNoTraders(final int npart, final int nfull) {
        StatisticalNumber result = new StatisticalNumber();
        if (nfull >= StatisticalService.MINIMUM_NUMBER_OF_VALUES && nfull > 0) {
            final double div = 1.0 - ((double) npart) / ((double) nfull);
            if (nfull - npart > 0) {
                final OneSampProp oneSampProp = new OneSampProp(StatisticalService.ALPHA, 0.5, "equal", nfull - npart, nfull);
                final Double lowerBound = oneSampProp.confidenceInterval[0] * 100;
                final Double upperBound = oneSampProp.confidenceInterval[1] * 100;
                result = new StatisticalNumber(div * 100, lowerBound, upperBound, new Integer(2).byteValue());
            } else {
                result = new StatisticalNumber(div * 100, new Integer(2).byteValue());
            }
        }
        return result;
    }

}