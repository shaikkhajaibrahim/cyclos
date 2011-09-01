/*
 Created on 30/06/2004

 Copyright (c) Strohalm and others.
 
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Cyclos is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Cyclos; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package nl.strohalm.cyclos.http;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import nl.strohalm.cyclos.http.lifecycle.AccountStatusInitialization;
import nl.strohalm.cyclos.http.lifecycle.AdCategoryInitialization;
import nl.strohalm.cyclos.http.lifecycle.ApplicationShutdown;
import nl.strohalm.cyclos.http.lifecycle.ApplicationStartup;
import nl.strohalm.cyclos.http.lifecycle.BrokerCommissionInitialization;
import nl.strohalm.cyclos.http.lifecycle.CardInitialization;
import nl.strohalm.cyclos.http.lifecycle.CertificationInitialization;
import nl.strohalm.cyclos.http.lifecycle.ContextFinalization;
import nl.strohalm.cyclos.http.lifecycle.ContextInitialization;
import nl.strohalm.cyclos.http.lifecycle.CustomizedFileInitialization;
import nl.strohalm.cyclos.http.lifecycle.DocumentInitialization;
import nl.strohalm.cyclos.http.lifecycle.GuaranteeInitialization;
import nl.strohalm.cyclos.http.lifecycle.GuaranteeLoanInitialization;
import nl.strohalm.cyclos.http.lifecycle.ImageInitialization;
import nl.strohalm.cyclos.http.lifecycle.InvoiceInitialization;
import nl.strohalm.cyclos.http.lifecycle.LoanInitialization;
import nl.strohalm.cyclos.http.lifecycle.MessageInitialization;
import nl.strohalm.cyclos.http.lifecycle.PaymentObligationInitialization;
import nl.strohalm.cyclos.http.lifecycle.PermissionInitialization;
import nl.strohalm.cyclos.http.lifecycle.ScheduledPaymentsInitialization;
import nl.strohalm.cyclos.http.lifecycle.SettingsInitialization;
import nl.strohalm.cyclos.http.lifecycle.SmsMailingsInitialization;
import nl.strohalm.cyclos.http.lifecycle.StrutsInitialization;
import nl.strohalm.cyclos.http.lifecycle.TranslationMessageInitialization;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.SpringHelper;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Listener for context events
 * @author luis
 */
public class LifecycleListener implements ServletContextListener, HttpSessionListener {
    private static final Log                        LOG             = LogFactory.getLog(LifecycleListener.class);

    private final Collection<ContextInitialization> initializations = new ArrayList<ContextInitialization>();
    private final Collection<ContextFinalization>   finalizations   = new ArrayList<ContextFinalization>();
    private AccessService                           accessService;
    private TransactionTemplate                     transactionTemplate;
    private AccountStatusHandler                    accountStatusHandler;
    private IndexHandler                            indexHandler;

    public LifecycleListener() {
        try {
            initializations.add(new StrutsInitialization());
            initializations.add(new TranslationMessageInitialization());
            initializations.add(new SettingsInitialization());
            initializations.add(new ScheduledPaymentsInitialization());
            initializations.add(new LoanInitialization());
            initializations.add(new PermissionInitialization());
            initializations.add(new CustomizedFileInitialization());
            initializations.add(new ImageInitialization());
            initializations.add(new DocumentInitialization());
            initializations.add(new AdCategoryInitialization());
            initializations.add(new InvoiceInitialization());
            initializations.add(new BrokerCommissionInitialization());
            initializations.add(new GuaranteeLoanInitialization());
            initializations.add(new CertificationInitialization());
            initializations.add(new GuaranteeInitialization());
            initializations.add(new PaymentObligationInitialization());
            initializations.add(new SmsMailingsInitialization());
            initializations.add(new MessageInitialization());
            initializations.add(new CardInitialization());

            // Warning: This should be after other initializations, because it's needed to ensure consistency of other initializations
            initializations.add(new AccountStatusInitialization());

            initializations.add(new ApplicationStartup());

            finalizations.add(new ApplicationShutdown());
        } catch (final Throwable e) {
            System.out.println("Error on ContextListener constructor");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    public void contextDestroyed(final ServletContextEvent event) {
        try {
            final ServletContext context = event.getServletContext();
            final WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);
            runAll(applicationContext, finalizations);
        } catch (final Throwable e) {
            LOG.error("Error on LifecycleListener.contextDestroyed()", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public void contextInitialized(final ServletContextEvent event) {
        try {
            final ServletContext context = event.getServletContext();

            context.setAttribute("systemOnline", true);

            final WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(context);
            accessService = SpringHelper.bean(applicationContext, "accessService");
            transactionTemplate = SpringHelper.bean(applicationContext, "transactionTemplate");
            accountStatusHandler = SpringHelper.bean(applicationContext, "accountStatusHandler");
            indexHandler = SpringHelper.bean(applicationContext, "indexHandler");

            resolveDependencies(applicationContext);
            runAll(applicationContext, initializations);

            // Suggest a GC in order to keep the heap low right after a startup
            System.gc();
        } catch (final Throwable e) {
            LOG.error("Error on LifecycleListener.contextInitialized()", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionCreated(final HttpSessionEvent event) {
        // Nothing to do
    }

    /**
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    public void sessionDestroyed(final HttpSessionEvent event) {
        final HttpSession session = event.getSession();
        final String sessionId = session == null ? null : session.getId();
        if (sessionId == null) {
            return;
        }

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                try {
                    accessService.logout(sessionId);
                } catch (final NotConnectedException e) {
                    // Ok, just ignore
                } catch (final RuntimeException e) {
                    LOG.warn("Error logging out member on session destroy", e);
                    status.setRollbackOnly();
                }
            }
        });
    }

    /**
     * @param applicationContext
     */
    private void resolveDependencies(final WebApplicationContext applicationContext) {
        // Inject dependencies on initializations and finalizations
        for (final Object object : CollectionUtils.union(initializations, finalizations)) {
            SpringHelper.injectBeans(applicationContext, object);
        }
    }

    /**
     * Run a single initialization / finalization inside a transaction
     */
    private void run(final ServletContext context, final Object object) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        if (object instanceof ContextInitialization) {
                            ((ContextInitialization) object).init(context);
                        } else if (object instanceof ContextFinalization) {
                            ((ContextFinalization) object).destroy(context);
                        }
                    } catch (final RuntimeException e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                }
            });
            // Process the current transaction data
            accountStatusHandler.processFromCurrentTransaction();
            indexHandler.processFromCurrentTransaction();
            CurrentTransactionData.runCurrentTransactionCommitListeners();
        } finally {
            // Ensure the current transaction data is cleared out
            CurrentTransactionData.cleanup();
        }
    }

    /**
     * Run all given initializations / finalizations
     */
    private void runAll(final WebApplicationContext applicationContext, final Collection<?> objects) {
        final ServletContext context = applicationContext.getServletContext();
        for (final Object object : objects) {
            run(context, object);
        }
    }
}
