package net.atos.entng.calendar.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ReminderService;
import org.bson.conversions.Bson;
import org.entcore.common.user.UserInfos;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;

public class ReminderServiceImpl implements ReminderService {
    private final MongoDb mongo;
    private final String collection;
    protected static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);

    public ReminderServiceImpl(String collection, MongoDb mongo) {
        this.mongo = mongo;
        this.collection = collection;
    }

    @Override
    public Future<JsonObject> getEventReminders(String eventId, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        final Bson query = and(
                eq(Field.EVENTID, eventId),
                eq(Field.USERID, user.getUserId())
        );

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(events -> {
            if(events.isLeft()){
                String errMessage = String.format("[Calendar@%s::getEventReminders] An error has occurred while retrieving reminder: %s",
                        this.getClass().getSimpleName(), events.left().getValue());
                log.error(errMessage, events.left().getValue());
                promise.fail(events.left().getValue());
            } else {
                promise.complete(events.right().getValue());
            }
        }));

        return promise.future();
    }
}
