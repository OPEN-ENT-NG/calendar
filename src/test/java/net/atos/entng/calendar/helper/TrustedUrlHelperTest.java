package net.atos.entng.calendar.helper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.TrustedUrlHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TrustedUrlHelperTest {

    private static final String TRUSTED_URL_TITLE = "title";
    private static final String TRUSTED_URL_REGEX_MOODLE = "^https?:\\/\\/.*moodle.*\\/calendar\\/.*";
    private static final String TRUSTED_URL_REGEX_GOOGLE = "^https:\\/\\/calendar\\.google\\.com\\/calendar\\/ical\\/.*\\.ics";
    private static final JsonArray trustedUrlList = new JsonArray()
            .add(new JsonObject()
                    .put(Field.TITLE, TRUSTED_URL_TITLE)
                    .put(Field.REGEX, TRUSTED_URL_REGEX_MOODLE)
            )
            .add(new JsonObject()
                    .put(Field.TITLE, TRUSTED_URL_TITLE)
                    .put(Field.REGEX, TRUSTED_URL_REGEX_GOOGLE)
            );

    @Test
    public void testCheckUrlInRegexWithTrustedUrl(TestContext context){
        String url = "https://calendar.google.com/calendar/ical/erejrw4732h234238hwfd%40group.calendar.google.com/public/basic.ics";

        context.assertTrue(TrustedUrlHelper.checkUrlInRegex(url, trustedUrlList));
    }

    @Test
    public void testCheckUrlInRegexWithUntrustedUrl(TestContext context){
        String url = "https://www.example.net/test";

        context.assertFalse(TrustedUrlHelper.checkUrlInRegex(url, trustedUrlList));
    }

    @Test
    public void testCheckUrlInRegexWithErroneousdUrl(TestContext context){
        String url = "df;lgjsdlfjlgh;sdfjsoaidfaskjbfpoasdjucoasdfoiu[PUU$DKFADC[OEDNjskdgjgfspdfs0f]fe[].,,.<>,d.s";

        context.assertFalse(TrustedUrlHelper.checkUrlInRegex(url, trustedUrlList));
    }

    @Test
    public void testCheckUrlInRegexWithEmptyUrl(TestContext context){
        String url = "";

        context.assertFalse(TrustedUrlHelper.checkUrlInRegex(url, trustedUrlList));
    }
}
