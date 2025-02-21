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

import static com.mongodb.client.model.Filters.eq;
import static net.atos.entng.calendar.Calendar.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.utils.DateUtils.DEFAULT_LOCAL_DATE_TIME_FORMATTER;
import static org.entcore.common.utils.DateUtils.UTC_ZONE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.collections.Joiner;
import io.vertx.core.*;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.models.User;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;

import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.UserService;
import net.atos.entng.calendar.utils.DateUtils;
import org.bson.conversions.Bson;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.eventbus.EventBus;
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
    private final ReminderHelper reminderHelper;

    private EventBus eb;

    public EventHelper(String collection, CrudService eventService, ServiceFactory serviceFactory,
                       TimelineHelper timelineHelper, EventBus eb, JsonObject config) {
        super(collection, null);
        this.eventService = (EventServiceMongo) eventService;
        this.crudService = eventService;
        this.calendarService = serviceFactory.calendarService();
        this.userService = serviceFactory.userService();
        this.notification = timelineHelper;
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Calendar.class.getSimpleName());
        this.eventHelper = new org.entcore.common.events.EventHelper(eventStore);
        this.mongo = MongoDb.getInstance();
        this.eb = eb;
        this.config = config;
        this.reminderHelper = new ReminderHelper(serviceFactory);
    }

    @Override
    public void list(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                String startDate = request.params().get(Field.STARTDATE);
                String endDate = request.params().get(Field.ENDDATE);
                eventService.list(calendarId, user, startDate, endDate)
                        .compose(eventList -> {
                            //add reminders if needed
                            if (Boolean.FALSE.equals(config.getBoolean(Field.ENABLEREMINDER))) {
                                return Future.succeededFuture(eventList);
                            }
                            return reminderHelper.getEventsReminders(eventList, user);
                        })
                        .onSuccess(finalEventList -> {
                            renderJson(request, finalEventList);
                        })
                        .onFailure(err -> {
                            renderError(request);
                        });
                ;
            }
        });
    }

    @Override
    public void create(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
            if (user != null) {
                RequestUtils.bodyToJson(request, object -> {
                    calendarService.hasExternalCalendarId(object.getJsonArray("calendar")
                                    .stream().map((Object::toString))
                                    .collect(Collectors.toList()))
                            .onSuccess(isExternal -> {
                                if(Boolean.FALSE.equals(isExternal)) {
                                    if (isEventValid(object)) {
                                        RbsHelper.saveBookingsInRbs(request, object, user, config, eb).onComplete(e ->
                                                eventService.create(calendarId, object, user, event -> {
                                                    if (event.isRight()) {
                                                        JsonObject eventId = event.right().getValue();
                                                        final JsonObject message = new JsonObject();
                                                        message.put(Field._ID, calendarId);
                                                        message.put(Field.EVENTID, eventId.getString("_id"));
                                                        message.put(Field.START_DATE, (String) null);
                                                        message.put(Field.END_DATE, (String) null);
                                                        message.put(Field.SENDNOTIF, object.getBoolean(Field.SENDNOTIF, null));
                                                        notifyEventCreatedOrUpdated(request, user, message, true);
                                                        renderJson(request, event.right().getValue(), 200);
                                                        eventHelper.onCreateResource(request, RESOURCE_NAME);
                                                    } else if (event.isLeft()) {
                                                        log.error("[Calendar@EventHelper::create] Error when getting notification informations.");
                                                    }
                                                })
                                        );

                                    } else {
                                        log.error(String.format("[Calendar@EventHelper::create] " + "Submitted event is not valid"),
                                                I18n.getInstance().translate("calendar.error.date.saving", getHost(request), I18n.acceptLanguage(request)));
                                        Renders.unauthorized(request);
                                    }
                                } else {
                                    unauthorized(request);
                                }
                            })
                            .onFailure(err -> {
                                renderError(request);
                            });

                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Override
    public void update(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            RequestUtils.bodyToJson(request, object -> {
                final String eventId = request.params().get(EVENT_ID_PARAMETER);
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                isExternalCalendarEventImmutable(eventId)
                        .onSuccess(isExternal -> {
                            if(Boolean.FALSE.equals(isExternal)) {
                                if (isEventValid(object)) {
                                    crudService.update(eventId, object, user, new Handler<Either<String, JsonObject>>() {
                                        public void handle(Either<String, JsonObject> event) {
                                            if (event.isRight()) {
                                                final JsonObject message = new JsonObject();
                                                message.put("id", calendarId);
                                                message.put("eventId", eventId);
                                                message.put("start_date", (String) null);
                                                message.put("end_date", (String) null);
                                                message.put("sendNotif", object.containsKey("sendNotif") ? object.getBoolean("sendNotif") : null);
                                                notifyEventCreatedOrUpdated(request, user, message, false);
                                                renderJson(request, event.right().getValue(), 200);
                                            } else if (event.isLeft()) {
                                                log.error("Error when getting notification informations.");
                                            }
                                        }
                                    });
                                } else {
                                    log.error(String.format("[Calendar@EventHelper::update] " + "Submitted event is not valid"),
                                            I18n.getInstance().translate("calendar.error.date.saving", getHost(request), I18n.acceptLanguage(request)));
                                    Renders.unauthorized(request);
                                }
                            } else {
                                unauthorized(request);
                            }
                        })
                        .onFailure(err -> {
                            renderError(request);
                        });
            });
        });
    }


    public void updateAllEvents(final HttpServerRequest request) {
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            RequestUtils.bodyToJson(request, object -> {
                final String eventId = request.params().get(EVENT_ID_PARAMETER);
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                isExternalCalendarEventImmutable(eventId)
                  .onSuccess(isExternal -> {
                      if(Boolean.FALSE.equals(isExternal)) {
                          if (isEventValid(object)) {
                              crudService.retrieve(eventId, eventData -> {
                                  if(eventData.isLeft()) {
                                      log.warn("Could not find event {0} in database", eventId);
                                      Renders.notFound(request);
                                  } else {
                                      final JsonObject eventInDatabase = eventData.right().getValue();
                                      final String parentId = eventInDatabase.getString("parentId");
                                      if(isEmpty(parentId) || !eventInDatabase.getBoolean("isRecurrent", false)) {
                                          log.warn("Tried to update the event {0} and all its occurrences but it is not a recurrent event", eventId);
                                          Renders.badRequest(request);
                                      } else {
                                          final JsonArray pipelines = createEventUpdateModifier(object, eventInDatabase);
                                          mongo.aggregate(CALENDAR_EVENT_COLLECTION, pipelines)
                                            .onFailure(th -> {
                                                log.error("An error occurred while updating calendar event {0}", eventId, th);
                                                Renders.renderError(request);
                                            })
                                            .onSuccess(e -> {
                                                    final JsonObject message = new JsonObject();
                                                    message.put("id", calendarId);
                                                    message.put("eventId", eventId);
                                                    message.put("start_date", (String) null);
                                                    message.put("end_date", (String) null);
                                                    message.put("sendNotif", object.containsKey("sendNotif") ? object.getBoolean("sendNotif") : null);
                                                    notifyEventCreatedOrUpdated(request, user, message, false);
                                                    renderJson(request, new JsonObject().put("status", "ok"), 200);
                                            });
                                      }
                                  }
                              });
                          } else {
                              log.error(String.format("[Calendar@EventHelper::update] " + "Submitted event is not valid"),
                                I18n.getInstance().translate("calendar.error.date.saving", getHost(request), I18n.acceptLanguage(request)));
                              Renders.unauthorized(request);
                          }
                      } else {
                          unauthorized(request);
                      }
                  })
                  .onFailure(err -> {
                      renderError(request);
                  });
            });
        });
    }

    private JsonArray createEventUpdateModifier(final JsonObject modification, JsonObject eventInDatabase) {
        final String parentId = eventInDatabase.getString("parentId");
        final JsonObject project = new JsonObject();
        final JsonArray pipelines = new JsonArray()
            .add(new JsonObject().put("$match", new JsonObject().put("parentId", parentId).put("isRecurrent", true)))
            .add(new JsonObject().put("$project", project))
            .add(new JsonObject().put("$merge", CALENDAR_EVENT_COLLECTION));
        for (String attr : modification.fieldNames()) {
            if(!attr.endsWith("Moment") && !"index".equals(attr)) {
                Object value = modification.getValue(attr);
                if(value instanceof Boolean || value instanceof Number) {
                    project.put(attr, new JsonObject().put("$literal", value));
                } else if(value instanceof JsonObject) {
                    modifyForAggregation((JsonObject) value);
                } else {
                    project.put(attr, value);
                }
            }
        }
        // If the time range of the event changed, we update the time range of EVERY event, thus overloading any
        // changes that might have been performed on a single event
        if(!modification.getString("startMoment").equals(eventInDatabase.getString("startMoment")) ||
           !modification.getString("endMoment").equals(eventInDatabase.getString("endMoment"))) {
            // Here, we will use the start and end moment of the current event to extract the new time bounds of the
            // event and set the same to all events
            final String startMomentOfIndex = modification.getString("startMoment");
            final String endMomentOfIndex = modification.getString("endMoment");
            final ZonedDateTime initialStart = Instant.parse(startMomentOfIndex).atZone(UTC_ZONE);
            final ZonedDateTime initialEnd = Instant.parse(endMomentOfIndex).atZone(UTC_ZONE);
            addAggregationOperationToSetHourForLocalDateTime("startMoment", initialStart, project);
            addAggregationOperationToSetHourForLocalDateTime("endMoment", initialEnd, project);

            final LocalDateTime initialNotifStart = LocalDateTime.parse(modification.getString("notifStartMoment"), DEFAULT_LOCAL_DATE_TIME_FORMATTER);
            final LocalDateTime initialNotifEnd = LocalDateTime.parse(modification.getString("notifEndMoment"), DEFAULT_LOCAL_DATE_TIME_FORMATTER);

            addAggregationOperationToSetHourForUTCDateTime("notifStartMoment", initialNotifStart, project);

            addAggregationOperationToSetHourForUTCDateTime("notifEndMoment", initialNotifEnd, project);
        }
        project.put("modified", new JsonObject().put("$toDate", System.currentTimeMillis()));
        return pipelines;
    }

    private JsonObject modifyForAggregation(JsonObject value) {
        if(value == null) {
            return null;
        } else {
            final JsonObject copy = value.copy();
            final Set<String> fieldNames = copy.fieldNames();
            for (String fieldName : fieldNames) {
                final Object eltValue = copy.getValue(fieldName);
                if(eltValue instanceof Boolean || eltValue instanceof Number) {
                    copy.put(fieldName, new JsonObject().put("$literal", eltValue));
                } else if(eltValue instanceof JsonObject) {
                    copy.put(fieldName, modifyForAggregation((JsonObject) eltValue));
                } else {
                    copy.put(fieldName, eltValue);
                }
            }
            return copy;
        }
    }

    private void addAggregationOperationToSetHourForUTCDateTime(final String fieldName, final LocalDateTime newDate, final JsonObject aggregate) {
        aggregate.put(fieldName, new JsonObject().put("$dateToString", new JsonObject()
          .put("format", "%d/%m/%Y %H:%M")
          .put("date", new JsonObject()
            .put("$dateFromParts", new JsonObject()
              .put("year", new JsonObject().put("$year", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" + fieldName).put("format", "%d/%m/%Y %H:%M"))))
              .put("month", new JsonObject().put("$month", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" + fieldName).put("format", "%d/%m/%Y %H:%M"))))
              .put("day", new JsonObject().put("$dayOfMonth", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" + fieldName).put("format", "%d/%m/%Y %H:%M"))))
              .put("hour", new JsonObject().put("$literal", newDate.getHour()))
              .put("minute", new JsonObject().put("$literal", newDate.getMinute()))))));
    }

    private void addAggregationOperationToSetHourForLocalDateTime(final String fieldName, final ZonedDateTime newDate, JsonObject aggregate) {
        aggregate.put(fieldName, new JsonObject().put("$dateToString", new JsonObject()
          .put("format", "%Y-%m-%dT%H:%M:%S.%LZ")
          .put("date", new JsonObject()
            .put("$dateFromParts", new JsonObject()
              .put("year", new JsonObject().put("$year", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" +fieldName))))
              .put("month", new JsonObject().put("$month", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" +fieldName))))
              .put("day", new JsonObject().put("$dayOfMonth", new JsonObject().put("$dateFromString", new JsonObject().put("dateString", "$" +fieldName))))
              .put("hour", new JsonObject().put("$literal", newDate.getHour()))
              .put("minute", new JsonObject().put("$literal", newDate.getMinute()))))));
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

    /**
     * Delete calendarEvent
     * The flow is the following:
     * - if optional parameter deleteBookings is true, get calendarEvent
     * - delete calendarEvent no matter what happens
     * - if calendarEvent has been retrieved successfully, make calls to delete bookings in RBS (using event bus),
     * if not the task is finished
     * @param request HttpServerRequest request from the server
     */
    @Override
    @SuppressWarnings("unchecked")
    public void delete(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, user -> {
            final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
            final String eventId = request.params().get(EVENT_ID_PARAMETER);
            final Boolean deleteBookings = Boolean.parseBoolean(request.params().get(Field.DELETEBOOKINGS));
            final Boolean authorizeExternalCalendarEventsDelete = Boolean.parseBoolean(request.params().get(Field.URL));

            isExternalCalendarEventImmutable(eventId, authorizeExternalCalendarEventsDelete)
                    .onSuccess(isExternal -> {
                        if(Boolean.FALSE.equals(isExternal)) {
                            Future<JsonObject> getCalendarEventInfos = Boolean.TRUE.equals(deleteBookings) ? eventService.retrieve(calendarId, eventId, user)
                                    : Future.succeededFuture(new JsonObject());

                            getCalendarEventInfos
                                    .compose(event -> eventService.delete(calendarId, eventId, user))
                                    .compose(result -> {
                                        if (!getCalendarEventInfos.result().isEmpty()) {
                                            return RbsHelper.checkAndDeleteBookingRights(user, getCalendarEventInfos.result(), eb);
                                        } else {
                                            return Future.succeededFuture(new JsonArray());
                                        }
                                    })
                                    .onSuccess(res -> {
                                        if (!res.isEmpty()) {
                                            List<JsonObject> failedDeletions = ((List<JsonObject>) res.getList()).stream()
                                                    .filter((result) -> result.getString(Field.STATUS).equals(Field.ERROR))
                                                    .collect(Collectors.toList());
                                            if (failedDeletions.size() > 0) {
                                                badRequest(request, I18n.getInstance().translate("calendar.rbs.sniplet.error.booking.deletion", getHost(request), I18n.acceptLanguage(request)));
                                                failedDeletions.forEach((failedDeletion) -> log.error(failedDeletion.getString(Field.MESSAGE)));
                                            }
                                        }
                                        ok(request);
                                    })
                                    .onFailure(err -> renderError(request));
                        } else {
                            unauthorized(request);
                        }
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });


        });
    }


    public void getIcal(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                calendarService.hasExternalCalendarId(Collections.singletonList(calendarId))
                        .onSuccess(isExternal -> {
                            if(Boolean.FALSE.equals(isExternal)) {
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
                            } else {
                                unauthorized(request);
                            }
                        })
                        .onFailure(err -> {
                            renderError(request);
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
                        calendarService.hasExternalCalendarId(Collections.singletonList(calendarId))
                                .onSuccess(isExternal -> {
                                    if(!isExternal) {
                                        String icsContent = object.getString(Field.ICS);
                                        JsonObject requestInfo = new JsonObject();
                                        requestInfo.put(Field.DOMAIN, getHost(request)).put(Field.ACCEPTLANGUAGE, I18n.acceptLanguage(request));
                                        eventService.importIcal(calendarId, icsContent, user, requestInfo, defaultResponseHandler(request));
                                    } else {
                                        unauthorized(request);
                                    }
                                })
                                .onFailure(err -> {
                                    renderError(request);
                                });
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
                    if (!calendar.isEmpty()) {
                        for (Object id : calendar) {
                            if (message.getBoolean("sendNotif") == null || Boolean.FALSE.equals(isCreated)) {
                                notifyUsersSharing(request, user, id.toString(), calendarEvent, isCreated, restrictedEvent);
                            }
                        }
                    }

                }

            }
        });
    }


    /**
     * @param request         HttpServerRequest request from the server
     * @param user            User Object user that created/edited the event
     * @param calendarId      JsonObject calendar in which the event appears
     * @param calendarEvent   JsonObject event that is created/edited
     * @param restrictedEvent Boolean that tells if event is restricted or not
     */
    public void notifyUsersSharing(final HttpServerRequest request, final UserInfos user, final String calendarId,
                                   final JsonObject calendarEvent, final boolean isCreated, Boolean restrictedEvent) {
        String collection;
        final Bson query;
        JsonObject keys;
        JsonArray fetch;

        collection = CALENDAR_COLLECTION;
        query = eq("_id", calendarId);
        keys = new JsonObject().put("calendar", 1);
        fetch = new JsonArray().add("shared");


        findRecipients(collection, query, keys, fetch, user, restrictedEvent, calendarEvent, event ->
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

    @SuppressWarnings("unchecked")
    public void genericSendNotificationToUser(HttpServerRequest request, String template, UserInfos user,
                                                     List<String> recipients, String calendarId, String calendarEventId, JsonObject notificationData, Boolean disableAntiFlood) {
        if (recipients != null) {
                notification.notifyTimeline(request, template, user, recipients, calendarId, calendarEventId,
                        notificationData, disableAntiFlood);
                }
    }

    private void findRecipients(String collection, final Bson query, JsonObject keys, final JsonArray fetch,
                                final UserInfos user, Boolean restrictedEvent, JsonObject calendarEvent, Handler<Map<String, Object>> handler) {
        findRecipients(collection, query, keys, fetch, null, user, restrictedEvent, calendarEvent, handler);

    }

    @SuppressWarnings("unhandle")
    private void findRecipients(String collection, Bson query, JsonObject keys, final JsonArray fetch,
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
                                if (restrictedEvent) {
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
                if (shared == null && Boolean.TRUE.equals(calendar.getBoolean("is_default")) && restrictedEvent) {
                    JsonArray defaultCalendarOwner = new JsonArray().add(new JsonObject().put("id", calendar.getJsonObject("owner", new JsonObject())
                            .getString("userId", null)));
                    proceedOnUserList(defaultCalendarOwner, calendar, handler);
                }
            } // end  if (event.isRight())
        });
    }

    /**
     * Check if an event is valid before saving it.
     * The event start date should be before the event end date
     * If the event lasts more than one day and is recurrent, it should last less than the recurrence length
     * and the recurrence must be at least weekly
     *
     * @param object  JsonObject, the event to save
     * @return true if the event meets the requirements mentionned before
     */
    private boolean isEventValid(JsonObject object) {
        if(!object.containsKey(Field.STARTMOMENT) || !object.containsKey(Field.ENDMOMENT)) return false;

        Date startDate = DateUtils.parseDate(object.getString(Field.STARTMOMENT), DateUtils.DATE_FORMAT_UTC);
        Date endDate = DateUtils.parseDate(object.getString(Field.ENDMOMENT), DateUtils.DATE_FORMAT_UTC);

        if(startDate == null || endDate == null)return false;

        Date refStartDate = DateUtils.parseDate(Field.REFSTARTDATE, DateUtils.DATE_FORMAT_UTC);
        Date refEndDate = DateUtils.getRefEndDate(startDate);

        boolean isStartMomentNotTooOld = DateUtils.isStrictlyAfter(startDate, refStartDate);
        boolean isEndMomentNotTooFar = DateUtils.isStrictlyBefore(endDate, refEndDate);
        boolean isOneDayEvent = DateUtils.isSameDay(startDate, endDate);
        boolean isNotRecurrentEvent = Boolean.FALSE.equals(object.getBoolean(Field.isRecurrent));
        boolean areDatesValid = DateUtils.isStrictlyBefore(startDate, endDate) && isStartMomentNotTooOld && isEndMomentNotTooFar && isRecurrentEndDateValid(object, startDate);

        long dayInMilliseconds = 1000 * 60 * 60 * 24;
        int eventDayLength = (int) ((endDate.getTime() - startDate.getTime()) / dayInMilliseconds);

        boolean isWeeklyRecurrenceValid = object.getValue(Field.recurrence) instanceof JsonObject
                && Field.every_week.equals(object.getJsonObject(Field.recurrence).getValue(Field.type))
                && (eventDayLength < (7 * object.getJsonObject(Field.recurrence).getInteger(Field.every)));


        return (areDatesValid && (isOneDayEvent || isNotRecurrentEvent || isWeeklyRecurrenceValid));
    }


    /**
     * Check if a recurrent event has valid end date.
     * If it has an end date, it must be earlier than the event start date + 80 years
     * If it's a number of recurrence type of event : this number should be more than 1 and less than 365
     * If it's a number of recurrence type of event : we calculate a periodicEndDate and compare it to the max possible endDate we fixed to be sure its under 80 years long
     * @param recurrentEvent  JsonObject, the event to save (must contain a jsonObject 'recurrence' with properties : String end_type, String 'end_on' and, int 'every' and int 'end_after' if end_type=='after'
     * @return true if the recurrence meets the requirements mentionned before
     */
    private boolean isRecurrentEndDateValid (JsonObject recurrentEvent, Date startDate) {
        Date refEndDate = DateUtils.getRefEndDate(startDate);
        if (Boolean.FALSE.equals(recurrentEvent.getBoolean(Field.isRecurrent))) {
            return true;
        } else {
            boolean result = false;
            String endType = recurrentEvent.getJsonObject(Field.recurrence, new JsonObject()).getString(Field.end_type, null);
            if (Field.on.equals(endType)) {
                String endOnString = recurrentEvent.getJsonObject(Field.recurrence, new JsonObject()).getString(Field.end_on, null);
                if(endOnString == null) return false;
                Date endOnDate = DateUtils.parseDate(endOnString, DateUtils.DATE_FORMAT_UTC);
                result = DateUtils.isStrictlyBefore(endOnDate, refEndDate);
            } else if (Field.after.equals(endType)) {
                int range = recurrentEvent.getJsonObject(Field.recurrence, new JsonObject()).getInteger(Field.end_after, 0);
                if(range <= Field.end_after_min_value || range >= Field.end_after_max_value){
                    return false;
                }
                Date periodicEndDate = DateUtils.getPeriodicEndDate(startDate, recurrentEvent);
                if(periodicEndDate == null) return false;
                result = DateUtils.isStrictlyBefore(periodicEndDate, refEndDate);
            }
            return result;
        }
    }

    /**
     * Prepare notification user list so that it contains only the people the event is shared with and that have access to the calendar,
     * and the calendar owner and the event owner if they are different from the person editing
     *
     * @param listOfUsers   JsonArray of the ids of the users with access to the calendar
     * @param user          User Object, the user that edited the event
     * @param calendar      JsonObject, the targeted calendar
     * @param calendarEvent JsonObject the edited event
     * @param handler       Handler used by notifyUsersSharing()
     */
    @SuppressWarnings("unchecked")
    private void restrictListOfUsers(JsonArray listOfUsers, final UserInfos user, JsonObject calendar, JsonObject calendarEvent,
                                     final Handler<Map<String, Object>> handler) {

        //make array with userIds & groupIds from event
        List<String> calendarEventShared = getSharedIds(calendarEvent.getJsonArray("shared", new JsonArray()));

        //get all userIds from groups for event
        userService.fetchUser(calendarEventShared, user, false)
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
                    if (!user.getUserId().equals(calendarEvent.getJsonObject("owner").getString("userId"))) {
                        userIds.add(calendarEvent.getJsonObject("owner").getString("userId"));
                    }

                    JsonArray finalUserIds = new JsonArray(userIds.stream()
                            .map((currentUser) -> new JsonObject().put("id", currentUser))
                            .collect(Collectors.toList()));

                    proceedOnUserList(finalUserIds, calendar, handler);
                })
                .onFailure(err -> {
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
     * @param calendar    JsonObject of the current calendar
     * @param handler     Handler used by notifyUsersSharing()
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

    private List<String> getSharedIds(JsonArray shared) {
        return getSharedIds(shared, null);
    }

    private List<String> getSharedIds(JsonArray shared, String filterRights) {
        List<String> shareIds = new ArrayList<>();
        for (Object o : shared) {
            if (!(o instanceof JsonObject)) continue;
            JsonObject userShared = (JsonObject) o;

            if (filterRights != null && !userShared.getBoolean(filterRights, false))
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
     * @param eventId calendar event identifier {@link String}
     * @param shared  body's shared sent {@link JsonObject}
     * @param user    user info {@link UserInfos}
     * @param host    host param {@link String}
     * @param lang    lang param {@link String}
     */
    public void addEventToUsersCalendar(String eventId, JsonObject shared, UserInfos user, String host, String lang) {
        List<String> sharedIds = new ArrayList<>(shared.getJsonObject("users").fieldNames());
        sharedIds.addAll(shared.getJsonObject("groups").fieldNames());

        Future<List<User>> userIdsFuture = userService.fetchUser(sharedIds, user, false);             // from payload API sent us
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
     * @param user                 user info {@link UserInfos}
     * @param calendarsEventFuture calendars and event data as {@link JsonObject}
     * @return {@link Future} of {@link List<User>} containing list of User fetched
     */
    private Future<List<User>> retrieveAllUsersFromCalendarsEvent(UserInfos user, Future<JsonObject> calendarsEventFuture) {
        JsonObject calendarsEvent = calendarsEventFuture.result();
        return retrieveAllUsersFromCalendarsEvent(user, calendarsEvent, false, false);
    }

    /**
     * With calendarsEvent {@link JsonObject} containing
     * {'calendarEvent': {@link JsonObject}, 'calendars': {@link JsonArray}}
     * will fetch all userIds and groupId to fetch all User
     *
     * @param user                user info {@link UserInfos}
     * @param calendarsEvent      calendars and event data as {@link JsonObject}
     * @param keepUserFromSession if the user from the session should be fetched {@link boolean}
     * @param keepCalendarsOwners if the calendarOwners should be fetched {@link boolean}
     * @return {@link Future} of {@link List<User>} containing list of User fetched
     */
    @SuppressWarnings("unchecked")
    private Future<List<User>> retrieveAllUsersFromCalendarsEvent(UserInfos user, JsonObject calendarsEvent,
                                                                  boolean keepUserFromSession, boolean keepCalendarsOwners) {
        List<String> shareIdsFromAllCalendar = ((List<JsonObject>) calendarsEvent.getJsonArray(Field.CALENDARS, new JsonArray()).getList()).stream()
                .flatMap(calendar -> ((List<JsonObject>) calendar.getJsonArray(Field.shared, new JsonArray()).getList())
                        .stream()
                        .map(s -> s.getString(Field.userId, s.getString(Field.groupId, null)))
                )
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (keepCalendarsOwners) {
            //add calendar owners to list
            List<String> calendarsOwners = ((List<JsonObject>) calendarsEvent.getJsonArray(Field.CALENDARS, new JsonArray()).getList()).stream()
                    .map(c -> c.getJsonObject(Field.OWNER, null).getString(Field.userId, null))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            shareIdsFromAllCalendar.addAll(calendarsOwners);
            //remove duplicates
            shareIdsFromAllCalendar.stream().distinct().collect(Collectors.toList());
        }

        return userService.fetchUser(shareIdsFromAllCalendar, user, keepUserFromSession);
    }

    /**
     * After all data fetched (users, calendar Event and calendars), will proceed on making
     * differences between users fetched from payload and calendars in order to get users that do not belong to both list
     * Afterwards, we will proceed on each user in order to get their default calendar identifier and add to our original event
     *
     * @param eventId                  calendar event identifer {@link String}
     * @param usersFromPayload         list of user identifier fetched from payload shared body sent info {@link List<User>}
     * @param calendarsEventFuture     calendarEvent and calendars as {@link JsonObject}
     * @param usersFromSharedCalendars list of shared containing user and group identifier from all calendars {@link List<User>}
     * @param host                     host param {@link String}
     * @param lang                     lang param {@link String}
     * @param userInfos                user session infos {@link Object}
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
        for (User userNotAccess : usersFromPayload) {
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
     * @param calendarEvent calendarEvent and calendars as {@link JsonObject}
     * @param userInfos     user session infos {@link Object}
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
     * @param user user data {@link User}
     * @param host host param {@link String}
     * @param lang lang param {@link String}
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
     * @param eventId calendar event identifier {@link String}
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
     * @param eventId calendar event identifier {@link String}
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


    /**
     * Check if the user has access to the event
     *
     * @param eventId the id of the event {@link String}
     * @param user    the user currently logged in {@link UserInfos}
     * @return {@link Boolean} being true if the user has access to the event, false instead
     */
    public Future<Boolean> hasAccessToEvent(String eventId, UserInfos user) {
        Promise<Boolean> promise = Promise.promise();
        // get calendar Event and its calendars
        fetchCalendarsAndEventById(eventId)
                .onFailure(err -> {
                    String message = String.format("[Calendar@%s::hasAccessToEvent]: an error has occurred while finding calendars containing event: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(message);
                    promise.fail(err.getMessage());
                })
                .onSuccess(eventInfos -> {
                    //check if user is event owner
                    String calendarEventOwner = eventInfos.getJsonObject(Field.calendarEvent).getJsonObject(Field.OWNER).getString(Field.userId);
                    if (calendarEventOwner.equals(user.getUserId())) {
                        promise.complete(true); //user is event owner
                    } else {
                        //get all users with access to the calendars of this event
                        retrieveAllUsersFromCalendarsEvent(user, eventInfos, true, true)
                                .onFailure(fail -> {
                                    String message = String.format("[Calendar@%s::hasAccessToEvent]: an error has occurred while finding users with access to event: %s",
                                            this.getClass().getSimpleName(), fail.getMessage());
                                    log.error(message);
                                    promise.fail(fail.getMessage());
                                })
                                .onSuccess(usersWithAccessToEvent -> {
                                    //find if user is among the users that can access the event
                                    List<User> matchingUsers = usersWithAccessToEvent
                                            .stream()
                                            .filter(currentUser -> currentUser.id().equals(user.getUserId()))
                                            .collect(Collectors.toList()); //check if users match user session

                                    promise.complete(!matchingUsers.isEmpty());
                                });
                    }
                });

        return promise.future();
    }

    /**
     * Get file from documents collection
     *
     * @param hasAccess    whether the user has access to the event or not
     * @param attachmentId the id of the file {@link String}
     * @return {@link Future} of {@link JsonObject} being true if the user is owner of the file, false instead
     */
    public Future<JsonObject> getAttachment(Boolean hasAccess, String attachmentId) {
        Promise<JsonObject> promise = Promise.promise();

        if (Boolean.FALSE.equals(hasAccess)) {
            promise.fail(String.format("[Calendar@EventHelper::getAttachment] User does not have access to file"));
        }

        // Query
        final Bson query = eq(Field._ID, attachmentId);

        mongo.findOne(Calendar.DOCUMENTS_COLLECTION, MongoQueryBuilder.build(query), validResultHandler(result -> {
            if (result.isLeft() || result.right().getValue().size() == 0 || !(result.right().getValue() instanceof JsonObject)) {
                String message = String.format("[Calendar@%s::getAttachment]:  an error has occurred while finding file: %s",
                        this.getClass().getSimpleName(), result.left().getValue());
                log.error(message);
                promise.fail(result.left().getValue());
                return;
            }

            promise.complete(result.right().getValue());
        }));

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    public Future<Boolean> isExternalCalendarEventImmutable(String calendarEventId) {
        return isExternalCalendarEventImmutable(calendarEventId, null);
    }

    @SuppressWarnings("unchecked")
    public Future<Boolean> isExternalCalendarEventImmutable(String calendarEventId, Boolean authorizeExternalCalendarEventChange) {
        Promise<Boolean> promise = Promise.promise();

        if(Boolean.TRUE.equals(authorizeExternalCalendarEventChange)) {
            promise.complete(false);
        }

        this.eventService.getCalendarEventById(calendarEventId)
                .compose( calendarEvent ->
                    this.calendarService.hasExternalCalendarId((
                            (List<String>) calendarEvent.getJsonArray(Field.CALENDAR).getList()))
                )
                .onFailure(err -> {
                    promise.fail(err.getMessage());
                    log.error(err.getMessage());
                })
                .onSuccess(promise::complete);

        return promise.future();
    }


}
