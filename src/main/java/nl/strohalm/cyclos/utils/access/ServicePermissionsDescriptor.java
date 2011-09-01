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
package nl.strohalm.cyclos.utils.access;

import java.lang.reflect.Method;

/**
 * It contains the permissions required to check a service method invocation
 * @author ameyer
 */
public class ServicePermissionsDescriptor extends PermissionsDescriptor {
    /**
     * The service method to be executed
     */
    private Method  method;

    /**
     * It's true if the method was annotates with the SystenAction annotation (e.g.: other services, aspects)
     */
    private boolean systemAction;
    /**
     * It's true if the method was annotates with the DontEnforcePermission annotation (e.g.: other services, aspects)
     */
    private boolean skipPermissionCheck;

    private boolean traceableAction;

    public boolean isForSystem() {
        return systemAction;
    }

    public boolean isOnlyForSystem() {
        return isForSystem() && !isAnnotated();
    }

    public boolean isSkipPermissionCheck() {
        // TODO: IMPORTANT- THIS IS ONLY BY BACKWARD COMPATIBILITY, THE FINAL VERSION MUST RETURN ONLY THE skipPermissionCheck flag
        return skipPermissionCheck || (!isAnnotated() && !isForSystem());
    }

    public boolean isTraceableAction() {
        return traceableAction;
    }

    public void setMethod(final Method method) {
        this.method = method;
    }

    public void setSkipPermissionCheck(final boolean skipPermissionCheck) {
        this.skipPermissionCheck = skipPermissionCheck;
    }

    public void setSystemAction(final boolean systemAction) {
        this.systemAction = systemAction;
    }

    public void setTraceableAction(final boolean traceableAction) {
        this.traceableAction = traceableAction;
    }

    @Override
    public String toString() {
        return method.toString() + ", for system ? " + systemAction + ", skip permission check?" + skipPermissionCheck;
    }
}
