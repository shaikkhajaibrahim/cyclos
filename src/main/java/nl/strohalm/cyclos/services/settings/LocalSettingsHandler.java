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
package nl.strohalm.cyclos.services.settings;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.Setting;
import nl.strohalm.cyclos.entities.settings.LocalSettings.CsvRecordSeparator;
import nl.strohalm.cyclos.entities.settings.LocalSettings.CsvStringQuote;
import nl.strohalm.cyclos.entities.settings.LocalSettings.CsvValueSeparator;
import nl.strohalm.cyclos.entities.settings.LocalSettings.DatePattern;
import nl.strohalm.cyclos.entities.settings.LocalSettings.DecimalInputMethod;
import nl.strohalm.cyclos.entities.settings.LocalSettings.Language;
import nl.strohalm.cyclos.entities.settings.LocalSettings.MemberResultDisplay;
import nl.strohalm.cyclos.entities.settings.LocalSettings.NumberLocale;
import nl.strohalm.cyclos.entities.settings.LocalSettings.Precision;
import nl.strohalm.cyclos.entities.settings.LocalSettings.TimePattern;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.FileUnits;
import nl.strohalm.cyclos.utils.TextFormat;
import nl.strohalm.cyclos.utils.TimePeriod.Field;
import nl.strohalm.cyclos.utils.conversion.CoercionConverter;
import nl.strohalm.cyclos.utils.conversion.Converter;
import nl.strohalm.cyclos.utils.conversion.IdConverter;
import nl.strohalm.cyclos.utils.conversion.TimeZoneConverter;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.lang.StringUtils;

/**
 * Local settings handler
 * @author luis
 */
public class LocalSettingsHandler extends BaseSettingsHandler<LocalSettings> {

    private GroupService       groupService;
    private FetchService       fetchService;
    private ChannelService     channelService;
    private CustomFieldService customFieldService;

    protected LocalSettingsHandler() {
        super(Setting.Type.LOCAL, LocalSettings.class);
    }

