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

import fr.wseduc.rs.*;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.eventbus.EventBus;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.EventHelper;

import net.atos.entng.calendar.security.CustomWidgetFilter;
import net.atos.entng.calendar.security.ShareEventConf;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.storage.Storage;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.user.UserUtils;

import java.util.List;

public class EventController extends MongoDbControllerHelper {

    private final EventHelper eventHelper;
    private final CalendarService calendarService;
    private final Storage storage;

    public EventController(String collection, CrudService eventService, ServiceFactory serviceFactory, TimelineHelper timelineHelper,
                           Storage storage, EventBus eb, JsonObject config) {
        super(collection);
        this.calendarService = serviceFactory.calendarService();
        eventHelper = new EventHelper(collection, eventService, serviceFactory, timelineHelper, eb, config);
        this.storage = storage;
    }

    @Get("/:id/events")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getEvents(HttpServerRequest request) {
        eventHelper.list(request);
    }

    @Post("/:id/events")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    @Trace(Actions.CREATE_EVENT)
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
    @Trace(Actions.UPDATE_EVENT)
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
    @Trace(Actions.DELETE_EVENT)
    public void deleteEvent(HttpServerRequest request) {
        eventHelper.delete(request);
    }

    @Get("/calendarevent/share/json/:id")
    @ApiDoc("Share event by id.")
    @ResourceFilter(ShareEventConf.class)
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareEvent(final HttpServerRequest request) {
        shareJson(request, false);
    }

    @Put("/calendarevent/share/resource/:id")
    @ApiDoc("Share calendar by id.")
    @ResourceFilter(ShareEventConf.class)
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    @Trace(Actions.SHARE_EVENT)
    public void shareResource(final HttpServerRequest request) {
        String host = getHost(request);
        String lang = I18n.acceptLanguage(request);
        RequestUtils.bodyToJson(request, body ->
                UserUtils.getUserInfos(eb, request, user -> {
                    if (user != null) {
                        final String id = request.params().get("id");
                        if (id == null || id.trim().isEmpty()) {
                            badRequest(request, "invalid.id");
                            return;
                        }

                        eventHelper.isExternalCalendarEventImmutable(id)
                                .onSuccess(isExternal -> {
                                    if(Boolean.FALSE.equals(isExternal)) {
                                        JsonObject params = new JsonObject();
                                        params.put("profilUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType());
                                        params.put("username", user.getUsername());
                                        params.put("eventUri", "/calendar#/view/" + id);
                                        params.put("resourceUri", params.getString("eventUri"));

                                        JsonObject pushNotif = new JsonObject()
                                                .put("title", "push.notif.calendar.share")
                                                .put("body", user.getUsername() + " " + I18n.getInstance().translate("calendar.shared.push.notif.body",
                                                        getHost(request), I18n.acceptLanguage(request)));

                                        params.put("pushNotif", pushNotif);
                                        shareResource(request, "calendar.share", false, params, "title");
                                        eventHelper.addEventToUsersCalendar(id, body, user, host, lang);
                                    } else {
                                        unauthorized(request);
                                    }
                                })
                                .onFailure(err -> {
                                    renderError(request);
                                });
                    }
                }));
    }

    @Get("/:id/ical")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getIcal(HttpServerRequest request) {
        eventHelper.getIcal(request);
    }

    @Put("/:id/ical")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    @Trace(Actions.IMPORT_ICAL)
    public void importIcal(HttpServerRequest request) {
        eventHelper.importIcal(request);
    }

    @Get("calendarevent/:eventid/attachment/:attachmentid")
    @SecuredAction(value = "calendar.read", type = ActionType.AUTHENTICATED)
    public void getEventAttachment(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                String eventId = request.params().get("eventid");
                String attachmentid = request.params().get("attachmentid");
                eventHelper.hasAccessToEvent(eventId, user)
                        .compose(hasAccess -> eventHelper.getAttachment(hasAccess, attachmentid))
                        .onSuccess(file -> {
                            String fileId = file.getString(Field.file);
                            JsonObject fileMetadata = file.getJsonObject(Field.metadata);
                            String fileName = file.getString(Field.name);
                            storage.sendFile(fileId, fileName, request, false, fileMetadata);
                        })
                        .onFailure(err -> renderError(request));
            } else {
                log.error("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    /**
     * Get nb events from a list of calendars (widget)
     *
     * @param request request
     */
    @Get("/events/widget")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CustomWidgetFilter.class)
    public void getWidgetEvents(HttpServerRequest request) {
        List<String> calendarIds = request.params().getAll("calendarId");
        int nbLimit;
        try {
            nbLimit = Integer.parseInt(request.params().get("nb"));
        } catch (NumberFormatException e) {
            nbLimit = 5;
        }
        eventHelper.listWidgetEvents(request, calendarIds.toArray(new String[0]), nbLimit);
    }
}
