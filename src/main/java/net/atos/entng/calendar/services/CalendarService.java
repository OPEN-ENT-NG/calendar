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
import io.vertx.core.json.JsonArray;
import net.atos.entng.calendar.models.CalendarModel;
import org.entcore.common.user.UserInfos;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface CalendarService {


    Future<JsonArray> list(List<String> calendarIds);

    /**
     * Returns external calendars from id list
     * @param calendarIds the calendar ids {@link List<String>}
     * @param isExternal whether we want the calendar to be external or not {@link Boolean}
     * @return {@link Future<JsonArray>} the calendars that fit the criteria
     */
    Future<JsonArray> list(List<String> calendarIds, Boolean isExternal);

    /**
     * Returns external calendars from id list
     * @param calendarIds the calendar ids {@link List<String>}
     * @param isExternal whether we want the calendar to be external or not {@link Boolean}
     * @param userId the user {@link String}
     * @return {@link Future<JsonArray>} the calendars that fit the criteria
     */
    Future<JsonArray> list(List<String> calendarIds, Boolean isExternal, String userId);

    /**
     * Get Default Calendar
     *
     * @param user {@link UserInfos}
     * @return FutureObject containing calendar {@link JsonObject}
     */
    Future<JsonObject> getDefaultCalendar(UserInfos user);


    /**
     * Create Default Calendar
     *
     * @param user User Object containing user id and displayed name
     * @param host domain host
     * @param lang accepted lang
     * @return Future {@link Future<JsonObject>} containing newly created default calendar or empty
     */
    Future<JsonObject> createDefaultCalendar(UserInfos user, String host, String lang);

    /**
     * Is Default Calendar
     *
     * @param calendarId String with the id of the calendar requested to be deleted
     * @return Future {@link Future<Boolean>} telling if calendar can be deleted or not
     */
    Future<Boolean> isDefaultCalendar(String calendarId);

    /**
     * Array Contains External Calendar
     *
     * @param calendarIds ids of the checked calendars {@link List<String>}
     * @return Future {@link Future<Boolean>} telling if at least one calendar is external
     */
    Future<Boolean> hasExternalCalendarId(List<String> calendarIds);

    /**
     * Updates calendar
     *
     * @param calendarId id of the calendar {@link String}
     * @param body the fields to change {@link JsonObject}
     * @return Future {@link Future<Void>} the updated calendar
     */
    Future<Void> update(String calendarId, JsonObject body);

    /**
     * Checks the value of a boolean field in the calendar
     *
     * @param calendarId id of the calendar {@link String}
     * @param field the field to check {@link String}
     * @return Future {@link Future<Boolean>} the field value
     */
    Future<Boolean> checkBooleanField(String calendarId, String field);

    /**
     * Delete a calendar
     * @param calendarId the id of the calendar we want to delete
     * @return {@link Future<Void>} Future response
     */
    Future<Void> delete(String calendarId);

    /**
     * Find platform calendar of a user
     * @param user the user {@link UserInfos}
     * @param platform the platform {@link String}
     * @return {@link Future<CalendarModel>} The calendar
     */
    Future<JsonObject> getPlatformCalendar(UserInfos user, String platform);

    }
