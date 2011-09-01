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

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.time.DateUtils;

/**
 * A date period. Both boundaries are considered in the period.
 * @author luis
 * 
 * Although the useTime flag may be true, the dates begin and end of the period may have a non-zero time.
 * @author jcomas
 */
public class Period implements Serializable, Cloneable {

    private static final long serialVersionUID = 6246167529034823152L;

    public static Period begginingAt(final Calendar begin) {
        return new Period(begin, null);
    }

    public static Period between(final Calendar begin, final Calendar end) {
        return new Period(begin, end);
    }

    public static Period betweenOneYear(final int year) {
        final Calendar begin = new GregorianCalendar(year, 0, 1);
        final Calendar end = new GregorianCalendar(year, 11, 31, 23, 59, 59);
        return between(begin, end);
    }

    public static Period day(Calendar day) {
        day = DateHelper.truncate(day);
        return new Period(day, day);
    }

    public static Period endingAt(final Calendar end) {
        return new Period(null, end);
    }

    public static Period exact(final Calendar time) {
        return new Period(time, time);
    }

    private Calendar begin;
    private Calendar end;
    private boolean  useTime;

    public Period() {
    }

    public Period(final Calendar begin, final Calendar end) {
        setBegin(begin);
        setEnd(end);
    }

    @Override
    public Period clone() {
        Period newPeriod;
        try {
            newPeriod = (Period) super.clone();
        } catch (final CloneNotSupportedException e) {
            // this should never happen, since it is Cloneable
            throw new InternalError(e.getMessage());
        }
        newPeriod.begin = begin == null ? null : (Calendar) begin.clone();
        newPeriod.end = end == null ? null : (Calendar) end.clone();

        return newPeriod;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Period)) {
            return false;
        }
        final Period p = (Period) obj;
        return new EqualsBuilder().append(begin, p.begin).append(end, p.end).isEquals();
    }

    public Calendar getBegin() {
        return begin;
    }

    public Quarter getBeginQuarter() {
        return getQuarter(begin);
    }

    /**
     * @return The difference between beginDate and endDate from period in seconds
     */
    public long getDifference() {
        if (begin == null || end == null) {
            throw new IllegalStateException("Not a complete period: " + this);
        }

        final double millis = end.getTimeInMillis() - begin.getTimeInMillis();
        return (long) Math.ceil(millis / DateUtils.MILLIS_PER_SECOND);
    }

    public Calendar getEnd() {
        return end;
    }

    public Quarter getEndQuarter() {
        return getQuarter(end);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(begin).append(end).toHashCode();
    }

    /**
     * Checks whether the given date is included in this period. When the useTime flag is true, the time of the parameter (date) and the time of the
     * begin and end dates of the period are taken into account to compute the result. When the useTime flag is false, the dates are truncated to
     * compute the result.
     * 
     */
    public boolean includes(final Calendar date) {

        if (date == null) {
            return false;
        } else if (begin == null && end == null) {
            return true;
        } else {

            if (useTime) {
                if (begin == null) {
                    return !date.after(end);
                } else if (end == null) {
                    return !date.before(begin);
                } else {
                    return !date.before(begin) && !date.after(end);
                }
            } else {

                final Calendar tDate = DateUtils.truncate(date, Calendar.DATE);
                Calendar tBegin = begin;
                Calendar tEnd = end;

                if (begin != null) {
                    tBegin = DateUtils.truncate(begin, Calendar.DATE);
                }
                if (end != null) {
                    // If not using time, we'll asume the end of the interval is
                    // the instant before the next day.
                    tEnd = DateHelper.truncateNextDay(end);
                }

                if (tBegin == null) {
                    // it's included if the date is an instant before the next day.
                    return tDate.before(tEnd);
                } else if (tEnd == null) {
                    // it's included if the date is not before the begin
                    return !tDate.before(tBegin);
                } else {
                    return !tDate.before(tBegin) && tDate.before(tEnd);
                }
            }
        }
    }

    public boolean isUseTime() {
        return useTime;
    }

    public void setBegin(final Calendar begin) {
        this.begin = begin;
    }

    public void setEnd(final Calendar end) {
        this.end = end;
    }

    public void setUseTime(final boolean useTime) {
        this.useTime = useTime;
    }

    @Override
    public String toString() {
        return "begin: " + FormatObject.formatObject(begin, "<null>") + ", end: " + FormatObject.formatObject(end, "<null>");
    }

    public Period useTime() {
        useTime = true;
        return this;
    }

    private Quarter getQuarter(final Calendar cal) {
        final int month = cal.get(Calendar.MONTH);
        int quarter = month / 3;
        quarter++;
        return Quarter.getQuarter(quarter);
    }
}
