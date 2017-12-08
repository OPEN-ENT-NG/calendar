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

package net.atos.entng.calendar.controllers;

import net.atos.entng.calendar.helpers.EventHelper;

import net.atos.entng.calendar.security.CustomWidgetFilter;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;

import java.util.List;

public class EventController extends BaseController {

    private final EventHelper eventHelper;

    public EventController(String collection, CrudService eventService, TimelineHelper timelineHelper) {
        eventHelper = new EventHelper(collection, eventService, timelineHelper);
    }

    @Get("/:id/events")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getEvents(HttpServerRequest request) {
        eventHelper.list(request);
    }

    @Post("/:id/events")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void createEvent(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                eventHelper.create(request);
            }
        });
    }

    @Get("/:id/event/:eventid")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getEvent(HttpServerRequest request) {
        eventHelper.retrieve(request);
    }

    @Put("/:id/event/:eventid")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void updateEvent(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                eventHelper.update(request);
            }
        });
    }

    @Delete("/:id/event/:eventid")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void deleteEvent(HttpServerRequest request) {
        eventHelper.delete(request);
    }

    @Get("/:id/ical")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getIcal(HttpServerRequest request) {
        eventHelper.getIcal(request);
    }

    @Put("/:id/ical")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void importIcal(HttpServerRequest request) {
        eventHelper.importIcal(request);

    }

    /**
     * Get nb events from a list of calendars (widget)
     * @param request request
     */
    @Get("/widget/events")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CustomWidgetFilter.class)
    public void getWidgetEvents (HttpServerRequest request) {
        List<String> calendarIds = request.params().getAll("calendarId");
        int nbLimit;
        try {
            nbLimit = Integer.parseInt(request.params().get("nb"));
        } catch(NumberFormatException e) {
            nbLimit = 5;
        }
        eventHelper.listWidgetEvents(request, calendarIds.toArray(new String[0]), nbLimit);
    }
}
