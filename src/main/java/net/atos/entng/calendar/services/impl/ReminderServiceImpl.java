package net.atos.entng.calendar.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ReminderService;
import org.bson.conversions.Bson;
import org.entcore.common.user.UserInfos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.mongodb.client.model.Filters.*;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

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
                eq(Field.EVENTID_CAMEL, eventId),
                eq(String.format(Field.OWNER + "." + Field.USERID), user.getUserId())
        );

        log.info(String.format("Service eventId:  %s", eventId));

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

    @Override
    public Future<JsonArray> fetchRemindersToSend() {
        Promise<JsonArray> promise = Promise.promise();

        // Obtenir les bornes de la minute actuelle
        Date dateNow = new Date();
        dateNow.setSeconds(0);
        Date now = getTruncatedCurrentMinute();
        Date nextMinute = new Date(now.getTime() + 59999);

        // Construction de la requête MongoDB
        final Bson query = elemMatch("reminderFrequency", and(
                        gte("$gte", now),
                        lt("$lt", nextMinute)
                )
        );

        mongo.find(this.collection, MongoQueryBuilder.build(query), validResultsHandler(events -> {
            if (events.isLeft()) {
                String errMessage = String.format(
                        "[Calendar@%s::fetchRemindersToSend] Error retrieving this minute's reminders: %s",
                        this.getClass().getSimpleName(), events.left().getValue()
                );
                log.error(errMessage, events.left().getValue());
                promise.fail(events.left().getValue());
            } else {
                promise.complete(events.right().getValue());
            }
        }));

        return promise.future();
    }

    private Date getTruncatedCurrentMinute() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

}
