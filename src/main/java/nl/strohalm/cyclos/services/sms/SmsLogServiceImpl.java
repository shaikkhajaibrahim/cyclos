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
package nl.strohalm.cyclos.services.sms;

import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.sms.SmsLogDAO;
import nl.strohalm.cyclos.dao.sms.SmsLogReportTotal;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.sms.SmsLog;
import nl.strohalm.cyclos.entities.sms.SmsLogQuery;
import nl.strohalm.cyclos.entities.sms.SmsLogReportQuery;
import nl.strohalm.cyclos.entities.sms.SmsLogReportVO;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.access.LoggedUser;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation for SMS log service
 * @author Jefferson Magno
 */
public class SmsLogServiceImpl implements SmsLogService {

    private SmsLogDAO    smsLogDao;
    private FetchService fetchService;

    public SmsLogReportVO getSmsLogReport(final SmsLogReportQuery query) {

        final AdminGroup loggedGroup = fetchService.fetch((AdminGroup) LoggedUser.group(), AdminGroup.Relationships.MANAGES_GROUPS);

        // Ensure that all groups are managed
        final Collection<MemberGroup> groups = query.getMemberGroups();
        final Collection<MemberGroup> managedGroups = loggedGroup.getManagesGroups();
        if (CollectionUtils.isEmpty(groups)) {
            query.setMemberGroups(managedGroups);
        } else {
            for (final MemberGroup group : groups) {
                if (!managedGroups.contains(group)) {
                    throw new PermissionDeniedException();
                }
            }
        }

        final SmsLogReportVO report = new SmsLogReportVO();
        if (query.isReturnTotals()) {
            List<SmsLogReportTotal> totals = smsLogDao.getReportTotals(query);
            for (final SmsLogReportTotal total : totals) {
                report.setTotals(total.getType(), total.getStatus(), total.getTotal());
            }
        }
        report.setLogs(smsLogDao.search(query));
        return report;
    }

    public SmsLog save(final SmsLog smsLog) {
        return smsLogDao.insert(smsLog);
    }

    public List<SmsLog> search(final SmsLogQuery query) {
        return smsLogDao.search(query);
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setSmsLogDao(final SmsLogDAO smsLogDao) {
        this.smsLogDao = smsLogDao;
    }

}