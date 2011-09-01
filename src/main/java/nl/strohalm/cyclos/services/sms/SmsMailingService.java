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

import nl.strohalm.cyclos.entities.sms.SmsMailing;
import nl.strohalm.cyclos.entities.sms.SmsMailingQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for SMS mailings
 * @author luis
 */
public interface SmsMailingService extends Service {

    /**
     * Resumes mailings which didn't finished sending to all members
     */
    @SystemAction
    void resumeUnfinished();

    /**
     * Search for SMS mailings
     * @return a list of SMS logs
     */
    @AdminAction(@Permission(module = "adminMemberSmsMailings", operation = "view"))
    @BrokerAction( { @Permission(module = "brokerSmsMailings", operation = "freeSmsMailings"), @Permission(module = "brokerSmsMailings", operation = "paidSmsMailings") })
    @IgnoreMember
    List<SmsMailing> search(SmsMailingQuery query);

    /**
     * Sends a free SMS mailing to groups of members
     */
    @AdminAction(@Permission(module = "adminMemberSmsMailings", operation = "freeSmsMailings"))
    @BrokerAction(@Permission(module = "brokerSmsMailings", operation = "freeSmsMailings"))
    @IgnoreMember
    SmsMailing sendFreeToGroups(SmsMailing smsMailing);

    /**
     * Sends a paid SMS mailing to groups of members
     */
    @AdminAction(@Permission(module = "adminMemberSmsMailings", operation = "paidSmsMailings"))
    @BrokerAction(@Permission(module = "brokerSmsMailings", operation = "paidSmsMailings"))
    @IgnoreMember
    SmsMailing sendPaidToGroups(SmsMailing smsMailing);

    /**
     * Sends a free SMS mailing to a specified member
     */
    @AdminAction(@Permission(module = "adminMemberSmsMailings", operation = "freeSmsMailings"))
    @BrokerAction(@Permission(module = "brokerSmsMailings", operation = "freeSmsMailings"))
    @PathToMember("member")
    SmsMailing sendToMember(SmsMailing smsMailing);

    /**
     * Validates the SMS mailing
     */
    void validate(final SmsMailing smsMailing, boolean isMemberRequired) throws ValidationException;

}