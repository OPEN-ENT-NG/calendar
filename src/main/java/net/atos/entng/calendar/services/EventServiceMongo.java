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
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface EventServiceMongo {

    void list(String calendarId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void retrieve(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void retrieveByIcsUid(String calendarId, String icsUid, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void update(String calendarId, String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void delete(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void getIcal(String calendarId, UserInfos user, Handler<Message<JsonObject>> handler);
    
    void importIcal(String calendarId, String ics, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void findOne(String Collection, QueryBuilder query, Handler<Either<String, JsonObject>> handler);

    void getCalendarEventById(String eventId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get nbLimit events from calendars where startMoment is greater than now
     * @param calendars Calendars to extract events
     * @param nbLimit nb of events to extract
     * @param handler handler
     */
    void getEventsByCalendarAndDate(String[] calendars, int nbLimit,  Handler<Either<String, JsonArray>> handler);
}
