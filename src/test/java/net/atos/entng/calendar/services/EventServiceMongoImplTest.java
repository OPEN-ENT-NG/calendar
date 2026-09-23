package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({MongoDb.class}) //Prepare the static class you want to test
public class EventServiceMongoImplTest {

    private Vertx vertx;
    private MongoDb mongo = mock(MongoDb.class);
    private EventServiceMongo eventService;
    private ServiceFactory serviceFactory;

    private static final String CALENDAR_ID = "111";
    private static final String USER_ID = "0eab1f72-f29d-4b7d-b82a-86f7c3efa6ed";
    private static final List<String> GROUP_IDS =
            Arrays.asList("856b15ba-c61d-445e-9c81-8ef42f5dba7b", "70f366e6-048f-4fb3-93c9-a0b65d428924");
    private static final String OWNED_CALENDAR_ID = "f2d55822-0816-47c0-9895-3864ec38bef9";
    private static final String SHARED_CALENDAR_ID = "1ddfec11-01af-45c1-be58-ca923225f0bb";


    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        MongoDb.getInstance().init(vertx.eventBus(), "fr.openent.calendar");
        serviceFactory = new ServiceFactory(vertx, null, null, mongo);
        mongo = Mockito.spy(MongoDb.getInstance());
        PowerMockito.spy(MongoDb.class);
        PowerMockito.when(MongoDb.getInstance()).thenReturn(mongo);
        this.eventService = Mockito.spy(new EventServiceMongoImpl(Calendar.CALENDAR_EVENT_COLLECTION, vertx.eventBus(), serviceFactory));
    }

    @Test
    public void testListCalendarEvent_Should_ONLY_retrieve_calendar_by_ID(TestContext ctx) {
        Async async = ctx.async();
        String calendarId = "f2d55822-0816-47c0-9895-3864ec38bef9";
        String startDate = "2022-05-04";
        String endDate = "2022-05-07";
        UserInfos user = new UserInfos();
        user.setUserId("0eab1f72-f29d-4b7d-b82a-86f7c3efa6ed");
        user.setGroupsIds(new ArrayList<>(
                Arrays.asList("856b15ba-c61d-445e-9c81-8ef42f5dba7b",
                        "70f366e6-048f-4fb3-93c9-a0b65d428924"))
        );

        String expectedCollection = "calendar";
        String expectedMatcher = "{\"_id\":\"f2d55822-0816-47c0-9895-3864ec38bef9\"}";

        vertx.eventBus().consumer("fr.openent.calendar", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals(expectedCollection, body.getString("collection"));
            ctx.assertEquals(expectedMatcher, body.getJsonObject("matcher").toString());
            async.complete();
        });
        this.eventService.list(calendarId, user, startDate, endDate, null);
    }

    @Test
    public void testDeleteDatesAfterComparisonDate(TestContext context) {
        Async async = context.async();

        String date = "2022-05-04";
        //Expected data
        String expectedCollection = Calendar.CALENDAR_EVENT_COLLECTION;
        JsonObject expectedQuery = new JsonObject()
                .put(Field.CALENDAR, CALENDAR_ID)
                .put(Field.STARTMOMENT,
                        new JsonObject()
                                .put("$gt", date)
                );

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(mongo).delete(Mockito.any(), Mockito.any(), Mockito.any());

        eventService.deleteDatesAfterComparisonDate(CALENDAR_ID, date);
        async.await(10000);
    }

    @Test
    public void testRetrieveByCalendarId(TestContext context) {
        Async async = context.async();
        //Expected data
        String expectedCollection = Calendar.CALENDAR_EVENT_COLLECTION;
        JsonObject expectedQuery = new JsonObject().put(Field.CALENDAR, CALENDAR_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(mongo).find(Mockito.any(), Mockito.any(), Mockito.any());

        eventService.retrieveByCalendarId(CALENDAR_ID);
        async.await(10000);
    }

    @Test
    public void testDeleteByCalendarId(TestContext context) {
        Async async = context.async();
        final String CALENDAR_ID = "111";

        String expectedCollection = Calendar.CALENDAR_EVENT_COLLECTION;
        JsonObject expectedQuery = new JsonObject().put(Field.CALENDAR, CALENDAR_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            async.complete();
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(), Mockito.any());

        eventService.deleteByCalendarId(CALENDAR_ID);
    }

    @Test
    public void testListUpcomingEvents_Should_fetch_the_owners_of_the_given_calendars(TestContext ctx) {
        Async async = ctx.async();

        Mockito.doAnswer(invocation -> {
            ctx.assertEquals(Calendar.CALENDAR_COLLECTION, (String) invocation.getArgument(0));
            ctx.assertEquals(new JsonObject().put(Field._ID, new JsonObject()
                    .put("$in", new JsonArray().add(OWNED_CALENDAR_ID).add(SHARED_CALENDAR_ID))),
                    invocation.getArgument(1));
            ctx.assertEquals(new JsonObject().put("owner.userId", 1), invocation.getArgument(3));
            async.complete();
            return null;
        }).when(mongo).find(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(), Mockito.any(),
                Mockito.any(Handler.class));

        eventService.list(Arrays.asList(OWNED_CALENDAR_ID, SHARED_CALENDAR_ID), user(), 3, null);
        async.await(10000);
    }

    @Test
    public void testListUpcomingEvents_Should_filter_the_events_of_a_calendar_the_user_does_not_own(TestContext ctx) {
        Async async = ctx.async();
        givenCalendars(new JsonArray()
                .add(calendar(OWNED_CALENDAR_ID, USER_ID))
                .add(calendar(SHARED_CALENDAR_ID, "9b99e69b-96a5-4b0b-8b31-4ff0a2a1e8c4")));

        JsonArray expectedSharingConditions = new JsonArray()
                .add(new JsonObject().put("owner.userId", USER_ID))
                .add(new JsonObject().put(Field.shared, new JsonObject().put("$exists", false)))
                .add(new JsonObject().put(Field.shared, new JsonObject().put("$size", 0)))
                .add(new JsonObject().put("shared.userId", new JsonObject().put("$in",
                        new JsonArray().add(USER_ID))))
                .add(new JsonObject().put("shared.groupId", new JsonObject().put("$in", GROUP_IDS)));

        JsonArray expectedCalendarConditions = new JsonArray()
                .add(new JsonObject().put(Field.CALENDAR, new JsonObject().put("$in",
                        new JsonArray().add(OWNED_CALENDAR_ID))))
                .add(new JsonObject().put(MongoField.$AND, new JsonArray()
                        .add(new JsonObject().put(Field.CALENDAR, new JsonObject().put("$in",
                                new JsonArray().add(SHARED_CALENDAR_ID))))
                        .add(new JsonObject().put(MongoField.$OR, expectedSharingConditions))));

        thenEventsQueryIs(ctx, async, expectedCalendarConditions, 3);

        eventService.list(Arrays.asList(OWNED_CALENDAR_ID, SHARED_CALENDAR_ID), user(), 3, null);
        async.await(10000);
    }

    @Test
    public void testListUpcomingEvents_Should_NOT_filter_the_events_of_a_calendar_the_user_owns(TestContext ctx) {
        Async async = ctx.async();
        givenCalendars(new JsonArray().add(calendar(OWNED_CALENDAR_ID, USER_ID)));

        JsonArray expectedCalendarConditions = new JsonArray()
                .add(new JsonObject().put(Field.CALENDAR, new JsonObject().put("$in",
                        new JsonArray().add(OWNED_CALENDAR_ID))));

        thenEventsQueryIs(ctx, async, expectedCalendarConditions, 10);

        eventService.list(Collections.singletonList(OWNED_CALENDAR_ID), user(), 10, null);
        async.await(10000);
    }

    /**
     * Makes the calendars lookup answer with the given calendars, so that the events query is built
     */
    private void givenCalendars(JsonArray calendars) {
        Message<JsonObject> reply = mock(Message.class);
        Mockito.when(reply.body()).thenReturn(new JsonObject().put("status", "ok").put("results", calendars));

        Mockito.doAnswer(invocation -> {
            ((Handler<Message<JsonObject>>) invocation.getArgument(4)).handle(reply);
            return null;
        }).when(mongo).find(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(), Mockito.any(),
                Mockito.any(Handler.class));
    }

    /**
     * Checks the events query : only the events to come, the expected calendars conditions, sorted by start moment
     * and limited. The date the events are compared to must be UTC, whatever the timezone of the JVM.
     */
    private void thenEventsQueryIs(TestContext ctx, Async async, JsonArray expectedCalendarConditions, int expectedLimit) {
        Mockito.doAnswer(invocation -> {
            ctx.assertEquals(Calendar.CALENDAR_EVENT_COLLECTION, (String) invocation.getArgument(0));
            ctx.assertEquals(new JsonObject().put(Field.STARTMOMENT, 1), invocation.getArgument(2));
            ctx.assertEquals(expectedLimit, (int) invocation.getArgument(5));

            JsonObject query = invocation.getArgument(1);
            String now = query.getJsonArray(MongoField.$AND).getJsonObject(0)
                    .getJsonObject(Field.ENDMOMENT).getString(MongoField.$GREATER_OR_EQUAL);
            ctx.assertTrue(now.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"),
                    "The date the events are compared to must be UTC, got " + now);

            ctx.assertEquals(new JsonObject().put(MongoField.$AND, new JsonArray()
                    .add(new JsonObject().put(Field.ENDMOMENT,
                            new JsonObject().put(MongoField.$GREATER_OR_EQUAL, now)))
                    .add(new JsonObject().put(MongoField.$OR, expectedCalendarConditions))), query);

            async.complete();
            return null;
        }).when(mongo).find(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(), Mockito.any(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Handler.class));
    }

    private JsonObject calendar(String calendarId, String ownerId) {
        return new JsonObject()
                .put(Field._ID, calendarId)
                .put(Field.OWNER, new JsonObject().put(Field.USERID, ownerId));
    }

    private UserInfos user() {
        UserInfos user = new UserInfos();
        user.setUserId(USER_ID);
        user.setGroupsIds(new ArrayList<>(GROUP_IDS));
        return user;
    }
}