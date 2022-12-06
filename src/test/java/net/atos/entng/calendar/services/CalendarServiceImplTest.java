package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
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
    public void testHasDefaultCalendarIfHasNoDefaultCalendar(TestContext context) {
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
    public void testCreateDefaultCalendarIfDoesNotExist(TestContext context) {
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
    public void testIsDefaultCalendarIfIsNotDefaultCalendar(TestContext context) {

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

    @Test
    public void testUpdateCalendar(TestContext context) {
        Async async = context.async();

        JsonObject now = MongoDb.now();


        JsonObject updateCalendar = new JsonObject()
                .put("title", "Mon agenda")
                .put("color", "grey")
                .put("created", now)
                .put("owner", new JsonObject()
                        .put("userId", USER_ID)
                        .put("displayName", (String) null)) //(String) null != null
                .put("is_default", false);


        //Expected data
        String expectedCollection = "calendar";
        JsonObject expectedCalendar = new JsonObject()
                .put("title", "Mon agenda")
                .put("color", "grey")
                .put("created", now)
                .put("owner", new JsonObject()
                        .put("userId", USER_ID)
                        .put("displayName", (String) null)) //(String) null != null
                .put("is_default", false);
        JsonObject expectedQuery = new JsonObject().put(Field._ID, CALENDAR_ID);


        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            JsonObject updatedCalendar = ((JsonObject) invocation.getArgument(2)).getJsonObject("$set");
            updatedCalendar.remove("modified");
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            context.assertEquals(updatedCalendar, expectedCalendar);
            async.complete();
            return null;
        }).when(mongo).update(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        calendarService.update(CALENDAR_ID, updateCalendar);
        async.await(10000);
    }

    @Test
    public void testDeleteCalendar(TestContext context) {
        Async async = context.async();

        String expectedCollection = Calendar.CALENDAR_COLLECTION;
        JsonObject expectedQuery = new JsonObject().put("_id", CALENDAR_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(), Mockito.any());

        calendarService.delete(CALENDAR_ID);
    }
}