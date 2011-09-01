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
package nl.strohalm.cyclos.utils;

/**
 * Holds data about the current invocation (web/service/aspect, etc)
 * @author ameyer
 */
public class CurrentInvocationData {
    private static ThreadLocal<Boolean> HOLDER = new ThreadLocal<Boolean>();

    public static void cleanup() {
        HOLDER.set(null);

    }

    public static boolean isSystemInvocation() {
        final Boolean v = HOLDER.get();
        return v == null ? true : v;
    }

    public static void setSystemRequest(final boolean systemRequest) {
        HOLDER.set(systemRequest);
    }
}
