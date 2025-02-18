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
        log.info("[Calendar@ReminderCalendarEventCron] ReminderCalendarEventCron started");
        final JsonObject message = new JsonObject();
        message.put(Field.ACTION, ReminderCalendarEventWorkerAction.SEND_REMINDERS);
        EventBusHelper.requestJsonObject(eb, CalendarReminderWorker.CALENDAR_REMINDER_HANDLER_ADDRESS, message)
                .onSuccess(res -> log.info("[Calendar@ReminderCalendarEventCron] Sync reminders"))
                .onFailure(err -> log.error("[Calendar@ReminderCalendarEventCron] Failed to sync reminders"));
    }
}
