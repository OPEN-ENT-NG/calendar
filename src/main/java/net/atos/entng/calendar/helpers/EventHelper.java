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

package net.atos.entng.calendar.helpers;

import static net.atos.entng.calendar.Calendar.CALENDAR_NAME;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

import com.mongodb.QueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.Joiner;
import net.atos.entng.calendar.services.EventServiceMongo;

import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.common.utils.Config;
import org.entcore.common.utils.DateUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

public class EventHelper extends MongoDbControllerHelper {

    private static final String EVENT_CREATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_CREATED";
    private static final String EVENT_UPDATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_UPDATED";

    private static final String CALENDAR_ID_PARAMETER = "id";

    private Neo4j neo4j = Neo4j.getInstance();

    private static final String EVENT_ID_PARAMETER = "eventid";

    private final EventServiceMongo eventService;

    private final TimelineHelper notification;

    public EventHelper(String collection, CrudService eventService, TimelineHelper timelineHelper) {
        super(collection, null);
        this.eventService = (EventServiceMongo) eventService;
        this.crudService = eventService;
        notification = timelineHelper;
    }

    @Override
    public void list(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                eventService.list(calendarId, user, arrayResponseHandler(request));
            }
        });

    }

    @Override
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                if (user != null) {
                    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject object) {
                            eventService.create(calendarId, object, user, new Handler<Either<String, JsonObject>>() {
                                public void handle(Either<String, JsonObject> event) {
                                    if (event.isRight()) {
                                        JsonObject eventId = event.right().getValue();
                                        final JsonObject message = new JsonObject();
                                        message.putString("id", calendarId);
                                        message.putString("eventId", eventId.getString("_id"));
                                        message.putString("start_date", null);
                                        message.putString("end_date", null);
                                        notifyEventCreatedOrUpdated(request, user, message, true);
                                        renderJson(request, event.right().getValue(), 200);
                                    } else if (event.isLeft()) {
                                        log.error("Error when getting notification informations.");
                                    }
                                }
                            });
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Override
    public void update(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject object) {
                            final String eventId = request.params().get(EVENT_ID_PARAMETER);
                            final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                            crudService.update(eventId, object, user, new Handler<Either<String, JsonObject>>() {
                                public void handle(Either<String, JsonObject> event) {
                                    if (event.isRight()) {
                                        final JsonObject message = new JsonObject();
                                        message.putString("id", calendarId);
                                        message.putString("eventId", eventId);
                                        message.putString("start_date", null);
                                        message.putString("end_date", null);
                                        notifyEventCreatedOrUpdated(request, user, message, false);
                                        renderJson(request, event.right().getValue(), 200);
                                    } else if (event.isLeft()) {
                                        log.error("Error when getting notification informations.");
                                    }
                                }
                            });
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Override
    public void retrieve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                String eventId = request.params().get(EVENT_ID_PARAMETER);

                eventService.retrieve(calendarId, eventId, user, notEmptyResponseHandler(request));
            }
        });
    }

    @Override
    public void delete(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                final String eventId = request.params().get(EVENT_ID_PARAMETER);

                eventService.delete(calendarId, eventId, user, defaultResponseHandler(request));
            }
        });
    }


    public void getIcal(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                eventService.getIcal(calendarId, user, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> reply) {
                        log.warn("Handling response");
                        JsonObject response = reply.body();
                        String content = response.getString("ics");
                        try {
                            File f = File.createTempFile(calendarId, "ics");
                            Files.write(Paths.get(f.getAbsolutePath()), content.getBytes());
                            request.response().putHeader("Content-Type", "text/calendar");
                            request.response().putHeader("Content-disposition", "attachment; filename=\"" + calendarId + ".ics\"");

                            request.response().sendFile(f.getAbsolutePath());

                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


    public void importIcal(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject object) {
                        String icsContent = object.getString("ics");
                        eventService.importIcal(calendarId, icsContent, user, defaultResponseHandler(request));
                    }
                });

            }
        });
    }

    /**
     * Notify moderators that an event has been created or updated
     */
    private void notifyEventCreatedOrUpdated(final HttpServerRequest request, final UserInfos user, final JsonObject message, final boolean isCreated) {

        final String calendarId = message.getString("id", null);
        final String eventId = message.getString("eventId", null);
        final String startDate = message.getString("start_date", null);
        final String endDate = message.getString("end_date", null);

        final String eventType = isCreated ? EVENT_CREATED_EVENT_TYPE : EVENT_UPDATED_EVENT_TYPE;

        if (calendarId == null || eventId == null /*|| startDate == null || endDate == null*/) {
            log.error("Could not get eventId, start_date or end_date from response. Unable to send timeline " + eventType + " notification.");
            return;
        }

        // get calendarEvent
        eventService.getCalendarEventById(eventId, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if( event.isRight()) {
                    // notify users
                    JsonObject calendarEvent = event.right().getValue();
                    notifyUsersSharing(request, user, calendarId, calendarEvent, isCreated);
                }
            }
        });
    }

    /**
     *
     * @param request
     * @param user
     * @param calendarId
     * @param calendarEvent
     */
    public void notifyUsersSharing(final HttpServerRequest request, final UserInfos user, final String calendarId, final JsonObject calendarEvent, final boolean isCreated ){
        QueryBuilder query = QueryBuilder.start("_id").is(calendarId);
        JsonObject keys = new JsonObject().putNumber("calendar", 1);
        JsonArray fetch = new JsonArray().addString("shared");

        findRecipiants("calendar", query, keys, fetch, user, new Handler<Map<String, Object>>() {
            @Override
            public void handle(Map<String, Object> event) {
                if (event != null) {
                    List<String> recipients = (List<String>) event.get("recipients");
                    String calendarTitle = (String) event.get("calendarTitle");
                    if (recipients != null) {
                        String template = isCreated ? "calendar.create" : "calendar.update";

                        Date startDate = null;
                        Date endDate = null;
                        try {
                            startDate = DateUtils.parseTimestampWithoutTimezone(calendarEvent.getString("startMoment"));
                            endDate = DateUtils.parseTimestampWithoutTimezone(calendarEvent.getString("endMoment"));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        JsonObject p = new JsonObject()
                                .putString("uri", Config.getInstance().getConfig().getString("host", "http://localhost:8090") +
                                        "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                                .putString("username", user.getUsername())
                                .putString("CalendarTitle", calendarTitle)
                                .putString("postTitle", calendarEvent.getString("title"))
                                .putString("profilUri", Config.getInstance().getConfig().getString("host", "http://localhost:8090") +
                                "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                                .putString("calendarUri", Config.getInstance().getConfig().getString("host", "http://localhost:8090") +
                                        "/calendar#/view/" + calendarId)
                                .putString("startMoment", DateUtils.format(startDate, "dd/MM/yyyy HH:mm"))
                                .putString("endMoment", DateUtils.format(endDate, "dd/MM/yyyy HH:mm"))
                                .putString("eventTitle", calendarEvent.getString("title"));
                        notification.notifyTimeline(request, template, user, recipients, calendarId, calendarEvent.getString("id"), p);
                    }
                }
            }
        });
    }

    private void findRecipiants(String collection, QueryBuilder query, JsonObject keys,
                                final JsonArray fetch, final UserInfos user, final Handler<Map<String, Object>> handler) {
        findRecipiants(collection, query, keys, fetch, null, user, handler);
    }
    private void findRecipiants(String collection, QueryBuilder query, JsonObject keys,
                                final JsonArray fetch, final String filterRights, final UserInfos user,
                                final Handler<Map<String, Object>> handler) {
        // getting the calendar id
        eventService.findOne(collection, query, new Handler<Either<String, JsonObject>>() {
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    final JsonObject calendar = event.right().getValue();
                    JsonArray shared = calendar.getArray("shared", new JsonObject().getArray("groupId")); //.getObject("calendar", new JsonObject()).getArray("shared");
                    if (shared != null) {
                        List<String> shareIds = getSharedIds(shared, filterRights);
                        if (!shareIds.isEmpty()) {
                            Map<String, Object> params = new HashMap<>();
                            params.put("userId", user.getUserId());
                            neo4j.execute(getNeoQuery(shareIds), params, new Handler<Message<JsonObject>>() {
                                @Override
                                public void handle(Message<JsonObject> res) {
                                    if ("ok".equals(res.body().getString("status"))) {
                                        JsonArray listOfUsers = res.body().getArray("result");
                                        List<String> recipients = new ArrayList<>();
                                        for (Object attr : listOfUsers ) {
                                            JsonObject obj = (JsonObject) attr;
                                            String id = obj.getString("id");
                                            if (id != null) {
                                                recipients.add(id);
                                            }
                                        }
                                        Map<String, Object> t = new HashMap<>();
                                        t.put("recipients", recipients);
                                        t.put("calendarTitle", calendar.getString("title"));
                                        handler.handle(t);
                                    } else {
                                        handler.handle(null);
                                    }
                                }
                            }); // end neo4j.execute
                        } // end if (!shareIds.isEmpty())
                        else {
                            handler.handle(null);
                        }
                    } // end if shared != null
                } // end  if (event.isRight())
            } // end handle
        });
    }

    private List<String> getSharedIds(JsonArray shared){
        return getSharedIds(shared, null);
    }
    private List<String> getSharedIds(JsonArray shared, String filterRights) {
        List<String> shareIds = new ArrayList<>();
        for (Object o : shared) {
            if (!(o instanceof JsonObject)) continue;
            JsonObject userShared = (JsonObject) o;

            if(filterRights != null && !userShared.getBoolean(filterRights, false))
                continue;

            String userOrGroupId = userShared.getString("groupId",
                    userShared.getString("userId"));
            if (userOrGroupId != null && !userOrGroupId.trim().isEmpty()) {
                shareIds.add(userOrGroupId);
            }
        }
        return shareIds;
    }

    private String getNeoQuery(List<String> shareIds) {
        String query =
                "MATCH (u:User) " +
                "WHERE u.id IN ['" +
                Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
                "RETURN distinct u.id as id"

                + " UNION " +

                "MATCH (n:ProfileGroup )<-[:IN]-(u:User) " +
                "WHERE n.id IN ['" +
                Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
                "RETURN distinct u.id as id"

                + " UNION " +

                "MATCH (n:ManualGroup )<-[:IN]-(u:User) " +
                "WHERE n.id IN ['" +
                Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
                "RETURN distinct u.id as id"

                + " UNION " +

                "MATCH (n:CommunityGroup )<-[:IN]-(u:User) " +
                "WHERE n.id IN ['" +
                Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
                "RETURN distinct u.id as id ";

        return query;
    }

    public void listWidgetEvents(final HttpServerRequest request, final String[] calendarIds, final int nbLimit) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                eventService.getEventsByCalendarAndDate(calendarIds, nbLimit, arrayResponseHandler(request));
            }
        });
    }

}
