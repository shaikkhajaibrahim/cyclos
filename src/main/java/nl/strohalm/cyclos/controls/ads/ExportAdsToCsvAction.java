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
package nl.strohalm.cyclos.controls.ads;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseCsvAction;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.ads.FullTextAdQuery;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.MessageHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.conversion.CustomFieldConverter;
import nl.strohalm.cyclos.utils.csv.CSVWriter;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Action used to export an ad search result as csv
 * @author luis
 */
public class ExportAdsToCsvAction extends BaseCsvAction {

    private AdService                   adService;
    private CustomFieldService          customFieldService;
    private DataBinder<FullTextAdQuery> dataBinder;
    private ReadWriteLock               lock = new ReentrantReadWriteLock(true);

    public AdService getAdService() {
        return adService;
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public DataBinder<FullTextAdQuery> getDataBinder() {
        try {
            lock.readLock().lock();
            if (dataBinder == null) {
                dataBinder = SearchAdsAction.adFullTextQueryDataBinder(getLocalSettings());
            }
            return dataBinder;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        try {
            lock.writeLock().lock();
            super.onLocalSettingsUpdate(event);
            dataBinder = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Inject
    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Override
    protected List<?> executeQuery(final ActionContext context) {
        final SearchAdsForm form = context.getForm();
        final FullTextAdQuery query = getDataBinder().readFromString(form.getQuery());
        query.setResultType(ResultType.ITERATOR);
        query.fetch(RelationshipHelper.nested(Ad.Relationships.CATEGORY, RelationshipHelper.nested(AdCategory.MAX_LEVEL, AdCategory.Relationships.PARENT)), Ad.Relationships.CURRENCY, Ad.Relationships.CUSTOM_VALUES, RelationshipHelper.nested(Ad.Relationships.OWNER, Element.Relationships.USER));
        return adService.fullTextSearch(query);
    }

    @Override
    protected String fileName(final ActionContext context) {
        final User loggedUser = context.getUser();
        return "ads_" + loggedUser.getUsername() + ".csv";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CSVWriter initCsvWriter(final ActionContext context) {
        final LocalSettings settings = getLocalSettings();
        final CSVWriter<Ad> csv = CSVWriter.instance(Ad.class, settings);
        csv.addColumn(context.message("ad.id"), "id");
        csv.addColumn(context.message("ad.tradeType"), "tradeType", MessageHelper.getMessageConverter(getServlet().getServletContext(), "ad.tradeType."));
        csv.addColumn(context.message("ad.title"), "title");
        csv.addColumn(context.message("ad.category"), "category.fullName");
        csv.addColumn(context.message("ad.price"), "price", settings.getNumberConverter());
        csv.addColumn(context.message("accountType.currency"), "currency.symbol");
        csv.addColumn(context.message("ad.permanent"), "permanent");
        csv.addColumn(context.message("ad.publicationPeriod.begin"), "publicationPeriod.begin", settings.getRawDateConverter());
        csv.addColumn(context.message("ad.publicationPeriod.end"), "publicationPeriod.end", settings.getRawDateConverter());
        csv.addColumn(context.message("member.username"), "owner.username");
        csv.addColumn(context.message("ad.owner"), "owner.name");
        csv.addColumn(context.message("ad.externalPublication"), "externalPublication");
        csv.addColumn(context.message("ad.description"), "description");
        final List<AdCustomField> customFields = (List<AdCustomField>) customFieldService.listByNature(CustomField.Nature.AD);
        for (final AdCustomField field : customFields) {
            csv.addColumn(field.getName(), "customValues", new CustomFieldConverter(field));
        }
        return csv;
    }
}
