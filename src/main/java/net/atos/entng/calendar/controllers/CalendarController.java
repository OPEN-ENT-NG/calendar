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
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Calendar;
import java.util.Map;

public class CalendarController extends MongoDbControllerHelper {
    static final String RESOURCE_NAME = "agenda";
    // Used for module "statistics"
    private final EventHelper eventHelper;
    private final CalendarService calendarService;

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
    }

    public CalendarController(String collection, CalendarService calendarService) {
        super(collection);
        this.calendarService = calendarService;
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Calendar.class.getSimpleName());
        this.eventHelper = new org.entcore.common.events.EventHelper(eventStore);
    }

    @Get("")
    @SecuredAction("calendar.view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                calendarService.hasDefaultCalendar(user)
                        .compose(res -> calendarService.createDefaultCalendar(res, user, request))
                        .onSuccess(res -> {
                            //renderview when process checkIfDefaultExists
                            renderView(request);
                            // Create event "access to application Calendar" and store it, for module "statistics"
                            eventHelper.onAccess(request);
                        })
                        .onFailure(err -> renderError(request));
            }
        });
    }

    @Get("/calendars")
    @SecuredAction("calendar.view")
    public void listCalendars(HttpServerRequest request) {
        list(request);
    }

    @Post("/calendars")
    @SecuredAction("calendar.create")
    public void createCalendar(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "calendar", object -> {
            super.create(request, r -> {
                if(r.succeeded()){
                    eventHelper.onCreateResource(request, RESOURCE_NAME);
                }
            });
        });
    }

    @Put("/:id")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void updateCalendar(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "calendar", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                update(request);
            }
        });
    }

    @Delete("/:id")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void deleteCalendar(HttpServerRequest request) {
        delete(request);
    }

    @Get("/share/json/:id")
    @ApiDoc("Share calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareCalendar(final HttpServerRequest request) {
        shareJson(request, false);
    }

    @Put("/share/json/:id")
    @ApiDoc("Share calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareCalendarSubmit(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final String id = request.params().get("id");
                if (id == null || id.trim().isEmpty()) {
                    badRequest(request);
                    return;
                }

                JsonObject params = new JsonObject();
                params.put("profilUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType());
                params.put("username", user.getUsername());
                params.put("calendarUri", "/calendar#/view/" + id);
                params.put("resourceUri", params.getString("calendarUri"));
                JsonObject pushNotif = new JsonObject()
                        .put("title", "push.notif.calendar.share")
                        .put("body", user.getUsername() + " " + I18n.getInstance().translate("calendar.shared.push.notif.body",
                                getHost(request), I18n.acceptLanguage(request)));

                params.put("pushNotif", pushNotif);
                shareJsonSubmit(request, "calendar.share", false, params, "title");
            }
        });
    }

    @Put("/share/remove/:id")
    @ApiDoc("Remove calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void removeShareCalendar(final HttpServerRequest request) {
        removeShare(request, false);
    }

    @Put("/share/resource/:id")
    @ApiDoc("Share calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareResource(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final String id = request.params().get("id");
                    if (id == null || id.trim().isEmpty()) {
                        badRequest(request, "invalid.id");
                        return;
                    }

                    JsonObject params = new JsonObject();
                    params.put("profilUri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType());
                    params.put("username", user.getUsername());
                    params.put("calendarUri", "/calendar#/view/" + id);
                    params.put("resourceUri", params.getString("calendarUri"));

                    JsonObject pushNotif = new JsonObject()
                            .put("title", "push.notif.calendar.share")
                            .put("body", user.getUsername() + " " + I18n.getInstance().translate("calendar.shared.push.notif.body",
                                    getHost(request), I18n.acceptLanguage(request)));

                    params.put("pushNotif", pushNotif);

                    shareResource(request, "calendar.share", false, params, "title");
                }
            }
        });
    }


}
