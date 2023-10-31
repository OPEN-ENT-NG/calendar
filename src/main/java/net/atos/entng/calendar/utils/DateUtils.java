package net.atos.entng.calendar.utils;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public final class DateUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String FRENCH_DATE_FORMAT ="dd-MM-yyyy";
    public static final String YEAR = "yyyy";
    public static final String DAY_MONTH_YEAR_HOUR_TIME = "dd/MM/yyyy HH:mm:ss";
    public static final String HOUR_MINUTE_SECOND = "HH:mm:ss";
    public static final String HOUR_MINUTE = "HH:mm";
    public static final String ICAL_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static final String ICAL_ALLDAY_FORMAT = "yyyyMMdd";

    private DateUtils()  {}

    public static Date parseDate(String dateToParse, String format) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(dateToParse);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Boolean isStrictlyBefore(Date d1, Date d2) {
        if ((d1 == null) || (d2 == null)) {
            return false;
        }
        return (d1.before(d2));
    }

    public static Boolean isStrictlyAfter(Date d1, Date d2) {
        if ((d1 == null) || (d2 == null)) {
            return false;
        }
        return (d1.after(d2));
    }

    public static Boolean isSameDay(Date d1, Date d2) {
        if ((d1 == null) || (d2 == null)) {
            return false;
        }
        final Date d1Arr = untimed(d1);
        final Date d2Arr = untimed(d2);
        return (d1Arr.equals(d2Arr));
    }

    private static Date untimed(Date date) {
        final Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);

        return cal.getTime();
    }

    public static String dateToString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_UTC);
        String dateString = dateFormat.format(date);

        return dateString;
    }

    public static Date getRefEndDate(Date startDate) {
        final Calendar cal = new GregorianCalendar();
        cal.setTime(startDate);
        cal.add(Calendar.YEAR, Field.REFENDDATE);
        return cal.getTime();
    }

    /**
     *  Calculates the end date of a periodic event based on the start date, recurrence type, range, and interval
     *  This date shows the real end date of the event if it would be saved
     *  So we return this calculated date and compare it to the max end date we fixed which must be under 80 years after start date
     *  Exemple : Calculation of the periodic end date based on an interval of 9 weeks and with 365 recurrences.
     *  Number of years = 63 added to the start date
     *  This means that with a maximum interval of 9 weeks and 365 maximum recurrences, you can reach a maximum of 63 years.
     */
    public static Date getPeriodicEndDate(Date startDate, JsonObject object) {
        int range = object.getJsonObject(Field.recurrence, new JsonObject()).getInteger(Field.end_after, 0);
        int every = object.getJsonObject(Field.recurrence, new JsonObject()).getInteger(Field.every, 0);
        if(range == 0 || every == 0) return null;
        final Calendar cal = new GregorianCalendar();
        String type = object.getJsonObject(Field.recurrence, new JsonObject()).getString(Field.type, null);
        cal.setTime(startDate);
        if(Field.every_day.equals(type)){
            cal.add(Calendar.DAY_OF_YEAR, range * every);
        } else if (Field.every_week.equals(type)) {
            cal.add(Calendar.WEEK_OF_YEAR, range * every);
        }
        return cal.getTime();
    }



}
