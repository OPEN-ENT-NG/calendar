package net.atos.entng.calendar.utils;

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



}
