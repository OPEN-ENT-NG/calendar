package net.atos.entng.calendar.helper;

import com.redis.S;
import fr.wseduc.mongodb.MongoDb;
import io.advantageous.boon.core.Str;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.PlatformHelper;
import net.atos.entng.calendar.helpers.UserHelper;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.ServiceFactory;
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
@PrepareForTest({PlatformHelper.class, UserHelper.class})
public class PlatformHelperTest {

    ServiceFactory serviceFactory = mock(ServiceFactory.class);

    private static final String PLATFORM_TITLE = "title";
    private static final String PLATFORM_REGEX_MOODLE = "^https?:\\/\\/.*moodle.*\\/calendar\\/.*";
    private static final String PLATFORM_REGEX_GOOGLE = "^https:\\/\\/calendar\\.google\\.com\\/calendar\\/ical\\/.*\\.ics";
    private static final JsonArray PLATFORM_LIST = new JsonArray()
            .add(new JsonObject()
                    .put(Field.TITLE, PLATFORM_TITLE)
                    .put(Field.REGEX, PLATFORM_REGEX_GOOGLE)
            )
            .add(new JsonObject()
                    .put(Field.TITLE, PLATFORM_TITLE)
                    .put(Field.REGEX, PLATFORM_REGEX_MOODLE)
            );

    private static final String USER_ID = "000";
    private PlatformHelper platformHelper;
    private JsonObject config = new JsonObject().put(Field.ENABLE_ZIMBRA, true);

    private final CalendarService calendarService = Mockito.spy(CalendarService.class);

    @Before
    public void setUp(TestContext context) {
        Mockito.doReturn(config).when(this.serviceFactory).getConfig();
        Mockito.doReturn(calendarService).when(this.serviceFactory).calendarService();
        PowerMockito.spy(PlatformHelper.class);
        this.platformHelper = Mockito.spy(new PlatformHelper(serviceFactory));

        PowerMockito.spy(UserHelper.class);
    }

    @Test
    public void testCheckUrlInRegexWithPlatformUrl(TestContext context){
        String url = "https://calendar.google.com/calendar/ical/erejrw4732h234238hwfd%40group.calendar.google.com/public/basic.ics";

        context.assertTrue(PlatformHelper.checkUrlInRegex(url, PLATFORM_LIST));
    }

    @Test
    public void testCheckUrlInRegexWithUntrustedPlatformUrl(TestContext context){
        String url = "https://www.example.net/test";

        context.assertFalse(PlatformHelper.checkUrlInRegex(url, PLATFORM_LIST));
    }

    @Test
    public void testCheckUrlInRegexWithErroneousdPlatformUrl(TestContext context){
        String url = "df;lgjsdlfjlgh;sdfjsoaidfaskjbfpoasdjucoasdfoiu[PUU$DKFADC[OEDNjskdgjgfspdfs0f]fe[].,,.<>,d.s";

        context.assertFalse(PlatformHelper.checkUrlInRegex(url, PLATFORM_LIST));
    }

    @Test
    public void testCheckUrlInRegexWithEmptyPlatformUrl(TestContext context){
        String url = "";

        context.assertFalse(PlatformHelper.checkUrlInRegex(url, PLATFORM_LIST));
    }

    @Test
    public void testCheckCalendarPlatform_platformCase(TestContext context) throws Exception {
        Async async = context.async();

        //Arguments
        UserInfos user = new UserInfos();
        user.setUserId(USER_ID);
        JsonObject calendar = new JsonObject().put(Field.PLATFORM, Field.ZIMBRA);

        Mockito.doAnswer(invocation -> {
            String userArgument = invocation.getArgument(0).toString();
            String platformArgument = invocation.getArgument(1);
            context.assertEquals(userArgument, user.toString());
            context.assertEquals(platformArgument, Field.ZIMBRA);
            return Future.succeededFuture(new JsonObject());
        }).when(calendarService).getPlatformCalendar(Mockito.any(), Mockito.anyString());

        PowerMockito.doReturn(true).when(UserHelper.class, "userHasApp", Mockito.any(), Mockito.anyString());

        platformHelper.checkCalendarPlatform(user, calendar)
                .onSuccess(result -> async.complete());
    }

}
