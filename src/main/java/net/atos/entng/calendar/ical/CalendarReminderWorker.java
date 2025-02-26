package net.atos.entng.calendar.ical;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.controllers.CalendarController;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.core.enums.ReminderCalendarEventWorkerAction;
import net.atos.entng.calendar.core.enums.I18nKeys;
import net.atos.entng.calendar.helpers.EventHelper;
import net.atos.entng.calendar.helpers.I18nHelper;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import net.atos.entng.calendar.utils.DateUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.http.Renders.getHost;


public class CalendarReminderWorker extends BusModBase implements Handler<Message<JsonObject>> {
    protected static final Logger log = LoggerFactory.getLogger(CalendarReminderWorker.class);
    private ReminderService reminderService;
    private EventServiceMongo eventServiceMongo;
    private TimelineHelper notification;


    @Override
    public void start() {
        super.start();
        WebClientOptions options = new WebClientOptions();
        ServiceFactory serviceFactory = new ServiceFactory(vertx, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), WebClient.create(vertx, options));
        this.reminderService = serviceFactory.reminderService();
        this.eventServiceMongo = new EventServiceMongoImpl(Calendar.CALENDAR_EVENT_COLLECTION, eb, serviceFactory);
        notification = new TimelineHelper(vertx, eb, config);
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        String action = message.body().getString(Field.ACTION, "");
        switch (action) {
            case Field.SEND_REMINDERS:
                findAndSendReminders(message);
                break;
            default:
                break;
        }

    }

    private void findAndSendReminders(Message<JsonObject> message) {
        reminderService.fetchRemindersToSend()
                .compose(this::sendReminders)
                .onSuccess(message::reply)
                .onFailure(err -> {
                    String errMessage = String.format("[Calendar@%s::findAndSendReminders]:  " +
                                    "an error has occurred while sending reminders: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(errMessage);
                    message.reply(err);
                });
    }

    private Future<Void> sendReminders(JsonArray reminders) {
        Promise<Void> promise = Promise.promise();
        List<Future> remindersActions = new ArrayList<>();
        log.info(String.format("REMINDERS %s", reminders.toString()));

        reminders.stream()
                .map(JsonObject.class::cast)
                .map(ReminderModel::new)
                .map(this::sendReminder)
                .map(remindersActions::add)
                .collect(Collectors.toList());

        CompositeFuture.all(remindersActions)
                .onSuccess(result -> promise.complete())
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::sendReminders]:  " +
                                    "an error has occurred while sending reminders: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private Future<Void> sendReminder(ReminderModel reminder) {
        Promise<Void> promise = Promise.promise();
        List<Future> reminderActions =  new ArrayList<>();

        if (reminder.getReminderType().isEmail()) {
//            reminderActions.add(); //add send email action
            log.info("CALENDAR send email action");
        }

        if (reminder.getReminderType().isTimeline()) {
            reminderActions.add(sendTimelineNotification(reminder)); //add send notification action
            log.info("CALENDAR send notification action");
        }
        CompositeFuture.all(reminderActions)
                .onSuccess(result -> promise.complete())
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::sendReminder]:  " +
                                    "an error has occurred while sending reminders: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private Future<Void> sendTimelineNotification (ReminderModel reminder) {
        Promise<Void> promise = Promise.promise();

        getCalendarEvent(reminder.getEventId())
                .compose(calendarEvent -> {
                    String template = "calendar.reminder";
                    UserInfos user = new UserInfos();
                    user.setUserId(reminder.getOwner().id());
                    user.setUsername(reminder.getOwner().displayName());
                    List<String> recipient = new JsonArray().add(reminder.getOwner().id()).getList();
                    String calendarId = calendarEvent.getJsonArray(Field.CALENDAR, new JsonArray()).getString(0);
                    JsonObject notificationParameters = null;
                    try {
                        notificationParameters = new JsonObject()
                                .put("username", user.getUsername())
                                .put("profilUri",
                                        "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                                .put("calendarUri",
                                        "/calendar#/view/" + calendarId)
                                .put("resourceUri", "/calendar#/view/" + calendarId)
                                .put("eventTitle", calendarEvent.getString(Field.TITLE, ""))
                                .put("remainingTime", getRemainingTime(reminder.getReminderFrequency().get(0), calendarEvent.getString(Field.STARTMOMENT)));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    JsonObject pushNotif = new JsonObject()
                            .put("title", "push.notif.event.reminder")
                            .put("body", user.getUsername() + " " + I18nHelper.getI18nValue(I18nKeys.CALENDAR_REMINDER_PUSH_NOTIF,
                                    Locale.getDefault().toString()));
                    notificationParameters.put("pushNotif", pushNotif);
                    notification.notifyTimeline(null, template, user, recipient, null, null,
                            notificationParameters, false);

                    return Future.succeededFuture();
                })
                .onSuccess(result -> promise.complete())
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::sendTimelineNotification]:  " +
                                    "an error has occurred while sending timeline reminder: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private Future<JsonObject> getCalendarEvent (String eventId) {
        Promise<JsonObject> promise = Promise.promise();

        getCalendarEventData(eventId)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::getCalendarId]:  " +
                                    "an error has occurred while fetching event data: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private Future<JsonObject> getCalendarEventData (String eventId) {
        Promise<JsonObject> promise = Promise.promise();

        eventServiceMongo.getCalendarEventById(eventId)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::getCalendarEventData]:  " +
                                    "an error has occurred while fetching event data: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private String getRemainingTime( String reminderTime, String calendarEventTime) throws ParseException {
        long diffInMillis = DateUtils.getTimeDifference(reminderTime, calendarEventTime);

        long months = diffInMillis / (30L * 24 * 60 * 60 * 1000);
        if (months > 0) return months + (months == 1 ? " month" : " months");

        long weeks = diffInMillis / (7L * 24 * 60 * 60 * 1000);
        if (weeks > 0) return weeks + (weeks == 1 ? " week" : " weeks");

        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        if (days > 0) return days + (days == 1 ? " day" : " days");

        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        return hours + (hours == 1 ? " hour" : " hours");
    }


}
