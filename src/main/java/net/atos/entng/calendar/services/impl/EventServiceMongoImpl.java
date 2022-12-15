/*
 * Copyright © Région Nord Pas de Calais-Picardie,  Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.atos.entng.calendar.services.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.helpers.FutureHelper;
import net.atos.entng.calendar.ical.ICalHandler;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.UserService;
import net.fortuna.ical4j.util.UidGenerator;
import org.apache.commons.lang3.mutable.MutableInt;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;

import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static net.atos.entng.calendar.Calendar.CALENDAR_COLLECTION;
import static org.entcore.common.mongodb.MongoDbResult.*;

public class EventServiceMongoImpl extends MongoDbCrudService implements EventServiceMongo {

    private final EventBus eb;
    private final UserService userService;
    private final CalendarService calendarService;
    protected static final Logger log = LoggerFactory.getLogger(CalendarServiceImpl.class);

    public EventServiceMongoImpl(String collection, EventBus eb, ServiceFactory serviceFactory) {
        super(collection);
        this.eb = eb;
        this.userService = serviceFactory.userService();
        this.calendarService = serviceFactory.calendarService();
    }

    /**
     * * fetch all the events of a calendar
     * @param calendarId calendar id from which the events are linked
     * @param user user id of the owner of the calendar
     */
    @Override
    public void list(String calendarId, UserInfos user, final Handler<Either<String, JsonArray>> handler) {
        list(calendarId, user, null, null, handler);
    }

    /**
     * * fetch the events of a calendar in a range of date
     * @param calendarId calendar id from which the events are linked
     * @param user user id of the owner of the calendar
     * @param startDate beginning of range from which to fetch the events (format "YYYY-MM-DD")
     * @param endDate end of range from which to fetch the events (format "YYYY-MM-DD")
     * if startDate and endDate are null, the function gets all elements from the calendar
     */
    @Override
    public void list(String calendarId, UserInfos user, String startDate, String endDate, final Handler<Either<String, JsonArray>> handler) {
        QueryBuilder queryCalendar = QueryBuilder.start(Field._ID).is(calendarId);

        mongo.findOne(CALENDAR_COLLECTION, MongoQueryBuilder.build(queryCalendar), validActionResultHandler(result -> {
            if (result.isLeft()) {
                log.error("[Calendar@EventServiceMongo::list]: an error has occurred while finding targeted " +
                        "calendar: ", result.left().getValue());
                handler.handle(new Either.Left<>(result.left().getValue()));
            } else {
                //result right
                JsonObject currentCalendar = result.right().getValue();

                //get calendar owner
                String currentCalendarOwnerId = currentCalendar
                        .getJsonObject("result", new JsonObject())
                        .getJsonObject("owner", new JsonObject())
                        .getString("userId");

                Boolean userIsCalendarOwner = user.getUserId().equals(currentCalendarOwnerId);

                JsonObject queryEvent = new JsonObject().put("calendar", calendarId);

                if (Boolean.FALSE.equals(userIsCalendarOwner)) {
                    JsonObject userIsEventOwner = new JsonObject().put("owner.userId", user.getUserId());
                    JsonObject isNotSharedEvent = new JsonObject().put("shared", new JsonObject().put("$exists", false));
                    JsonObject sharedIsEmpty = new JsonObject().put("shared", new JsonObject().put("$size", 0));
                    JsonObject sharedContainsUserId = new JsonObject().put("shared.userId",
                            new JsonObject().put("$in", new JsonArray().add(user.getUserId())));
                    JsonObject sharedContainsUserGroupIds = new JsonObject().put("shared.groupId",
                            new JsonObject().put("$in", user.getGroupsIds()));

                    queryEvent.put("$or",
                            new JsonArray()
                                    .add(userIsEventOwner)
                                    .add(isNotSharedEvent)
                                    //case 'shared' field is empty
                                    .add(sharedIsEmpty)
                                    //case shared to the user individually
                                    .add(sharedContainsUserId)
                                    //case shared to the user by groupId
                                    .add(sharedContainsUserGroupIds)

                    );
                }
                JsonObject sort = new JsonObject().put("modified", -1);
                // Projection
                JsonObject projection = new JsonObject();
                //JsonArray newQuery = new JsonArray();

                if (startDate != null && endDate != null) {
                    if (queryEvent.containsKey("$or")) {
                        queryEvent.getJsonArray("$or").addAll(fetchEventsWithDates(startDate, endDate));
                    } else {
                        queryEvent.put("$or", fetchEventsWithDates(startDate, endDate));
                    }
                }

                mongo.find(this.collection, queryEvent, sort, projection, validResultsHandler(handler));
            }
        }));
    }

    public JsonArray fetchEventsWithDates(String startDate, String endDate) {
        JsonArray eventsFilterByDate = new JsonArray();

        if (startDate != null && endDate != null) {
            //fetch the single-day events
            JsonObject datesISO = new JsonObject();
            datesISO.put("$gte", startDate);
            datesISO.put("$lt", endDate);

            eventsFilterByDate.add(new JsonObject().put(Field.STARTMOMENT, datesISO));

            //fetch the multiday events
            JsonObject datesISOStart = new JsonObject();
            JsonObject datesISOEnd = new JsonObject();
            datesISOStart.put("$lt", startDate);
            datesISOEnd.put("$gte", startDate);

            eventsFilterByDate.add(new JsonObject().put(Field.STARTMOMENT, datesISOStart));
            eventsFilterByDate.add(new JsonObject().put(Field.ENDMOMENT, datesISOEnd));
        }

        return eventsFilterByDate;
    }
    @Override
    public void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        create(calendarId, body, user, null, handler);
    }

    public void create(String calendarId, JsonObject body, UserInfos user, String collection, Handler<Either<String, JsonObject>> handler) {
        // Clean data
        body.remove("_id");

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
        body.put("owner", new JsonObject().put("userId", user.getUserId()).put("displayName", user.getUsername()));
        body.put("created", now);
        body.put("modified", now);
        body.put("icsUid", icsUid);
        if (body.getValue("calendar") == null) {
            body.put("calendar", new JsonArray().add(calendarId));
        }

        mongo.save((collection != null) ? collection : this.collection, body, validActionResultHandler(handler));
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

    public Future<JsonObject> retrieve(String calendarId, String eventId, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();
        retrieve(calendarId, eventId, user, FutureHelper.handlerJsonObject(promise));
        return promise.future();
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
    public Future<JsonArray> retrieveByCalendarId(String calendarId) {
        Promise<JsonArray> promise = Promise.promise();
        // Query
        QueryBuilder query = QueryBuilder.start(Field.CALENDAR).is(calendarId);

        mongo.find(this.collection, MongoQueryBuilder.build(query), result -> {
            if (result.body().isEmpty()) {
                String message = String.format("[Calendar@%s::retrieveByCalendarId]:  " +
                                "could not retrieve external calendar events",
                        this.getClass().getSimpleName());
                log.error(message);
                promise.fail(message);
            } else {
                promise.complete(result.body().getJsonArray(Field.RESULTS));
            }
        });

        return promise.future();
    }

    @Override
    public void update(String calendarId, String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        query.put("calendar").is(calendarId);

        // Clean data
        body.remove("_id");

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attr : body.fieldNames()) {
            modifier.set(attr, body.getValue(attr));
        }
        modifier.set("modified", MongoDb.now());
        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validActionResultHandler(handler));

    }

    @Override
    public void update(String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);

        // Clean data
        body.remove("_id");

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attr : body.fieldNames()) {
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

    public Future<JsonObject> delete(String calendarId, String eventId, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();
        delete(calendarId, eventId, user, FutureHelper.handlerJsonObject(promise));
        return promise.future();
    }

    @Override
    public Future<Void> deleteDatesAfterComparisonDate(String calendarId, String comparisonDate) {
        Promise<Void> promise = Promise.promise();

        // Query
        QueryBuilder query = QueryBuilder.start(Field.CALENDAR).is(calendarId);
        query.put(Field.STARTMOMENT).greaterThan(comparisonDate);

        mongo.delete(this.collection, MongoQueryBuilder.build(query), result -> {
            if (result.body().isEmpty()) {
                String message = String.format("[Calendar@%s::deleteDatesAfterComparisonDate]:  " +
                                "could not delete events after date",
                        this.getClass().getSimpleName());
                log.error(message);
                promise.fail(message);
            } else {
                promise.complete();
            }
        });

        return promise.future();
    }

    public Future<Void> deleteByCalendarId(String calendarId) {
        Promise<Void> promise = Promise.promise();

        QueryBuilder query = QueryBuilder.start(Field.CALENDAR).is(calendarId);

        mongo.delete(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if (event.isLeft()) {
                String errMessage = String.format("[Calendar@%s::deleteByCalendarId] An error has occurred while deleting events by calendar id: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage);
                promise.fail(event.left().getValue());
            } else {
                promise.complete();
            }
        }));
        return promise.future();
    }

    @Override
    public void getIcal(String calendarId, UserInfos user, final Handler<Message<JsonObject>> handler) {
        final JsonObject message = new JsonObject();
        message.put("action", ICalHandler.ACTION_GET);
        this.list(calendarId, user, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                JsonArray values = event.right().getValue();
                message.put("events", values);
                eb.send(ICalHandler.ICAL_HANDLER_ADDRESS, message, handlerToAsyncHandler(handler));
            }
        });
    }

    @Override
    public void importIcal(final String calendarId, String ics, final UserInfos user, JsonObject requestInfo, final Handler<Either<String, JsonObject>> handler) {
        importIcal(calendarId, ics, user, requestInfo, this.collection, null, null, handler);
    }

    @Override
    public Future<JsonObject> importIcal(final String calendarId, String ics, final UserInfos user, JsonObject requestInfo, String collection) {
        Promise<JsonObject> promise = Promise.promise();

        this.importIcal(calendarId, ics, user, requestInfo, collection, null, null, FutureHelper.handlerJsonObject(promise));

        return promise.future();
    }

    @Override
    public Future<JsonObject> importIcal(final String calendarId, String ics, final UserInfos user, JsonObject requestInfo,
                                         String collection, String action, String lastUpdate) {
        Promise<JsonObject> promise = Promise.promise();

        this.importIcal(calendarId, ics, user, requestInfo, collection, action, lastUpdate, FutureHelper.handlerJsonObject(promise));

        return promise.future();
    }

    @Override
    public void importIcal(final String calendarId, String ics, final UserInfos user, JsonObject requestInfo, String collection,
                           String action, String lastUpdate, final Handler<Either<String, JsonObject>> handler) {
        final JsonObject message = new JsonObject();
        message.put(Field.ACTION, (action != null)? action : ICalHandler.ACTION_PUT);
        message.put(Field.CALENDARID, calendarId);
        message.put(Field.ICS, ics);
        message.put(Field.REQUESTINFO, requestInfo);
        if((action != null) && action.equals(ExternalICalEventBusActions.SYNC.method()) && lastUpdate != null) message.put(Field.UPDATED, lastUpdate);
        final EventServiceMongoImpl eventService = this;
        final MutableInt i = new MutableInt();

        eb.send(ICalHandler.ICAL_HANDLER_ADDRESS, message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                final JsonObject result = new JsonObject();
                if ("ko".equals(reply.body().getString(Field.STATUS)) || Field.ERROR.equals(reply.body().getString(Field.STATUS))) {
                    handler.handle(new Either.Left<String, JsonObject>(new String("Error")));
                } else {
                    JsonObject body = reply.body();
                    JsonArray calendarEvents = body.getJsonArray("events");
                    final JsonArray invalidCalendarEvents = body.getJsonArray("invalidEvents");
                    result.put("invalidEvents", invalidCalendarEvents);
                    result.put("createdEvents", calendarEvents.size());
                    if (calendarEvents.size() == 0) {
                        handler.handle(new Either.Right<String, JsonObject>(result));
                    }
                    i.add(calendarEvents.size());

                    for (Object e : calendarEvents) {
                        final JsonObject calendarEvent = (JsonObject) e;
                        eventService.retrieveByIcsUid(calendarId, calendarEvent.getString("icsUid"), user, new Handler<Either<String, JsonObject>>() {
                            @Override
                            public void handle(Either<String, JsonObject> event) {
                                // No existing event found
                                if (event.isRight() && event.right().getValue().size() == 0) {
                                    eventService.create(calendarId, calendarEvent, user, collection, new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> event) {
                                            i.subtract(1);
                                            // There is no more events to create
                                            if (i.toInteger() == 0) {
                                                handler.handle(new Either.Right<String, JsonObject>(result));
                                            }
                                        }
                                    });
                                } // Existing event found
                                else if (event.isRight() && event.right().getValue().size() > 0) {
                                    eventService.update(calendarId, event.right().getValue().getString("_id"), calendarEvent, user, new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> event) {
                                            i.subtract(1);
                                            // There is no more events to create
                                            if (i.toInteger() == 0) {
                                                handler.handle(new Either.Right<String, JsonObject>(result));
                                            }
                                        }
                                    });
                                } // An error occured while retrieving the event
                                else {
                                    i.subtract(1);
                                    if (i.toInteger() == 0) {
                                        handler.handle(new Either.Right<String, JsonObject>(result));
                                    }
                                }

                            }
                        });
                    }
                }
            }
        }));
    }

    @Override
    public void findOne(String collection, QueryBuilder query, Handler<Either<String, JsonObject>> handler) {
        JsonObject projection = new JsonObject();
        mongo.findOne(collection, MongoQueryBuilder.build(query), validResultHandler(handler));
    }

    @Override
    public Future<JsonObject> getCalendarEventById(String eventId) {
        Promise<JsonObject> promise = Promise.promise();
        getCalendarEventById(eventId, event -> {
            if (event.isLeft()) {
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        });
        return promise.future();
    }

    @Override
    public void getCalendarEventById(String eventId, Handler<Either<String, JsonObject>> handler) {
        JsonObject projection = new JsonObject();
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        mongo.findOne("calendarevent", MongoQueryBuilder.build(query), validResultHandler(handler));
    }

    @Override
    public void getEventsByCalendarAndDate(String[] calendars, int nbLimit, Handler<Either<String, JsonArray>> handler) {
        QueryBuilder query;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String dateToIso = df.format(new Date());
        JsonObject sort = new JsonObject().put("startMoment", 1);

        query = new QueryBuilder().and(QueryBuilder.start("calendar").in(calendars).get(),
                QueryBuilder.start("endMoment").greaterThanEquals(dateToIso).get());

        mongo.find("calendarevent", MongoQueryBuilder.build(query),
                sort, null, -1, nbLimit, 2147483647,
                validResultsHandler(handler));
    }

}
