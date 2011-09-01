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
package nl.strohalm.cyclos.controls;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.entities.EntityReference;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.Navigation;
import nl.strohalm.cyclos.utils.SpringHelper;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Contains all objects used on the Struts context
 * @author luis
 */
public class ActionContext extends AbstractActionContext {

    private static final long  serialVersionUID = 1550749272139495248L;
    private final FetchService fetchService;

    public ActionContext(final ActionMapping actionMapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response, final User user) {
        this(actionMapping, actionForm, request, response, user, null);
    }

    public ActionContext(final ActionMapping actionMapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response, final User user, final FetchService fetchService) {
        super(actionMapping, actionForm, request, response, user);
        if (fetchService == null) {
            this.fetchService = SpringHelper.bean(getServletContext(), "fetchService");
        } else {
            this.fetchService = fetchService;
        }
    }

    /**
     * Returns a forward to the last visited action
     */
    public ActionForward back() {
        return ActionHelper.back(getActionMapping());
    }

    public FetchService getFetchService() {
        return fetchService;
    }

    /**
     * Returns the navigation object
     */
    public Navigation getNavigation() {
        return Navigation.find(getSession());
    }

    /**
     * Checks whether the logged member is the given member's broker
     */
    public boolean isBrokerOf(Member member) {
        if (!isBroker()) {
            return false;
        }
        if (member == null) {
            return false;
        }
        if (member instanceof EntityReference) {
            member = fetchService.fetch(member, Member.Relationships.BROKER);
        }
        final Element loggedElement = getElement();
        return loggedElement.equals(member.getBroker());
    }

    /**
     * Checks whether the logged member is the given operator's member
     */
    public boolean isMemberOf(Operator operator) {
        if (!isMember()) {
            return false;
        }
        if (operator == null) {
            return false;
        }
        if (operator instanceof EntityReference) {
            operator = fetchService.fetch(operator, Operator.Relationships.MEMBER);
        }
        final Element loggedElement = getElement();
        return loggedElement.equals(operator.getMember());
    }

    /**
     * Returns an action forward for the error page, showing the message of the given key / arguments
     */
    public ActionForward sendError(final String key, final Object... arguments) {
        return ActionHelper.sendError(getActionMapping(), getRequest(), getResponse(), key, arguments);
    }

}
