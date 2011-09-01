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
package nl.strohalm.cyclos.controls.members.sms;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.infotexts.InfoTextService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.SetConverter;

import org.apache.struts.action.ActionForward;

public class EditInfoTextAction extends BaseFormAction {
    private DataBinder<InfoText> dataBinder;
    private InfoTextService      infoTextService;

    @Inject
    public void setInfoTextService(final InfoTextService service) {
        infoTextService = service;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final EditInfoTextForm form = context.getForm();
        final InfoText infoText = getDataBinder().readFromString(form.getValues());
        if (form.getInfoTextId() != null && form.getInfoTextId() != 0) {
            infoText.setId(form.getInfoTextId());
        }

        infoTextService.save(infoText);

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("infoTextId", infoText.getId());
        return ActionHelper.redirectWithParams(context.getRequest(), context.getSuccessForward(), params);
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final EditInfoTextForm form = context.getForm();
        if (form.getInfoTextId() != null) {
            final InfoText infoText = infoTextService.load(form.getInfoTextId());
            context.getRequest().setAttribute("currentInfoText", infoText);
        }
        context.getRequest().setAttribute("hasManagePermissions", getPermissionService().checkPermission("systemInfoTexts", "manage"));
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final EditInfoTextForm form = context.getForm();
        final InfoText infoText = getDataBinder().readFromString(form.getValues());
        infoTextService.validate(infoText);
    }

    @SuppressWarnings("unchecked")
    private DataBinder<InfoText> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<InfoText> binder = BeanBinder.instance(InfoText.class);

            final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            binder.registerBinder("validity", DataBinderHelper.rawPeriodBinder(settings, "validity"));
            binder.registerBinder("aliases", PropertyBinder.instance(TreeSet.class, "aliases", new SetConverter(TreeSet.class, ",")));
            binder.registerBinder("subject", PropertyBinder.instance(String.class, "subject"));
            binder.registerBinder("body", PropertyBinder.instance(String.class, "body"));
            binder.registerBinder("enabled", PropertyBinder.instance(Boolean.TYPE, "enabled"));
            dataBinder = binder;
        }
        return dataBinder;
    }
}
