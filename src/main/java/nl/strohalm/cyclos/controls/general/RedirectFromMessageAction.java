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
package nl.strohalm.cyclos.controls.general;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile.Type;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupFilterService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.LoginHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper.CustomizationData;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used to redirect the user to the correct page when coming from a message or e-mail link
 * 
 * @author luis
 */
public class RedirectFromMessageAction extends Action {

    private ElementService     elementService;
    private GroupService       groupService;
    private GroupFilterService groupFilterService;

    @Override
    public ActionForward execute(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws Exception {

        final RedirectFromMessageForm form = (RedirectFromMessageForm) actionForm;

        // Get the path
        final String path = StringUtils.trimToNull(form.getPath());
        if (path == null) {
            return null;
        }

        // Get the element
        final long userId = form.getUserId();
        Element element = null;
        if (userId > 0L) {
            try {
                element = elementService.load(userId, Element.Relationships.GROUP);
            } catch (final Exception e) {
                // ok, leave element null
            }
        }

        // Find the currently logged user
        final User loggedUser = LoginHelper.getLoggedUser(request);
        HttpSession session = request.getSession();
        if (userId > 0L && loggedUser != null) {
            if (loggedUser.getId().equals(userId)) {
                // The expected user is already logged in. Redirect to the path directly
                return new ActionForward(path, true);
            } else {
                // When there was another user logged in, invalidate the session, because we expect a fixed user
                session.invalidate();
                session = request.getSession();
            }
        }

        // Check for a customized login page
        String customizedLoginParam = null;
        Long customizedLoginId = null;
        String containerUrl = null;
        if (element != null) {
            final CustomizationData customization = CustomizationHelper.findCustomizationOf(getServlet().getServletContext(), Type.STATIC_FILE, element.getGroup(), null, "login.jsp");
            switch (customization.getLevel()) {
                case GROUP:
                    try {
                        final Group group = groupService.load(customization.getId());
                        containerUrl = group.getContainerUrl();
                        customizedLoginParam = "groupId";
                        customizedLoginId = customization.getId();
                    } catch (final Exception e) {
                        // Just ignore
                    }
                    break;
                case GROUP_FILTER:
                    try {
                        final GroupFilter groupFilter = groupFilterService.load(customization.getId());
                        containerUrl = groupFilter.getContainerUrl();
                        customizedLoginParam = "groupFilterId";
                        customizedLoginId = customization.getId();
                    } catch (final Exception e) {
                        // Just ignore
                    }
                    break;
            }
        }

        // Get the container url if not have from group / group filter already
        if (containerUrl == null) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(request);
            containerUrl = localSettings.getContainerUrl();
        }

        // Set the returnTo on the session, so that after logging in, the user will be redirected to this page
        session.setAttribute("returnTo", path);

        // Set the containerUrl to session
        session.setAttribute("containerUrl", containerUrl);

        // There was an expected user. Put it on session to the login page
        session.setAttribute("loginElement", element);

        // Find the login forward
        final Map<String, Object> parameters = new HashMap<String, Object>();
        if (customizedLoginParam != null) {
            // There is a customized login page
            parameters.put(customizedLoginParam, customizedLoginId);
        }

        final ActionForward forward = mapping.findForward("login");
        return ActionHelper.redirectWithParams(request, forward, parameters);
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupFilterService(final GroupFilterService groupFilterService) {
        this.groupFilterService = groupFilterService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }
}
