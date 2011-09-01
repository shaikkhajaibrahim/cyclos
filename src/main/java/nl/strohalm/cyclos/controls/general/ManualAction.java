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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.utils.MessageHelper;
import nl.strohalm.cyclos.utils.StringHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used to display the manual
 * @author luis
 */
public class ManualAction extends Action {

    public static final Collection<String> ADMIN_HELPS;
    public static final Collection<String> MEMBER_HELPS;
    static {
        // TODO Put all help files
        final List<String> adminHelps = new ArrayList<String>();
        adminHelps.add("help_howto");
        adminHelps.add("access_devices");
        adminHelps.add("account_management");
        adminHelps.add("ads_interest");
        adminHelps.add("advertisements");
        adminHelps.add("alerts_logs");
        adminHelps.add("bookkeeping");
        adminHelps.add("brokering");
        adminHelps.add("content_management");
        adminHelps.add("custom_fields");
        adminHelps.add("documents");
        adminHelps.add("groups");
        adminHelps.add("guarantees");
        adminHelps.add("home");
        adminHelps.add("invoices");
        adminHelps.add("loan_groups");
        adminHelps.add("loans");
        adminHelps.add("member_records");
        adminHelps.add("messages");
        adminHelps.add("notifications");
        adminHelps.add("operators");
        adminHelps.add("passwords");
        adminHelps.add("payments");
        adminHelps.add("profiles");
        adminHelps.add("references");
        adminHelps.add("reports");
        adminHelps.add("settings");
        adminHelps.add("statistics");
        adminHelps.add("transaction_feedback");
        adminHelps.add("translation");
        adminHelps.add("user_management");
        ADMIN_HELPS = Collections.unmodifiableList(adminHelps);

        final List<String> memberHelps = new ArrayList<String>();

        memberHelps.add("help_howto");
        memberHelps.add("access_devices");
        memberHelps.add("ads_interest");
        memberHelps.add("advertisements");
        memberHelps.add("brokering");
        memberHelps.add("documents");
        memberHelps.add("home");
        memberHelps.add("invoices");
        memberHelps.add("loans");
        memberHelps.add("member_records");
        memberHelps.add("messages");
        memberHelps.add("notifications");
        memberHelps.add("operators");
        memberHelps.add("passwords");
        memberHelps.add("payments");
        memberHelps.add("profiles");
        memberHelps.add("guarantees");
        memberHelps.add("references");
        memberHelps.add("reports");
        memberHelps.add("transaction_feedback");
        memberHelps.add("user_management");

        MEMBER_HELPS = Collections.unmodifiableList(memberHelps);
    }

    @Override
    public ActionForward execute(final ActionMapping mapping, final ActionForm form, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final HttpSession session = request.getSession(false);
        boolean isAdmin;
        try {
            isAdmin = (Boolean) session.getAttribute("isAdmin");
        } catch (final Exception e) {
            isAdmin = false;
        }
        final boolean printVersion = mapping.getPath().startsWith("/print");
        if (!printVersion) {
            User loggedUser = null;
            if (session != null) {
                loggedUser = (User) session.getAttribute("loggedUser");
            }
            if (loggedUser == null) {
                return mapping.findForward("login");
            }
        }
        final List<String> helps = new ArrayList<String>(isAdmin ? ADMIN_HELPS : MEMBER_HELPS);
        // Sort the helps according to the translation messages
        Collections.sort(helps, new Comparator<String>() {
            public int compare(final String help1, final String help2) {
                if ("help_howto".equals(help1)) {
                    return -1;
                }
                final String message1 = MessageHelper.message(getServlet().getServletContext(), "help.title." + help1);
                final String message2 = MessageHelper.message(getServlet().getServletContext(), "help.title." + help2);
                return message1.compareTo(message2);
            }
        });
        request.setAttribute("helps", helps);
        final String page = StringUtils.trimToNull(StringHelper.removeMarkupTags(request.getParameter("page")));
        request.setAttribute("page", page);
        request.setAttribute("printVersion", printVersion);
        request.setAttribute("pagePrefix", request.getContextPath() + session.getAttribute("pathPrefix") + "/manual?page=");

        // When there's no specific page (section), get the global manual
        if (page == null) {
            return mapping.getInputForward();
        } else {
            return mapping.findForward("section");
        }
    }

}
