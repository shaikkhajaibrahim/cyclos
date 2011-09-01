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
package nl.strohalm.cyclos.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import nl.strohalm.cyclos.entities.reports.ThroughTimeRange;
import nl.strohalm.cyclos.entities.settings.LocalSettings;

import org.apache.commons.lang.time.DateUtils;

/**
 * Helper class for dates
 * @author luis
 */
public class DateHelper {

    /**
     * Returns the number of days between 2 dates. Will be negative if date2 &lt; date1, 0 if both are the same day or positive otherwise
     */
    public static int daysBetween(Calendar date1, Calendar date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        date1 = truncate(date1);
        date2 = truncate(date2);
        return (int) ((date2.getTimeInMillis() - date1.getTimeInMillis()) / DateUtils.MILLIS_PER_DAY);
    }

    /**
     * inverse function of decimalDaysBetween: adds a BigDecimal number of days to a Calendar.
     * @param date1 the date to which something is added
     * @param augend the number of days which is added, as a BigDecimal. May be negative.
     * @return null if one of the arguments is null, else the date + augend days.
     */
    public static Calendar decimalDaysAdd(final Calendar date1, final BigDecimal augend) {
        if (date1 == null || augend == null) {
            return null;
        }
        final BigDecimal date1InMillis = new BigDecimal(date1.getTimeInMillis());
        final BigDecimal augendInMillis = augend.multiply(new BigDecimal(DateUtils.MILLIS_PER_DAY));
        final BigDecimal resultAsBig = date1InMillis.add(augendInMillis);
        final Calendar result = Calendar.getInstance();
        result.setTimeInMillis(resultAsBig.longValue());
        return result;
    }

    /**
     * Returns the number of days between 2 dates as a BigDecimal. Will be negative if date2 &lt; date1, or positive otherwise.
     */
    public static BigDecimal decimalDaysBetween(final Calendar date1, final Calendar date2) {
        if (date1 == null || date2 == null) {
            return BigDecimal.ZERO;
        }
        final BigDecimal difference = new BigDecimal(date2.getTimeInMillis() - date1.getTimeInMillis());
        final MathContext mathContext = new MathContext(LocalSettings.MAX_PRECISION);
        final BigDecimal result = difference.divide(new BigDecimal(DateUtils.MILLIS_PER_DAY), mathContext);
        return result;
    }

    /**
     * sets the time of the day of the first argument, so that it is equal to the time of the day of the second argument.
     * @param toEqualize a Calendar, whose time will be equalized to the source's time of the day
     * @param source the time of the day of this Calendar is taken.
     * @return a new Calendar object, with the date equal to toEqualize, but the time of the date is equal to that of the source Calendar.
     */
    public static Calendar equalizeTime(final Calendar toEqualize, final Calendar source) {
        if (source == null) {
            return null;
        }
        final int hourOfDay = source.get(Calendar.HOUR_OF_DAY);
        final int minute = source.get(Calendar.MINUTE);
        final int second = source.get(Calendar.SECOND);
        final int milliSecond = source.get(Calendar.MILLISECOND);
        final Calendar result = (Calendar) toEqualize.clone();
        result.set(Calendar.HOUR_OF_DAY, hourOfDay);
        result.set(Calendar.MINUTE, minute);
        result.set(Calendar.SECOND, second);
        result.set(Calendar.MILLISECOND, milliSecond);
        return result;
    }

    /**
     * Returns the begin date of the given period, handling null and applying the correct TimeZone
     */
    public static Calendar getBeginForParameter(final Period period) {
        if (period == null) {
            return null;
        }
        final Calendar begin = period.getBegin();
        if (begin == null) {
            return null;
        }
        final TimeZone parameterTimeZone = begin.getTimeZone();
        // We must consider the time when explicitly set or when time zone is in use (and different from the server time zone)
        if (period.isUseTime() || !TimeZone.getDefault().equals(parameterTimeZone)) {
            // When using time as is
            return begin;
        } else {
            // Truncate the begin date
            return DateHelper.truncate(begin);
        }
    }

    /**
     * Returns the end date of the given period, handling null and applying the correct TimeZone
     */
    public static Calendar getEndForParameter(final Period period) {
        if (period == null) {
            return null;
        }
        Calendar end = period.getEnd();
        if (end == null) {
            return null;
        }
        final TimeZone parameterTimeZone = end.getTimeZone();
        // We must consider the time when explicitly set or when time zone is in use (and different from the server time zone)
        if (period.isUseTime() || !TimeZone.getDefault().equals(parameterTimeZone)) {
            // Add a second to end
            end = (Calendar) end.clone();
            end.add(Calendar.SECOND, 1);
        } else {
            // Truncate the end date and set the next day
            end = DateHelper.truncateNextDay(end);
        }
        return end;
    }

