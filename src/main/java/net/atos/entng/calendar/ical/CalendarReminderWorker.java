package net.atos.entng.calendar.ical;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.enums.EventBusAction;
import net.atos.entng.calendar.core.enums.I18nKeys;
import net.atos.entng.calendar.helpers.I18nHelper;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.*;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import net.atos.entng.calendar.utils.DateUtils;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.vertx.java.busmods.BusModBase;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CalendarReminderWorker extends BusModBase implements Handler<Message<JsonObject>> {
    protected static final Logger log = LoggerFactory.getLogger(CalendarReminderWorker.class);
    private ReminderService reminderService;
    private EventServiceMongo eventServiceMongo;
    private UserService userService;
    private TimelineHelper notification;


    @Override
    public void start() {
        super.start();
        WebClientOptions options = new WebClientOptions();
        ServiceFactory serviceFactory = new ServiceFactory(vertx, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), WebClient.create(vertx, options));
        this.reminderService = serviceFactory.reminderService();
        this.eventServiceMongo = new EventServiceMongoImpl(Calendar.CALENDAR_EVENT_COLLECTION, eb, serviceFactory);
        this.userService = serviceFactory.userService();
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
        HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
        log.info("I18N DATA");
        log.info(I18n.getInstance().translate("calendar.reminder.push.notif.body", I18n.DEFAULT_DOMAIN, I18n.acceptLanguage(request)));
        log.info(I18n.acceptLanguage(request));

        if (reminder.getReminderType().isEmail()) {
//            reminderActions.add(sendReminderEmail(reminder)); //add send email action
            log.info("CALENDAR send email action");
        }

        if (reminder.getReminderType().isTimeline()) {
            reminderActions.add(sendTimelineNotification(reminder, request)); //add send notification action
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

    private Future<Void> sendTimelineNotification (ReminderModel reminder, HttpServerRequest request) {
        Promise<Void> promise = Promise.promise();

        getCalendarEvent(reminder.getEventId())
                .compose(calendarEvent -> {
                    String template = "calendar.reminder";
                    UserInfos user = new UserInfos();
                    user.setUserId(reminder.getOwner().userId());
                    user.setUsername(reminder.getOwner().displayName());
                    List<String> recipient = new JsonArray().add(reminder.getOwner().userId()).getList();
                    String calendarId = calendarEvent.getJsonArray(Field.CALENDAR, new JsonArray()).getString(0);
                    JsonObject notificationParameters = null;
                    try {
                        notificationParameters = new JsonObject()
                                .put(Field.PROFILURI,
                                        "/userbook/annuaire#" + reminder.getOwner().userId())
                                .put(Field.CALENDARURI,
                                        "/calendar#/view/" + calendarId)
                                .put(Field.RESOURCEURI, "/calendar#/view/" + calendarId)
                                .put(Field.EVENTURI, "/calendar#/view/" + calendarId)
                                .put("eventTitle", calendarEvent.getString(Field.TITLE, ""))
                                .put("remainingTime", getRemainingTime(reminder.getReminderFrequency().get(0), calendarEvent.getString(Field.STARTMOMENT), request));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                    JsonObject pushNotif = new JsonObject()
                            .put(Field.TITLE, "push.notif.event.reminder")
                            .put(Field.BODY, user.getUsername() + " " + I18n.getInstance().translate("calendar.reminder.push.notif.body",
                                    I18n.DEFAULT_DOMAIN, I18n.acceptLanguage(request)));
//                                    I18nHelper.getI18nValue(I18nKeys.CALENDAR_REMINDER_PUSH_NOTIF,
//                                    Locale.getDefault().toString()));
                notificationParameters.put(Field.PUSHNOTIF, pushNotif);
                    notification.notifyTimeline(request, template, user, recipient, null, null,
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

    private String getRemainingTime( String reminderTime, String calendarEventTime, HttpServerRequest request) throws ParseException {
        long diffInMillis = DateUtils.getTimeDifference(reminderTime, calendarEventTime);

        long months = diffInMillis / (30L * 24 * 60 * 60 * 1000);
        if (months > 0) return months + " " + I18n.getInstance().translate(months == 1 ? "calendar.month" : "calendar.recurrence.monthes",
                Renders.getHost(request), I18n.acceptLanguage(request));
//                Locale.getDefault().toString()));

        long weeks = diffInMillis / (7L * 24 * 60 * 60 * 1000);
        if (weeks > 0) return weeks + " " +I18n.getInstance().translate(weeks == 1 ? "calendar.week" : "calendar.recurrence.weeks",
                Renders.getHost(request), I18n.acceptLanguage(request));
//                Locale.getDefault().toString());

        long days = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        if (days > 0) return days + " " + I18n.getInstance().translate(days == 1 ? "calendar.day" : "calendar.recurrence.days",
                Renders.getHost(request), I18n.acceptLanguage(request));
//                Locale.getDefault().toString());

        long hours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        return hours + " " + I18n.getInstance().translate(hours == 1 ? "calendar.hour" : "calendar.hours.lc",
                Renders.getHost(request), I18n.acceptLanguage(request));
//                Locale.getDefault().toString());
    }

    private Future<Void> sendReminderEmail(ReminderModel reminder) {
        Promise<Void> promise = Promise.promise();

        getCalendarEvent(reminder.getEventId())
                .compose(calendarEvent -> {
                    log.info(String.format("Calevent : %s", calendarEvent.toString()));
                    try {
                        return sendEmail(reminder, calendarEvent);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onSuccess(result -> promise.complete())
                .onFailure(error -> {
                    String errMessage = String.format("[Calendar@%s::sendReminderEmail]:  " +
                                    "an error has occurred while sending email reminder: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(errMessage);
                    promise.fail(errMessage);
                });

        return promise.future();
    }

    private Future<JsonObject> sendEmail(ReminderModel reminder, JsonObject calendarEvent) throws ParseException {
        Promise<JsonObject> promise = Promise.promise();

//        EXAMPLE
//        Bonjour,
//        Nous vous rappelons que l’événement “[Nom de l'événement]”commence dans [durer].
//
//        Détails de l’événement :
//        [Description]
//        Lieu : [Lieu]
//        Du dd/mm/yyyy à hh:mm au dd/mm/yyyy à hh:mm
//        Consultez l’application Agenda pour en savoir plus.
//
//        Avec :
//        Si pas de description on n'affiche pas la ligne dans la liste à puces
//        Si pas de lieu on n'affiche pas la ligne dans la liste à puces
//        Si l'événement dure toute la journée, on affiche juste "Du dd/mm/yyyy au dd/mm/yyyy"
//        Agenda = lien vers l'application Agenda / Calendar

//        "calendar.search.date.to": "à",
//        "calendar.filters.date.from": "Du",
//        "calendar.filters.date.to": "au",


//        StringBuilder body = new StringBuilder(I18nHelper.getI18nValue(I18nKeys.CALENDAR_HELLO, Locale.getDefault().toString())
//                + ", <br /> <br />"
//                + "Nous vous rappelons que l’événement \"" + calendarEvent.getString(Field.TITLE, "") + "\" commence dans "
//                + getRemainingTime(reminder.getReminderFrequency().get(0), calendarEvent.getString(Field.STARTMOMENT)) + "."
//                + "<ul>"
//                + (calendarEvent.containsKey(Field.DESCRIPTION) ? ("<br/>" + "<li>" + "Détails de l’événement" + " :" + "<br/>" + calendarEvent.getString(Field.DESCRIPTION) + "</li>") : "")
//                + (calendarEvent.containsKey(Field.LOCATION) ? ("<br/>" + "<li>" + "Lieu :" + calendarEvent.getString(Field.LOCATION) + "</li>") : "")
//                + "<br/>" + "<li>" +  "Du " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.DATE_MONTH_YEAR)
//                + (Boolean.FALSE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES)) : "")
//                + " au " + DateUtils.getStringDate(calendarEvent.getString(Field.ENDMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.DATE_MONTH_YEAR)
//                + (Boolean.FALSE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.ENDMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES)  + "</li>") : "")
//                + (Boolean.TRUE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES) + "</li>") : "")
//                + "</ul>"
//                + "<br/>" +  I18nHelper.getI18nValue(I18nKeys.CALENDAR_REMINDER_PUSH_NOTIF,
//                Locale.getDefault().toString()) //"Consultez l’application " + "Agenda" + " pour en savoir plus."
//                );

        StringBuilder body = new StringBuilder("Bonjour, <br /> <br />"
                + "Nous vous rappelons que l’événement \"" + calendarEvent.getString(Field.TITLE, "") + "\" commence dans 1 jour."
                + "<ul>"
                + (calendarEvent.containsKey(Field.DESCRIPTION) ? ("<br/>" + "<li>" + "Détails de l’événement :" + "<br/>" + calendarEvent.getString(Field.DESCRIPTION) + "</li>") : "")
                + (calendarEvent.containsKey(Field.LOCATION) ? ("<br/>" + "<li>" + "Lieu :" + calendarEvent.getString(Field.LOCATION) + "</li>") : "")
                + "<br/>" + "<li>" +  "Du " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.DATE_MONTH_YEAR)
                + (Boolean.FALSE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES)) : "")
                + " au " + DateUtils.getStringDate(calendarEvent.getString(Field.ENDMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.DATE_MONTH_YEAR)
                + (Boolean.FALSE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.ENDMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES)  + "</li>") : "")
                + (Boolean.TRUE.equals(calendarEvent.getBoolean(Field.ALLDAY_LC)) ? (" à " + DateUtils.getStringDate(calendarEvent.getString(Field.STARTMOMENT, ""), DateUtils.DATE_FORMAT_UTC, DateUtils.HOURS_MINUTES) + "</li>") : "")
                + "</ul>"
                + "<br/>" +  "Consultez l’application " + "Agenda" + " pour en savoir plus."
        );



        log.info(String.format("sendEmail body : %s", body));

        JsonObject message = new JsonObject()
                .put(Field.SUBJECT, I18nHelper.getI18nValue(I18nKeys.CALENDAR_REMINDER_EMAIl_TITLE, Locale.getDefault().toString()))
                .put(Field.BODY, body)
                .put(Field.TO, new JsonArray())
                .put(Field.CCI, reminder.getOwner().userId());

        JsonObject action = new JsonObject()
                .put(Field.ACTION, Field.SEND)
                .put(Field.USERID, reminder.getOwner().userId())
                .put(Field.USERNAME, reminder.getOwner().displayName())
                .put(Field.MESSAGE, message);


        eb.request(EventBusAction.CONVERSATION_ADDRESS.method(), action, (Handler<AsyncResult<Message<JsonObject>>>) messageEvt -> {
            if (!messageEvt.result().body().getString(Field.STATUS).equals(Field.OK)) {
                log.error("[Formulaire@FormController::sendReminder] Failed to send email reminder : " + messageEvt.cause());
                promise.fail(messageEvt.cause());
            } else {
                promise.complete(messageEvt.result().body());
            }
        });

        return promise.future();
    }
}