    public FetchService getFetchService() {
        return fetchService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected Map<String, Converter<?>> createConverters() {
        final Map<String, Converter<?>> localConverters = new LinkedHashMap<String, Converter<?>>();
        localConverters.put("applicationName", CoercionConverter.instance(String.class));
        localConverters.put("applicationUsername", CoercionConverter.instance(String.class));
        localConverters.put("language", CoercionConverter.instance(Language.class));
        localConverters.put("charset", CoercionConverter.instance(String.class));
        localConverters.put("numberLocale", CoercionConverter.instance(NumberLocale.class));
        localConverters.put("precision", CoercionConverter.instance(Precision.class));
        localConverters.put("highPrecision", CoercionConverter.instance(Precision.class));
        localConverters.put("decimalInputMethod", CoercionConverter.instance(DecimalInputMethod.class));
        localConverters.put("datePattern", CoercionConverter.instance(DatePattern.class));
        localConverters.put("timePattern", CoercionConverter.instance(TimePattern.class));
        localConverters.put("timeZone", TimeZoneConverter.instance());
        localConverters.put("containerUrl", CoercionConverter.instance(String.class));
        localConverters.put("maxIteratorResults", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxPageResults", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxAjaxResults", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxUploadSize", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxUploadUnits", CoercionConverter.instance(FileUnits.class));
        localConverters.put("maxImageWidth", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxImageHeight", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxThumbnailWidth", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxThumbnailHeight", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("referenceLevels", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("csvUseHeader", CoercionConverter.instance(Boolean.TYPE));
        localConverters.put("csvRecordSeparator", CoercionConverter.instance(CsvRecordSeparator.class));
        localConverters.put("csvValueSeparator", CoercionConverter.instance(CsvValueSeparator.class));
        localConverters.put("csvStringQuote", CoercionConverter.instance(CsvStringQuote.class));
        localConverters.put("cyclosId", CoercionConverter.instance(String.class));
        localConverters.put("sendSmsWebServiceUrl", CoercionConverter.instance(String.class));
        localConverters.put("smsChannelName", CoercionConverter.instance(String.class));
        localConverters.put("smsCustomFieldId", IdConverter.instance());
        localConverters.put("transactionNumber.prefix", CoercionConverter.instance(String.class));
        localConverters.put("transactionNumber.padLength", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("transactionNumber.suffix", CoercionConverter.instance(String.class));
        localConverters.put("emailRequired", CoercionConverter.instance(Boolean.TYPE));
        localConverters.put("emailUnique", CoercionConverter.instance(Boolean.TYPE));
        localConverters.put("brokeringExpirationPeriod.number", CoercionConverter.instance(Integer.class));
        localConverters.put("brokeringExpirationPeriod.field", CoercionConverter.instance(Field.class));
        localConverters.put("deleteMessagesOnTrashAfter.number", CoercionConverter.instance(Integer.class));
        localConverters.put("deleteMessagesOnTrashAfter.field", CoercionConverter.instance(Field.class));
        localConverters.put("deletePendingRegistrationsAfter.number", CoercionConverter.instance(Integer.class));
        localConverters.put("deletePendingRegistrationsAfter.field", CoercionConverter.instance(Field.class));
        localConverters.put("memberResultDisplay", CoercionConverter.instance(MemberResultDisplay.class));
        localConverters.put("adDescriptionFormat", CoercionConverter.instance(TextFormat.class));
        localConverters.put("messageFormat", CoercionConverter.instance(TextFormat.class));
        localConverters.put("schedulingHour", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("schedulingMinute", CoercionConverter.instance(Integer.TYPE));
        localConverters.put("maxChargebackTime.number", CoercionConverter.instance(Integer.class));
        localConverters.put("maxChargebackTime.field", CoercionConverter.instance(Field.class));
        localConverters.put("chargebackDescription", CoercionConverter.instance(String.class));
        localConverters.put("showCountersInAdCategories", CoercionConverter.instance(Boolean.TYPE));
        localConverters.put("fullNameExpression", CoercionConverter.instance(String.class));

        return localConverters;
    }

    @Override
    protected Validator createValidator() {
        final Validator localValidator = new Validator("settings.local");
        localValidator.property("applicationName").required().length(1, 100);
        localValidator.property("applicationUsername").required().length(1, 100);
        localValidator.property("language").required();
        localValidator.property("charset").required().add(new PropertyValidation() {
            private static final long serialVersionUID = 0L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                if (!(value instanceof String)) {
                    return null;
                }
                final String charSet = (String) value;
                if (!Charset.availableCharsets().containsKey(charSet)) {
                    return new InvalidError();
                }
                return null;
            }
        });
        localValidator.property("numberLocale").required();
        localValidator.property("precision").required().between(0, 6);
        localValidator.property("highPrecision").required().between(0, 6);
        localValidator.property("decimalInputMethod").required();
        localValidator.property("datePattern").required();
        localValidator.property("timePattern").required();
        localValidator.property("containerUrl").url();
        localValidator.property("maxIteratorResults").required().positive();
        localValidator.property("maxPageResults").required().positiveNonZero();
        localValidator.property("maxAjaxResults").required().positiveNonZero();
        localValidator.property("maxUploadSize").required().positiveNonZero();
        localValidator.property("maxUploadUnits").required();
        localValidator.property("maxImageWidth").required().between(10, 10000);
        localValidator.property("maxImageHeight").required().between(10, 10000);
        localValidator.property("maxThumbnailWidth").required().between(10, 200);
        localValidator.property("maxThumbnailHeight").required().between(10, 200);
        localValidator.property("referenceLevels").required().anyOf(3, 5);
        localValidator.property("csvRecordSeparator").required();
        localValidator.property("csvValueSeparator").required();
        localValidator.property("csvStringQuote").required();
        localValidator.property("cyclosId").length(0, 100);
        localValidator.property("transactionNumber.prefix").length(0, 50);
        localValidator.property("transactionNumber.padLength").between(0, 20);
        localValidator.property("transactionNumber.suffix").length(0, 50);
        localValidator.property("brokeringExpirationPeriod.number").between(0, 999);
        localValidator.property("brokeringExpirationPeriod.field").required();
        localValidator.property("deleteMessagesOnTrashAfter.number").between(0, 999);
        localValidator.property("deleteMessagesOnTrashAfter.field").required();
        localValidator.property("deletePendingRegistrationsAfter.number").between(0, 999);
        localValidator.property("deletePendingRegistrationsAfter.field").required();
        localValidator.property("memberResultDisplay").required();
        localValidator.property("adDescriptionFormat").required();
        localValidator.property("messageFormat").required();
        localValidator.property("schedulingHour").required().between(0, 23);
        localValidator.property("schedulingMinute").required().between(0, 59);
        localValidator.property("maxChargebackTime.number").between(0, 999);
        localValidator.property("maxChargebackTime.field").required();
        localValidator.property("chargebackDescription").required();
        localValidator.property("sendSmsWebServiceUrl").key("settings.local.sms.sendSmsWebServiceUrl").length(0, 256).add(new PropertyValidation() {
            private static final long serialVersionUID = 0L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final LocalSettings settigns = (LocalSettings) object;
                if (StringUtils.isNotEmpty(settigns.getSmsChannelName()) && StringUtils.isEmpty((String) value)) {
                    return new RequiredError();
                } else {
                    return null;
                }
            }
        });

        localValidator.property("smsChannelName").add(new PropertyValidation() {
            private static final long serialVersionUID = 0L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                if (StringUtils.isEmpty((String) value)) {
                    return null;
                }
                boolean channelFound = false;
                for (final Channel channel : channelService.listNonBuiltin()) {
                    if (channel.getInternalName().equals(value)) {
                        channelFound = true;
                        break;
                    }
                }
                return channelFound ? null : new InvalidError();
            }
        });

        localValidator.property("smsCustomFieldId").key("settings.local.sms.customField").add(new PropertyValidation() {
            private static final long serialVersionUID = 0L;

            public ValidationError validate(final Object object, final Object property, final Object value) {
                final LocalSettings settigns = (LocalSettings) object;
                final Long id = (Long) value;
                // When sms notifications are not used, the custom field is not validated
                if (!settigns.isSmsNotificationEnabled()) {
                    return null;
                }
                if (id == null || id.intValue() < 0) {
//                    return new RequiredError();
                    return null;
                }
                try {
                    // Load and ensure it's a member custom field
                    final CustomField customField = customFieldService.load(id);
                    if (!(customField instanceof MemberCustomField)) {
                        throw new EntityNotFoundException();
                    }
                    return null;
                } catch (final EntityNotFoundException e) {
                    return new InvalidError();
                }
            }
        });

        return localValidator;
    }

    @Override
    protected LocalSettings read() {
        final LocalSettings localSettings = super.read();

        // Validate the transaction number
        if (localSettings.getTransactionNumber() != null && localSettings.getTransactionNumber().getPadLength() <= 0) {
            localSettings.setTransactionNumber(null);
        }

        return localSettings;
    }

}