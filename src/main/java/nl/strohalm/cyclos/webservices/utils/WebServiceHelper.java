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
package nl.strohalm.cyclos.webservices.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.exceptions.QueryParseException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.pos.Pos;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.InvalidPosPinException;
import nl.strohalm.cyclos.services.accounts.pos.exceptions.PosPinBlockedException;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.webservices.WebServiceContext;
import nl.strohalm.cyclos.webservices.WebServiceFaultsEnum;
import nl.strohalm.cyclos.webservices.payments.WebServiceFault;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

/**
 * Contains helper methods for web services
 * @author luis
 */
public class WebServiceHelper {

    private static final String CODE_PREFIX = "cyclos";
    private static final Log    logger      = LogFactory.getLog(WebServiceHelper.class);

    /**
     * Returns a SOAP fault
     */
    public static SoapFault fault(final Throwable exception) {
        WebServiceFault fault;
        if ((exception instanceof ValidationException) || (exception instanceof IllegalArgumentException)) {
            fault = WebServiceFaultsEnum.INVALID_PARAMETERS;
        } else if (exception instanceof EntityNotFoundException) {
            final Class<? extends Entity> entityType = ((EntityNotFoundException) exception).getEntityType();
            if (Element.class.isAssignableFrom(entityType) || User.class.isAssignableFrom(entityType)) {
                fault = WebServiceFaultsEnum.MEMBER_NOT_FOUND;
            } else {
                fault = WebServiceFaultsEnum.INVALID_PARAMETERS;
            }
        } else if (exception instanceof QueryParseException) {
            fault = WebServiceFaultsEnum.QUERY_PARSE_ERROR;
        } else if (exception instanceof InvalidCredentialsException || exception instanceof InvalidPosPinException) {
            fault = WebServiceFaultsEnum.INVALID_CREDENTIALS;
        } else if (exception instanceof BlockedCredentialsException || exception instanceof PosPinBlockedException) {
            fault = WebServiceFaultsEnum.BLOCKED_CREDENTIALS;
        } else {
            fault = WebServiceFaultsEnum.UNEXPECTED_ERROR;
        }

        return fault(fault, exception);
    }

    public static SoapFault fault(final WebServiceFault fault) {
        return fault(fault.code(), null);
    }

    public static SoapFault fault(final WebServiceFault fault, final Throwable cause) {
        return fault(fault.code(), cause);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParameter(final SoapMessage message) {
        final List params = message.getContent(java.util.List.class);
        if (CollectionUtils.isEmpty(params)) {
            return null;
        } else {
            if (params.size() > 1) {
                logger.warn("The operation '" + getWebServiceOperationName(message) + "' has more than one parameter (" + params.size() + "). Returning only the first.");
            }
            return (T) params.get(0);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParameters(final SoapMessage message) {
        final MessageInfo messageInfo = message.get(MessageInfo.class);
        final List parameterValues = message.getContent(java.util.List.class);
        final Map<String, Object> result = new HashMap<String, Object>();

        if (parameterValues != null) {
            for (int i = 0; i < messageInfo.getMessageParts().size(); i++) {
                final String name = messageInfo.getMessagePart(i).getName().getLocalPart();
                if (!"credentials posPin fromMemberCredentials".contains(name)) {
                    result.put(name, parameterValues.get(i));
                }
            }
        } else {
            final Object param = message.getContent(Object.class);
            result.put("param", param);
        }

        return result;
    }

    public static String getWebServiceOperationName(final SoapMessage message) {
        final MessageInfo messageInfo = message.get(MessageInfo.class);
        final OperationInfo operation = messageInfo.getOperation();
        final QName operationQName = operation.getName();

        return operationQName.getLocalPart();
    }

    /**
     * Initialize the POS Web Service Context.
     */
    public static void initializeContext(final Pos pos, final SoapMessage message) {
        WebServiceContext.set(pos, servletContextOf(message), requestOf(message), message);
    }

    /**
     * Initialize the Web Service Context for all WS using Services Clients.
     */
    public static void initializeContext(final ServiceClient client, final SoapMessage message) {
        WebServiceContext.set(client, servletContextOf(message), requestOf(message), message);
    }

    /**
     * @return true if the specified client's id is equals to the restricted (used in this request) client's id
     */
    public static boolean isCurrentClient(final Long clientId) {
        if (WebServiceContext.isPosContext()) {
            return false;
        } else {
            return ObjectUtils.equals(WebServiceContext.getClient().getId(), clientId);
        }
    }

    /**
     * Checks whether the given fault was generated by Cyclos
     */
    public static boolean isFromCyclos(final Fault fault) {
        return CODE_PREFIX.equals(fault.getFaultCode().getNamespaceURI());
    }

    /**
     * Returns the HttpServletRequest instance for the given SOAP message
     */
    public static HttpServletRequest requestOf(final SoapMessage message) {
        return (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
    }

    /**
     * Returns the HttpServletRequest instance for the given SOAP message
     */
    public static ServletContext servletContextOf(final SoapMessage message) {
        return (ServletContext) message.get(AbstractHTTPDestination.HTTP_CONTEXT);
    }

    /**
     * Returns a SOAP fault
     */
    private static SoapFault fault(final String code, final Throwable th) throws SoapFault {
        SoapFault sf = new SoapFault("Server error: " + code, th, faultCode(code));
        if (th != null) {
            org.w3c.dom.Element el = sf.getOrCreateDetail();
            el.setTextContent(th.getMessage());
            sf.setDetail(el);
        }
        return sf;
    }

    /**
     * Returns a qualified name for a fault code
     */
    private static QName faultCode(final String code) {
        return new QName(CODE_PREFIX, code);
    }

    private LoggingHandler loggingHandler;

    public void error(final String error) {
        error(new Exception(error));
    }

    public void error(final Throwable th) {
        try {
            /* the context could not be initialized, for example, if there was an error in the unmarshalling phase */
            if (WebServiceContext.isInitialized()) {
                final String remoteAddr = WebServiceContext.getRequest().getRemoteAddr();
                final String methodName = WebServiceHelper.getWebServiceOperationName(WebServiceContext.getSoapMessage());
                final Map<String, Object> params = WebServiceHelper.getParameters(WebServiceContext.getSoapMessage());

                if (WebServiceContext.isPosContext()) {
                    loggingHandler.traceWebServiceError(remoteAddr, WebServiceContext.getPos(), methodName, params, th);
                } else {
                    loggingHandler.traceWebServiceError(remoteAddr, WebServiceContext.getClient(), methodName, params, th);
                }
            } else {
                loggingHandler.traceWebServiceError(th);
            }
        } finally {
            // in case of a Fault we are interested in the cause to be set as the error in the TxData
            if (th instanceof Fault && th.getCause() != null) {
                CurrentTransactionData.setError(th.getCause());
            } else {
                CurrentTransactionData.setError(th);
            }
        }
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void trace(final String message) {
        if (WebServiceContext.isPosContext()) {
            loggingHandler.traceWebService(WebServiceContext.getRequest().getRemoteAddr(), WebServiceContext.getPos(), getWebServiceOperationName(WebServiceContext.getSoapMessage()), message);
        } else {
            loggingHandler.traceWebService(WebServiceContext.getRequest().getRemoteAddr(), WebServiceContext.getClient(), getWebServiceOperationName(WebServiceContext.getSoapMessage()), message);
        }
    }
}
