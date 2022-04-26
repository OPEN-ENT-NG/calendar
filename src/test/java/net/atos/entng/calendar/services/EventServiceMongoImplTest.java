package net.atos.entng.calendar.services;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class EventServiceMongoImplTest {

    private Vertx vertx;
    private final MongoDb mongo = mock(MongoDb.class);
    private EventServiceMongo eventService;
    private ServiceFactory serviceFactory;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        MongoDb.getInstance().init(vertx.eventBus(), "fr.openent.calendar");
        serviceFactory = new ServiceFactory(vertx, null, null, mongo);
        this.eventService = new EventServiceMongoImpl(Calendar.CALENDAR_COLLECTION, vertx.eventBus(), serviceFactory);
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

}