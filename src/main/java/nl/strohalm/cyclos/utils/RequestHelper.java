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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for request operations
 * @author luis
 */
public abstract class RequestHelper {

    public static String arrayToString(final Long[] list) {
        final StringBuilder strBuilder = new StringBuilder("[");
        for (final Long id : list) {
            strBuilder.append(id).append(",");
        }
        strBuilder.delete(strBuilder.length() - 1, strBuilder.length()).append("]");
        return strBuilder.toString();
    }

    /**
     * Finds a cookie with the given name, returning null when it's not found
     */
    public static Cookie getCookie(final ServletRequest servletRequest, final String name) {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Finds a cookie value with the given name, returning null when it's not found
     */
    public static String getCookieValue(final ServletRequest servletRequest, final String name) {
        final Cookie cookie = getCookie(servletRequest, name);
        return cookie == null ? null : cookie.getValue();
    }

    /**
     * Extract the username and password from the Authorization HTTP header, supporting both BASIC and USER authentications, or null when not informed
     */
    public static String[] getCredentials(final HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (StringUtils.isEmpty(header)) {
            return null;
        }
        final String[] parts = header.split("\\s");
        // There should be 2 parts separated by a blank space: the method and the username:password section
        if (parts.length != 2) {
            return null;
        }
        final String method = parts[0];
        String credentials = parts[1];
        if (method.equalsIgnoreCase("basic")) {
            credentials = new String(Base64.decodeBase64(credentials.getBytes()));
        } else if (!method.equalsIgnoreCase("user")) {
            // Unsupported method
            return null;
        }
        // Return the credentials parts
        return credentials.split(":", 2);
    }

    /**
     * Returns a path prefix according to the logged user, like /do/admin or /do/member
     */
    public static String getPathPrefix(final HttpServletRequest request) {
        String pathPrefix = (String) request.getSession().getAttribute("pathPrefix");
        if (pathPrefix == null) {
            if (request.getRequestURI().contains("/member")) {
                pathPrefix = "/do/member";
            } else {
                pathPrefix = "/do/admin";
            }
        }
        return pathPrefix;
    }

    /**
     * Returns if the given request was triggered by the menu
     */
    public static boolean isFromMenu(final HttpServletRequest request) {
        return "true".equals(request.getParameter("fromMenu"));
    }

    /**
     * Returns if the given request is a POST
     */
    public static boolean isGet(final HttpServletRequest request) {
        return !isValidation(request) && isMethod(request, "GET");
    }

    /**
     * Returns if the given request is a POST
     */
    public static boolean isPost(final HttpServletRequest request) {
        return !isValidation(request) && isMethod(request, "POST");
    }

    /**
     * Returns if the given request is a validation request
     */
    public static boolean isValidation(final HttpServletRequest request) {
        return "true".equals(request.getParameter("validation"));
    }

    /**
     * Checks whether the given request is for a web service
     */
    public static boolean isWebService(final HttpServletRequest request) {
        return request.getRequestURI().contains("/services/");
    }

    /**
     * Store the enum as an array, under the unqualified class name as key (with the first letter lowercased: ex: nl.strohalm...MyEnum = myEnum)
     */
    public static <E extends Enum<?>> void storeEnum(final HttpServletRequest request, final Class<E> enumType) {
        String name = ClassHelper.getClassName(enumType);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        storeEnum(request, enumType, name);
    }

    /**
     * Store the enum as an array under the specified key
     */
    public static <E extends Enum<?>> void storeEnum(final HttpServletRequest request, final Class<E> enumType, final String key) {
        final E[] values = EnumHelper.values(enumType);
        request.setAttribute(key, values);
    }

    /**
     * Store the enum as a Map under the specified key
     */
    public static <E extends Enum<?>> void storeEnumMap(final HttpServletRequest request, final Class<E> enumType, final String key) {
        final E[] values = EnumHelper.values(enumType);
        final Map<String, E> map = new LinkedHashMap<String, E>();
        for (final E e : values) {
            map.put(e.name(), e);
        }
        request.setAttribute(key, map);
    }

    private static boolean isMethod(final HttpServletRequest request, final String method) {
        try {
            return request.getMethod().equalsIgnoreCase(method);
        } catch (final Exception e) {
            return false;
        }
    }
}
