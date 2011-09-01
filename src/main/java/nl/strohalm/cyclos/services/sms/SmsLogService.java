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

import java.util.List;

import nl.strohalm.cyclos.entities.sms.SmsLog;
import nl.strohalm.cyclos.entities.sms.SmsLogQuery;
import nl.strohalm.cyclos.entities.sms.SmsLogReportQuery;
import nl.strohalm.cyclos.entities.sms.SmsLogReportVO;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for SMS logs
 * @author Jefferson Magno
 */
public interface SmsLogService extends Service {

    /**
     * Get a report of sent SMS messages to member, ordering the results by the name of the member
     */
    @AdminAction(@Permission(module = "systemReports", operation = "smsLogs"))
    SmsLogReportVO getSmsLogReport(SmsLogReportQuery query);

    /**
     * Inserts a SMS log into the database
     */
    @SystemAction
    SmsLog save(SmsLog smsLog);

    /**
     * Search SMS logs
     * @return a list of SMS logs
     */
    @AdminAction(@Permission(module = "adminMemberSms", operation = "view"))
    @BrokerAction(@Permission(module = "brokerMemberSms", operation = "view"))
    @MemberAction(@Permission(module = "memberSms", operation = "view"))
    @PathToMember("member")
    List<SmsLog> search(SmsLogQuery query);

}