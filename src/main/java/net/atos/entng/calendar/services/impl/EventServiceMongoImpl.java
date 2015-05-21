package net.atos.entng.calendar.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

import java.net.SocketException;

import net.atos.entng.calendar.handlers.IcsImportHandler;
import net.atos.entng.calendar.ical.ICalHandler;
import net.atos.entng.calendar.services.EventService;
import net.fortuna.ical4j.util.UidGenerator;

import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;

public class EventServiceMongoImpl extends MongoDbCrudService implements EventService {

    private final EventBus eb;

    public EventServiceMongoImpl(String collection, EventBus eb) {
        super(collection);
        this.eb = eb;
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

        // ics Uid generate
        UidGenerator uidGenerator;
        String icsUid = "";
        try {
            uidGenerator = new UidGenerator("1");
            icsUid = uidGenerator.generateUid().toString();
        } catch (SocketException e) {
            handler.handle(new Either.Left<String, JsonObject>(new String("Error")));
        }

        // Prepare data
        JsonObject now = MongoDb.now();
        body.putObject("owner", new JsonObject().putString("userId", user.getUserId()).putString("displayName", user.getUsername()));
        body.putObject("created", now);
        body.putObject("modified", now);
        body.putString("calendar", calendarId);
        body.putString("icsUid", icsUid);
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
    public void retrieveByIcsUid(String calendarId, String icsUid, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("icsUid").is(icsUid);
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

    @Override
    public void getIcal(String calendarId, UserInfos user, final Handler<Message<JsonObject>> handler) {
        final JsonObject message = new JsonObject();
        message.putString("action", ICalHandler.ACTION_GET);
        this.list(calendarId, user, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                JsonArray values = event.right().getValue();
                message.putArray("events", values);
                eb.send(ICalHandler.ICAL_HANDLER_ADDRESS, message, handler);

            }
        });
    }

    @Override
    public void importIcal(final String calendarId, String ics, final UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        final JsonObject message = new JsonObject();
        message.putString("action", ICalHandler.ACTION_PUT);
        message.putString("calendarId", calendarId);
        message.putString("ics", ics);
        final EventServiceMongoImpl eventService = this;
        eb.send(ICalHandler.ICAL_HANDLER_ADDRESS, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                final JsonObject result = new JsonObject();
                if ("ko".equals(reply.body().getString("status"))) {
                    handler.handle(new Either.Left<String, JsonObject>(new String("Error")));
                } 
                else {
                    JsonObject body = reply.body();
                    JsonArray calendarEvents = body.getArray("events");
                    final JsonArray invalidCalendarEvents = body.getArray("invalidEvents");
                    result.putArray("invalidEvents", invalidCalendarEvents);
                    result.putNumber("createdEvents", calendarEvents.size());
                    for (Object e : calendarEvents) {
                        final JsonObject calendarEvent = (JsonObject) e;
                        Handler<Either<String, JsonObject>> icsImportHandler = new IcsImportHandler(eventService, calendarId, calendarEvent, user, handler, result);               
                        eventService.retrieveByIcsUid(calendarId, calendarEvent.getString("icsUid"), user, icsImportHandler);
                    }
                }
            }
        });
    }
}
