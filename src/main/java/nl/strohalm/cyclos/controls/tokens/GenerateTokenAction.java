/*
 *
 *    This file is part of Cyclos.
 *
 *    Cyclos is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Cyclos is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Cyclos; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 */

package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import org.apache.struts.action.ActionForward;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GenerateTokenAction extends BaseTokenAction<GenerateTokenDTO> {


    ActionForward tokenSubmit(BaseTokenForm form, Member loggedMember, ActionContext context) throws Exception {
        final GenerateTokenDTO generateTokenDTO = getDataBinder().readFromString(form.getValues());
        LocalSettings localSettings = settingsService.getLocalSettings();
        Long ttId = context.isBroker() ? localSettings.getBrokerTokenGenerationTransferType()
                : localSettings.getMemberTokenGenerationTransferType();
        generateTokenDTO.setTransferTypeId(ttId);
        generateTokenDTO.setFrom(loggedMember.getUsername());
        if (!context.isBroker()) {
            generateTokenDTO.setSenderMobilePhone(null);
        }

        context.getSession().setAttribute("token", generateTokenDTO);
        return ActionHelper.redirectWithParams(context.getRequest(), context.getSuccessForward(), Collections.<String, Object>emptyMap());
    }

    @Override
    protected void prepareForm(ActionContext context) throws Exception {
        final BaseTokenForm form = context.getForm();
        final GenerateTokenDTO generateTokenDTO = getDataBinder().readFromString(form.getValues());
        context.getRequest().setAttribute("token", generateTokenDTO);
    }

    @Override
    protected DataBinder createBinder(LocalSettings localSettings) {
        final BeanBinder<GenerateTokenDTO> binder = BeanBinder.instance(GenerateTokenDTO.class);
        binder.registerBinder("amount", PropertyBinder.instance(BigDecimal.class, "amount", localSettings.getNumberConverter()));
        binder.registerBinder("senderMobilePhone", PropertyBinder.instance(String.class, "senderMobilePhone"));
        binder.registerBinder("recipientMobilePhone", PropertyBinder.instance(String.class, "recipientMobilePhone"));
        return binder;
    }
}
