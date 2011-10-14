package nl.strohalm.cyclos.utils.validation;

import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.SettingsHelper;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Validate DateOfBirth for members older than 13 jears old
 * User: Kryzoo
 * Date: 14.10.11
 * Time: 12:53
 */
public class DateOfBirthValidation implements PropertyValidation {
    private static final long serialVersionUID = -9030397272125246574L;
    public static final int MIN_AGE_IN_YEARS = 13;

    SettingsService settingsService;


    public DateOfBirthValidation() {
    }

    @Override
    public ValidationError validate(Object object, Object property, Object value) {
//        BeanFactoryLocator bfl = SingletonBeanFactoryLocator.getInstance();
//        BeanFactoryReference bf = bfl.useBeanFactory("mainContext");
//        nl.strohalm.cyclos.services.settings.SettingsServiceImpl settingsService = (nl.strohalm.cyclos.services.settings.SettingsServiceImpl) bf.getFactory().getBean("settingsService");
//
//        String pattern = settingsService.getLocalSettings().getDatePattern().getPattern();
        String pattern = "dd/MM/yyyy";
        Date dob = null;
        try {
            dob = parseDateOfBirth(value, pattern);
        } catch (Exception e) {
            return new InvalidError();
        }

        Date now = new Date();

        if (!isTimeDifferenceMore(dob, now, MIN_AGE_IN_YEARS))
            return new ValidationError("Age should be more than " + MIN_AGE_IN_YEARS + "years.");

        return null;
    }

    protected boolean isTimeDifferenceMore(Date from, Date to, int years) {
        if (DateUtils.addYears(from, years).getTime() > to.getTime()){
            return false;
        }
        return true;
    }

    protected Date parseDateOfBirth(Object value, String pattern) throws Exception{
        if(!(value instanceof String)){
            throw new Exception("value NOT instanceof String");
        }
        String stringDate = (String)value;
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Date date = null;
        try {
            date = format.parse(stringDate);
        } catch (ParseException e) {
            throw new Exception("parse exception", e);
        }
        return date;
    }
}
