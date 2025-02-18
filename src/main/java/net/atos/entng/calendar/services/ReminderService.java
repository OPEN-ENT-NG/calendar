package net.atos.entng.calendar.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ReminderService {
    /**
     * Return a platform by id
     * @param eventId The id of the event
     * @param user The user
     * @return {@link Future<JsonObject>} Future containing the reminder
     */
    Future<JsonObject> getEventReminders(String eventId, UserInfos user);

    /**
     * Return a platform by id
     * @return {@link Future<JsonArray>} Future containing the reminders that should be sent this minute
     */
    Future<JsonArray> fetchRemindersToSend();
}
