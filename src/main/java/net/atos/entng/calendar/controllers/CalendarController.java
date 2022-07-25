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
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.security.ShareEventConf;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.mongodb.MongoDbConf;
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

    public CalendarController(String collection, ServiceFactory serviceFactory) {
        super(collection);
        this.calendarService = serviceFactory.calendarService();
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Calendar.class.getSimpleName());
        this.eventHelper = new org.entcore.common.events.EventHelper(eventStore);
    }

    @Get("")
    @SecuredAction("calendar.view")
    public void view(HttpServerRequest request) {
        String host = getHost(request);
        String lang = I18n.acceptLanguage(request);
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final JsonObject context = new JsonObject();
                context.put(Field.ENABLERBS, config.getBoolean(Field.ENABLE_RBS, false));
                calendarService.getDefaultCalendar(user)
                        .onSuccess(calendar -> {
                            if (calendar.isEmpty() || calendar.fieldNames().isEmpty()) {
                                calendarService.createDefaultCalendar(user, host, lang)
                                        .onSuccess(res -> renderView(request, context))
                                        .onFailure(err -> renderError(request));
                            } else {
                                renderView(request, context);
                            }
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
                if (r.succeeded()) {
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
        String calendarId = request.params().get("id");
        calendarService.isDefaultCalendar(calendarId)
                .onSuccess(res -> {
                    if (Boolean.FALSE.equals(res)) {
                        delete(request);
                    } else {
                        forbidden(request, I18n.getInstance().translate("cannot.delete.default.calendar", getHost(request), I18n.acceptLanguage(request)));
                    }
                })
                .onFailure(err -> renderError(request));
    }

    @Get("/share/json/:id")
    @ApiDoc("Share calendar by id.")
    @ResourceFilter(ShareEventConf.class)
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareCalendar(final HttpServerRequest request) {
        shareJson(request, false);

        final MongoDbConf confEvent = MongoDbConf.getInstance();
        confEvent.setCollection(net.atos.entng.calendar.Calendar.CALENDAR_COLLECTION);
        confEvent.setResourceIdLabel("id");
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
    @ResourceFilter(ShareEventConf.class)
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareResource(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final String id = request.params().get("id");
                if (id == null || id.trim().isEmpty()) {
                    badRequest(request, "invalid.id");
                    return;
                }
                request.pause();
                calendarService.isDefaultCalendar(id)
                        .onSuccess(res -> {
                            request.resume();
                            if (Boolean.FALSE.equals(res)) {
                                JsonObject params = new JsonObject();
                                params.put(Field.PROFILURI, "/userbook/annuaire#" + user.getUserId() + "#" + user.getType());
                                params.put(Field.USERNAME, user.getUsername());
                                params.put(Field.CALENDARID, "/calendar#/view/" + id);
                                params.put(Field.RESOURCEURI, params.getString(Field.CALENDARID));

                                JsonObject pushNotif = new JsonObject()
                                        .put(Field.TITLE, "push.notif.calendar.share")
                                        .put(Field.BODY, user.getUsername() + " " + I18n.getInstance().translate("calendar.shared.push.notif.body",
                                                getHost(request), I18n.acceptLanguage(request)));

                                params.put(Field.PUSHNOTIF, pushNotif);
                                shareResource(request, "calendar.share", false, params, Field.TITLE);

                            } else {
                                unauthorized(request);
                            }
                        })
                        .onFailure(err -> {
                            request.resume();
                            renderError(request);
                        });
            } else {
                unauthorized(request);
            }
        });
    }

    private void proceedOnShare(HttpServerRequest request, UserInfos user) {
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


}
