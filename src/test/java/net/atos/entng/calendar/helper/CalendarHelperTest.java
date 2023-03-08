package net.atos.entng.calendar.helper;

import com.redis.M;
import com.redis.S;
import com.redis.U;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.eventbus.ResultMessage;
import io.advantageous.boon.core.Str;
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
import net.atos.entng.calendar.core.constants.Field;
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
    private CalendarModel calendarModel;

    @Before
    public void setUp(TestContext context) throws Exception {
        PowerMockito.spy(EventServiceMongoImpl.class);
        PowerMockito.spy(CalendarHelper.class);
        PowerMockito.spy(CalendarModel.class);
        this.eventServiceMongoImpl = mock(EventServiceMongoImpl.class);
        PowerMockito.whenNew(EventServiceMongoImpl.class).withArguments(Mockito.anyString(), Mockito.any(), Mockito.any()).thenReturn(this.eventServiceMongoImpl);
        this.calendarHelper = PowerMockito.spy(new CalendarHelper(Calendar.CALENDAR_COLLECTION, serviceFactory, eventBus, new JsonObject()));
        this.calendarModel = mock(CalendarModel.class);
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

        Mockito.doAnswer(invocation -> {
            String address = invocation.getArgument(0);
            context.assertEquals(address, "fr.openent.zimbra");
            async.complete();

            return null;
        }).when(eventBus).request(Mockito.any(), Mockito.any(), Mockito.any());

        Whitebox.invokeMethod(calendarHelper, "getICalFromExternalPlatform", user, ZIMBRA,
                new CalendarModel(calendar), host, i18nLang);
    }

    @Test
    public void testGetAndSaveExternalCalendarEvents_platformCase(TestContext context) throws Exception {
        Async async = context.async();

        //Arguments
        UserInfos user = new UserInfos();
        JsonObject calendar = new JsonObject().put(Field.PLATFORM, Field.ZIMBRA);
        String host = "";
        String i18nLang = "";
        String action = "";

        //Expected arguments
        String expectedPlatform = Field.ZIMBRA;

        PowerMockito.doAnswer(event -> {
            String platform = event.getArgument(1);
            context.assertEquals(platform, expectedPlatform);
            async.complete();

            return Future.succeededFuture();
        }).when(calendarHelper, "getICalFromExternalPlatform", Mockito.any(),
                Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString());

        PowerMockito.whenNew(CalendarModel.class).withAnyArguments().thenReturn(calendarModel);

        Whitebox.invokeMethod(calendarHelper, "getAndSaveExternalCalendarEvents", user, calendar,
                host, i18nLang, action);
        async.await(10000);

    }

    @Test
    public void testGetAndSaveExternalCalendarEvents_urlCase(TestContext context) throws Exception {
        Async async = context.async();

        //Arguments
        UserInfos user = new UserInfos();
        JsonObject calendar = new JsonObject().put(Field.ICSLINK, "aaa");
        String host = "";
        String i18nLang = "";
        String action = "";

        //Expected arguments
        JsonObject expectedCalendar = new JsonObject().put(Field.ICSLINK, "aaa");

        PowerMockito.doAnswer(event -> {
            String argumentCalendar = event.getArgument(1).toString();
            context.assertEquals(argumentCalendar, expectedCalendar.toString());
            async.complete();

            return Future.succeededFuture();
        }).when(calendarHelper, "callLinkImportEventBus", Mockito.any(),
                Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        Whitebox.invokeMethod(calendarHelper, "getAndSaveExternalCalendarEvents", user, calendar,
                host, i18nLang, action);
        async.await(10000);

    }

}
