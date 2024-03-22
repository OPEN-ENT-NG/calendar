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

import com.mongodb.DBObject;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.CalendarService;
import org.bson.conversions.Bson;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static org.entcore.common.mongodb.MongoDbResult.*;


public class CalendarServiceImpl implements CalendarService {
    private final MongoDb mongo;
    private final String collection;
    protected static final Logger log = LoggerFactory.getLogger(CalendarServiceImpl.class);


    public CalendarServiceImpl(String collection, MongoDb mongo) {
        this.collection = collection;
        this.mongo = mongo;
    }


    @Override
    public Future<JsonArray> list(List<String> calendarIds) {
        Promise<JsonArray> promise = Promise.promise();
        // Query
        final Bson query = in("_id", calendarIds);
        JsonObject sort = new JsonObject().put("modified", -1);
        // Projection
        JsonObject projection = new JsonObject();

        mongo.find(this.collection, MongoQueryBuilder.build(query), sort, projection, validResultsHandler(event -> {
            if (event.isLeft()) {
                String message = String.format("[Calendar@%s::list] An error has occured" +
                        " during fetch calendars: %s", this.getClass().getSimpleName(), event.left().getValue());
                log.error(message, event.left().getValue());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }

    public Future<JsonArray> list(List<String> calendarIds, Boolean isExternal) {
        return list(calendarIds, isExternal, null);
    }
    public Future<JsonArray> list(List<String> calendarIds, Boolean isExternal, String userId) {
        Promise<JsonArray> promise = Promise.promise();

        // filter by ids
        Bson query = in(Field._ID, calendarIds);
        if(userId != null) {
            query = eq(String.format("%s.%s", Field.OWNER, Field.USERID), userId);
        }


        // if a calendar is external it contains "isExternal" = true and a string icsUrl
        if (Boolean.TRUE.equals(isExternal)) {
            query = or(
                eq(Field.ISEXTERNAL, true),
                not(in(Field.ICSLINK, "", null))
            );
        }

        JsonObject sort = new JsonObject().put("modified", -1);

        // Projection
        JsonObject projection = new JsonObject();

        mongo.find(this.collection, MongoQueryBuilder.build(query), sort, projection, validResultsHandler(event -> {
            if (event.isLeft()) {
                String message = String.format("[Calendar@%s::list] An error has occured" +
                        " during fetch calendars: %s", this.getClass().getSimpleName(), event.left().getValue());
                log.error(message, event.left().getValue());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }
    @Override
    public Future<JsonObject> getDefaultCalendar(UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();
        // Query
        final Bson query = and(
          eq("owner.userId", user.getUserId()),
          eq(Field.IS_DEFAULT, true)
        );

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::hasDefaultCalendar]: an error has occurred while finding default calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            promise.complete(result.right().getValue());
        }));
        return promise.future();
    }

    @Override
    public Future<JsonObject> createDefaultCalendar(UserInfos user, String host, String lang) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject defaultCalendar = new JsonObject();
        JsonObject now = MongoDb.now();

        String title = I18n.getInstance().translate("calendar.default.title", host, lang);

        defaultCalendar.put("title", title + " " + user.getUsername());
        defaultCalendar.put("color", "grey");
        defaultCalendar.put("created", now);
        defaultCalendar.put("modified", now);
        defaultCalendar.put("owner", new JsonObject().put("userId", user.getUserId()).put("displayName", user.getUsername()));
        defaultCalendar.put(Field.IS_DEFAULT, true);

        insertDefaultCalendar(promise, defaultCalendar);

        return promise.future();
    }

    @Override
    public Future<Boolean> isDefaultCalendar(String calendarId) {
        Promise<Boolean> promise = Promise.promise();
        // Query
        final Bson query = and(
          eq("_id", calendarId),
          eq(Field.IS_DEFAULT, true)
        );

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::isDefaultCalendar]: an error has occurred while finding calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            boolean isDefault = !result.right().getValue().isEmpty();
            promise.complete(isDefault);
        }));
        return promise.future();
    }

    /**
     * Insert Default Calendar
     *
     * @param promise promise to complete or fail
     * @param defaultCalendar JsonObject containing the data for the default calendar
     */
    private void insertDefaultCalendar(Promise<JsonObject> promise, JsonObject defaultCalendar) {
        mongo.insert(this.collection, defaultCalendar, validActionResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::createDefaultCalendar]: an error has occurred while creating default calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            promise.complete(result.right().getValue());
        }));
    }

    public Future<Boolean> hasExternalCalendarId(List<String> calendarIds) {
        Promise<Boolean> promise = Promise.promise();

        this.list(calendarIds, true)
                .onSuccess(result -> {
                    boolean isExternal = !result.isEmpty();
                    promise.complete(isExternal);
                })
                .onFailure(err -> {
                    log.error("[Calendar@CalendarService::hasExternalCalendarId]: an error has occurred while checking external calendars: ",
                            err.getMessage());
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    public Future<Void> update(String calendarId, JsonObject body) {
        Promise<Void> promise = Promise.promise();
        // Query
        final Bson query = eq(Field._ID, calendarId);

        // Modifier
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attribute : body.fieldNames()) {
            modifier.set(attribute, body.getValue(attribute));
        }
        JsonObject now = MongoDb.now();
        modifier.set(Field.MODIFIED, now);

        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::update]: an error has occurred while updating calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
            } else {
                promise.complete();
            }
        }));

        return promise.future();
    }

    public Future<Boolean> checkBooleanField(String calendarId, String field) {
        Promise<Boolean> promise = Promise.promise();

        this.list(Collections.singletonList(calendarId))
                .onSuccess(result -> {
                    Boolean fieldValue = (result != null && !result.isEmpty()) ?
                            result.getJsonObject(0).getBoolean(field, null) : null;
                    promise.complete(fieldValue);
                })
                .onFailure(err -> {
                    log.error("[Calendar@CalendarService::checkBooleanField]: an error has occurred while checking calendar boolean field: ",
                            err.getMessage());
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    public Future<Void> delete(String calendarId) {
        Promise<Void> promise = Promise.promise();

        final Bson query = eq(Field._ID, calendarId);

        mongo.delete(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if (event.isLeft()) {
                String errMessage = String.format("[Calendar@%s::delete] An error has occurred while deleting a calendar %s",
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
    public Future<JsonObject> getPlatformCalendar(UserInfos user, String platform) {
        Promise<JsonObject> promise = Promise.promise();
        // Query
        final Bson query = and(
          eq("owner.userId", user.getUserId()),
          eq(Field.PLATFORM, platform)
        );

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@%s::getPlatformCalendar]: an error has occurred while finding platform calendar: ",
                        this.getClass().getSimpleName(), result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            promise.complete(result.right().getValue());
        }));
        return promise.future();
    }
}
