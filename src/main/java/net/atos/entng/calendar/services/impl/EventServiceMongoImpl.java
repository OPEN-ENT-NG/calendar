package net.atos.entng.calendar.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;
import net.atos.entng.calendar.services.EventService;

import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;

public class EventServiceMongoImpl extends MongoDbCrudService implements EventService {

    public EventServiceMongoImpl(String collection) {
        super(collection);

    }

    @Override
    public void list(String calendarId, UserInfos user, final Handler<Either<String, JsonArray>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("calendar").is(calendarId);
        JsonObject sort = new JsonObject().putNumber("modified", -1);

        // Projection
        JsonObject projection = new JsonObject();

        mongo.find(this.collection, MongoQueryBuilder.build(query), sort, projection, validResultsHandler(handler));
    }

    @Override
    public void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Clean data
        body.removeField("_id");
        body.removeField("calendar");

        // Prepare data
        JsonObject now = MongoDb.now();
        body.putObject("owner", new JsonObject().putString("userId", user.getUserId()).putString("displayName", user.getUsername())).putObject("created", now).putObject("modified", now).putString("calendar", calendarId);

        mongo.save(this.collection, body, validActionResultHandler(handler));
    }

    @Override
    public void retrieve(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        query.put("calendar").is(calendarId);

        // Projection
        JsonObject projection = new JsonObject();

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), projection, validResultHandler(handler));
    }

    @Override
    public void update(String calendarId, String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        query.put("calendar").is(calendarId);

        // Clean data
        body.removeField("_id");
        body.removeField("calendar");

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attr : body.getFieldNames()) {
            modifier.set(attr, body.getValue(attr));
        }
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));

    }

    @Override
    public void delete(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        query.put("calendar").is(calendarId);
        mongo.delete(this.collection, MongoQueryBuilder.build(query), validActionResultHandler(handler));

    }

}
