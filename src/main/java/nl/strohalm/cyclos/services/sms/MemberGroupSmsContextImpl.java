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
package nl.strohalm.cyclos.services.sms;

import java.math.BigDecimal;

import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.utils.TimePeriod;

/**
 * This sms context implementation is the default used by Cyclos. <br>
 * It gets the related data from the member group (ignoring the specified member)
 * @author ameyer
 */
public class MemberGroupSmsContextImpl implements ISmsContext {

    public BigDecimal getAdditionalChargeAmount(final Member member) {
        return member.getMemberGroup().getMemberSettings().getSmsChargeAmount();
    }

    public TimePeriod getAdditionalChargedPeriod(final Member member) {
        return member.getMemberGroup().getMemberSettings().getSmsAdditionalChargedPeriod();
    }

    public int getAdditionalChargedSms(final Member member) {
        return member.getMemberGroup().getMemberSettings().getSmsAdditionalCharged();
    }

    public int getFreeSms(final Member member) {
        return member.getMemberGroup().getMemberSettings().getSmsFree();
    }

    public boolean showFreeSms(final MemberSmsStatus status) {
        final Member member = status.getMember();
        final int threshold = member.getMemberGroup().getMemberSettings().getSmsShowFreeThreshold();
        return getFreeSms(member) - status.getFreeSmsSent() <= threshold;
    }
}
