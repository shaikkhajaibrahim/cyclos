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
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.tokens.TokenService;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GenerateTokenAction extends BaseFormAction implements LocalSettingsChangeListener {

    private TokenService tokenService;

    private DataBinder<GenerateTokenDTO> dataBinder;

    private ReadWriteLock lock = new ReentrantReadWriteLock(true);

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void formAction(ActionContext context) throws Exception {

        final BaseTokenForm form = context.getForm();
        final GenerateTokenDTO generateTokenDTO = getDataBinder().readFromString(form.getValues());

        User user = context.getUser();
        generateTokenDTO.setFrom(user.getUsername());

        tokenService.generateToken(generateTokenDTO);
    }

    @Override
    protected void prepareForm(ActionContext context) throws Exception {
        final BaseTokenForm form = context.getForm();
        final GenerateTokenDTO generateTokenDTO = getDataBinder().readFromString(form.getValues());
        context.getRequest().setAttribute("token", generateTokenDTO);
    }

    public DataBinder<GenerateTokenDTO> getDataBinder() {
        try {
            lock.writeLock().lock();
            if (dataBinder == null) {
                final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());

                final BeanBinder<GenerateTokenDTO> binder = BeanBinder.instance(GenerateTokenDTO.class);
                binder.registerBinder("amount", PropertyBinder.instance(BigDecimal.class, "amount", localSettings.getNumberConverter()));
                binder.registerBinder("tokenSender", PropertyBinder.instance(String.class, "tokenSender"));

                dataBinder = binder;
            }
        } finally {
            lock.writeLock().unlock();
        }
        return dataBinder;
    }

    @Override
    public void onLocalSettingsUpdate(LocalSettingsEvent event) {
        try {
            lock.writeLock().lock();
            dataBinder = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