    public static Map<String, Object> getLastCompletedMonthAndYear() {
        final Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int year = now.get(Calendar.YEAR);
        if (month == 0) {
            month = 12;
            year--;
        } else {
            month--;
        }
        final Map<String, Object> completedMonthAndYear = new HashMap<String, Object>();
        completedMonthAndYear.put("month", month);
        completedMonthAndYear.put("year", year);
        return completedMonthAndYear;
    }

    public static Map<String, Object> getLastCompletedQuarterAndYear() {
        final Calendar now = Calendar.getInstance();
        final int month = now.get(Calendar.MONTH);
        int year = now.get(Calendar.YEAR);
        Quarter quarter = null;
        switch (month) {
            case 0:
            case 1:
            case 2:
                quarter = Quarter.FOURTH;
                year--;
                break;
            case 3:
            case 4:
            case 5:
                quarter = Quarter.FIRST;
                break;
            case 6:
            case 7:
            case 8:
                quarter = Quarter.SECOND;
                break;
            case 9:
            case 10:
            case 11:
                quarter = Quarter.THIRD;
                break;
        }
        final Map<String, Object> completedQuarterAndYear = new HashMap<String, Object>();
        completedQuarterAndYear.put("quarter", quarter);
        completedQuarterAndYear.put("year", year);
        return completedQuarterAndYear;
    }

    public static Period[] getPeriodsThroughTheTime(final Period period, final ThroughTimeRange throughTimeRange) {
        final Calendar calendarIni = period.getBegin();
        final Calendar calendarFini = period.getEnd();
        final List<Period> result = new ArrayList<Period>();

        final int monthIni = calendarIni.get(Calendar.MONTH);
        final int monthFini = calendarFini.get(Calendar.MONTH);
        final int yearIni = calendarIni.get(Calendar.YEAR);
        final int yearFini = calendarFini.get(Calendar.YEAR);

        for (int year = calendarIni.get(Calendar.YEAR); year <= calendarFini.get(Calendar.YEAR); year++) {
            Period periodAux = null;
            int monthIniAux = 0;
            int monthFiniAux = 11;

            if (year == yearIni) {
                monthIniAux = monthIni;
            } else {
                monthIniAux = 0;
            }

            if (year == yearFini) {
                monthFiniAux = monthFini;
            } else {
                monthFiniAux = 11;
            }
            int increment = 1;

            // months or quarters
            if (throughTimeRange == ThroughTimeRange.MONTH || throughTimeRange == ThroughTimeRange.QUARTER) {
                // only quarters
                if (throughTimeRange == ThroughTimeRange.QUARTER) {
                    increment = 3;
                }
                for (int month = monthIniAux; month <= monthFiniAux; month = month + increment) {
                    final Calendar calendarIniAux = new GregorianCalendar(year, month, 1, 0, 0, 0);
                    final Calendar calendarFiniHlp = new GregorianCalendar(year, (month + increment - 1), 1);
                    final Calendar calendarFiniAux = new GregorianCalendar(year, (month + increment - 1), calendarFiniHlp.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                    periodAux = new Period(calendarIniAux, calendarFiniAux);
                    result.add(periodAux);
                }
            }
            // years, it doesn't need to iterate over months.
            else if (throughTimeRange == ThroughTimeRange.YEAR) {
                final Calendar calendarIniAux = new GregorianCalendar(year, 0, 1, 0, 0, 0);
                final Calendar calendarFiniAux = new GregorianCalendar(year, 11, 31, 23, 59, 59);
                periodAux = new Period(calendarIniAux, calendarFiniAux);
                result.add(periodAux);
            }
        }
        final Period[] periodResult = new Period[result.size()];
        return result.toArray(periodResult);
    }

    public static Period getYearPeriod(final int year) {
        // First day of the year
        Calendar begin = Calendar.getInstance();
        begin.set(Calendar.YEAR, year);
        begin = truncate(begin);

        // First day of the next year
        final Calendar end = Calendar.getInstance();
        end.set(Calendar.YEAR, year + 1);

        final Period yearPeriod = new Period();
        yearPeriod.setBegin(begin);
        yearPeriod.setEnd(end);
        return yearPeriod;
    }

    /**
     * Truncates a date, handling null. Doesn't modifies the Calendar parameter, returning a new modified instance
     */
    public static Calendar truncate(final Calendar date) {
        if (date == null) {
            return null;
        }
        return DateUtils.truncate(date, Calendar.DATE);
    }

    /**
     * Truncates a date and adds 1 day, handling null. Doesn't modifies the Calendar parameter, returning a new modified instance
     */
    public static Calendar truncateNextDay(Calendar date) {
        if (date == null) {
            return null;
        }
        date = (Calendar) date.clone();
        date.add(Calendar.DATE, 1);
        return truncate(date);
    }

    /**
     * Truncates a date and subtracts 1 day, handling null. Doesn't modifies the Calendar parameter, returning a new modified instance
     */
    public static Calendar truncatePreviosDay(Calendar date) {
        if (date == null) {
            return null;
        }
        date = (Calendar) date.clone();
        date.add(Calendar.DATE, -1);
        return truncate(date);
    }

}
