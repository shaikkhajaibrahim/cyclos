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
package mp.platform.cyclone.webservices;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.alerts.MemberAlert;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.ApplicationException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.accounts.pos.MemberPosService;
import nl.strohalm.cyclos.services.accounts.pos.PosService;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;
import mp.platform.cyclone.webservices.pos.BasePosParameters;
import mp.platform.cyclone.webservices.pos.IPosPinParameter;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

/**
 * Interceptor for POS requests, basically to allow access to the {@link HttpServletRequest}
 * 
 * @author luis
 */
public class PosInterceptor extends AbstractSoapInterceptor {

    private class BlockedPosException extends ApplicationException {
        private static final long serialVersionUID = 1L;

        private BlockedPosException(final String msg) {
            super(msg);
            setShouldRollback(false);
        }
    }

    private PosService       posService;
    private AlertService     alertService;
    private AccessService    accessService;
    private MemberPosService memberPosService;
    private LoggingHandler   loggingHandler;

    public PosInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(final SoapMessage message) throws Fault {
        Pos pos = null;
        try {
            final BasePosParameters params = WebServiceHelper.getParameter(message);
            final String posId = params.getPosId();
            pos = StringUtils.isEmpty(posId) ? null : posService.loadByPosId(posId, Member.Relationships.CHANNELS, RelationshipHelper.nested(Pos.Relationships.MEMBER_POS, MemberPos.Relationships.MEMBER), RelationshipHelper.nested(Pos.Relationships.MEMBER_POS, MemberPos.Relationships.POS));
            if (pos == null || pos.getMemberPos() == null) {
                WebServiceFaultsEnum.UNAUTHORIZED_ACCESS.throwFault("The POS was not assigned to a member");
            } else if (!accessService.isChannelEnabledForMember(Channel.POS, pos.getMemberPos().getMember())) { // validates the POS channel
                WebServiceFaultsEnum.UNAUTHORIZED_ACCESS.throwFault("The POS channel is not enabled for the member: " + pos.getMemberPos().getMember());
            } else if (pos.getMemberPos().getStatus() != MemberPos.Status.ACTIVE) {
                Throwable th;
                if (pos.getMemberPos().getStatus() == MemberPos.Status.BLOCKED) { // generate a system alert if the pos was blocked
                    final String remoteAddress = WebServiceHelper.requestOf(message).getRemoteAddr();
                    alertService.create(pos.getMemberPos().getMember(), MemberAlert.Alerts.BLOCKED_POS_USED, pos.getPosId(), remoteAddress);
                    // we don't want the Tx will be rolled back because we has just inserted an alert!
                    th = new BlockedPosException("Current POS status: " + pos.getMemberPos().getStatus());
                } else {
                    th = new Exception("Current POS status: " + pos.getMemberPos().getStatus());
                }
                WebServiceFaultsEnum.INACTIVE_POS.throwFault(th);
            } else if (params instanceof IPosPinParameter) { // validate the pos pin too
                final IPosPinParameter posPinParams = (IPosPinParameter) params;
                memberPosService.checkPin(pos.getMemberPos(), posPinParams.getPosPin());
            }
            loggingHandler.traceWebService(WebServiceHelper.requestOf(message).getRemoteAddr(), pos, WebServiceHelper.getWebServiceOperationName(message), WebServiceHelper.getParameters(message));
            LoggedUser.init(pos.getMemberPos().getMember().getUser(), WebServiceHelper.requestOf(message).getRemoteAddr());
        } catch (final SoapFault fault) {
            throw fault;
        } catch (final Exception e) {
            WebServiceFaultsEnum.UNAUTHORIZED_ACCESS.throwFault(e);
        } finally {
            WebServiceHelper.initializeContext(pos, message);
        }
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setMemberPosService(final MemberPosService memberPosService) {
        this.memberPosService = memberPosService;
    }

    public void setPosService(final PosService posService) {
        this.posService = posService;
    }
}
