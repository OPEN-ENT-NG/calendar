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

package net.atos.entng.calendar.services;

import com.mongodb.QueryBuilder;
import io.vertx.core.Future;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

import java.util.Date;

public interface EventServiceMongo {

    void list(String calendarId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void list(String calendarId, UserInfos user,  String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void create(String calendarId, JsonObject body, UserInfos user, String collection, Handler<Either<String, JsonObject>> handler);

    void retrieve(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    Future<JsonObject> retrieve(String calendarId, String eventId, UserInfos user);

    void retrieveByIcsUid(String calendarId, String icsUid, UserInfos user, Handler<Either<String, JsonObject>> handler);

    Future<JsonArray> retrieveByCalendarId(String calendarId);

    void update(String calendarId, String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void update(String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void delete(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    Future<JsonObject> delete(String calendarId, String eventId, UserInfos user);

    Future<Void> deleteDatesAfterComparisonDate(String calendarId, String comparisonDate);

    void getIcal(String calendarId, UserInfos user, Handler<Message<JsonObject>> handler);
    
    void importIcal(String calendarId, String ics, UserInfos user, JsonObject requestInfo, Handler<Either<String, JsonObject>> handler);

    Future<JsonObject> importIcal(String calendarId, String ics, UserInfos user, JsonObject requestInfo, String collection);

    Future<JsonObject> importIcal(String calendarId, String ics, UserInfos user, JsonObject requestInfo, String collection, String action, String lastUpdate);


    void importIcal(String calendarId, String ics, UserInfos user, JsonObject requestInfo, String collection, String action, String lastUpdate, Handler<Either<String, JsonObject>> handler);

    void findOne(String Collection, QueryBuilder query, Handler<Either<String, JsonObject>> handler);

    /**
     * Gets a calendarEvent from its id
     * @param eventId the id of the event {@link String}
     * @return {@link JsonObject} the calendar event
     */
    Future<JsonObject> getCalendarEventById(String eventId);

    void getCalendarEventById(String eventId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get nbLimit events from calendars where startMoment is greater than now
     * @param calendars Calendars to extract events
     * @param nbLimit nb of events to extract
     * @param handler handler
     */
    void getEventsByCalendarAndDate(String[] calendars, int nbLimit,  Handler<Either<String, JsonArray>> handler);

}
