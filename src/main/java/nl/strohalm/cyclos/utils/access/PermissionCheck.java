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

public class PermissionCheck {
    private boolean    granted;
    private Permission permission;

    public PermissionCheck(final boolean granted, final Permission permission) {
        super();
        this.granted = granted;
        this.permission = permission;
    }

    public Permission getPermission() {
        return permission;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(final boolean granted) {
        this.granted = granted;
    }

    public void setPermission(final Permission permission) {
        this.permission = permission;
    }

}
