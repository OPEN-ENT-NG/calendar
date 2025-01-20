package net.atos.entng.calendar.models.reminders;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;
import net.atos.entng.calendar.models.CalendarModel;
import net.atos.entng.calendar.models.IModel;

public class ReminderFrontEndModel implements IModel<ReminderFrontEndModel> {

    private String id;
    private String eventId;
    private ReminderTypeModel reminderType;
    private ReminderFrequencyFrontEndModel reminderFrequency;

    public void ReminderFrontEnd (JsonObject reminder) {
        this.id = reminder.getString(Field._ID, "");
        this.eventId = reminder.getString(Field.EVENTID_CAMEL, "");
        this.reminderType = (ReminderTypeModel) reminder.getValue(Field.REMINDERTYPE, new ReminderTypeModel());
        this.reminderFrequency = (ReminderFrequencyFrontEndModel) reminder.getValue(Field.REMINDERFREQUENCY, new ReminderFrequencyFrontEndModel(new JsonObject())); //pass through converter
    }

    public ReminderTypeModel getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderTypeModel reminderType) {
        this.reminderType = reminderType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public ReminderFrequencyFrontEndModel getReminderFrequency() {
        return reminderFrequency;
    }

    public void setReminderFrequency(ReminderFrequencyFrontEndModel reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }

    public JsonObject toJson() {
        JsonObject reminderObject = IModelHelper.toJson(this, true, false);

        reminderObject.put(Field._ID, this.id);
        reminderObject.remove(Field.ID);

        return reminderObject;
    }
}


