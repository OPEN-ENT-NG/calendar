package net.atos.entng.calendar.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.PlatformHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PlatformHelperTest {

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
}
