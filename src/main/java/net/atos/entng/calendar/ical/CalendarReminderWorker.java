package net.atos.entng.calendar.ical;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.core.enums.ReminderCalendarEventWorkerAction;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CalendarReminderWorker extends BusModBase implements Handler<Message<JsonObject>> {
    protected static final Logger log = LoggerFactory.getLogger(CalendarReminderWorker.class);
    public static final String CALENDAR_REMINDER_HANDLER_ADDRESS = "calendar.reminder.handler";
    private ReminderService reminderService;
    private WebClient webClient;


    @Override
    public void start() {
        super.start();
        WebClientOptions options = new WebClientOptions();
        ServiceFactory serviceFactory = new ServiceFactory(vertx, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), WebClient.create(vertx, options));
        this.reminderService = serviceFactory.reminderService();
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
                .forEach(reminder -> remindersActions.add(sendReminder(reminder)));

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
//            reminderActions.add(); //add send notification action
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
}
