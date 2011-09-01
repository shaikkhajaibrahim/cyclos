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
package nl.strohalm.cyclos.utils.conversion;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Converter that escapes / unescapes strings for JavaScript
 * @author luis
 */
public class JavaScriptStringEscapeConverter implements Converter<String> {

    private static final long                            serialVersionUID = 7231330753744204245L;
    private static final JavaScriptStringEscapeConverter INSTANCE         = new JavaScriptStringEscapeConverter();

    public static JavaScriptStringEscapeConverter instance() {
        return INSTANCE;
    }

    private JavaScriptStringEscapeConverter() {
    }

    public String toString(final String object) {
        if (object == null) {
            return "''";
        }
        return "'" + StringEscapeUtils.escapeJavaScript(object) + "'";
    }

    public String valueOf(String string) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("'")) {
            if (string.length() > 1) {
                string = string.substring(1);
            } else {
                string = "";
            }
        }
        if (string.endsWith("'")) {
            if (string.length() > 1) {
                string = string.substring(1, string.length());
            } else {
                string = "";
            }
        }
        return StringEscapeUtils.unescapeJavaScript(string);
    }

}
