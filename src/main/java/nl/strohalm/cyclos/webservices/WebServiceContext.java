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
package nl.strohalm.cyclos.webservices;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.services.ServiceOperation;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.utils.SpringHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;

import org.apache.cxf.binding.soap.SoapMessage;

/**
 * The context for a given web service call
 * @author luis
 */
public class WebServiceContext {

    /* We need this enum because we can't inspect from the pos/client fields since they could be null (e.g. if the pos/client was not found) */
    private enum ContextType {
        POS, SERVICE_CLIENT;
    }

    private static final ThreadLocal<WebServiceContext> HOLDER = new ThreadLocal<WebServiceContext>();

    /**
     * Removes any state for this request
     */
    public static void cleanup() {
        final WebServiceContext context = HOLDER.get();
        if (context != null) {
            context.client = null;
            context.pos = null;
            context.request = null;
            context.servletContext = null;
            context.soapMessage = null;
        }

        HOLDER.remove();
        LoggedUser.cleanup();
    }

    /**
     * Returns the {@link Channel} for which the current {@link ServiceClient} is restricted, or null when none
     */
    public static Channel getChannel() {
        final WebServiceContext context = assertContext(null);
        if (context.isPosContextType()) {
            final ChannelService channelService = SpringHelper.bean(context.servletContext, "channelService");
            return channelService.loadByInternalName(Channel.POS);
        } else {
            return context.client.getChannel();
        }
    }

    /**
     * Returns the {@link ServiceClient} for this request
     */
    public static ServiceClient getClient() {
        return assertContext(ContextType.SERVICE_CLIENT).client;
    }

    /**
     * Returns the {@link Member} for which the current {@link ServiceClient} is restricted, or null when none
     */
    public static Member getMember() {
        final WebServiceContext context = assertContext(null);
        if (context.isPosContextType()) {
            return context.pos.getMemberPos().getMember();
        } else {
            return context.client.getMember();
        }
    }

    /**
     * Returns the {@link MemberPos} for this request
     */
    public static Pos getPos() {
        return assertContext(ContextType.POS).pos;
    }

    /**
     * Returns the {@link HttpServletRequest} for this request
     */
    public static HttpServletRequest getRequest() {
        return assertContext(null).request;
    }

    /**
     * Returns the {@link ServletContext} for this request
     */
    public static ServletContext getServletContext() {
        return assertContext(null).servletContext;
    }

    /**
     * Returns the {@link SoapMessage} for this request
     */
    public static SoapMessage getSoapMessage() {
        return assertContext(null).soapMessage;
    }

    /**
     * Checks whether the current client has the given permission
     */
    public static boolean hasPermission(final ServiceOperation operation) {
        return getClient().getPermissions().contains(operation);
    }

    public static boolean isInitialized() {
        return HOLDER.get() != null;
    }

    public static boolean isPosContext() {
        return assertContext(null).isPosContextType();
    }

    public static void set(final Pos pos) {
        if (pos == null || pos.getMemberPos() == null || pos.getMemberPos().getMember() == null) {
            throw new IllegalArgumentException("Invalid Pos: either the pos or pos member are null");
        }

        assertContext(ContextType.POS).pos = pos;
    }

    /**
     * Sets the thread local's current context.<br>
     * The context for a POS web service operation is initialized by this method<br>
     * In case of POS web service the web services clients are not used.
     */
    public static void set(final Pos pos, final ServletContext servletContext, final HttpServletRequest request, final SoapMessage soapMessage) {
        HOLDER.set(new WebServiceContext(pos, servletContext, request, soapMessage));
    }

    /**
     * Sets the thread local's current context.<br>
     * It's invoked for those web services using a web service client.
     */
    public static void set(final ServiceClient client, final ServletContext servletContext, final HttpServletRequest request, final SoapMessage soapMessage) {
        HOLDER.set(new WebServiceContext(client, servletContext, request, soapMessage));
    }

    private static WebServiceContext assertContext(final ContextType requiredCtx) {
        final WebServiceContext context = HOLDER.get();
        if (context == null) {
            throw new IllegalStateException("The web service context was not initialized yet");
        } else if (requiredCtx != null && requiredCtx != context.contextType) {
            throw new IllegalStateException(String.format("Invalid invocation: context type: %1$s", context.contextType));
        }

        return context;
    }

    private ServiceClient      client;
    private Pos                pos;
    private HttpServletRequest request;
    private ServletContext     servletContext;
    private SoapMessage        soapMessage;

    private ContextType        contextType;

    private WebServiceContext(final Pos pos, final ServletContext servletContext, final HttpServletRequest request, final SoapMessage soapMessage) {
        this(servletContext, request, soapMessage);
        this.pos = pos;
        contextType = ContextType.POS;
    }

    private WebServiceContext(final ServiceClient client, final ServletContext servletContext, final HttpServletRequest request, final SoapMessage soapMessage) {
        this(servletContext, request, soapMessage);
        this.client = client;
        contextType = ContextType.SERVICE_CLIENT;
    }

    private WebServiceContext(final ServletContext servletContext, final HttpServletRequest request, final SoapMessage soapMessage) {
        this.request = request;
        this.servletContext = servletContext;
        this.soapMessage = soapMessage;
    }

    private boolean isPosContextType() {
        return contextType == ContextType.POS;
    }
}
