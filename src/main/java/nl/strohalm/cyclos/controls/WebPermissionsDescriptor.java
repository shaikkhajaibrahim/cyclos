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
package nl.strohalm.cyclos.controls;

import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.utils.access.PermissionsDescriptor;

/**
 * It contains the permissions required to check a struts action invocation
 * @author ameyer
 */

public class WebPermissionsDescriptor extends PermissionsDescriptor {
    private RequestParameter parameter;
    private BaseAction       baseAction;

    public RequestParameter getParameter() {
        return parameter;
    }

    public void setAction(final BaseAction baseAction) {
        this.baseAction = baseAction;
    }

    public void setParameter(final RequestParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return baseAction.getClass().getName();
    }
}
