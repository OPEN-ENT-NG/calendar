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

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.user.UserInfos;
import io.vertx.core.json.JsonObject;

public interface CalendarService {

    /**
     * Has Default Calendar
     *
     * @param user User Object containing user id
     * @return Future {@link Future<Boolean>} expressing if user already has a default calendar
     */
    Future<Boolean> hasDefaultCalendar(UserInfos user);

    /**
     * Create Default Calendar
     *
     * @param exists Boolean expressing if user already has a default calendar
     * @param user User Object containing user id and displayed name
     * @param request HTTP Server Request
     * @return Future {@link Future<JsonObject>} containing newly created default calendar or empty
     */
    Future<JsonObject> createDefaultCalendar(Boolean exists, UserInfos user, HttpServerRequest request);

    /**
     * Is Default Calendar
     *
     * @param calendarId String with the id of the calendar requested to be deleted
     * @return Future {@link Future<Boolean>} telling if calendar can be deleted or not
     */
    Future<Boolean> isDefaultCalendar(String calendarId);

}
