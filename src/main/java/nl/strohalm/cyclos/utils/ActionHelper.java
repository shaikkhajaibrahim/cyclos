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
package nl.strohalm.cyclos.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.alerts.ErrorLogService;
import nl.strohalm.cyclos.services.transactions.exceptions.CreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.TransferMinimumPaymentException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Contains helper methods for struts actions
 * @author luis
 */
public final class ActionHelper {

    /**
     * Interface for implementations that extracts the by element from an entity.
     * @author ameyer
     * 
     */
    public interface ByElementExtractor {
        Element getByElement(Entity entity);
    }

    private static Log LOG = LogFactory.getLog(ActionHelper.class);

    /**
     * Returns an action forward to go back to the previous action
     */
    public static ActionForward back(final ActionMapping actionMapping) {
        return actionMapping.findForward("back");
    }

    /**
     * Generate an error log for the given exception, on a separate transaction
     */
    @SuppressWarnings("unchecked")
    public static void generateLog(final HttpServletRequest request, final ServletContext servletContext, final Throwable error) {
        CurrentTransactionData.setError(error);
        try {
            final PlatformTransactionManager transactionManager = SpringHelper.bean(servletContext, "transactionManager");
            final TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    final ErrorLogService els = SpringHelper.bean(servletContext, "errorLogService");
                    els.insert(error, request.getRequestURI(), request.getParameterMap());
                };
            });
        } catch (final Exception e) {
            LOG.warn("Error while creating error log entry", e);
        }
    }

    /**
     * Extracts the elements from the entities and returns a map that might contain two items: the by element and the type of element. When the
     * extracted element denotes a system task or an Administrator and the logged user is not an Administrator, only the type of element will appear
     * in the corresponding map.
     */
    @SuppressWarnings("unchecked")
    public static Collection<Map<String, Object>> getByElements(final ActionContext context, final Collection<? extends Entity> entities, final ByElementExtractor extractor) {
        if (entities.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        final Collection<Map<String, Object>> byCollection = new ArrayList<Map<String, Object>>();
        for (final Entity entity : entities) {
            final Element by = extractor.getByElement(entity);
            final Map map = new HashMap<String, Object>();
            if (by == null) {
                map.put("byType", "SystemTask");
            } else if (by instanceof Administrator) {
                if (context.isAdmin()) {
                    map.put("by", by);
                    map.put("byType", "Admin");
                } else {
                    map.put("byType", "System");
                }
            } else if ((by instanceof Operator) && (context.isMemberOf((Operator) by) || context.getElement().equals(by))) {
                map.put("by", by);
                map.put("byType", "Operator");
            } else {
                final Member member = (Member) by.getAccountOwner();
                map.put("by", member);
                map.put("byType", "Member");
            }

            byCollection.add(map);
        }

        return byCollection;
    }

    public static ActionForward handleValidationException(final ActionMapping actionMapping, final HttpServletRequest request, final HttpServletResponse response, final ValidationException e) {
        if (e == null) {
            return null;
        }
        String key = "error.validation";
        List<Object> args = Collections.emptyList();
        if (!e.getGeneralErrors().isEmpty()) {
            final ValidationError error = e.getGeneralErrors().iterator().next();
            key = error.getKey();
            args = error.getArguments();
        } else if (!e.getErrorsByProperty().isEmpty()) {
            final Entry<String, Collection<ValidationError>> entry = e.getErrorsByProperty().entrySet().iterator().next();
            final Collection<ValidationError> errors = entry.getValue();
            if (!errors.isEmpty()) {
                // We must show the validation error in a friendly way
                final String propertyName = entry.getKey();
                final ValidationError error = errors.iterator().next();
                key = error.getKey();
                args = new ArrayList<Object>();
                // First, check if there's a fixed display name for the property...
                String propertyLabel = e.getPropertyDisplayName(propertyName);
                if (StringUtils.isEmpty(propertyLabel)) {
                    // ... it doesn't. Check if there's a message key...
                    final String propertyKey = e.getPropertyKey(propertyName);
                    if (StringUtils.isNotEmpty(propertyKey)) {
                        // ... the key is set! Get the property label from the message bundle.
                        propertyLabel = MessageHelper.message(request, e.getPropertyKey(propertyName));
                    } else {
                        // ... we're out of luck! There's no property key. Use the raw property name as label, which is ugly!
                        propertyLabel = propertyName;
                    }
                }
                // The first message argument is always the property label
                args.add(propertyLabel);
                if (error.getArguments() != null) {
                    // If there are more, add them as well.
                    args.addAll(error.getArguments());
                }
            }
        }
        // With the key and arguments, we can show a friendly message to the user
        return sendError(actionMapping, request, response, key, args.toArray());
    }

    /**
     * Return a redirect for the ActionForward with the specified parameter
     */
    public static ActionForward redirectWithParam(final HttpServletRequest request, final ActionForward forward, final String name, final Object value) {
        return redirectWithParams(request, forward, Collections.singletonMap(name, value));
    }

    /**
     * Return a redirect for the ActionForward with the specified parameters
     */
    public static ActionForward redirectWithParams(final HttpServletRequest request, ActionForward forward, final Map<String, Object> params) {
        if (forward == null) {
            return null;
        }
        final LocalSettings settings = SettingsHelper.getLocalSettings(request);
        forward = new ActionForward(forward);
        final StringBuilder path = new StringBuilder();
        path.append(forward.getPath());
        if (MapUtils.isNotEmpty(params)) {
            path.append('?');
            for (final Entry<String, Object> entry : params.entrySet()) {
                final Object value = entry.getValue();
                try {
                    path.append(entry.getKey()).append('=').append(URLEncoder.encode(value == null ? "" : value.toString(), settings.getCharset()));
                } catch (final UnsupportedEncodingException e) {
                }
                path.append('&');
            }
            if (path.charAt(path.length() - 1) == '&') {
                path.setLength(path.length() - 1);
            }
            forward.setPath(path.toString());
        }
        forward.setRedirect(true);
        return forward;
    }

    /**
     * Given a credits exception, resolve it's error key
     */
    public static String resolveErrorKey(final CreditsException exception) {
        if (exception instanceof MaxAmountPerDayExceededException) {
            return "payment.error.maxAmountOnDayExceeded";
        } else if (exception instanceof NotEnoughCreditsException) {
            if (((NotEnoughCreditsException) exception).isOriginalAccount()) {
                return "payment.error.enoughCredits";
            } else {
                return "payment.error.enoughCreditsOtherAccount";
            }
        } else if (exception instanceof TransferMinimumPaymentException) {
            return "payment.error.transferMinimum";
        } else if (exception instanceof UpperCreditLimitReachedException) {
            return "payment.error.upperCreditLimit";
        } else {
            return "error.general";
        }
    }

    public static Object[] resolveParameters(final CreditsException exception) {
        if (exception instanceof MaxAmountPerDayExceededException) {
            return new Object[] { ((MaxAmountPerDayExceededException) exception).getTransferType().getName() };
        } else if (exception instanceof NotEnoughCreditsException) {
            return new Object[] { exception.getAccount().getType().getName() };
        } else if (exception instanceof TransferMinimumPaymentException) {
            return new Object[] { ((TransferMinimumPaymentException) exception).getMinimunPayment() };
        } else if (exception instanceof UpperCreditLimitReachedException) {
            return new Object[] { exception.getAccount().getType().getName(), ((UpperCreditLimitReachedException) exception).getUpperLimit() };
        } else {
            return new Object[] {};
        }
    }

    /**
     * Sends an error message to the error page
     * @return The ActionForward to the error page
     */
    public static ActionForward sendError(final ActionMapping actionMapping, final HttpServletRequest request, final HttpServletResponse response, final String key, final Object... arguments) {
        final HttpSession session = request.getSession();
        session.setAttribute("errorKey", key);
        session.setAttribute("errorArguments", arguments);
        return actionMapping.findForward("error");
    }

    /**
     * Sends a message to the next page
     */
    public static void sendMessage(final HttpServletRequest request, final HttpServletResponse response, final String key, final Object... arguments) {
        final HttpSession session = request.getSession();
        session.setAttribute("messageKey", key);
        session.setAttribute("messageArguments", arguments);
        response.addCookie(new Cookie("showMessage", "true"));
    }
}
