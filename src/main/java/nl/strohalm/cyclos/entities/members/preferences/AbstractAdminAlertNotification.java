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
package nl.strohalm.cyclos.entities.members.preferences;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.alerts.AlertType;

/**
 * Abstract class for admin alert notifications
 * @author luis
 */
public abstract class AbstractAdminAlertNotification extends Entity {

    private static final long           serialVersionUID = -8274553661438775843L;
    private AdminNotificationPreference notificationPreference;

    public AdminNotificationPreference getNotificationPreference() {
        return notificationPreference;
    }

    public void setNotificationPreference(final AdminNotificationPreference notificationPreference) {
        this.notificationPreference = notificationPreference;
    }

    @Override
    public String toString() {
        final AlertType alertType = getAlertType();
        return getId() + " - " + (alertType == null ? "unknown" : alertType.getName());
    }

    protected abstract AlertType getAlertType();

    protected abstract void setAlertType(AlertType type);

}