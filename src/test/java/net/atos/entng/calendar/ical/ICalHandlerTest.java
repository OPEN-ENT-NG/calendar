package net.atos.entng.calendar.ical;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.exception.UnhandledEventException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import java.util.Date;

@RunWith(VertxUnitRunner.class)
public class ICalHandlerTest {

    @Test
    public void formatIcsDateTest(TestContext ctx) throws Exception {
        String icsDateString = "20220125T105800Z";
        String expectedDateString = "2022-01-25T10:58:00.000Z";
        Date icsDate = Mockito.mock(Date.class);
        Mockito.doReturn(icsDateString).when(icsDate).toString();

        String resultString = Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
        ctx.assertEquals(expectedDateString, resultString);
    }

    @Test
    public void formatIcsAlldayDateTest(TestContext ctx) throws Exception {
        String icsDateString = "20220125";
        String expectedDateString = "2022-01-25T00:00:00.000Z";
        Date icsDate = Mockito.mock(Date.class);
        Mockito.doReturn(icsDateString).when(icsDate).toString();

        String resultString = Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
        ctx.assertEquals(expectedDateString, resultString);
    }

    @Test
    public void formatIcsBadDateTest(TestContext ctx) throws Exception {
        Async async = ctx.async(1);
        String icsDateString = "abcdefghijklm";
        Date icsDate = Mockito.mock(Date.class);
        Mockito.doReturn(icsDateString).when(icsDate).toString();

        try {
            Whitebox.invokeMethod(ICalHandler.class, "formatIcsDate", icsDate);
            ctx.fail();
        } catch (UnhandledEventException e) {
            async.countDown();
        }
    }

}