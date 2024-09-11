package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import net.atos.entng.calendar.services.impl.DefaultUserServiceImpl;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.*;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultUserServiceImplTest {

    Neo4j neo4j = mock(Neo4j.class);
    private DefaultUserServiceImpl defaultUserService;

    private static final String USER_ID = "000";

    @Before
    public void setUp(TestContext context) {
        this.defaultUserService = new DefaultUserServiceImpl(neo4j);
    }

    @Test
    public void testFetchUserIfOneIdInList(TestContext context){
        //Arguments
        UserInfos user = new UserInfos();
        user.setUserId(USER_ID);

        List<String> groupIds = Arrays.asList("000");

        //Expected data
        JsonObject expectedUser = new JsonObject().put(Field.USERID, USER_ID).put(Field.IDS, groupIds);
        String expectedQuery = "MATCH (u:User) " +
                "WHERE u.id IN {ids} AND u.id <> {userId} " +
                "RETURN distinct u.id as id, u.displayName as displayName"

                + " UNION " +

                "MATCH (n:Group )<-[:IN]-(u:User) " +
                "WHERE n.id IN {ids} AND u.id <> {userId} " +
                "RETURN distinct u.id as id, u.displayName as displayName";

        Mockito.doAnswer(invocation -> {
            String shareIdUserGroupInfoQuery = invocation.getArgument(0);
            JsonObject userObject = invocation.getArgument(1);
            context.assertEquals(userObject, expectedUser);
            context.assertEquals(shareIdUserGroupInfoQuery, expectedQuery);
            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        defaultUserService.fetchUser(groupIds, user, false);
    }

    @Test
    public void testFetchUserIfOneIdInListAndKeepUser(TestContext context){
        //Arguments
        UserInfos user = new UserInfos();
        user.setUserId(USER_ID);

        List<String> groupIds = Arrays.asList("000");

        //Expected data
        JsonObject expectedUser = new JsonObject().put(Field.USERID, USER_ID).put(Field.IDS, groupIds);
        String expectedQuery = "MATCH (u:User) " +
                "WHERE u.id IN {ids} " +
                "RETURN distinct u.id as id, u.displayName as displayName"

                + " UNION " +

                "MATCH (n:Group )<-[:IN]-(u:User) " +
                "WHERE n.id IN {ids} " +
                "RETURN distinct u.id as id, u.displayName as displayName";

        Mockito.doAnswer(invocation -> {
            String shareIdUserGroupInfoQuery = invocation.getArgument(0);
            JsonObject userObject = invocation.getArgument(1);
            context.assertEquals(userObject, expectedUser);
            context.assertEquals(shareIdUserGroupInfoQuery, expectedQuery);
            return null;
        }).when(neo4j).execute(Mockito.anyString(), Mockito.any(JsonObject.class), Mockito.any(Handler.class));

        defaultUserService.fetchUser(groupIds, user, true);
    }

}
