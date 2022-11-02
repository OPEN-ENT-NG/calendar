package net.atos.entng.calendar.services;


import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.PlatformServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class PlatformServiceImplTest {


    MongoDb mongo = mock(MongoDb.class);

    private PlatformServiceImpl platformService;

    private final String PLATFORM_ID = "123";


    @Before
    public void setUp(TestContext context) {
        this.platformService = new PlatformServiceImpl(Calendar.PLATFORMS_COLLECTION, mongo);
    }

    @Test
    public void testCreatePlatform(TestContext context){

        JsonObject platform = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        //Expected data
        String expectedCollection = "calendar.platforms";
        JsonObject expectedPlatform = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject newTrustedUrl = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(newTrustedUrl, expectedPlatform);
            return null;
        }).when(mongo).insert(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        platformService.create(platform);
    }

    @Test
    public void testUpdate(TestContext context){

        JsonObject platform = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");

        //Expected data
        String expectedCollection = "calendar.platforms";
        JsonObject expectedPlatform = new JsonObject()
                .put("title", "moodle")
                .put("regex", "^https?:\\/\\/.*moodle.*\\/calendar\\/.*");
        JsonObject expectedQuery = new JsonObject().put("_id", PLATFORM_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            JsonObject newTrustedUrl = invocation.getArgument(2);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            context.assertEquals(newTrustedUrl, expectedPlatform);
            return null;
        }).when(mongo).update(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(JsonObject.class));

        platformService.update(PLATFORM_ID, platform);
    }

    @Test
    public void testDeletePlatform(TestContext context){

        //Expected data
        String expectedCollection = "calendar.platforms";
        JsonObject expectedQuery = new JsonObject().put("_id", PLATFORM_ID);

        Mockito.doAnswer(invocation -> {
            String collection = invocation.getArgument(0);
            JsonObject query = invocation.getArgument(1);
            context.assertEquals(collection, expectedCollection);
            context.assertEquals(query, expectedQuery);
            return null;
        }).when(mongo).delete(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        platformService.delete(PLATFORM_ID);
    }



}
