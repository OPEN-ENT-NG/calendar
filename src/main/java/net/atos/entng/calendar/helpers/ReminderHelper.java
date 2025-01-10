package net.atos.entng.calendar.helpers;

import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.enums.ErrorEnum;
import net.atos.entng.calendar.models.User;
import net.atos.entng.calendar.services.*;
import net.atos.entng.calendar.utils.DateUtils;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static net.atos.entng.calendar.Calendar.CALENDAR_COLLECTION;
import static net.atos.entng.calendar.Calendar.CALENDAR_NAME;
import static org.entcore.common.http.response.DefaultResponseHandler.*;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;

public class ReminderHelper extends MongoDbControllerHelper {

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

    private EventBus eb;

    public EventHelper(String collection, ServiceFactory serviceFactory, JsonObject config) {
        super(collection, null);
        this.reminderService = serviceFactory.reminderService();
        this.mongo = MongoDb.getInstance();
        this.config = config;
    }

    @Override
    public Promise<JsonObject> addRemindersToEvents(JsonArray calendarEvents, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        //for each event id
        List<Future> duplicateFutures = new ArrayList<>();

        for (JsonObject event : calendarEvents) {
            if (event.hasKey(Field._ID)) {
                duplicateFutures.add(addRemindersToEvents(event, user));
            }
        }

        CompositeFuture.all(duplicateFutures)
                .onSuccess(promise::complete)
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::addRemindersToEvents] Failed to integrate reminders into calendarEvents : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<JsonObject> addRemindersToEvents(JsonObject calendarEvent, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        //get reminders
        ReminderService.getEventReminder(calendarEvent.getString(Field._ID), user)
                //convert reminders
                .compose(reminder -> ReminderConverter.convertToReminderFrontEndModel(reminder, calendarEvent.getString(Field.STARTMOMENT)))
                //put reminders in event
                .compose(frontEndReminder -> Future.succeededFuture(calendarEvent.put(Field.REMINDER, frontEndReminder.result().body())))
                .onSuccess(promise::complete)
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::getEventReminder] Failed to integrate reminders into calendarEvents : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });


        return promise.future();
    }
}
