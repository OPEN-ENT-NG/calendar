package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
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
}