package net.atos.entng.calendar.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface ReminderService {
    /**
     * Return event reminders
     * @param eventId The id of the event
     * @param user The user
     * @return {@link Future<JsonObject>} Future containing the reminder
     */
    Future<JsonObject> getEventReminders(String eventId, UserInfos user);

    /**
     * Return all reminders between the current minute and next minute
     * @return {@link Future<JsonArray>} Future containing the reminders that should be sent this minute
     * WITH ONLY THE FITTING DATES IN REMINDERFREQUENCY
     */
    Future<JsonArray> fetchRemindersToSend();

    /**
     * Create a reminder
     * @param body The fields of the new reminder
     * @return {@link Future<Void>} Future response
     */
    Future<Void> create(JsonObject body);

    /**
     * Update reminder.* @param eventId The reminder's event id
     * @param eventId The reminder's event id
     * @param id the id of the reminder to update
     * @param body the fields to change
     * @return {@link Future<Void>} Future response
     */
    Future<Void> update(String eventId, String id, JsonObject body);

    /**
     * Delete reminder
     * @param eventId The reminder's event id
     * @param id the id of the reminder we want to delete
     * @return {@link Future<Void>} Future response
     */
    Future<Void> delete(String eventId, String id);

//    /**
//     * Delete reminder
//     * @param eventId The reminder's event id
//     * @return {@link Future<Void>} Future response
//     */
//    Future<Void> deleteRemindersByEvent(String eventId);

    /**
     * Get reminders by event id
     * @param eventId The reminder's event id
     * @return {@link Future<List<String>>} Future response
     */
    Future<List<String>> getEventIdReminders(String eventId);
}
