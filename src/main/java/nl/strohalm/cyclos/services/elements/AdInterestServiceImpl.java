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
package nl.strohalm.cyclos.services.elements;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.strohalm.cyclos.dao.members.AdInterestDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.adInterests.AdInterest;
import nl.strohalm.cyclos.entities.members.adInterests.AdInterestQuery;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CacheCleaner;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service implementation for advertisements
 * @author luis
 */
public class AdInterestServiceImpl implements AdInterestService, DisposableBean {

    /**
     * Validates the ad interest before saving. The ad interest must have at least one of the data: message category, member or keyword
     * @author jefferson
     */
    public class AdInterestValidation implements GeneralValidation {

        private static final long serialVersionUID = -4657610144838426258L;

        public ValidationError validate(final Object object) {
            final AdInterest adInterest = (AdInterest) object;

            if (adInterest.getCategory() == null && adInterest.getMember() == null && StringUtils.isEmpty(adInterest.getKeywords())) {
                return new ValidationError("adInterest.error.missingData");
            }
            return null;
        }
    }

    private class MemberNotificationThread extends Thread {
        private BlockingQueue<Ad> notificationQueue = new LinkedBlockingQueue<Ad>();

        @Override
        public void run() {
            try {
                while (true) {
                    final Ad ad = notificationQueue.take();
                    try {
                        // Notify the ad inside a transaction
                        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                                notifyMembers(status, ad);
                                adService.markMembersNotified(ad);
                            }
                        });
                    } catch (final Exception e) {
                        LOG.warn("Error while notifying interests for " + ad, e);

                        // On error, mark the ad as notified anyway
                        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                                try {
                                    adService.markMembersNotified(ad);
                                } catch (final Exception e) {
                                    LOG.warn("Error while marking ad as notified: " + ad, e);
                                }
                            }
                        });
                    }
                }
            } catch (final InterruptedException e) {
                // Interrupted: just leave the loop
            }
        }

        private void enqueue(final Ad ad) {
            notificationQueue.offer(ad);
        }

        private void notifyMembers(final TransactionStatus status, final Ad ad) {
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            final String subject = MessageProcessingHelper.processVariables(messageSettings.getAdInterestSubject(), ad, localSettings);
            final String body = MessageProcessingHelper.processVariables(messageSettings.getAdInterestMessage(), ad, localSettings);

            final CacheCleaner cacheCleaner = new CacheCleaner(fetchService);
            final Iterator<Member> iterator = adInterestDao.resolveMembersToNotify(ad);
            while (iterator.hasNext()) {
                final Member member = iterator.next();
                try {
                    final SendMessageFromSystemDTO dto = new SendMessageFromSystemDTO();
                    dto.setType(Message.Type.AD_INTEREST);
                    dto.setToMember(member);
                    dto.setSubject(subject);
                    dto.setBody(body);
                    dto.setEntity(ad);

                    // Send message to member
                    messageService.sendFromSystem(dto);

                    // Ensure the cache is cleared to avoid many objects in memory
                    cacheCleaner.clearCache();
                } catch (final Exception e) {
                    status.setRollbackOnly();
                    LOG.warn("Error notifying " + member + " of advertisement " + ad, e);
                    break;
                }
            }
        }
    }

    private static final Log                  LOG = LogFactory.getLog(AdInterestServiceImpl.class);

    private AdInterestDAO                     adInterestDao;
    private AdService                         adService;
    private MessageService                    messageService;
    private SettingsService                   settingsService;
    private TransactionTemplate               transactionTemplate;
    private FetchService                      fetchService;
    private volatile MemberNotificationThread memberNotificationThread;

    public synchronized void destroy() throws Exception {
        if (memberNotificationThread != null) {
            memberNotificationThread.interrupt();
            memberNotificationThread = null;
        }
    }

    public AdInterest load(final Long id, final Relationship... fetch) {
        return adInterestDao.load(id, fetch);
    }

    public void notifyInterestedMembers(final Ad ad) {
        if (ad.getStatus().isActive() && !ad.isMembersNotified()) {
            getMemberNotificationThread().enqueue(ad);
        }
    }

    public void notifyInterestedMembers(final Calendar time) {
        final AdQuery query = new AdQuery();
        query.fetch(Ad.Relationships.OWNER, Ad.Relationships.CUSTOM_VALUES);
        query.setResultType(ResultType.ITERATOR);
        query.setMembersNotified(false);
        query.setBeginDate(time);
        final List<Ad> ads = adService.search(query);
        for (final Ad ad : ads) {
            notifyInterestedMembers(ad);
        }
    }

    public int remove(final Long[] ids) {
        return adInterestDao.delete(ids);
    }

    public AdInterest save(AdInterest adInterest) {
        // It there is not a price range, set currency to null
        if (adInterest.getInitialPrice() == null && adInterest.getFinalPrice() == null) {
            adInterest.setCurrency(null);
        }

        // Validates before saving
        validate(adInterest);

        if (adInterest.isTransient()) {
            adInterest = adInterestDao.insert(adInterest);
        } else {
            adInterest = adInterestDao.update(adInterest);
        }
        return adInterest;
    }

    public List<AdInterest> search(final AdInterestQuery query) {
        return adInterestDao.search(query);
    }

    public void setAdInterestDao(final AdInterestDAO adInterestDao) {
        this.adInterestDao = adInterestDao;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void validate(final AdInterest adInterest) throws ValidationException {
        getValidator().validate(adInterest);
    }

    private synchronized MemberNotificationThread getMemberNotificationThread() {
        if (memberNotificationThread == null || !memberNotificationThread.isAlive()) {
            final LocalSettings localSettings = settingsService.getLocalSettings();
            memberNotificationThread = new MemberNotificationThread();
            memberNotificationThread.setName("Member notification of ad interests for " + localSettings.getApplicationName());
            memberNotificationThread.start();
        }
        return memberNotificationThread;
    }

    private Validator getValidator() {
        final Validator validator = new Validator("adInterest");
        validator.general(new AdInterestValidation());
        validator.property("title").required().maxLength(100);
        validator.property("type").required();
        validator.property("initialPrice").key("adInterest.priceRange").positive();
        validator.property("finalPrice").key("adInterest.priceRange").positiveNonZero();
        return validator;
    }
}