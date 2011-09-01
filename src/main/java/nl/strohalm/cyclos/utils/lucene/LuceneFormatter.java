/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.utils.lucene;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Formats basic types to Strings in order to be indexed
 * @author luis
 */
public class LuceneFormatter {
    public static String            MAX_DECIMAL    = "9999999999999999.999999";
    public static String            MIN_DECIMAL    = "-999999999999999.999999";
    public static String            MAX_DATE       = "99999999999999";
    public static String            MIN_DATE       = "00000000000000";

    private static SimpleDateFormat DATE_FORMAT    = new SimpleDateFormat("yyyyMMddHHmmss");
    private static DecimalFormat    NUMBER_FORMAT  = new DecimalFormat("0000000000000000");
    private static DecimalFormat    DECIMAL_FORMAT = new DecimalFormat("0000000000000000.000000", new DecimalFormatSymbols(Locale.US));

    /**
     * Formats a date
     */
    public static String format(final Calendar date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date.getTime());
    }

    /**
     * Formats a number
     */
    public static String format(final Number number) {
        if (number == null) {
            return null;
        }
        if (number instanceof Integer || number instanceof Long || number instanceof BigInteger) {
            return NUMBER_FORMAT.format(number.longValue());
        } else {
            return DECIMAL_FORMAT.format(number.doubleValue());
        }
    }

}
