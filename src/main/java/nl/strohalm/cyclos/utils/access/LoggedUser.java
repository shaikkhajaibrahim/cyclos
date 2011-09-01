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
package nl.strohalm.cyclos.utils.access;

import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;

/**
 * Class that provides static access to the logged user, as well as custom attributes, using a ThreadLocal
 * @author luis
 */
public class LoggedUser {
    private static final String                           ELEMENT_KEY        = "cyclos.loggedUser.element";
    private static final String                           GROUP_KEY          = "cyclos.loggedUser.group";
    private static final String                           USER_KEY           = "cyclos.loggedUser.user";
    private static final String                           REMOTE_ADDRESS_KEY = "cyclos.loggedUser.remoteAddress";
    private static final ThreadLocal<Map<String, Object>> ATTRIBUTES         = new ThreadLocal<Map<String, Object>>();

    /**
     * Return the current account owner - system when an admin is logged, or the member
     */
    public static AccountOwner accountOwner() {
        final Element loggedElement = element();
        return loggedElement.getAccountOwner();
    }

    /**
     * Release resources. Should be called before the current thread dies
     */
    public static void cleanup() {
        final Map<String, Object> map = ATTRIBUTES.get();
        if (map != null) {
            map.clear();
        }
        ATTRIBUTES.remove();
    }

    /**
     * Returns the logged element
     */
    @SuppressWarnings("unchecked")
    public static <E extends Element> E element() {
        final Element element = getAttribute(ELEMENT_KEY);
        if (element == null) {
            throw new NotConnectedException();
        }
        return (E) element;
    }

    /**
     * Returns the attribute for the given key
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(final String key) {
        final Map<String, Object> map = ATTRIBUTES.get();
        if (map != null) {
            return (T) map.get(key);
        }
        return null;
    }

    /**
     * Returns the logged user's group
     */
    @SuppressWarnings("unchecked")
    public static <G extends Group> G group() {
        final Group group = getAttribute(GROUP_KEY);
        if (group == null) {
            throw new NotConnectedException();
        }
        return (G) group;
    }

    /**
     * Initializes. Should be called before any other methods on the current thread
     */
    public static void init(final User user) {
        init(user, null, null);
    }

    /**
     * Initializes. Should be called before any other methods on the current thread
     */
    public static void init(final User user, final String remoteAddress) {
        init(user, remoteAddress, null);
    }

    /**
     * Initializes. Should be called before any other methods on the current thread
     */
    public static void init(final User user, final String remoteAddress, final Map<String, Object> attributes) {
        final Element element = user.getElement();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(USER_KEY, user);
        map.put(ELEMENT_KEY, element);
        map.put(GROUP_KEY, element.getGroup());
        map.put(REMOTE_ADDRESS_KEY, remoteAddress);
        if (attributes != null) {
            map.putAll(attributes);
        }
        ATTRIBUTES.set(map);
    }

    /**
     * Returns if the logged user is an administrator
     */
    public static boolean isAdministrator() {
        final Group loggedGroup = group();
        return loggedGroup instanceof AdminGroup;
    }

    /**
     * Returns if the logged user is an broker
     */
    public static boolean isBroker() {
        final Group loggedGroup = group();
        return loggedGroup instanceof BrokerGroup;
    }

    /**
     * Returns if the logged user is a member
     */
    public static boolean isMember() {
        final Group loggedGroup = group();
        return loggedGroup instanceof MemberGroup;
    }

    /**
     * Return if the logged user is an operator
     */
    public static boolean isOperator() {
        final Group loggedGroup = group();
        return loggedGroup instanceof OperatorGroup;
    }

    /**
     * Returns if the current thread has been setted up
     */
    public static boolean isValid() {
        return ATTRIBUTES.get() != null;
    }

    /**
     * Returns the remote address of the logged user
     */
    public static String remoteAddress() {
        return getAttribute(REMOTE_ADDRESS_KEY);
    }

    /**
     * Sets an attribute for the given key
     */
    public static void setAttribute(final String key, final Object value) {
        final Map<String, Object> map = ATTRIBUTES.get();
        if (map != null) {
            map.put(key, value);
        }
    }

    /**
     * Returns the logged user
     */
    @SuppressWarnings("unchecked")
    public static <U extends User> U user() {
        final User user = getAttribute(USER_KEY);
        if (user == null) {
            throw new NotConnectedException();
        }
        return (U) user;
    }
}
