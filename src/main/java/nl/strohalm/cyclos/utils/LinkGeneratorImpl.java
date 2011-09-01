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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.CyclosConfiguration;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.accounts.guarantees.Certification;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligation;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.alerts.ErrorLogEntry;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.GeneralReference;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.TransactionFeedback;
import nl.strohalm.cyclos.entities.members.TransactionFeedbackRequest;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ServletContextAware;

/**
 * Implementation for link generator
 * @author jefferson
 */
public class LinkGeneratorImpl implements LinkGenerator, ServletContextAware {

    private static final Log LOG = LogFactory.getLog(LinkGeneratorImpl.class);
    private ServletContext   servletContext;
    private String           baseUrl;

    public String generateForApplicationRoot() {
        return buildTagFor(getRootUrl());
    }

    public String generateLinkFor(final Element element, final Entity entity) throws EntityNotFoundException {
        String relativePath = "";
        if (entity instanceof Ad) {
            relativePath = "viewAd?id=";
        } else if (entity instanceof Member) {
            relativePath = "profile?memberId=";
        } else if (entity instanceof Invoice) {
            relativePath = "invoiceDetails?invoiceId=";
        } else if (entity instanceof Transfer) {
            relativePath = "viewTransaction?transferId=";
        } else if (entity instanceof GeneralReference) {
            relativePath = "generalReferenceDetails?referenceId=";
        } else if (entity instanceof TransactionFeedback) {
            relativePath = "transactionFeedbackDetails?referenceId=";
        } else if (entity instanceof TransactionFeedbackRequest) {
            final TransactionFeedbackRequest transactionFeedbackRequest = (TransactionFeedbackRequest) entity;
            String paramName;
            if (transactionFeedbackRequest.getPayment() instanceof ScheduledPayment) {
                paramName = "scheduledPaymentId";
            } else {
                paramName = "transferId";
            }
            relativePath = "transactionFeedbackDetails?" + paramName + "=";
        } else if (entity instanceof Loan) {
            relativePath = "loanDetails?loanId=";
        } else if (entity instanceof ErrorLogEntry) {
            relativePath = "viewErrorLogEntry?entryId=";
        } else if (entity instanceof BrokerCommissionContract) {
            relativePath = "editBrokerCommissionContract?brokerCommissionContractId=";
        } else if (entity instanceof Certification) {
            relativePath = "editCertification?certificationId=";
        } else if (entity instanceof Guarantee) {
            relativePath = "guaranteeDetails?guaranteeId=";
        } else if (entity instanceof PaymentObligation) {
            relativePath = "editPaymentObligation?paymentObligationId=";
        } else {
            throw new EntityNotFoundException(entity.getClass());
        }

        final String path = "do/" + element.getNature().name().toLowerCase() + "/" + relativePath + entity.getId();
        final String url = baseUrl + "/do/redirectFromMessage?userId=" + element.getId() + "&path=" + StringHelper.encodeUrl("/" + path);
        return buildTagFor(url);
    }

    public String generateLinkForMailValidation(final String key) {
        return buildTagFor(getMailValidationUrl(key));
    }

    public String getMailValidationUrl(final String key) {
        return baseUrl + "/do/validateRegistration?key=" + key;
    }

    public String getRootUrl() {
        return baseUrl + "/";
    }

    public void init() {
        Properties properties;
        try {
            properties = CyclosConfiguration.getCyclosProperties();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        baseUrl = StringUtils.trimToEmpty(properties.getProperty("cyclos.host.url"));
        if (baseUrl.length() == 0) {
            LOG.warn("No url was set on cyclos.host.url property");
        } else {
            try {
                final URL url = new URL(baseUrl);
                if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                    LOG.warn("Invalid protocolo for url on cyclos.host.url property: " + baseUrl);
                }
            } catch (final MalformedURLException e) {
                LOG.warn("Malformed url on cyclos.host.url property: " + baseUrl);
            }
            // Remove a possible trailing slash
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
        }
    }

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Build the anchor tag for the given url, using a message key 'message.link.label' to retrieve the label
     */
    private String buildTagFor(final String url) {
        final String label = MessageHelper.message(servletContext, "message.link.label");
        return buildTagFor(url, label);
    }

    /**
     * Build the anchor tag for the given url, with a given label key
     */
    private String buildTagFor(final String url, final String label) {
        return "<a class=\"default\" href=\"" + url + "\">" + label + "</a>";
    }
}