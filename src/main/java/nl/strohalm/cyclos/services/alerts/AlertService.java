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
package nl.strohalm.cyclos.services.alerts;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.alerts.Alert;
import nl.strohalm.cyclos.entities.alerts.AlertQuery;
import nl.strohalm.cyclos.entities.alerts.MemberAlert;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for alerts
 * @author rafael
 */
public interface AlertService extends Service {

    /**
     * Creates a member alert
     */
    MemberAlert create(Member member, MemberAlert.Alerts alert, Object... arguments);

    /**
     * Creates a system alert
     */
    SystemAlert create(SystemAlert.Alerts alert, Object... arguments);

    /**
     * Returns the number of alerts of the given type
     * @return number of alerts
     */
    int getAlertCount(Alert.Type type);

    /**
     * Loads the alert
     * @return The alert loaded
     */
    Alert load(Long id, Relationship... fetch);

    /**
     * Removes the specified member alerts
     * @return The number of removed alerts
     */
    @AdminAction(@Permission(module = "systemAlerts", operation = "manageMemberAlerts"))
    int removeMemberAlerts(Long... ids);

    /**
     * Removes the specified system alerts
     * @return The number of removed alerts
     */
    @AdminAction(@Permission(module = "systemAlerts", operation = "manageSystemAlerts"))
    int removeSystemAlerts(Long... ids);

    /**
     * Search the existing ads based on the AlertQuery object.
     * @return a list of alerts
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    List<? extends Alert> search(AlertQuery queryParameters);
}
