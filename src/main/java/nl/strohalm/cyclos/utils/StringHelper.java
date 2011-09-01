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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for handling strings
 * @author Jefferson Magno
 */
public class StringHelper {
    private static final Pattern ANY_TAG_PATTERN   = Pattern.compile("<\\/?[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final String  MASK_VARIABLES    = "#09aAlLuUcC?_";
    private static final String  DIGITS            = "0123456789";
    private static final String  LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String  UPERCASE_LETTERS  = LOWERCASE_LETTERS.toUpperCase();
    private static final String  LETTERS           = LOWERCASE_LETTERS + UPERCASE_LETTERS;

    /**
     * Removes the given mask from the given value
     */
    public static String applyMask(final String mask, final String value) {
        if (StringUtils.isEmpty(mask) || value == null) {
            return value;
        }
        final StringBuilder sb = new StringBuilder();
        boolean nextIsLiteral = false;
        int pos = 0;
        for (int i = 0; i < mask.length(); i++) {
            final char c = mask.charAt(i);
            if (c == '\\') {
                nextIsLiteral = true;
            } else if (!nextIsLiteral && MASK_VARIABLES.indexOf(c) >= 0) {
                try {
                    sb.append(value.charAt(pos++));
                } catch (final StringIndexOutOfBoundsException e) {
                    continue;
                }
            } else {
                nextIsLiteral = false;
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Decodes the url
     */
    public static String decodeUrl(final String url) {
        if (url == null) {
            return null;
        }
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Encodes the url
     */
    public static String encodeUrl(final String url) {
        if (url == null) {
            return null;
        }
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return url;
        }
    }

    /**
     * Returns whether the given string has at least one digit
     */
    public static boolean hasDigits(final String string) {
        return StringUtils.containsAny(string, DIGITS);
    }

    /**
     * Returns whether the given string has at least one ASCII letter
     */
    public static boolean hasLetters(final String string) {
        return StringUtils.containsAny(string, LETTERS);
    }

    /**
     * Returns whether the given string has at least one special character (nor digit, nor ASCII letter)
     */
    public static boolean hasSpecial(final String string) {
        final int length = string == null ? 0 : string.length();
        if (length == 0) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            final char c = string.charAt(i);
            if (LETTERS.indexOf(c) < 0 && DIGITS.indexOf(c) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the string is a valid Java identifier. It's not checked if the string is a reserved Java word.
     */
    public static boolean isValidJavaIdentifier(final String string) {
        if (StringUtils.isEmpty(string)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(string.charAt(0))) {
            return false;
        }
        for (int i = 1; i < string.length(); i++) {
            if (!Character.isJavaIdentifierPart(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static void main(final String[] args) {
        System.out.println(applyMask("(\\###) ####-####", "5191234567"));
        System.out.println(applyMask("(\\###) ####-####", "51912345"));
        System.out.println(removeMask("(\\###) ####-####", "(#51) 9123-4567"));
        System.out.println(removeMask("(\\###) ####-####", "519123-45"));
        System.out.println(removeMask("(\\###) ####-####", "519123-456789", true));
        System.out.println(removeMask("(\\###) ####-####", "519123-456789", false));
    }

    /**
     * Returns a string containing only the numbers in the given string, or null if input is null
     */
    public static String onlyNumbers(final String string) {
        if (string == null) {
            return null;
        }
        final StringBuilder numbers = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (Character.isDigit(c)) {
                numbers.append(c);
            }
        }
        return numbers.toString();
    }

    /**
     * Removes the CR (Window) characters from the text
     * @param text
     */
    public static String removeCarriageReturnCharater(final String text) {
        return StringUtils.replace(text, "\r\n", "\n");
    }

    /**
     * Removes all html/xml tags
     */
    public static String removeMarkupTags(final String string) {
        return removeMarkupTagsAndUnescapeEntities(string, false);
    }

    /**
     * Removes all html/xml tags and entities
     */
    public static String removeMarkupTagsAndUnescapeEntities(final String string) {
        return removeMarkupTagsAndUnescapeEntities(string, true);
    }

    /**
     * Removes the given mask from the given value, trimming to mask length
     */
    public static String removeMask(final String mask, final String value) {
        return removeMask(mask, value, true);
    }

    /**
     * Removes the given mask from the given value, optionally trimming at mask max length (ex: mask is ###, 12345 would result 123 it true, and 12345
     * if false)
     */
    public static String removeMask(final String mask, final String value, final boolean trimToMask) {
        if (StringUtils.isEmpty(mask) || value == null) {
            return value;
        }
        final StringBuilder sb = new StringBuilder();
        boolean nextIsLiteral = false;
        int pos = 0;
        for (int i = 0; i < mask.length(); i++) {
            final char c = mask.charAt(i);
            if (c == '\\') {
                nextIsLiteral = true;
            } else if (!nextIsLiteral && MASK_VARIABLES.indexOf(c) >= 0) {
                try {
                    while (!Character.isLetterOrDigit(value.charAt(pos))) {
                        pos++;
                    }
                    sb.append(value.charAt(pos++));
                } catch (final StringIndexOutOfBoundsException e) {
                    break;
                }
            } else {
                nextIsLiteral = false;
            }
        }
        if (!trimToMask) {
            sb.append(value.substring(pos));
        }
        return sb.toString();
    }

    /**
     * Remove a variable from a query string
     */
    public static String removeQueryStringVariable(String queryString, final String var) {
        final int pos = queryString.indexOf(var + "=");
        if (pos >= 0) {
            int end = queryString.indexOf('&', pos);
            if (end < 0) {
                end = queryString.length();
            }
            queryString = queryString.substring(0, pos) + queryString.substring(end);
        }
        return queryString;
    }

    private static String removeMarkupTagsAndUnescapeEntities(String string, final boolean unescapeEntities) {
        if (string == null) {
            return null;
        }
        string = ANY_TAG_PATTERN.matcher(string).replaceAll("");
        if (unescapeEntities) {
            string = StringEscapeUtils.unescapeHtml(string);
        }
        return string;
    }

}
