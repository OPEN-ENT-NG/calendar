package net.atos.entng.calendar.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.cron.ReminderCalendarEventCron;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	final ReminderCalendarEventCron reminderCalendarEventCron;

	public TaskController(ReminderCalendarEventCron reminderCalendarEventCron) {
		this.reminderCalendarEventCron = reminderCalendarEventCron;
	}

	@Post("api/internal/reminder/calendar-event")
	public void sendCalendarReminder(final HttpServerRequest request) {
		log.info("Triggered send calendar reminder task");
		reminderCalendarEventCron.handle(0L);
		render(request, null, 202);
	}
}
