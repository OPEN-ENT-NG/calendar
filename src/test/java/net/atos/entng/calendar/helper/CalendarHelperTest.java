package net.atos.entng.calendar.helper;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.eventbus.ResultMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.event.CalendarSearchingEvents;
import net.atos.entng.calendar.helpers.CalendarHelper;
import net.atos.entng.calendar.models.CalendarModel;
import net.atos.entng.calendar.services.ServiceFactory;
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
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({EventServiceMongoImpl.class, CalendarHelper.class})
public class CalendarHelperTest {
    ServiceFactory serviceFactory = mock(ServiceFactory.class);
    EventBus eventBus = mock(EventBus.class);

    private static final String USER_ID = "000";
    private static final String CALENDAR_ID = "111";
    private static final String ZIMBRA = "Zimbra";

    private EventServiceMongoImpl eventServiceMongoImpl;
    private CalendarHelper calendarHelper;

    @Before
    public void setUp(TestContext context) throws Exception {
        PowerMockito.spy(EventServiceMongoImpl.class);
        PowerMockito.spy(CalendarHelper.class);
        this.eventServiceMongoImpl = mock(EventServiceMongoImpl.class);
        PowerMockito.whenNew(EventServiceMongoImpl.class).withArguments(Mockito.anyString(), Mockito.any(), Mockito.any()).thenReturn(this.eventServiceMongoImpl);
        this.calendarHelper = Mockito.spy(new CalendarHelper(Calendar.CALENDAR_COLLECTION, serviceFactory, eventBus, new JsonObject()));
    }

    @Test
    public void testGetICalFromExternalPlatform_normalUse(TestContext context) throws Exception {
        Async async = context.async();

        // Arguments
        UserInfos user = new UserInfos();
        user.setOtherProperty("owner", new JsonObject());
        user.setUserId(USER_ID);

        JsonObject calendar = new JsonObject()
                .put("_id", CALENDAR_ID)
                .put("updated", new JsonObject().put("$date", 0L));
        String host = "";
        String i18nLang = "";


        // Expected data
        JsonObject expectedEvent = new JsonObject()
                .put("owner.userId", USER_ID)
                .put("is_default", true);

        Mockito.doAnswer(invocation -> {
            String address = invocation.getArgument(0);
            context.assertEquals(address, "fr.openent.zimbra");
            Handler<AsyncResult<Message<JsonObject>>> replyHandler = invocation.getArgument(2);
            replyHandler.handle(Future.succeededFuture(new ResultMessage(new JsonObject().put("result", new JsonObject().put("ics", "icsResult")))));

            return null;
        }).when(eventBus).request(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer(event -> {
            String ical = event.getArgument(1);
            context.assertEquals(ical, "icsResult");
            async.complete();

            return Future.succeededFuture();
        }).when(eventServiceMongoImpl).importIcal(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.anyString(), Mockito.anyString(), Mockito.any());

        Whitebox.invokeMethod(calendarHelper, "getICalFromExternalPlatform", user, ZIMBRA,
                new CalendarModel(calendar), host, i18nLang);
        async.await(10000);
    }

}
