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
import net.atos.entng.calendar.helpers.EventHelper;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.http.Renders.getHost;


public class CalendarReminderWorker extends BusModBase implements Handler<Message<JsonObject>> {
    protected static final Logger log = LoggerFactory.getLogger(CalendarReminderWorker.class);
    private ReminderService reminderService;
    private CalendarService calendarService;
    private EventServiceMongo eventServiceMongo;
    private WebClient webClient;
    private TimelineHelper notification;

    @Override
    public void start() {
        super.start();
        WebClientOptions options = new WebClientOptions();
        ServiceFactory serviceFactory = new ServiceFactory(vertx, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), WebClient.create(vertx, options));
        this.reminderService = serviceFactory.reminderService();
        this.calendarService = serviceFactory.calendarService();
        this.eventServiceMongo = new EventServiceMongoImpl(Calendar.CALENDAR_EVENT_COLLECTION, eb, serviceFactory);
        notification = new TimelineHelper(Vertx.vertx(), eb, config);
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

        reminders.stream()
                .map(JsonObject.class::cast)
                .map(ReminderModel::new)
//                .forEach(reminder -> remindersActions.add(sendReminder(reminder)));
                .map(this::sendReminder)
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

        getCalendarId(reminder.getEventId())
                .compose(calendarId -> {
                    HttpServerRequest request = new Http ;
                    String template = "calendar.reminder";
                    UserInfos user = new UserInfos();
                    user.setUserId(reminder.getOwner().id());
                    user.setUsername(reminder.getOwner().displayName());
                    List<String> recipient = new JsonArray().add(reminder.getOwner().id()).getList();
                    String calendarEventId = reminder.getEventId();
                    JsonObject notificationParameters = new JsonObject()
                            .put("username", user.getUsername())
                            .put("profilUri",
                                    "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
                            .put("calendarUri",
                                    "/calendar#/view/" + calendarId)
                            .put("resourceUri", "/calendar#/view/" + calendarId);

                    JsonObject pushNotif = new JsonObject()
                            .put("title", "push.notif.event.reminder")
                            .put("body", user.getUsername() + " " + I18n.getInstance().translate("calendar.reminder.push.notif.body",
                                    getHost(request), I18n.acceptLanguage(request)));
                    notificationParameters.put("pushNotif", pushNotif);
                    EventHelper.genericSendNotificationToUser(request, template, user, recipient, calendarId, calendarEventId,
                            notificationParameters, true);

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

    private Future<String> getCalendarId (String eventId) {
        Promise<String> promise = Promise.promise();

        getCalendarEventData(eventId)
                .onSuccess(calendarEvent -> {
                    String calendarId = calendarEvent.getJsonArray(Field.CALENDARS).getString(0);
                    promise.complete(calendarId);
                })
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

}
