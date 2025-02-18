package net.atos.entng.calendar.ical;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;;
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

import java.util.Date;


public class CalendarReminderWorker extends BusModBase implements Handler<Message<JsonObject>> {
    protected static final Logger log = LoggerFactory.getLogger(CalendarReminderWorker.class);
    public static final String CALENDAR_REMINDER_HANDLER_ADDRESS = "calendar.reminder.handler";
    private ReminderService reminderService;
    private WebClient webClient;


    @Override
    public void start() {
        super.start();
        WebClientOptions options = new WebClientOptions();
//        options.setSsl(true);
//        options.setTrustAll(true);
//        if (System.getProperty("httpclient.proxyHost") != null) {
//            ProxyOptions proxyOptions = new ProxyOptions();
//            proxyOptions.setHost(System.getProperty("httpclient.proxyHost"));
//            proxyOptions.setPort(Integer.parseInt(System.getProperty("httpclient.proxyPort")));
//            proxyOptions.setUsername(System.getProperty("httpclient.proxyUsername"));
//            proxyOptions.setPassword(System.getProperty("httpclient.proxyPassword"));
//            proxyOptions.setType(ProxyType.HTTP);
//            options.setProxyOptions(proxyOptions);
//        }
        ServiceFactory serviceFactory = new ServiceFactory(vertx, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), WebClient.create(vertx, options));
        this.reminderService = serviceFactory.reminderService();
//        this.webClient = serviceFactory.webClient();
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        String userId = message.body().getString(Field.USERID);
        UserUtils.getUserInfos(eb, userId, user -> {
            String action = message.body().getString(Field.ACTION, "");
            switch (action) {
                case Field.SEND_REMINDERS:
                    findAndSendReminders();
                    break;
                default:
                    break;
            }
        });
    }

    private void findAndSendReminders() {
        //init email & notif lists

        //fetch this minute's reminders
        ReminderService.fetchRemindersToSend()
                .compose(remindersToSend -> sendReminders(remindersToSend))
                .onSuccess()
                .onFailure()


        //ONE SEND AT THE TIME
        //send email &/or notif
    }
}
