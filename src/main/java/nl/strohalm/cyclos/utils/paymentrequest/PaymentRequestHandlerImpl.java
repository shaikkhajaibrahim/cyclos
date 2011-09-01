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
package nl.strohalm.cyclos.utils.paymentrequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentRequestTicket;
import nl.strohalm.cyclos.entities.accounts.transactions.Ticket;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.alerts.ErrorLogService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.PaymentRequestHandler;
import nl.strohalm.cyclos.services.transactions.TicketService;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.SpringHelper;
import nl.strohalm.cyclos.utils.WorkerThreads;
import nl.strohalm.cyclos.utils.CurrentTransactionData.TransactionCommitListener;
import nl.strohalm.cyclos.webservices.external.ExternalWebServiceHelper;
import nl.strohalm.cyclos.webservices.external.paymentrequest.PaymentRequestWebService;
import nl.strohalm.cyclos.webservices.model.PaymentRequestTicketVO;
import nl.strohalm.cyclos.webservices.utils.server.TicketHelper;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.ServletContextAware;

/**
 * Implementation for payment request handler that invokes the PaymentRequestWebService
 * 
 * @author luis
 */
public class PaymentRequestHandlerImpl implements PaymentRequestHandler, ServletContextAware, DisposableBean {

    /**
     * A payment request handler which never sends a payment request
     * @author luis
     */
    private static class OfflineHandler implements PaymentRequestWebService {
        public boolean requestPayment(final String cyclosId, final PaymentRequestTicketVO ticket) {
            return false;
        }
    }

    private class PaymentRequestSenderThreads extends WorkerThreads<PaymentRequestTicket> {
        protected PaymentRequestSenderThreads(final String name, final int threadCount) {
            super(name, threadCount);
        }

        @Override
        protected void process(final PaymentRequestTicket t) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    final PaymentRequestTicket ticket = fetchService.fetch(t, Ticket.Relationships.FROM, Ticket.Relationships.TO, PaymentRequestTicket.Relationships.FROM_CHANNEL, PaymentRequestTicket.Relationships.TO_CHANNEL);
                    final LocalSettings localSettings = settingsService.getLocalSettings();
                    final Channel channel = ticket.getToChannel();

                    try {
                        final PaymentRequestWebService ws = proxyFor(channel);
                        final boolean result = ws.requestPayment(localSettings.getCyclosId(), ticketHelper.toVO(ticket));
                        if (!result) {
                            throw new Exception("The PaymentRequestWebService returned an error status");
                        }
                    } catch (final Exception e) {
                        ticketService.markAsFailedtoSend(ticket);
                        final Map<String, String> params = new HashMap<String, String>();
                        params.put("ticket", ticket.getTicket());
                        params.put("payer username", ticket.getFrom().getUsername());
                        params.put("receiver username", ticket.getTo().getUsername());
                        errorLogService.insert(e, channel.getPaymentRequestWebServiceUrl(), params);
                    }
                }
            });
        }
    }

    private TicketHelper                           ticketHelper;
    private SettingsService                        settingsService;
    private TicketService                          ticketService;
    private Map<Channel, PaymentRequestWebService> proxiesByChannel = new ConcurrentHashMap<Channel, PaymentRequestWebService>();
    private Map<Channel, String>                   urlsByChannel    = new ConcurrentHashMap<Channel, String>();
    private ErrorLogService                        errorLogService;
    private ServletContext                         servletContext;
    private boolean                                initialized;
    private PaymentRequestSenderThreads            senderThreads;
    private int                                    maxThreads       = 5;
    private TransactionTemplate                    transactionTemplate;
    private FetchService                           fetchService;

    public void destroy() throws Exception {
        if (senderThreads != null) {
            senderThreads.interrupt();
            senderThreads = null;
        }
    }

    public void sendRequest(final PaymentRequestTicket ticket) {
        maybeInitialize();

        if (senderThreads == null) {
            return;
        }
        CurrentTransactionData.addTransactionCommitListener(new TransactionCommitListener() {
            public void onTransactionCommit() {
                senderThreads.enqueue(ticket);
            }
        });
    }

    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private synchronized void maybeInitialize() {
        // As the standard setter injection was causing problems with other beans, for recursive injection,
        // which caused unproxied instances to be injected, the setter injection is no longer used here
        if (!initialized) {
            settingsService = SpringHelper.bean(servletContext, "settingsService");
            ticketHelper = SpringHelper.bean(servletContext, "ticketHelper");
            errorLogService = SpringHelper.bean(servletContext, "errorLogService");
            ticketService = SpringHelper.bean(servletContext, "ticketService");
            transactionTemplate = SpringHelper.bean(servletContext, "transactionTemplate");
            fetchService = SpringHelper.bean(servletContext, "fetchService");

            senderThreads = new PaymentRequestSenderThreads("Payment request sender for " + settingsService.getLocalSettings().getApplicationName(), maxThreads);

            initialized = true;
        }
    }

    private PaymentRequestWebService proxyFor(final Channel channel) throws IOException {
        final String url = channel.getPaymentRequestWebServiceUrl();
        PaymentRequestWebService proxy = proxiesByChannel.get(channel);
        if (proxy != null) {
            // Check whether the url has changed
            if (!url.equals(urlsByChannel.get(channel))) {
                // The url has changed! Ensure the proxy will be recreated
                proxy = null;
            }
        }
        if (proxy == null) {
            // Create the proxy
            if (StringUtils.isEmpty(url)) {
                // There is no information for payment request URL. Assume the system is offline
                proxy = new OfflineHandler();
            } else {
                // Create the proxy
                proxy = ExternalWebServiceHelper.proxyFor(PaymentRequestWebService.class, url);
            }
            // Store on cache
            proxiesByChannel.put(channel, proxy);
            urlsByChannel.put(channel, url);
        }
        return proxy;
    }
}
