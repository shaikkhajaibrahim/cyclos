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
package nl.strohalm.cyclos.services.stats.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.reports.StatisticalQuery;
import nl.strohalm.cyclos.entities.reports.StatisticsWhatToShow;
import nl.strohalm.cyclos.utils.Period;

/**
 * Class with common helper functions for the statistics summarizing something per member.
 * @author Rinke
 */
public abstract class MemberStats {

    /**
     * the cached Map with the number of members per group per period.
     */
    private static Map<String, Integer> numberOfMembersPerGroupPerPeriod = null;

    /**
     * the cached StatisticalQuery
     */
    protected static StatisticalQuery   queryParameters;

    /**
     * gets the Number of Members belonging to the specified group in all needed periods. Reads the result Map from a static field if it has been
     * calculated before, and the QueryParameters have not changed. Used for retrieving the total number of members for GrossProduct and Number Of
     * Transactions in activity stats.
     * 
     * @param queryParameters the query object
     * @param elementDao an <code>ElementDAO</code> to be passed from the <code>Service</code>.
     * @return a <code>Map</code> with <code>String</code>s as keys, representing the Period name, and the value is an Integer indicating the
     * number of members in that period and set of groups.
     */
    public static Map<String, Integer> getNumberOfMembersPerGroupPerPeriod(final StatisticalQuery queryParameters, final ElementDAO elementDao) {
        // with any new request, a new StatisticalQuery Object is created, so we can simply use equals to know if we can use the cached data
        if (numberOfMembersPerGroupPerPeriod == null || !queryParameters.equals(MemberStats.queryParameters)) {
            // first, get all the periods from ...
            final Collection<Period> periods = new ArrayList<Period>();
            // ... through the time periods
            if (queryParameters.getWhatToShow() == StatisticsWhatToShow.THROUGH_TIME) {
                periods.addAll(Arrays.asList(queryParameters.getPeriods()));
            }
            // ... compare periods: two periods and ...
            if (queryParameters.getWhatToShow() == StatisticsWhatToShow.COMPARE_PERIODS) {
                final Period periodMain = queryParameters.getPeriodMain();
                final Period periodComparedTo = queryParameters.getPeriodComparedTo();
                periods.add(periodMain);
                periods.add(periodComparedTo);
            }
            // (Caution) ... single period or histogram and Top ten -> only MAIN PERIOD.
            if (queryParameters.getWhatToShow() == StatisticsWhatToShow.SINGLE_PERIOD || queryParameters.getWhatToShow() == StatisticsWhatToShow.DISTRIBUTION) {
                final Period periodMain = queryParameters.getPeriodMain();
                periods.add(periodMain);
            }
            final Collection<Group> groups = queryParameters.getGroups();
            final Map<String, Integer> membersIdsMap = new HashMap<String, Integer>();

            // Once we have the periods list, for each period, we do a search in the database
            // to get all the members in the select groups for respective period.
            for (final Period period : periods) {
                final Integer numberOfMembers = elementDao.getNumberOfMembersInGroupsInPeriod(groups, period);
                membersIdsMap.put(period.toString(), numberOfMembers);
            }
            MemberStats.numberOfMembersPerGroupPerPeriod = membersIdsMap;
            MemberStats.queryParameters = queryParameters;
        }
        return MemberStats.numberOfMembersPerGroupPerPeriod;
    }

    protected Period                      period;
    protected Collection<? extends Group> groups;
    protected int                         numberOfMembersForPeriod;
    protected ElementDAO                  elementDao;

    public MemberStats(final StatisticalQuery queryParameters, final Period period, final ElementDAO elementDao) {
        this.period = period;
        this.elementDao = elementDao;
        final Map<String, Integer> numberOfMembersPerGroupPerPeriod = MemberStats.getNumberOfMembersPerGroupPerPeriod(queryParameters, elementDao);
        if (period != null) {
            numberOfMembersForPeriod = numberOfMembersPerGroupPerPeriod.get(period.toString());
        }
        groups = queryParameters.getGroups();// TODO 9A: if empty, fill with all available groups
    }

    /**
     * gets the number of members for the default period.
     * @return an int indicating the number of members.
     */
    public int getNumberOfMembers() {
        return numberOfMembersForPeriod;
    }

}