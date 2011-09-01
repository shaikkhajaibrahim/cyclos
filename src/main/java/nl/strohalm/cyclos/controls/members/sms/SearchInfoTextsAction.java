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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseQueryAction;
import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.infotexts.InfoTextQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.infotexts.InfoTextService;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.query.QueryParameters;

public class SearchInfoTextsAction extends BaseQueryAction {
    private DataBinder<InfoTextQuery> dataBinder;
    private InfoTextService           infoTextService;

    @Inject
    public void setInfoTextService(final InfoTextService service) {
        infoTextService = service;
    }

    @Override
    protected void executeQuery(final ActionContext context, final QueryParameters queryParameters) {
        final HttpServletRequest request = context.getRequest();
        final InfoTextQuery query = (InfoTextQuery) queryParameters;

        if (query.getStartIn() != null && query.getStartIn().getBegin() != null && query.getStartIn().getEnd() != null) {
            if (query.getStartIn().getBegin().after(query.getStartIn().getEnd())) {
                context.sendMessage("errors.periodInvalidBounds", SettingsHelper.getLocalSettings(getServlet().getServletContext()).getDateConverter().toString(query.getStartIn().getBegin()));
                return;
            }
        }

        if (query.getEndIn() != null && query.getEndIn().getBegin() != null && query.getEndIn().getEnd() != null) {
            if (query.getEndIn().getBegin().after(query.getEndIn().getEnd())) {
                context.sendMessage("errors.periodInvalidBounds", SettingsHelper.getLocalSettings(getServlet().getServletContext()).getDateConverter().toString(query.getEndIn().getBegin()));
                return;
            }
        }

        final List<InfoText> result = infoTextService.search(query);
        request.setAttribute("infoTexts", result);

    }

    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final SearchInfoTextsForm form = context.getForm();
        final InfoTextQuery query = getDataBinder().readFromString(form.getQuery());
        context.getRequest().setAttribute("hasManagePermissions", getPermissionService().checkPermission("systemInfoTexts", "manage"));

        return query;
    }

    @Override
    protected boolean willExecuteQuery(final ActionContext context, final QueryParameters queryParameters) throws Exception {
        // The query is always executed
        return true;
    }

    private DataBinder<InfoTextQuery> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<InfoTextQuery> binder = BeanBinder.instance(InfoTextQuery.class);

            final LocalSettings settings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            binder.registerBinder("keywords", PropertyBinder.instance(String.class, "keywords"));
            binder.registerBinder("startIn", DataBinderHelper.rawPeriodBinder(settings, "validity"));
            binder.registerBinder("endIn", DataBinderHelper.rawPeriodBinder(settings, "validityEnd"));

            binder.registerBinder("pageParameters", DataBinderHelper.pageBinder());

            dataBinder = binder;
        }
        return dataBinder;
    }
}
