package net.atos.entng.calendar.services;

import io.vertx.core.Promise;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class CalendarServiceImplTest {

    MongoDb mongo = mock(MongoDb.class);
    private CalendarServiceImpl calendarService;

    private static final String USER_ID = "000";
    private static final String CALENDAR_ID = "111";

    @Before
    public void setUp(TestContext context) {

        this.calendarService = new CalendarServiceImpl(Calendar.CALENDAR_COLLECTION, mongo);
    }

    @Test
    public void testHasDefaultCalendarIfHasNoDefaultCalendar(TestContext context){
        // Arguments
        UserInfos user = new UserInfos();
        user.setOtherProperty("owner", new JsonObject());
        user.setUserId(USER_ID);

        // Expected data
        String expectedCollection = "calendar";
        JsonObject expectedQuery = new JsonObject()
                .put("owner.userId", USER_ID)
                .put("is_default", true);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            return null;
        }).when(mongo).findOne(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        calendarService.getDefaultCalendar(user);
    }

    @Test
    public void testCreateDefaultCalendarIfDoesNotExist(TestContext context){
        JsonObject now = MongoDb.now();

        // Arguments
        Promise<JsonObject> promise = Promise.promise();

        UserInfos user = new UserInfos();
        user.setOtherProperty("owner", new JsonObject());
        user.setUserId(USER_ID);

        JsonObject defaultCalendar = new JsonObject();
        defaultCalendar.put("title", "Mon agenda");
        defaultCalendar.put("color", "grey");
        defaultCalendar.put("created", now);
        defaultCalendar.put("modified", now);
        defaultCalendar.put("owner", new JsonObject().put("userId", "000").put("displayName", (String) null));
        defaultCalendar.put("is_default", true);

        // Expected data
        String expectedCollection = "calendar";
        JsonObject expectedCalendar = new JsonObject()
                .put("title", "Mon agenda")
                .put("color", "grey")
                .put("created", now) //Date not @ same time
                .put("modified", now)
                .put("owner", new JsonObject()
                        .put("userId", USER_ID)
                        .put("displayName", (String) null)) //(String) null != null
                .put("is_default", true);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject newCalendar = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(newCalendar, expectedCalendar);
            return null;
        }).when(mongo).insert(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        try {
            Whitebox.invokeMethod(calendarService, "insertDefaultCalendar", promise, defaultCalendar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIsDefaultCalendarIfIsNotDefaultCalendar(TestContext context){

        // Expected data
        String expectedCollection = "calendar";
        JsonObject expectedQuery = new JsonObject()
                .put("_id", CALENDAR_ID)
                .put("is_default", true);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            return null;
        }).when(mongo).findOne(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        calendarService.isDefaultCalendar(CALENDAR_ID);
    }

}