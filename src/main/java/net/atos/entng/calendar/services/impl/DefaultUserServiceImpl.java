package net.atos.entng.calendar.services.impl;

import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.UserHelper;
import net.atos.entng.calendar.models.User;
import net.atos.entng.calendar.services.UserService;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class DefaultUserServiceImpl implements UserService {

    private final Neo4j neo4j;
    private static final Logger log = LoggerFactory.getLogger(DefaultUserServiceImpl.class);

    public DefaultUserServiceImpl(Neo4j neo4j) {
        this.neo4j = neo4j;
    }

    /**
     * fetch users from shared data
     *
     * @param ids             list of ids being user or group identifier {@link List<String>}
     * @param user            user info (in this case, we using only its id) {@link UserInfos}
     * @param keepUserFromSession   if the code should include the user logged in or not {@link boolean}
     *
     * @return {@link Future} of {@link List<User>} containing list of user fetched
     */
    @Override
    public Future<List<User>> fetchUser(List<String> ids, UserInfos user, boolean keepUserFromSession) {
        Promise<List<User>> promise = Promise.promise();
        JsonObject params = new JsonObject().put(Field.USERID, user.getUserId()).put(Field.IDS, ids);
        neo4j.execute(getShareIdUserGroupInfoQuery(keepUserFromSession), params, Neo4jResult.validResultHandler(res -> {
            if (res.isLeft()) {
                String message = String.format("[Calendar@%s::fetchUser] An error has occurred" +
                        " during fetch users from its id/groups: %s", this.getClass().getSimpleName(), res.left().getValue());
                log.error(message);
                promise.fail(res.left().getValue());
            } else {
                promise.complete(UserHelper.usersList(res.right().getValue()));
            }
        }));
        return promise.future();
    }

    /**
     * Neo4j Query to fetch all user and belonged groups
     *
     * @param keepUserFromSession   if the request should include the user logged in or not {@link boolean}
     *
     * @return {@link String} of neo4j Query
     */
    private String getShareIdUserGroupInfoQuery(boolean keepUserFromSession) {
        return "MATCH (u:User) " +
                "WHERE u.id IN {ids} " + (keepUserFromSession ? "" : "AND u.id <> {userId} ") +
                "RETURN distinct u.id as id, u.displayName as displayName"

                + " UNION " +

                "MATCH (n:Group )<-[:IN]-(u:User) " +
                "WHERE n.id IN {ids} " + (keepUserFromSession ? "" : "AND u.id <> {userId} ") +
                "RETURN distinct u.id as id, u.displayName as displayName";
    }


    /**
     * fetch user with id
     *
     * @param userId             the user's id {@link String}
     *
     * @return {@link Future} of {@link User} containing the user fetched
     */
    @Override
    public Future<User> fetchUserById(String userId) {
        Promise<User> promise = Promise.promise();
        JsonObject params = new JsonObject().put(Field.USERID, userId);

        String query = "MATCH (U: User) WHERE U.id = {userId} RETURN U";
        neo4j.execute(query, params, Neo4jResult.validUniqueResultHandler(res -> {
            if (res.isLeft()) {
                String message = String.format("[Calendar@%s::fetchUserById] An error has occurred" +
                        " during fetch user from its id: %s", this.getClass().getSimpleName(), res.left().getValue());
                log.error(message);
                promise.fail(res.left().getValue());
            } else {
                promise.complete(new User(res.right().getValue()));
            }
        }));
        return promise.future();
    }

}
