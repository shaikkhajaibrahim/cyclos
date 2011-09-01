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
package nl.strohalm.cyclos.services.customization;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import nl.strohalm.cyclos.dao.customizations.TranslationMessageDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.customization.translationMessages.TranslationMessage;
import nl.strohalm.cyclos.entities.customization.translationMessages.TranslationMessageQuery;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.CacheCleaner;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation for message service
 * @author luis
 */
public class TranslationMessageServiceImpl implements TranslationMessageService {

    private TranslationMessageDAO translationMessageDao;
    private Properties            cachedProperties;
    private FetchService          fetchService;

    public synchronized Properties exportAsProperties() {
        if (cachedProperties == null) {
            // Rebuild the cached properties
            cachedProperties = translationMessageDao.listAsProperties();
        }
        return cachedProperties;

    }

    public Properties getAsProperties() {
        return exportAsProperties();
    }

    public void importFromProperties(final Properties properties, MessageImportType importType) {
        // Delete all messages if we will replace with the new file
        if (importType == MessageImportType.REPLACE) {
            translationMessageDao.deleteAll();
            importType = MessageImportType.ONLY_NEW;
        }

        if (importType == MessageImportType.ONLY_NEW) {
            importOnlyNewProperties(properties);
        } else {
            final boolean emptyOnly = importType == MessageImportType.NEW_AND_EMPTY;
            importNewAndModifiedProperties(properties, emptyOnly);
        }

        invalidateCache();
    }

    public void initialize(final Properties properties) {
        importFromProperties(properties, MessageImportType.NEW_AND_EMPTY);
    }

    public TranslationMessage load(final Long id) {
        return translationMessageDao.load(id);
    }

    public int remove(final Long... ids) {
        final int count = translationMessageDao.delete(ids);

        invalidateCache();

        return count;
    }

    public TranslationMessage save(TranslationMessage translationMessage) {
        validate(translationMessage);
        if (translationMessage.isTransient()) {
            translationMessage = translationMessageDao.insert(translationMessage);
        } else {
            translationMessage = translationMessageDao.update(translationMessage);
        }

        invalidateCache();

        return translationMessage;
    }

    public List<TranslationMessage> search(final TranslationMessageQuery query) {
        return translationMessageDao.search(query);
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setTranslationMessageDao(final TranslationMessageDAO translationMessageDao) {
        this.translationMessageDao = translationMessageDao;
    }

    public void validate(final TranslationMessage translationMessage) {
        getValidator().validate(translationMessage);
    }

    protected synchronized void invalidateCache() {
        cachedProperties = null;
    }

    private Validator getValidator() {
        final Validator validator = new Validator();
        validator.property("key").required().maxLength(100);
        validator.property("value").maxLength(4000);
        return validator;
    }

    private void importNewAndModifiedProperties(final Properties properties, final boolean emptyOnly) {
        // Process existing messages. This is done with Object[], otherwise hibernate will load each message with a separate select
        final Iterator<Object[]> existing = translationMessageDao.listData();
        try {
            while (existing.hasNext()) {
                final Object[] data = existing.next();
                final String key = (String) data[1];
                final String currentValue = (String) data[2];
                final String newValue = properties.getProperty(key);
                if (newValue != null) {
                    final boolean shallUpdate = !newValue.equals(currentValue) && (!emptyOnly || StringUtils.isEmpty(currentValue));
                    if (shallUpdate) {
                        final TranslationMessage message = new TranslationMessage();
                        message.setId((Long) data[0]);
                        message.setKey(key);
                        message.setValue(newValue);
                        translationMessageDao.update(message, false);
                    }
                    properties.remove(key);
                }
            }
        } finally {
            DataIteratorHelper.close(existing);
        }
        fetchService.clearCache();

        // Only those who have to be inserted are left in properties
        insertAll(properties);
    }

    private void importOnlyNewProperties(final Properties properties) {
        final Iterator<String> allKeys = translationMessageDao.listAllKeys();
        try {
            while (allKeys.hasNext()) {
                final String key = allKeys.next();
                properties.remove(key);
            }
        } finally {
            DataIteratorHelper.close(allKeys);
        }

        // Only new keys are left on the properties object
        insertAll(properties);
    }

    private void insertAll(final Properties properties) {
        final CacheCleaner cacheCleaner = new CacheCleaner(fetchService);
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            final String key = (String) entry.getKey();
            final String value = (String) entry.getValue();
            final TranslationMessage translationMessage = new TranslationMessage();
            translationMessage.setKey(key);
            translationMessage.setValue(value);

            try {
                // Try to load first
                final TranslationMessage existing = translationMessageDao.load(key);
                // Existing - update
                existing.setValue(value);
                translationMessageDao.update(existing, false);
            } catch (final EntityNotFoundException e) {
                // Not found - insert
                translationMessageDao.insert(translationMessage);
            }
            // Clear the entity cache to avoid an explosion of messages in cache
            cacheCleaner.clearCache();
        }
    }
}
