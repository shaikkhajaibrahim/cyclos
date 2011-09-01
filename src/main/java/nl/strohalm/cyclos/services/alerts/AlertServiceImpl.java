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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.alerts.AlertDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.alerts.Alert;
import nl.strohalm.cyclos.entities.alerts.AlertQuery;
import nl.strohalm.cyclos.entities.alerts.MemberAlert;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.alerts.Alert.Type;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation class for alerts service interface
 * @author rafael
 */
public class AlertServiceImpl implements AlertService {

    private FetchService      fetchService;
    private PermissionService permissionService;
    private SettingsService   settingsService;
    private AlertDAO          alertDao;

    public MemberAlert create(final Member member, final MemberAlert.Alerts alert, final Object... arguments) {
        final MemberAlert toSave = new MemberAlert();
        populate(toSave, alert.getValue(), arguments);
        toSave.setMember(member);
        return (MemberAlert) save(toSave);
    }

    public SystemAlert create(final SystemAlert.Alerts alert, final Object... arguments) {
        final SystemAlert toSave = new SystemAlert();
        populate(toSave, alert.getValue(), arguments);
        return (SystemAlert) save(toSave);
    }

    public int getAlertCount(final Type type) {
        if (Alert.Type.MEMBER.equals(type)) {
            if (LoggedUser.isValid()) {
                AdminGroup adminGroup = LoggedUser.group();
                adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                final AlertQuery alertQuery = new AlertQuery();
                alertQuery.setType(type);
                alertQuery.setGroups(adminGroup.getManagesGroups());
                alertQuery.setPageForCount();
                return PageHelper.getTotalCount(alertDao.search(alertQuery));
            } else {
                return alertDao.getCount(type);
            }
        }
        return alertDao.getCount(type);
    }

    public Alert load(final Long id, final Relationship... fetch) {
        return alertDao.load(id, fetch);
    }

    public int removeMemberAlerts(final Long... ids) {
        for (final Long id : ids) {
            final Alert alert = alertDao.load(id);
            if (!(alert instanceof MemberAlert)) {
                throw new UnexpectedEntityException();
            }
        }
        return alertDao.delete(ids);
    }

    public int removeSystemAlerts(final Long... ids) {
        for (final Long id : ids) {
            final Alert alert = alertDao.load(id);
            if (!(alert instanceof SystemAlert)) {
                throw new UnexpectedEntityException();
            }
        }
        return alertDao.delete(ids);
    }

    public List<Alert> search(final AlertQuery queryParameters) {
        final PermissionRequestorImpl permissionRequestor = new PermissionRequestorImpl();

        if (queryParameters.getType() == Alert.Type.MEMBER) {
            permissionRequestor.adminPermissions("systemAlerts", "viewMemberAlerts");
        } else if (queryParameters.getType() == Alert.Type.SYSTEM) {
            permissionRequestor.adminPermissions("systemAlerts", "viewSystemAlerts");
        } else { // null or unknown type
            throw new PermissionDeniedException();
        }

        if (!permissionService.checkPermissions(permissionRequestor)) {
            throw new PermissionDeniedException();
        }

        return alertDao.search(queryParameters);
    }

    public void setAlertDao(final AlertDAO alertDao) {
        this.alertDao = alertDao;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private Validator getMemberValidator() {
        final Validator memberValidator = new Validator("alert");
        memberValidator.property("date").required();
        memberValidator.property("key").required();
        memberValidator.property("member").key("member.member").required();
        return memberValidator;
    }

    private Validator getSystemValidator() {
        final Validator systemValidator = new Validator("alert");
        systemValidator.property("date").required();
        systemValidator.property("key").required();
        return systemValidator;
    }

    private void populate(final Alert alert, final String key, final Object... arguments) {
        String[] args = null;
        if (arguments != null) {
            // Convert each argument to string
            args = new String[arguments.length];
            final LocalSettings localSettings = settingsService.getLocalSettings();
            for (int i = 0; i < args.length; i++) {
                final Object argument = arguments[i];
                String arg;
                if (argument == null) {
                    arg = "";
                } else if (argument instanceof BigDecimal) {
                    arg = localSettings.getNumberConverter().toString((BigDecimal) argument);
                } else if (argument instanceof Calendar) {
                    final Calendar cal = (Calendar) argument;
                    final Calendar trunc = DateHelper.truncate(cal);
                    if (cal.equals(trunc)) {
                        arg = localSettings.getDateConverter().toString(cal);
                    } else {
                        arg = localSettings.getDateTimeConverter().toString(cal);
                    }
                } else {
                    arg = StringUtils.trimToEmpty(argument.toString());
                }
                args[i] = arg;
            }
        }
        alert.setDate(Calendar.getInstance());
        alert.setDescription(key, args);
    }

    private Alert save(final Alert alert) {
        if (alert.getDate() == null) {
            alert.setDate(Calendar.getInstance());
        }
        validate(alert);
        return alertDao.insert(alert);
    }

    private void validate(final Alert alert) throws ValidationException {
        if (alert instanceof SystemAlert) {
            getSystemValidator().validate(alert);
        } else {
            getMemberValidator().validate(alert);
        }
    }

}