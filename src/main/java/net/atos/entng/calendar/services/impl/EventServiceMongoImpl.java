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

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.ical.ICalHandler;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.utils.Course;
import net.fortuna.ical4j.util.UidGenerator;

import org.apache.commons.lang3.mutable.MutableInt;
import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;

public class EventServiceMongoImpl extends MongoDbCrudService implements EventServiceMongo {

    private final EventBus eb;
    public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    final SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_24H_FULL_FORMAT);
    protected static final Logger log = LoggerFactory.getLogger(EventServiceMongoImpl.class);

    public EventServiceMongoImpl(String collection, EventBus eb) {
        super(collection);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.eb = eb;
    }

    @Override
    public void list(String calendarId, UserInfos user, final Handler<Either<String, JsonArray>> handler) {
        // Query
        QueryBuilder query = QueryBuilder.start("calendar").is(calendarId);
        JsonObject sort = new JsonObject().put("modified", -1);

        // Projection
        JsonObject projection = new JsonObject();

        mongo.find(this.collection, MongoQueryBuilder.build(query), sort, projection, validResultsHandler(handler));
    }

    private void insertInMongo(Course course, Handler<Either<String, JsonObject>> handler) {
        JsonObject body = course.toJson();
        JsonObject now = MongoDb.now();
        body.put("created", now);
        body.put("modified", now);

        String icsUid = generateUid(handler);

        body.put("icsUid", icsUid);
        mongo.save(this.collection, body, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (!event.body().containsKey("status") || !event.body().getValue("status").equals("ok")) {
                    handleLeft(handler, "no id");
                }
            }
        });

    }

    @Override
    public void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Clean data
        body.remove("_id");
        body.remove("calendar");

        // ics Uid generate
        String icsUid = generateUid(handler);

        // Prepare data
        JsonObject now = MongoDb.now();
        body.put("owner", new JsonObject().put("userId", user.getUserId()).put("displayName", user.getUsername()));
        body.put("created", now);
        body.put("modified", now);
        body.put("calendar", calendarId);
        body.put("icsUid", icsUid);
        System.out.println(body);
        mongo.save(this.collection, body, validActionResultHandler(handler));

    }

    @Override
    public void createFirstRecurrence(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        // Clean data
        body.remove("_id");
        body.remove("calendar");

        // ics Uid generate
        String icsUid = generateUid(handler);

        // Prepare data
        JsonObject now = MongoDb.now();
        body.put("owner", new JsonObject().put("userId", user.getUserId()).put("displayName", user.getUsername()));
        body.put("created", now);
        body.put("modified", now);
        body.put("calendar", calendarId);
        body.put("icsUid", icsUid);
        boolean noWeekDayWithWeekMod = false;

        JsonObject recurrence =  body.getJsonObject("recurrence");
        if(recurrence.getString("type").equals("every_week")){
            noWeekDayWithWeekMod = checkFirstDateOfEveryWeek(body);
        }
        if(!noWeekDayWithWeekMod) {
            mongo.save(this.collection, body, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    if (event.body().containsKey("status") && event.body().getValue("status").equals("ok")) {
                        createRecurrences(body, event.body().getString("_id"), handler);
//                    handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("_id", event.body().getValue("_id"))));
                    } else {
                        handleLeft(handler, "no id");

                    }
                }
            });
        }else {
            handleLeft(handler,"no days available with week recurrence");
        }
    }

    private boolean checkFirstDateOfEveryWeek(JsonObject body) {
        JsonObject recurrence =  body.getJsonObject("recurrence");
        JsonObject weekDays =  recurrence.getJsonObject("week_days");

        String startDate = body.getString("startMoment");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(startDate));
            int differenceBetweenCalendarAndMongo;
            differenceBetweenCalendarAndMongo = ((c.get(Calendar.DAY_OF_WEEK) + 5 ) % 7) + 1;
            boolean isAvailableDay = weekDays.getBoolean(Integer.toString(differenceBetweenCalendarAndMongo));
            if(!isAvailableDay){
                int nbDays = 0 ;
                while (!isAvailableDay && nbDays < 7){
                    c.add(Calendar.DATE, 1);
                    differenceBetweenCalendarAndMongo = ((c.get(Calendar.DAY_OF_WEEK) + 5 ) % 7) + 1;
                    isAvailableDay = weekDays.getBoolean(Integer.toString(differenceBetweenCalendarAndMongo));
                    nbDays++;
                }
                if(nbDays == 7){
                    return  true;
                }else {
                    body.put("startMoment", getIncrementDate(body.getString("startMoment"), nbDays));
                    body.put("endMoment", getIncrementDate(body.getString("endMoment"), nbDays));
                    return false;
                }
            }else{
                return false;
            }
        } catch (ParseException e) {
            log.error("ERROR in checkFirstDateOfEveryWeek " + e.getMessage());
            return  true;
        }
    }

    private String generateUid(Handler<Either<String, JsonObject>> handler) {
        UidGenerator uidGenerator;
        String icsUid = "";
        try {
            uidGenerator = new UidGenerator("1");
            icsUid = uidGenerator.generateUid().toString();
        } catch (SocketException e) {
            handler.handle(new Either.Left<String, JsonObject>(new String("Error")));
        }
        return icsUid;
    }

    private void createRecurrences(JsonObject body, String parentId, Handler<Either<String, JsonObject>> handler) {
        if(body.containsKey("recurrence")){
            JsonObject params = body.getJsonObject("recurrence");
            if(params.containsKey("type") && params.containsKey("end_type")){
                if (params.getString("type").equals("every_week")){
                    if( params.getString("end_type").equals("after")){
                        createCoursesAfterAndEveryWeek(params,body,parentId,handler);
                    }else if(params.getString("end_type").equals("on")){
                        createCoursesOnAndEveryWeek(params,body,parentId,handler);
                    }else{
                        handleLeft(handler,"Error no recognizable end_type given in recurrence");
                    }
                }else if (params.getString("type").equals("every_day")){
                    if( params.getString("end_type").equals("after")){
                        createCoursesAfterAndEveryDay(params,body,parentId,handler);
                    }else if(params.getString("end_type").equals("on")){
                        createCoursesOnAndEveryDay(params,body,parentId,handler);
                    }else{
                        handleLeft(handler,"Error no recognizable type given in recurrence");
                    }
                }else{
                    handleLeft(handler,"Error no recognizable end_type given in recurrence");
                }
            }else{
                handleLeft(handler, "Error no argument type or end_type in recurrence  ");
            }
        }else{
            handleLeft(handler, "Error no argument recurrence ");
        }
    }

    private void handleRight(String parentId, Handler<Either<String, JsonObject>> handler) {
        handler.handle(new Either.Right<>(new JsonObject().put("_id",parentId)));
    }

    private void createCoursesAfterAndEveryWeek(JsonObject params, JsonObject firstOccurenceBody, String parentId, Handler<Either<String, JsonObject>> handler) {
        int nb_occurence = params.getInteger("end_after");
        if(nb_occurence <= 1){
            handleRight(parentId, handler);
        }else{
            for (int i =1 ; i< nb_occurence;i++){
                Course course = createCourseWeek(firstOccurenceBody,firstOccurenceBody.getJsonObject("recurrence"),i);
                log.info(firstOccurenceBody.getString("startMoment"));
                log.info(course.getStartMoment());

                firstOccurenceBody.put("startMoment",course.getStartMoment());
                firstOccurenceBody.put("endMoment",course.getEndMoment());
                course.setParentId(parentId);
                insertInMongo(course,handler);
            }
            handleRight(parentId, handler);
        }
    }


    private void createCoursesOnAndEveryWeek(JsonObject params, JsonObject firstOccurenceBody, String parentId, Handler<Either<String, JsonObject>> handler) {
    }
    private void createCoursesAfterAndEveryDay(JsonObject params, JsonObject firstOccurenceBody, String parentId, Handler<Either<String, JsonObject>> handler) {
        int nb_occurence = params.getInteger("end_after");
        if(nb_occurence <= 1){
            handleRight(parentId, handler);
        }else{
            for (int i =1 ; i< nb_occurence;i++){
                Course course = createCourseDay(firstOccurenceBody,firstOccurenceBody.getJsonObject("recurrence"),i);
                course.setParentId(parentId);
                insertInMongo(course,handler);
            }
            handleRight(parentId, handler);
        }
    }


    private void createCoursesOnAndEveryDay(JsonObject params, JsonObject firstOccurenceBody, String parentId, Handler<Either<String, JsonObject>> handler) {
        String date_startStr = params.getString("start_on");
        String date_endStr = params.getString("end_on");
        int nb_occurence = getNbOccurenceWhenLimitDate(date_startStr,date_endStr);
        if(nb_occurence <= 1){
            handleRight(parentId, handler);
        }else{
            for (int i =1 ; i< nb_occurence;i++){
                Course course = createCourseDay(firstOccurenceBody,firstOccurenceBody.getJsonObject("recurrence"),i);
                course.setParentId(parentId);
                insertInMongo(course,handler);
            }
            handleRight(parentId, handler);
        }
    }

    private int getNbOccurenceWhenLimitDate(String date_startStr, String date_endStr) {
        Calendar c = Calendar.getInstance();
        try {
            Date startDate = sdf.parse(date_startStr);
            Date endDate = sdf.parse(date_endStr);
            long diffInMillies = Math.abs(endDate.getTime() - startDate.getTime());
            return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        } catch (ParseException e) {
            log.error("ERROR in getIncrementDate " + e.getMessage());
            return 0;
        }
    }

    private Course createCourseWeek(JsonObject firstOccurenceBody, JsonObject recurrence, int index) {
        Course courseToCreate = createCourse(firstOccurenceBody, recurrence, index);
        int gapWeeks = recurrence.getInteger("every");
        JsonObject weekDays =  recurrence.getJsonObject("week_days");
        String startDate = firstOccurenceBody.getString("startMoment");
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(startDate));
            c.add(Calendar.DATE, 1);

            int differenceBetweenCalendarAndMongo;
            differenceBetweenCalendarAndMongo = ((c.get(Calendar.DAY_OF_WEEK) + 5 ) % 7) + 1 ;

            boolean isAvailableDay = weekDays.getBoolean(Integer.toString(differenceBetweenCalendarAndMongo));

            if(!isAvailableDay) {
                int nbDays = 1;
                while (!isAvailableDay) {
                    c.add(Calendar.DATE, 1);
                    differenceBetweenCalendarAndMongo = ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1;
                    isAvailableDay = weekDays.getBoolean(Integer.toString(differenceBetweenCalendarAndMongo));
                    nbDays++;
                }
                courseToCreate.setStartMoment( getIncrementDate(firstOccurenceBody.getString("startMoment"), nbDays));
                courseToCreate.setEndMoment( getIncrementDate(firstOccurenceBody.getString("endMoment"), nbDays));

            }
        } catch (ParseException e) {
            log.error("ERROR in createCourseWeek " + e.getMessage());
        }
        return  courseToCreate;
    }

    private Course createCourseDay(JsonObject firstOccurenceBody, JsonObject recurrence, int index) {
        Course courseToCreate = createCourse(firstOccurenceBody, recurrence, index);
        int gapDays = recurrence.getInteger("every");
        try {

            courseToCreate.setStartMoment(getIncrementDate(firstOccurenceBody.getString("startMoment"),gapDays*index));
            courseToCreate.setEndMoment(getIncrementDate(firstOccurenceBody.getString("endMoment"),gapDays*index));
        } catch (Exception e) {
            log.error("ERROR IN createCourseDay " + e.getMessage());
        }
        return  courseToCreate;
    }

    private Course createCourse(JsonObject firstOccurenceBody, JsonObject recurrence, int index) {
        Course courseToCreate = new Course();
        courseToCreate.setAllDay(firstOccurenceBody.getBoolean("allday"));
        courseToCreate.setIndex(index);
        courseToCreate.setRecurrence(recurrence);
        courseToCreate.setRecurrent(firstOccurenceBody.getBoolean("isRecurrent"));
        courseToCreate.setTitle(firstOccurenceBody.getString("title"));
        courseToCreate.setCalendarId(firstOccurenceBody.getString("calendar"));
        JsonObject owner = firstOccurenceBody.getJsonObject("owner");
        courseToCreate.setUser(owner.getString("userId"),owner.getString("displayName"));
        return courseToCreate;
    }

    private String getIncrementDate(String moment, int numberOfDaysToAdd){
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(moment));

            c.add(Calendar.DATE, numberOfDaysToAdd);
            return  sdf.format(c.getTime());
        } catch (ParseException e) {
            log.error("ERROR in getIncrementDate " + e.getMessage());
            return "null";
        }


    }

    private void handleLeft(Handler<Either<String, JsonObject>> handler, String s) {
        handler.handle(new Either.Left<>(s));
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
        body.remove("_id");
        body.remove("calendar");

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
    public void importIcal(final String calendarId, String ics, final UserInfos user, final Handler<Either<String, JsonObject>> handler) {
        final JsonObject message = new JsonObject();
        message.put("action", ICalHandler.ACTION_PUT);
        message.put("calendarId", calendarId);
        message.put("ics", ics);
        final EventServiceMongoImpl eventService = this;
        final MutableInt i = new MutableInt();

        eb.send(ICalHandler.ICAL_HANDLER_ADDRESS, message, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                final JsonObject result = new JsonObject();
                if ("ko".equals(reply.body().getString("status"))) {
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
                                    eventService.create(calendarId, calendarEvent, user, new Handler<Either<String, JsonObject>>() {
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
    public void getCalendarEventById(String eventId, Handler<Either<String, JsonObject>> handler) {
        JsonObject projection = new JsonObject();
        QueryBuilder query = QueryBuilder.start("_id").is(eventId);
        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(handler));
    }

    @Override
    public void getEventsByCalendarAndDate(String[] calendars, int nbLimit, Handler<Either<String, JsonArray>> handler) {
        QueryBuilder query;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String dateToIso = df.format(new Date());
        JsonObject sort = new JsonObject().put("startMoment", 1);

        query = new QueryBuilder().and(QueryBuilder.start("calendar").in(calendars).get(),
                QueryBuilder.start("endMoment").greaterThanEquals(dateToIso).get());

        mongo.find(this.collection, MongoQueryBuilder.build(query),
                sort, null, -1, nbLimit, 2147483647,
                validResultsHandler(handler));
    }
}