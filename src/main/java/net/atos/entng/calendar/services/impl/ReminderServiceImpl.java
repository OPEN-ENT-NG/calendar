package net.atos.entng.calendar.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
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
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

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

        String now = getCurrentMinuteISO();
        String nextMinute = getNextMinuteISO();

        final Bson query = and(
                gte(Field.REMINDERFREQUENCY, now),
                lt(Field.REMINDERFREQUENCY, nextMinute)
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
                JsonArray reminders = events.right().getValue();
                JsonArray filteredReminders = new JsonArray();

                // only keep relevant frequency in reminder list
                reminders
                        .stream()
                        .map(JsonObject.class::cast)
                        .forEach(reminder -> {
                            JsonArray frequencyList = reminder.getJsonArray(Field.REMINDERFREQUENCY, new JsonArray());

                            if (!frequencyList.isEmpty()) {
                                List<String> filteredList = frequencyList.stream()
                                        .map(Object::toString)
                                        .filter(time -> time.compareTo(now) >= 0 && time.compareTo(nextMinute) < 0)
                                        .limit(1) // Ensure only one item
                                        .collect(Collectors.toList());

                                reminder.put(Field.REMINDERFREQUENCY, new JsonArray(filteredList));
                            }
                            filteredReminders.add(reminder);
                        });

                promise.complete(filteredReminders);
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
        nowSdf.setTimeZone(TimeZone.getTimeZone(DateUtils.UTC));
        return nowSdf.format(nowDate.getTime());
    }

    private String getNextMinuteISO() {
        Calendar nextMinuteDate = Calendar.getInstance();
        nextMinuteDate.setTime(new Date());
        nextMinuteDate.set(Calendar.SECOND, 0);
        nextMinuteDate.set(Calendar.MILLISECOND, 0);
        nextMinuteDate.add(Calendar.MINUTE, 1);

        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.DATE_FORMAT_UTC);
        sdf.setTimeZone(TimeZone.getTimeZone(DateUtils.UTC));
        return sdf.format(nextMinuteDate.getTime());
    }

    @Override
    public Future<Void> create(JsonObject body) {
        Promise<Void> promise = Promise.promise();

        mongo.insert(this.collection, body, validResultHandler(event -> {
            if (event.isLeft()){
                String errMessage = String.format("[Calendar@%s::create] An error has occurred while creating a new reminder: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> update(String eventId, String id, JsonObject body) {
        Promise<Void> promise = Promise.promise();

        final Bson query = eq(Field._ID, id);

        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attribute : body.fieldNames()) {
            modifier.set(attribute, body.getValue(attribute));
        }

        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::update] An error has occurred while deleting a reminder: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> delete(String eventId, String id) {
        Promise<Void> promise = Promise.promise();

        final Bson query = and(eq(Field._ID, id), eq(Field.EVENTID, eventId));

        mongo.delete(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::delete] An error has occurred while deleting a reminder: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));
        return promise.future();
    }



}
