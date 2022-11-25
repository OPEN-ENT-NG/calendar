package net.atos.entng.calendar.event;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareEverythingForTest;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({I18n.class, CalendarSearchingEvents.class})
public class CalendarSearchingEventsTest {

    I18n i18n = mock(I18n.class);

    @Before
    public void setUp(TestContext context) {
        Whitebox.setInternalState(CalendarSearchingEvents.class, "i18n", i18n);
    }

    @Test
    public void testFormatSearchResult_shouldReturnDefaultObjectArray (TestContext ctx) throws Exception {
        JsonObject resultEvent = new JsonObject()
                .put(Field._ID, "333")
                .put(Field.TITLE, "test")
                .put(Field.STARTMOMENT, "2023-01-01T10:00:00.000Z")
                .put(Field.ENDMOMENT, "2023-01-01T11:00:00.000Z")
                .put(Field.CALENDAR, new JsonArray().add("000"))
                .put(Field.OWNER, new JsonObject().put(Field.USERID, "111").put(Field.DISPLAYNAME, "NOM Prenom"))
                .put(Field.MODIFIED, new JsonObject().put("$date",
                        new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.sss'Z'").parse( "2022-01-01T10:00:00.000Z").getTime()));
        JsonArray results = new JsonArray().add(resultEvent);

        JsonArray columnsHeader = new JsonArray().add(Field.TITLE).add(Field.DESCRIPTION).add(Field.MODIFIED)
                .add("ownerDisplayName").add("ownerId").add(Field.URL);

        HashMap<String,String> mapIdTitle = new HashMap<String,String>();
        mapIdTitle.put("000","Agenda");

        String locale = "fr";

        String expectedMessage = "Événement 'test' du 01/01/2023 à 10:00 au 01/01/2023 à 11:00";

        JsonObject expectedObject = new JsonObject()
                .put(Field.TITLE, "Agenda")
                .put(Field.DESCRIPTION, "Événement 'test' du 01/01/2023 à 10:00 au 01/01/2023 à 11:00")
                .put(Field.MODIFIED, new JsonObject().put("$date",
                        new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.sss'Z'").parse( "2022-01-01T10:00:00.000Z").getTime()))
                .put("ownerDisplayName", "NOM Prenom")
                .put("ownerId", "111")
                .put(Field.URL, "/calendar#/view/000");
        JsonArray expectedArray = new JsonArray().add(expectedObject);

        Mockito.doReturn("à").when(i18n).translate(Mockito.eq("calendar.search.date.to"),
                Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(expectedMessage).when(i18n).translate(Mockito.eq("calendar.search.description.min"),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        JsonArray resultArray = Whitebox.invokeMethod(new CalendarSearchingEvents(), "formatSearchResult",
                results, columnsHeader, mapIdTitle, locale);
        ctx.assertEquals(expectedArray.toString(), resultArray.toString());
    }



    @Test
    public void testFormatDescription_shouldReturnDefaultMessage(TestContext ctx) throws Exception {
        String locale = "fr";
        String title = "test";
        String description = "";
        String location = "";
        String startDate = "2023-01-01T10:00:00.000Z";
        String endDate = "2023-01-01T11:00:00.000Z";

        String expectedMessage = "Événement 'test' du 01/01/2023 à 10:00 au 01/01/2023 à 11:00";

        Mockito.doReturn("à").when(i18n).translate(Mockito.eq("calendar.search.date.to"),
                Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(expectedMessage).when(i18n).translate(Mockito.eq("calendar.search.description.min"),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        String resultMessage = Whitebox.invokeMethod(new CalendarSearchingEvents(), "formatDescription",
                locale, title, description, location, startDate, endDate);
        ctx.assertEquals(expectedMessage, resultMessage);
    }
}
