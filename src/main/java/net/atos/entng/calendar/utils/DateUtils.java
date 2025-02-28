package net.atos.entng.calendar.utils;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;


public final class DateUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String YEAR = "yyyy";
    public static final String ICAL_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static final String ICAL_ALLDAY_FORMAT = "yyyyMMdd";
    public static final String DATE_MONTH_YEAR = "yyyyMMdd";
    public static final String HOURS_MINUTES = "HH:mm";
    public static final String UTC = "UTC";

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
        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
          cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
          cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
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

    public static long getTimeDifference(String date1, String date2) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        Date d1 = formatter.parse(date1);
        Date d2 = formatter.parse(date2);

        return Math.abs(d2.getTime() - d1.getTime());
    }

    /**
     * converts a date string to another format
     * @param dateString the initial date
     * @param inputFormat the initial format of the date
     * @param outputFormat the format we want
     * @return the date in the output format
     */
    public static String getStringDate(String dateString, String inputFormat, String outputFormat) {

        try {
            SimpleDateFormat inputDate = new SimpleDateFormat(inputFormat);
            inputDate.setTimeZone(TimeZone.getTimeZone(ZoneId.of(UTC)));

            SimpleDateFormat outputDate = new SimpleDateFormat(outputFormat);
            outputDate.setTimeZone(TimeZone.getTimeZone(ZoneId.of(Locale.getDefault().toString())));

            Date date = inputDate.parse(dateString);

            return outputDate.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

}
