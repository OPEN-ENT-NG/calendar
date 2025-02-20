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
import net.atos.entng.calendar.utils.DateUtils;
import org.bson.conversions.Bson;
import org.entcore.common.user.UserInfos;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:00.000'Z'")
                .withZone(ZoneOffset.UTC);
//        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC)
//                .withSecond(0)
//                .withNano(0);
//        ZonedDateTime nextMinute = now.plusMinutes(1);

        // Obtenir les bornes de la minute actuelle
        Date dateNow = new Date();
//        dateNow.setSeconds(0);
        String now = getCurrentMinuteISO();
        String nextMinute = getNextMinuteISO();

        // Construction de la requête MongoDB
        final Bson query =and(
                        gte("reminderFrequency", now),
                        lt("reminderFrequency", nextMinute)
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
                log.info(String.format("GOT REMINDRS %s", events.right().getValue()));
                promise.complete(events.right().getValue());
            }
        }));

        return promise.future();
    }

    private String getCurrentMinuteISO() {
        Calendar nowDate = Calendar.getInstance();
        nowDate.setTime(new Date());
        nowDate.set(Calendar.SECOND, 0);
        nowDate.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat nowSdf = new SimpleDateFormat(DateUtils.DATE_FORMAT_UTC);
        nowSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return nowSdf.format(nowDate.getTime());
    }

    private String getNextMinuteISO() {
        Calendar nextMinuteDate = Calendar.getInstance();
        nextMinuteDate.setTime(new Date());
        nextMinuteDate.set(Calendar.SECOND, 0);
        nextMinuteDate.set(Calendar.MILLISECOND, 0);
        nextMinuteDate.add(Calendar.MINUTE, 1);

        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_FORMAT_UTC);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(nextMinuteDate.getTime());
    }



}
