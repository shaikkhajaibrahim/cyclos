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
package nl.strohalm.cyclos.utils.validation;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;

/**
 * A validation for urls
 * @author luis
 */
public class URLValidation implements PropertyValidation {
    private static final long          serialVersionUID = -7933981104151866154L;
    private static final URLValidation INSTANCE         = new URLValidation();

    public static URLValidation instance() {
        return INSTANCE;
    }

    private URLValidation() {
    }

    public ValidationError validate(final Object object, final Object property, final Object value) {
        String str = (String) value;
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        try {
            // Use http as the default protocol
            if (!str.contains("://")) {
                str = "http://" + str;
            }
            final URL url = new URL(str);
            final String protocol = url.getProtocol();
            // Only allow http or https
            if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
                throw new MalformedURLException();
            }
            return null;
        } catch (final MalformedURLException e) {
            return new InvalidError();
        }
    }
}
