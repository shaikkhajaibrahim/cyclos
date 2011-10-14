package nl.strohalm.cyclos.utils.validation;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: Kryzoo
 * Date: 14.10.11
 * Time: 16:15
 */
public class DateOfBirthValidationTest{

    public static final String YYYY_MM_DD = "dd/MM/yyyy";

    @Test
    public void parseDateOfBirthTest() throws Exception {
        DateOfBirthValidation dobValidation= new DateOfBirthValidation();

        Date date = dobValidation.parseDateOfBirth("1/1/2010", YYYY_MM_DD);
        SimpleDateFormat formatter = new SimpleDateFormat(YYYY_MM_DD);
        Date newDate = new Date();
        newDate = DateUtils.setYears(newDate, 2010);
        newDate = DateUtils.setMonths(newDate, 1-1);
        newDate = DateUtils.setDays(newDate, 1);
        newDate = DateUtils.setHours(newDate, 0);
        newDate = DateUtils.setMinutes(newDate, 0);
        newDate = DateUtils.setSeconds(newDate, 0);
        newDate = DateUtils.setMilliseconds(newDate, 0);
        assertEquals(newDate, date);
    }

    @Test
    public void isTimeDifferenceMoreTestSameMoment(){
        DateOfBirthValidation dobValidation= new DateOfBirthValidation();

        assertFalse(dobValidation.isTimeDifferenceMore(new Date(), new Date(), 1));
    }

    @Test
    public void isTimeDifferenceMoreTestEqual(){
        DateOfBirthValidation dobValidation= new DateOfBirthValidation();

        assertTrue(dobValidation.isTimeDifferenceMore(new Date(), DateUtils.addYears(new Date(), 1), 1));
    }

    @Test
    public void isTimeDifferenceMoreTestLess(){
        DateOfBirthValidation dobValidation= new DateOfBirthValidation();
        Date endDate = DateUtils.addDays(new Date(), 364);
        Date nowDate = new Date();
        //System.out.println("now: " + nowDate + "endDate: " + endDate + "diff: " + (endDate.getTime()-nowDate.getTime()));
        assertFalse(dobValidation.isTimeDifferenceMore(nowDate, endDate, 1));
    }

    @Test
    public void isTimeDifferenceMoreTestMore(){
        DateOfBirthValidation dobValidation= new DateOfBirthValidation();
        Date endDate = DateUtils.addDays(new Date(), 367);

        assertTrue(dobValidation.isTimeDifferenceMore(new Date(), endDate, 1));
    }


}
