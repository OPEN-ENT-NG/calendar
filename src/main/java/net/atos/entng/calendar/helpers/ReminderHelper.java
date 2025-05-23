package net.atos.entng.calendar.helpers;

import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.models.reminders.ReminderFrontEndModel;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import net.atos.entng.calendar.utils.DateUtils;
import net.atos.entng.calendar.utils.ReminderConverter;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class ReminderHelper {

    protected static final Logger log = LoggerFactory.getLogger(PlatformHelper.class);


    private final ReminderService reminderService;
    private final EventServiceMongo eventServiceMongo;

    public ReminderHelper(ServiceFactory serviceFactory, EventBus eb) {
        this.reminderService = serviceFactory.reminderService();
        this.eventServiceMongo = new EventServiceMongoImpl(Calendar.CALENDAR_EVENT_COLLECTION, eb, serviceFactory);
    }

    public Future<JsonArray> getEventsReminders(JsonArray calendarEvents, UserInfos user) {
        Promise<JsonArray> promise = Promise.promise();

        List<Future> duplicateFutures = calendarEvents.stream()
                .map(existingEvent -> new JsonObject(existingEvent.toString()))
                .map(event -> addRemindersToEvent(event, user))
                .collect(Collectors.toList());

        CompositeFuture.all(duplicateFutures)
                .onSuccess(eventsWithReminders -> promise.complete(new JsonArray(eventsWithReminders.list())))
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::addRemindersToEvent] Failed to integrate reminders into calendarEvents : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<JsonObject> addRemindersToEvent(JsonObject calendarEvent, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        //get reminders
        reminderService.getEventReminders(calendarEvent.getString(Field._ID), user)
                //convert reminders
                .compose(reminder -> Future.succeededFuture(ReminderConverter.convertToReminderFrontEndModel(new ReminderModel(reminder),
                        DateUtils.parseDate(calendarEvent.getString(Field.STARTMOMENT), DateUtils.DATE_FORMAT_UTC))))
                //put reminders in event
                .onSuccess(eventReminders -> {
                    if (eventReminders.getId() != null) {
                        calendarEvent.put(Field.REMINDERS, eventReminders);
                    }
                    promise.complete(calendarEvent);
                })
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::addRemindersToEvent] Failed to integrate reminders into calendarEvents : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }


    //post/put
    public Future<Void> remindersEventFormActions(String action, String eventId, UserInfos user, JsonObject body) {
        return remindersEventFormActions(action, eventId, user, body, null);
    }

    //delete
    public Future<Void> remindersEventFormActions(String action, String eventId, String reminderId) {
        return remindersEventFormActions(action, eventId, null, null, reminderId);
    }

    //delete reminders by event
    public Future<Void> remindersEventFormActions(String action, String eventId) {
        return remindersEventFormActions(action, eventId, null, null, null);
    }

    /**
     * Propagate calendarEvent action to its reminders
     * @param action the action made on the event
     * @param eventId the event
     * @param user the user
     * @param body the reminder body
     */
    private Future<Void> remindersEventFormActions(String action, String eventId, UserInfos user, JsonObject body, String reminderId) {
        Promise<Void> promise = Promise.promise();

        switch (action) {
            case (Actions.CREATE_REMINDER):
                getFormattedReminder(body, eventId, user)
                        .compose(reminder ->  reminderService.create(reminder))
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
                break;
            case (Actions.UPDATE_REMINDER):
                getFormattedReminder(body, eventId, user)
                        .compose(reminder ->
                                reminderService.update(eventId, reminder.getString(Field._ID, ""), reminder))
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
                break;
            case (Actions.UPDATE_ALL_REMINDERS):
                reminderService.getEventReminders(eventId, user)
                        .compose(eventReminder -> getUpdatedReminder(new ReminderModel(eventReminder), eventId, user, new ReminderFrontEndModel(body)))
                        .compose(reminder ->
                                reminderService.update(eventId, reminder.getString(Field._ID, ""), reminder))
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
                break;
            case (Actions.DELETE_REMINDER):
                reminderService.delete(eventId, (reminderId != null) ? reminderId : body.getString(Field.ID, ""))
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
                break;
            case (Actions.DELETE_ALL_EVENT_REMINDERS):
                reminderService.getEventIdReminders(eventId)
                        .compose(eventReminderIds -> deleteEventReminders(eventReminderIds, eventId))
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
                break;
        }

        return promise.future();
    }

    public Future<Void> deleteEventReminders(List<String> reminderIds, String eventId) {
        Promise<Void> promise = Promise.promise();

        List<Future> duplicateFutures = reminderIds.stream()
                .map(reminderId -> reminderService.delete(eventId, reminderId))
                .collect(Collectors.toList());

        CompositeFuture.all(duplicateFutures)
                .onSuccess(eventsWithReminders -> promise.complete())
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::deleteEventReminders] Failed to relete event reminders: %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }

    public Future<JsonObject> getFormattedReminder(JsonObject reminder, String eventId, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        if (Boolean.FALSE.equals(reminder.containsKey(Field.EVENTID_CAMEL))) {
            reminder.put(Field.EVENTID_CAMEL, eventId);
        }

        //get reminders
        eventServiceMongo.getCalendarEventById(eventId)
                //convert reminders
                .compose(calendarEvent -> Future.succeededFuture(ReminderConverter.convertToReminderModel(new ReminderFrontEndModel(reminder),
                        DateUtils.parseDate(calendarEvent.getString(Field.STARTMOMENT), DateUtils.DATE_FORMAT_UTC),
                        user)))
                .onSuccess(formattedReminder -> promise.complete(formattedReminder.toJson()))
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::getFormattedReminder] Failed to format reminders into back end model : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }

    public Future<JsonObject> getUpdatedReminder(ReminderModel reminder, String eventId, UserInfos user, ReminderFrontEndModel newReminder) {
        Promise<JsonObject> promise = Promise.promise();

        eventServiceMongo.getCalendarEventById(eventId)
                .compose(calendarEvent -> Future.succeededFuture(ReminderConverter.updateReminderModel(reminder,
                        DateUtils.parseDate(calendarEvent.getString(Field.STARTMOMENT), DateUtils.DATE_FORMAT_UTC),
                        user, newReminder)))
                .onSuccess(formattedReminder -> promise.complete(formattedReminder.toJson()))
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::getFormattedReminder] Failed to format reminders into back end model : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }
}
