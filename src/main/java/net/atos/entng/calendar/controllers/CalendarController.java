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
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.Rights;
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.helpers.CalendarHelper;
import net.atos.entng.calendar.helpers.PlatformHelper;
import net.atos.entng.calendar.security.ShareEventConf;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.PlatformService;
import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

public class CalendarController extends MongoDbControllerHelper {
    static final String RESOURCE_NAME = "agenda";
    // Used for module "statistics"
    private final EventHelper eventHelper;
    private final CalendarService calendarService;

    private final CalendarHelper calendarHelper;
    private final PlatformService platformService;

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);
    }

    public CalendarController(String collection, ServiceFactory serviceFactory, EventBus eb, JsonObject config) {
        super(collection);
        this.calendarService = serviceFactory.calendarService();
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Calendar.class.getSimpleName());
        this.eventHelper = new org.entcore.common.events.EventHelper(eventStore);
        this.calendarHelper = new CalendarHelper(collection, serviceFactory, eb, config);
        this.platformService = serviceFactory.platformService();
    }

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        renderJson(request, config);
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

    @Get("/calendars/:id")
    @SecuredAction(Rights.GET)
    @Trace(Actions.GET_CALENDAR)
    public void getCalendar(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            final String id = request.params().get(Field.ID);
            if (id == null || id.trim().isEmpty()) {
                badRequest(request);
                return;
            }
            calendarService.list(Collections.singletonList(id), true, user.getUserId())
                    .onSuccess(result -> {
                        if (result != null && result.size() == 0) {
                            String message = String.format("[Calendar@%s::getCalendar] An error has occured :" +
                                    " could not retrieve calendar", this.getClass().getSimpleName());
                            log.error(message);
                            renderError(request);
                        } else {
                            Renders.renderJson(request, new JsonObject().put(Field.CALENDAR, result));
                        }
                    })
                    .onFailure(error -> {
                        String message = String.format("[Calendar@%s::getCalendar] An error has occured" +
                                " when getting calendar: %s", this.getClass().getSimpleName(), error.getMessage());
                        log.error(message, error.getMessage());
                        renderError(request);
                    });
        });
    }

    @Post("/calendars")
    @SecuredAction("calendar.create")
    @Trace(Actions.CREATE_CALENDAR)
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
    @Trace(Actions.UPDATE_CALENDAR)
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
    @Trace(Actions.DELETE_CALENDAR)
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

    @Post("/url")
    @SecuredAction(Rights.SYNC)
    @Trace(Actions.IMPORT_EXTERNAL_CALENDAR)
    public void importExternalCalendar(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            RequestUtils.bodyToJson(request, pathPrefix + "calendar", body -> {
                String url = body.getString(Field.ICSLINK).trim();
                platformService.retrieveAll()
                        .compose(platformList -> {
                            if (PlatformHelper.checkUrlInRegex(url, platformList)) {
                                return createFuture(user, body);
                            } else {
                                return Future.failedFuture("URL not authorized");
                            }
                        })
                        .compose(calendarId -> {
                            String host = getHost(request);
                            String i18nLang = I18n.acceptLanguage(request);
                            return calendarHelper.externalCalendarSync(calendarId.getString(Field._ID, null),
                                    user, host, i18nLang, ExternalICalEventBusActions.POST.method());
                        })
                        .onSuccess(result -> {
                            Renders.ok(request);
                        })
                        .onFailure(error -> {
                            String message = String.format("[Calendar@%s::importExternalCalendar] An error has occured" +
                                    " during calendar sync: %s", this.getClass().getSimpleName(), error.getMessage());
                            log.error(message, error.getMessage());
                            if (error.getMessage().equals("URL not authorized")) {
                                renderError(request, new JsonObject().put(Field.MESSAGE, error.getMessage()));
                            } else {
                                renderError(request);
                            }
                        });
            });
        });
    }

    Future<JsonObject> createFuture(UserInfos user, JsonObject body) {
        Promise<JsonObject> promise = Promise.promise();

        crudService.create(body, user, r -> {
            if (r.isLeft()) {
                String message = String.format("[Calendar@%s::createFuture] An error has occured" +
                        " during calendar creation: %s", this.getClass().getSimpleName(), r.left().getValue());
                log.error(message, r.left().getValue());
                promise.fail(message);
            } else {
                promise.complete(r.right().getValue());
            }
        });

        return promise.future();
    }

    @Put("/:id/url")
    @SecuredAction(Rights.UPDATE)
    @Trace(Actions.SYNC_EXTERNAL_CALENDAR)
    public void syncExternalCalendar(final HttpServerRequest request) {
        String calendarId = request.params().get(Field.ID);
        if (calendarId == null) {
            badRequest(request);
            return;
        }
        UserUtils.getUserInfos(eb, request, user -> {
            if (user == null) {
                unauthorized(request);
                return;
            }
            String host = getHost(request);
            String i18nLang = I18n.acceptLanguage(request);
            calendarHelper.externalCalendarSync(calendarId,
                            user, host, i18nLang, ExternalICalEventBusActions.PUT.method())
                    .onSuccess(result -> {
                        Renders.ok(request);
                    })
                    .onFailure(error -> {
                        String message = String.format("[Calendar@%s::syncExternalCalendar] An error has occured" +
                                " during calendar sync: %s", this.getClass().getSimpleName(), error.getMessage());
                        log.error(message, error.getMessage());
                        log.info(error.getMessage().equals("last update was too recent"));
                        if(error.getMessage().equals("last update was too recent")) {
                            renderError(request, new JsonObject().put(Field.MESSAGE, error.getMessage()));
                        } else {
                            renderError(request);
                        }
                    });
        });

    }

    @Get("/:id/url")
    @SecuredAction(Rights.CHECKUPDATE)
    @Trace(Actions.CHECK_EXTERNAL_CALENDAR)
    public void checkSyncExternalCalendar(final HttpServerRequest request) {
        String calendarId = request.params().get(Field.ID);
        if (calendarId == null) {
            badRequest(request);
            return;
        }
        calendarService.checkBooleanField(calendarId, Field.ISUPDATING)
                .onSuccess(result -> {
                    Renders.renderJson(request, new JsonObject().put(Field.ISUPDATING, result));
                })
                .onFailure(err -> {
                    log.error("[Calendar@CalendarController::checkSyncExternalCalendar]: an error has occurred while checking calendar: ",
                            err.getMessage());
                    Renders.renderError(request);
                });
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
    @Trace(Actions.SHARE_CALENDAR_SUBMIT)
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
    @Trace(Actions.SHARE_CALENDAR_REMOVE)
    public void removeShareCalendar(final HttpServerRequest request) {
        removeShare(request, false);
    }

    @Put("/share/resource/:id")
    @ApiDoc("Share calendar by id.")
    @ResourceFilter(ShareEventConf.class)
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    @Trace(Actions.SHARE_CALENDAR)
    public void shareResource(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            if (user != null) {
                final String id = request.params().get("id");
                if (id == null || id.trim().isEmpty()) {
                    badRequest(request, "invalid.id");
                    return;
                }
                request.pause();
                calendarService.hasExternalCalendarId(Collections.singletonList(id))
                        .onSuccess(isExternal -> {
                            if(Boolean.FALSE.equals(isExternal)) {
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
                        })
                        .onFailure(err -> {
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
