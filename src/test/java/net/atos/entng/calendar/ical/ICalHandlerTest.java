package net.atos.entng.calendar.ical;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.exception.CalendarException;
import net.atos.entng.calendar.exception.UnhandledEventException;
import net.atos.entng.calendar.utils.DateUtils;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Uid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(VertxUnitRunner.class)
public class ICalHandlerTest {



    @Test
    public void formatIcsDateTest(TestContext ctx) throws Exception {
        String icsDateString = "20220125T105800Z";
        String expectedDateString = "2022-01-25T10:58:00.000Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.ICAL_DATE_FORMAT);
        simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        Date icsDate = simpleDateFormat.parse(icsDateString);

        String resultString = Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
        ctx.assertEquals(expectedDateString, resultString);
    }

    @Test
    public void formatIcsAlldayDateTest(TestContext ctx) throws Exception {
        String icsDateString = "20220125";
        String expectedDateString = "2022-01-25T00:00:00.000Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.ICAL_ALLDAY_FORMAT);
        simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        Date icsDate = simpleDateFormat.parse(icsDateString);

        String resultString = Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
        ctx.assertEquals(expectedDateString, resultString);
    }

    @Test
    public void formatIcsTimezoneDateTest(TestContext ctx) throws Exception {
        String icsDateString = "20220125T125800";
        String expectedDateString = "2022-01-25T10:58:00.000Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        simpleDateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Kaliningrad"));
        Date icsDate = simpleDateFormat.parse(icsDateString);

        String resultString = Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
        ctx.assertEquals(expectedDateString, resultString);
    }

    @Test
    public void formatAllDayImportDateTest(TestContext ctx) throws Exception {
        String expectedDateString = "2022-01-25T05:00:00.000Z";
        net.fortuna.ical4j.model.Date startDate = new net.fortuna.ical4j.model.Date("20220125");
        VEvent event = new VEvent(startDate, "title");
        JsonObject jsonEvent = new JsonObject();


        Whitebox.invokeMethod(new ICalHandler(), "setEventDates", event, jsonEvent, new JsonObject());
        ctx.assertEquals(expectedDateString, jsonEvent.getString("startMoment", null));
    }

    @Test
    public void addAllDayEventTest(TestContext ctx) throws Exception {
        Calendar calendar = new Calendar();
        Date dateStart = new SimpleDateFormat(DateUtils.DATE_FORMAT).parse("2022-01-25T05:00:00.000Z");
        Date dateEnd = new SimpleDateFormat(DateUtils.DATE_FORMAT).parse("2022-01-25T18:00:00.000Z");
        net.fortuna.ical4j.model.Date start = new net.fortuna.ical4j.model.Date(dateStart.getTime());
        net.fortuna.ical4j.model.Date end = new net.fortuna.ical4j.model.Date(dateEnd.getTime());
        String title = "title";
        String icsId = "000";

        VEvent expectedEvent = new VEvent(start, end, "title");
        Uid uid = new Uid();
        uid.setValue(icsId);
        expectedEvent.getProperties().add(uid);

        Whitebox.invokeMethod(new ICalHandler(), "addAllDayEvent", calendar, start, end, title, icsId, null, null);
        ctx.assertEquals(expectedEvent, calendar.getComponents().get(0));
    }

}