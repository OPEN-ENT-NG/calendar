package net.atos.entng.calendar.cron;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.enums.ReminderCalendarEventWorkerAction;
import net.atos.entng.calendar.helpers.EventBusHelper;
import net.atos.entng.calendar.ical.CalendarReminderWorker;
import net.atos.entng.calendar.ical.ExternalImportICal;
import net.atos.entng.calendar.services.ReminderService;

public class ReminderCalendarEventCron  implements Handler<Long> {
    private static final Logger log = LoggerFactory.getLogger(ReminderCalendarEventCron.class);

    private final ReminderService reminderService;
    private final EventBus eb;

    public ReminderCalendarEventCron(ReminderService reminderService, EventBus eb) {
        this.reminderService = reminderService;
        this.eb = eb;
    }

    @Override
    public void handle(Long event) {
        log.info(String.format("[Calendar@%s::handle] ReminderCalendarEventCron started", this.getClass().getSimpleName()));
        final JsonObject message = new JsonObject();
        message.put(Field.ACTION, ReminderCalendarEventWorkerAction.SEND_REMINDERS.method());
        eb.request(CalendarReminderWorker.class.getName(), message, result -> {
            if(result.failed()) {
                String errMessage = String.format("[Calendar@%s::handle]: Failed to sync reminders: %s",
                        this.getClass().getSimpleName(), result.cause().getMessage());
                log.error(errMessage);
            } else {
                log.info(String.format("[Calendar@%s::handle]: Synchronized reminders", this.getClass().getSimpleName()));
            }
        });
    }
}
