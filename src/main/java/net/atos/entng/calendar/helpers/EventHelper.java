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

import static net.atos.entng.calendar.Calendar.*;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mongodb.QueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.models.User;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;

import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.UserService;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

public class EventHelper extends MongoDbControllerHelper {

    private static final String EVENT_CREATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_CREATED";
    private static final String EVENT_UPDATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_UPDATED";

    private static final String CALENDAR_ID_PARAMETER = "id";

    private Neo4j neo4j = Neo4j.getInstance();
    static final String RESOURCE_NAME = "agenda_event";
    private static final String EVENT_ID_PARAMETER = "eventid";

    private final EventServiceMongo eventService;
    private final CalendarService calendarService;
    private final UserService userService;

    private final TimelineHelper notification;
    private final org.entcore.common.events.EventHelper eventHelper;

    public EventHelper(String collection, CrudService eventService, ServiceFactory serviceFactory, TimelineHelper timelineHelper) {
        super(collection, null);
        this.eventService = (EventServiceMongo) eventService;
        this.crudService = eventService;
        this.calendarService = serviceFactory.calendarService();
        this.userService = serviceFactory.userService();
        notification = timelineHelper;
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Calendar.class.getSimpleName());
        this.eventHelper = new org.entcore.common.events.EventHelper(eventStore);
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
                            if (object.getString("notifStartMoment").substring(0,10).equals(object.getString("notifEndMoment").substring(0,10))) {
                                eventService.create(calendarId, object, user, new Handler<Either<String, JsonObject>>() {
                                    public void handle(Either<String, JsonObject> event) {
                                        if (event.isRight()) {
                                            JsonObject eventId = event.right().getValue();
                                            final JsonObject message = new JsonObject();
                                            message.put("id", calendarId);
                                            message.put("eventId", eventId.getString("_id"));
                                            message.put("start_date", (String) null);
                                            message.put("end_date", (String) null);
                                            message.put("sendNotif", object.containsKey("sendNotif")? object.getBoolean("sendNotif"):null);
                                            notifyEventCreatedOrUpdated(request, user, message, true);
                                            renderJson(request, event.right().getValue(), 200);
                                            eventHelper.onCreateResource(request, RESOURCE_NAME);
                                        } else if (event.isLeft()) {
                                            log.error("Error when getting notification informations.");
                                        }
                                    }
                                });
                            }else{
                                log.error("The beginning and end date of the event are not the same");
                                Renders.unauthorized(request);
                            }
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
                                        message.put("id", calendarId);
                                        message.put("eventId", eventId);
                                        message.put("start_date", (String) null);
                                        message.put("end_date", (String) null);
                                        message.put("sendNotif", object.containsKey("sendNotif")? object.getBoolean("sendNotif"):null);
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
                if (event.isRight()) {
                    JsonObject calendarEvent = event.right().getValue();
                    JsonArray calendar = calendarEvent.getJsonArray("calendar", new JsonArray());

                    Boolean restrictedEvent = (calendarEvent.containsKey("shared") || Boolean.TRUE.equals(calendarEvent.getBoolean("is_default")))
                            && Boolean.FALSE.equals(calendarEvent.getJsonArray("shared").isEmpty());
                    if(!calendar.isEmpty()){
                        for(Object id : calendar){
                            if(message.getBoolean("sendNotif") == null || Boolean.FALSE.equals(isCreated)) {
                                notifyUsersSharing(request, user, id.toString(), calendarEvent, isCreated, restrictedEvent);
                            }
                        }
                    }

                }

            }
        });
    }


    /**
     *
     * @param request HttpServerRequest request from the server
     * @param user User Object user that created/edited the event
     * @param calendarId JsonObject calendar in which the event appears
     * @param calendarEvent JsonObject event that is created/edited
     * @param restrictedEvent Boolean that tells if event is restricted or not
     */
    public void notifyUsersSharing(final HttpServerRequest request, final UserInfos user, final String calendarId,
                                   final JsonObject calendarEvent, final boolean isCreated, Boolean restrictedEvent) {
        String collection;
        QueryBuilder query;
        JsonObject keys;
        JsonArray fetch;

        collection = CALENDAR_COLLECTION;
        query = QueryBuilder.start("_id").is(calendarId);
        keys = new JsonObject().put("calendar", 1);
        fetch = new JsonArray().add("shared");


        findRecipiants(collection, query, keys, fetch, user, restrictedEvent, calendarEvent, event ->
                sendNotificationToRecipients(event, isCreated, user, calendarEvent, calendarId, request));
    }

    @SuppressWarnings("unchecked")
    private void sendNotificationToRecipients(Map<String, Object> event, boolean isCreated, UserInfos user, JsonObject calendarEvent, String calendarId, HttpServerRequest request) {
        if (event != null) {
            List<String> recipients = (List<String>) event.get("recipients");
            String calendarTitle = (String) event.get("calendarTitle");
            if (recipients != null) {
                String template = isCreated ? "calendar.create" : "calendar.update";

                JsonObject p = new JsonObject()
                        .put("uri",
                                "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                        .put("username", user.getUsername())
                        .put("CalendarTitle", calendarTitle)
                        .put("postTitle", calendarEvent.getString("title"))
                        .put("profilUri",
                                "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                        .put("calendarUri",
                                "/calendar#/view/" + calendarId)
                        .put("resourceUri", "/calendar#/view/" + calendarId)
                        .put("startMoment", calendarEvent.getString("notifStartMoment"))
                        .put("endMoment", calendarEvent.getString("notifEndMoment"))
                        .put("eventTitle", calendarEvent.getString("title"));
                JsonObject pushNotif = new JsonObject()
                        .put("title", isCreated ? "push.notif.event.created" : "push.notif.event.updated")
                        .put("body", user.getUsername() + " " + I18n.getInstance().translate(
                                isCreated ? "calendar.event.created.push.notif.body" : "calendar.event.updated.push.notif.body",
                                getHost(request), I18n.acceptLanguage(request)
                        ) + " " + calendarEvent.getString("title"));

                p.put("pushNotif", pushNotif);
                notification.notifyTimeline(request, template, user, recipients, calendarId, calendarEvent.getString("id"),
                        p, true);
            }
        }
    }

    private void findRecipiants(String collection, QueryBuilder query, JsonObject keys, final JsonArray fetch,
                                final UserInfos user, Boolean restrictedEvent, JsonObject calendarEvent, Handler<Map<String, Object>> handler) {
        findRecipiants(collection, query, keys, fetch, null, user, restrictedEvent, calendarEvent, handler);

    }

    @SuppressWarnings("unhandle")
    private void findRecipiants(String collection, QueryBuilder query, JsonObject keys, final JsonArray fetch,
                                final String filterRights, final UserInfos user, Boolean restrictedEvent, JsonObject calendarEvent,
                                final Handler<Map<String, Object>> handler) {
        // getting the calendar id
        // end handle
        eventService.findOne(collection, query, event -> {
            if (event.isRight()) {
                final JsonObject calendar = event.right().getValue();
                JsonArray shared = calendar.getJsonArray("shared", new JsonObject().getJsonArray("groupId")); //.getJsonObject("calendar", new JsonObject()).getArray("shared");
                if (shared != null) {
                    List<String> shareIds = getSharedIds(shared, filterRights);
                    if (!shareIds.isEmpty()) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("userId", user.getUserId());
                        neo4j.execute(getNeoQuery(shareIds), params, res -> {
                            if ("ok".equals(res.body().getString("status"))) {
                                JsonArray listOfUsers = res.body().getJsonArray("result");
                                // param => rajouter la logique
                                if (restrictedEvent){
                                    restrictListOfUsers(listOfUsers, user, calendar, calendarEvent, handler);
                                } else {
                                    proceedOnUserList(listOfUsers, calendar, handler);
                                }
                            } else {
                                handler.handle(null);
                            }
                        }); // end neo4j.execute
                    } // end if (!shareIds.isEmpty())
                    else {
                        handler.handle(null);
                    }
                } // end if shared != null
                if(shared == null && Boolean.TRUE.equals(calendar.getBoolean("is_default")) && restrictedEvent){
                    JsonArray defaultCalendarOwner = new JsonArray().add(new JsonObject().put("id", calendar.getJsonObject("owner", new JsonObject())
                            .getString("userId", null)));
                    proceedOnUserList(defaultCalendarOwner, calendar, handler);
                }
            } // end  if (event.isRight())
        });
    }

    /**
     * Prepare notification user list so that it contains only the people the event is shared with and that have access to the calendar,
     * and the calendar owner and the event owner if they are different from the person editing
     *
     * @param listOfUsers JsonArray of the ids of the users with access to the calendar
     * @param user User Object, the user that edited the event
     * @param calendar JsonObject, the targeted calendar
     * @param calendarEvent JsonObject the edited event
     * @param handler Handler used by notifyUsersSharing()
     */
    @SuppressWarnings("unchecked")
    private void restrictListOfUsers(JsonArray listOfUsers, final UserInfos user, JsonObject calendar, JsonObject calendarEvent,
                                     final Handler<Map<String, Object>> handler) {

        //make array with userIds & groupIds from event
        List<String> calendarEventShared = getSharedIds(calendarEvent.getJsonArray("shared", new JsonArray()));

        //get all userIds from groups for event
        userService.fetchUser(calendarEventShared, user)
            .onSuccess(e -> {
                List<String> calendarEventShareIds = e.stream().map(User::id).collect(Collectors.toList());

                //keep ids from shareIds that appear in calendarEventShareIds
                List<String> userIds = ((List<JsonObject>) listOfUsers.getList())
                        .stream()
                        .map((currentUser) -> currentUser.getString("id"))
                        .collect(Collectors.toList());
                userIds.retainAll(calendarEventShareIds);

                if (!user.getUserId().equals(calendar.getJsonObject("owner").getString("userId"))) {
                    userIds.add(calendar.getJsonObject("owner").getString("userId"));
                }
                if (!user.getUserId().equals(calendarEvent.getJsonObject("owner").getString("userId"))){
                    userIds.add(calendarEvent.getJsonObject("owner").getString("userId"));
                }

                JsonArray finalUserIds = new JsonArray(userIds.stream()
                        .map((currentUser) -> new JsonObject().put("id", currentUser))
                        .collect(Collectors.toList()));

                proceedOnUserList(finalUserIds, calendar, handler);
            })
            .onFailure( err -> {
                String message = String.format("[Calendar@%s::restrictListOfUsers] An error has occured" +
                                " during fetching userIds, see previous logs: %s",
                        this.getClass().getSimpleName(), err.getMessage());
                log.error(message, err.getMessage());
            });
    }

    /**
     * Prepare information to notify users
     *
     * @param listOfUsers JsonArray of the users (ids) that should be notified
     * @param calendar JsonObject of the current calendar
     * @param handler Handler used by notifyUsersSharing()
     */
    private void proceedOnUserList(JsonArray listOfUsers, JsonObject calendar, Handler<Map<String, Object>> handler) {
        List<String> recipients = new ArrayList<>();
        for (Object attr : listOfUsers) {
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
        return "MATCH (u:User) " +
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
                "RETURN distinct u.id as id"

                + " UNION " +

                "MATCH (n:Group )<-[:IN]-(u:User) " +
                "WHERE n.id IN ['" +
                Joiner.on("','").join(shareIds) + "'] AND u.id <> {userId} " +
                "RETURN distinct u.id as id";

    }

    public void listWidgetEvents(final HttpServerRequest request, final String[] calendarIds, final int nbLimit) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                eventService.getEventsByCalendarAndDate(calendarIds, nbLimit, arrayResponseHandler(request));
            }
        });
    }

    /**
     * method launched in background to add extra calendar into Calendar Event
     *
     * @param eventId       calendar event identifier {@link String}
     * @param shared        body's shared sent {@link JsonObject}
     * @param user          user info {@link UserInfos}
     * @param host          host param {@link String}
     * @param lang          lang param {@link String}
     */
    public void addEventToUsersCalendar(String eventId, JsonObject shared, UserInfos user, String host, String lang) {
        List<String> sharedIds = new ArrayList<>(shared.getJsonObject("users").fieldNames());
        sharedIds.addAll(shared.getJsonObject("groups").fieldNames());

        Future<List<User>> userIdsFuture = userService.fetchUser(sharedIds, user);             // from payload API sent us
        Future<JsonObject> calendarsEventFuture = fetchCalendarsAndEventById(eventId);         // calendar Event and all calendars

        CompositeFuture.all(userIdsFuture, calendarsEventFuture)
                .compose(ar -> retrieveAllUsersFromCalendarsEvent(user, calendarsEventFuture))
                .compose(shareIdsFromCalendar -> proceedOnUsersFetched(eventId, userIdsFuture.result(), calendarsEventFuture.result(),
                        shareIdsFromCalendar, host, lang, user))
                .onFailure(err -> {
                    String message = String.format("[Calendar@%s::addEventToUsersCalendar] An error has occured" +
                            " during fetching userIds or calendar event, see previous logs: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(message, err.getMessage());
                });
    }

    /**
     * With calendarsEventFuture {@link Future<JsonObject>} containing
     * {'calendarEvent': {@link JsonObject}, 'calendars': {@link JsonArray}}
     * will fetch all userIds and groupId to fetch all User
     *
     * @param user                      user info {@link UserInfos}
     * @param calendarsEventFuture      calendars and event data as {@link JsonObject}
     *
     * @return {@link Future} of {@link List<User>} containing list of User fetched
     */
    @SuppressWarnings("unchecked")
    private Future<List<User>> retrieveAllUsersFromCalendarsEvent(UserInfos user, Future<JsonObject> calendarsEventFuture) {
        JsonObject calendarsEvent = calendarsEventFuture.result();

        List<String> shareIdsFromAllCalendar = ((List<JsonObject>) calendarsEvent.getJsonArray("calendars", new JsonArray()).getList()).stream()
                .flatMap(calendar -> ((List<JsonObject>) calendar.getJsonArray("shared", new JsonArray()).getList())
                        .stream()
                        .map(s -> s.getString("userId", s.getString("groupId", null)))
                )
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return userService.fetchUser(shareIdsFromAllCalendar, user);
    }

    /**
     * After all data fetched (users, calendar Event and calendars), will proceed on making
     * differences between users fetched from payload and calendars in order to get users that do not belong to both list
     * Afterwards, we will proceed on each user in order to get their default calendar identifier and add to our original event
     *
     * @param eventId                   calendar event identifer {@link String}
     * @param usersFromPayload          list of user identifier fetched from payload shared body sent info {@link List<User>}
     * @param calendarsEventFuture      calendarEvent and calendars as {@link JsonObject}
     * @param usersFromSharedCalendars  list of shared containing user and group identifier from all calendars {@link List<User>}
     * @param host                      host param {@link String}
     * @param lang                      lang param {@link String}
     * @param userInfos                 user session infos {@link Object}
     *
     * @return {@link Future} of {@link Void}
     */
    private Future<Void> proceedOnUsersFetched(String eventId, List<User> usersFromPayload, JsonObject calendarsEventFuture,
                                               List<User> usersFromSharedCalendars, String host, String lang, UserInfos userInfos) {
        Promise<Void> promise = Promise.promise();

        // difference to get users ids that is not in both Set
        Set<String> idsFromCalendar = usersFromSharedCalendars.stream().map(User::id).collect(Collectors.toSet());
        usersFromPayload.removeIf(user -> idsFromCalendar.contains(user.id()));

        // we fetch originalCalendars where event has been created to keep persisting its ids
        List<String> originalCalendars = determineOriginalCalendars(calendarsEventFuture, userInfos);

        List<Future<String>> futures = new ArrayList<>();
        for (User userNotAccess: usersFromPayload) {
            futures.add(fetchDefaultCalendar(userNotAccess, host, lang));
        }

        FutureHelper.all(futures)
                .onSuccess(result -> {
                    List<String> calendarIds = futures.stream().map(Future::result).collect(Collectors.toList());
                    calendarIds.addAll(originalCalendars);
                    eventService.update(eventId, new JsonObject().put("calendar", new JsonArray(calendarIds)), null, event -> {
                        if (event.isLeft()) {
                            log.info(String.format("[Calendar@%s::proceedOnUsersFetched] An error has occured: %s",
                                    this.getClass().getSimpleName(), event.left().getValue()), event.left().getValue());
                        }
                    });
                })
                .onFailure(err -> log.info(String.format("[Calendar@%s::proceedOnUsersFetched] An error has occured: %s",
                        this.getClass().getSimpleName(), err.getMessage()), err.getMessage()));
        return promise.future();
    }

    /**
     * Determine among these calendars who were the originals when the calendar event was created to keep persisting
     * its identifier(s)
     *
     * @param calendarEvent      calendarEvent and calendars as {@link JsonObject}
     * @param userInfos                 user session infos {@link Object}
     *
     * @return {@link Future} of {@link List<String>} the "original(s)" calendar(s)
     */
    @SuppressWarnings("unchecked")
    private List<String> determineOriginalCalendars(JsonObject calendarEvent, UserInfos userInfos) {
        JsonObject event = calendarEvent.getJsonObject("calendarEvent", new JsonObject());
        JsonArray calendars = calendarEvent.getJsonArray("calendars", new JsonArray());
        return (((List<JsonObject>) calendars.getList())
                .stream()
                .filter(calendar -> {
                    JsonObject calendarOwner = calendar.getJsonObject("owner", new JsonObject());
                    JsonObject eventOwner = event.getJsonObject("owner", new JsonObject());
                    Boolean doesNotContainDefault = Boolean.FALSE.equals(calendar.containsKey("is_default"));
                    Boolean isDefaultCalendarOwner = Boolean.TRUE.equals(calendar.containsKey("is_default")) &&
                            Boolean.TRUE.equals(calendarOwner.getString("userId").equals(eventOwner.getString("userId")));
                    Boolean isSessionOwner = Boolean.TRUE.equals(calendar.containsKey("is_default")) &&
                            Boolean.TRUE.equals(calendarOwner.getString("userId").equals((userInfos.getUserId())));
                    return doesNotContainDefault || isDefaultCalendarOwner || isSessionOwner;
                }))
                .map(calendar -> calendar.getString("_id"))
                .collect(Collectors.toList());

    }


    /**
     * retrieve default calendar identifier
     *
     * @param user  user data {@link User}
     * @param host  host param {@link String}
     * @param lang  lang param {@link String}
     *
     * @return {@link Future} of {@link String} default calendar identifier
     */
    private Future<String> fetchDefaultCalendar(User user, String host, String lang) {
        Promise<String> promise = Promise.promise();
        UserInfos userInfos = new UserInfos();
        userInfos.setUserId(user.id());
        userInfos.setUsername(user.displayName());
        calendarService.getDefaultCalendar(userInfos)
                .onSuccess(calendar -> {
                    if (calendar.isEmpty() || calendar.fieldNames().isEmpty()) {
                        calendarService.createDefaultCalendar(userInfos, host, lang)
                                .onSuccess(res -> promise.complete(res.getString("_id")))
                                .onFailure(promise::fail);
                    } else {
                        promise.complete(calendar.getString("_id"));
                    }
                })
                .onFailure(promise::fail);
        return promise.future();
    }


    /**
     * fetch calendarEvent and all calendars linked to calendarEvent
     *
     * @param eventId       calendar event identifier {@link String}
     *
     * @return {@link Future} of {@link JsonObject} containing JsonObject of
     * {'calendarEvent': {@link JsonObject}, 'calendars': {@link JsonArray}}
     */
    @SuppressWarnings("unchecked")
    private Future<JsonObject> fetchCalendarsAndEventById(String eventId) {
        Promise<JsonObject> promise = Promise.promise();
        // Object sequentially built as :

        // calendarEvent -> JsonObject calendar
        // calendars -> JsonArray containing all calendars belonging to calendar event
        JsonObject calendarAndEvent = new JsonObject();
        fetchCalendarEventById(eventId)
                .compose(calendarEvent -> {
                    calendarAndEvent.put("calendarEvent", new JsonObject(calendarEvent.toString()));
                    List<String> calendarList = calendarEvent.getJsonArray("calendar", new JsonArray()).getList();
                    return calendarService.list(calendarList);
                })
                .onSuccess(ar -> {
                    calendarAndEvent.put("calendars", ar);
                    promise.complete(calendarAndEvent);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * fetch calendarEvent
     *
     * @param eventId       calendar event identifier {@link String}
     *
     * @return {@link Future} of {@link JsonObject} containing calendarEvent
     */
    private Future<JsonObject> fetchCalendarEventById(String eventId) {
        Promise<JsonObject> promise = Promise.promise();
        eventService.getCalendarEventById(eventId, event -> {
            if (event.isLeft()) {
                String message = String.format("[Calendar@%s::fetchCalendarId] An error has occured" +
                        " during fetch calendar event by id: %s", this.getClass().getSimpleName(), event.left().getValue());
                log.error(message, event.left().getValue());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }

        });
        return promise.future();
    }

}
