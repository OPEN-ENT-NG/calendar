package net.atos.entng.calendar.helpers;

import fr.wseduc.webutils.I18n;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.utils.DateUtils;
import net.atos.entng.calendar.utils.ReminderConverter;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class ReminderHelper {

    protected static final Logger log = LoggerFactory.getLogger(PlatformHelper.class);


    private final ReminderService reminderService;

    public ReminderHelper(ServiceFactory serviceFactory) {
        this.reminderService = serviceFactory.reminderService();
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
                .onSuccess(eventReminders -> promise.complete(calendarEvent.put(Field.REMINDER, eventReminders)))
                .onFailure(fail -> {
                    String message = String.format("[Magneto@%s::addRemindersToEvent] Failed to integrate reminders into calendarEvents : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    promise.fail(message);
                });

        return promise.future();
    }
}
