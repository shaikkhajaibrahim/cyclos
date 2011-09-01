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
package nl.strohalm.cyclos.services.stats.tests;

public interface TestStatsConstants {

    // CONVENIENT CONSTANTS FOR GROUP ID'S:
    static final long     FULLMEMBERS         = 5L;
    static final long     FULLBROKERS         = 10L;
    static final long     NOCHANGE            = 0L;
    static final double[] AMOUNTS             = { 3, 5, 7, 11, 13, 17, 103, 105, 107, 111, 113, 117, 0.03, 0.05, 0.07, 0.11, 0.13, 0.17 };

    // WHILE TESTING, SET THESE FINAL CONSTANTS
    // broker, who pays all but does not have group changes nor earnings, nor logins
    static final String   brokerCreationDate  = "2007/01/01";

    // member 1
    static final String   member1CreationDate = "2007/07/11";
    static final long     mem1StartGroup      = FULLMEMBERS;
    // group changes
    static final String[] member1GroupChanges = { "2007/09/01" };
    static final long[]   member1NewGroups    = { NOCHANGE };
    // earings
    static final String[] member1EarnDates    = new String[] { "2007/08/01", "2007/09/11", "2007/10/15" };
    // logins
    static final String[] member1LoginList    = member1EarnDates;

    // member 2
    static final String   member2CreationDate = "2007/01/11";
    static final long     mem2StartGroup      = FULLMEMBERS;
    static final String[] member2GroupChanges = { "2007/11/01" };
    static final long[]   member2NewGroups    = { NOCHANGE };
    static final String[] member2EarnDates    = new String[] { "2007/08/01", "2007/09/11", "2007/11/11" };
    static final String[] member2LoginList    = member2EarnDates;

    // member 3
    static final String   member3CreationDate = "2007/01/11";
    static final long     mem3StartGroup      = FULLMEMBERS;
    static final String[] member3GroupChanges = { "2007/11/01" };
    static final long[]   member3NewGroups    = { NOCHANGE };
    static final String[] member3EarnDates    = new String[] { "2007/08/16", "2007/09/11", "2007/09/12" };
    static final String[] member3LoginList    = member3EarnDates;

    // member 4
    static final String   member4CreationDate = "2007/01/01";
    static final long     mem4StartGroup      = 5L;
    static final String[] member4GroupChanges = { "2007/02/01" };
    static final long[]   member4NewGroups    = { NOCHANGE };
    static final String[] member4EarnDates    = new String[] { "2007/06/01", "2007/08/01", "2007/09/01", "2007/09/14" };
    static final String[] member4LoginList    = member4EarnDates;

    // END OF FINAL TEST CONSTANTS.

}
