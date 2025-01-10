package net.atos.entng.calendar.services;

import org.entcore.common.user.UserInfos;

public interface ReminderService {
    /**
     * Return a platform by id
     * @param id The id of the event
     * @return {@link Future<JsonObject>} Future containing the reminder
     */
    Future<JsonObject> getEventReminder(String eventId, UserInfos user);
}
