package net.atos.entng.calendar.services;


import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.TrustedUrlServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class TrustedUrlServiceImplTest {


    MongoDb mongo = mock(MongoDb.class);

    private TrustedUrlServiceImpl trustedUrlService;

    private final String TRUSTED_URL_ID = "123";


    @Before
    public void setUp(TestContext context) {
        this.trustedUrlService = new TrustedUrlServiceImpl(Calendar.URL_COLLECTION, mongo);
    }

    @Test
    public void testCreateTrustedUrl(TestContext context){

        JsonObject trustedUrl = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        //Expected data
        String expectedCollection = "trustedurl";
        JsonObject expectedTrustedUrl = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject newTrustedUrl = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(newTrustedUrl, expectedTrustedUrl);
            return null;
        }).when(mongo).insert(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        trustedUrlService.create(trustedUrl);
    }

    @Test
    public void testUpdate(TestContext context){

        JsonObject trustedUrl = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        //Expected data
        String expectedCollection = "trustedurl";
        JsonObject expectedTrustedUrl = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");
        JsonObject expectedQuery = new JsonObject().put("_id", TRUSTED_URL_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            JsonObject newTrustedUrl = invocation.getArgument(2);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            context.assertEquals(newTrustedUrl, expectedTrustedUrl);
            return null;
        }).when(mongo).update(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(JsonObject.class));

        trustedUrlService.update(TRUSTED_URL_ID, trustedUrl);
    }

    @Test
    public void testDeleteTrustedUrl(TestContext context){

        //Expected data
        String expectedCollection = "trustedurl";
        JsonObject expectedQuery = new JsonObject().put("_id", TRUSTED_URL_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        trustedUrlService.delete(TRUSTED_URL_ID);
    }



}
