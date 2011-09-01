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
/**
 * 
 */
package nl.strohalm.cyclos.controls.accounts.pos;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.accounts.pos.Pos.Status;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.accounts.pos.PosService;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;

import org.apache.struts.action.ActionForward;

/**
 * 
 * @author rodrigo
 */
@AdminAction( { @Permission(module = "adminMemberPos", operation = "manage") })
@BrokerAction( { @Permission(module = "brokerPos", operation = "manage") })
@IgnoreMember
public class RemovePosAction extends BaseAction {

    private PosService posService;

    @Inject
    public void setPosService(final PosService posService) {
        this.posService = posService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final EditPosForm form = context.getForm();
        final long posMainId = form.getId();
        final Pos pos = posService.load(posMainId, Pos.Relationships.MEMBER_POS);
        if (!SearchPosAction.canManagePos(context, pos, getFetchService()) && (pos.getStatus() != Status.DISCARDED)) {
            throw new PermissionDeniedException();
        }
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {

        final EditPosForm form = context.getForm();
        final long id = form.getId();
        try {
            posService.deletePos(id);
            context.sendMessage("pos.removed");
        } catch (final Exception e) {
            context.sendMessage("pos.error.removing");
        }
        return context.getSuccessForward();
    }
}
