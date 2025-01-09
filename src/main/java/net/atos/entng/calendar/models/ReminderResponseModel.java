package net.atos.entng.reminder.front.end.models;

import io.vertx.core.json.JsonObject;


import java.util.ArrayList;
import java.util.Date;

public class ReminderFrontEndModel implements IModel<ReminderFrontEndModel> {

    private final String id;
    private final String eventId;
    private final ReminderType reminderType;
    private final ReminderFrequency reminderFrequency;

    public ReminderFrontEndModel(JsonObject reminder) {
        this.id = reminder.getString(Field._ID, "");
        this.title = reminder.getString(Field.EVENTID_CAMEL, "");

        this.reminderType = reminder.getJsonArray(Field.REMINDERTYPE, new ReminderType());
        this.reminderFrequency = reminder.getJsonArray(Field.REMINDERFREQUENCY, new ReminderFrequency()); //pass through converter
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public JsonObject getOwner() {
        return owner;
    }

    public JsonArray<ReminderType> getReminderType() {
        return reminderType;
    }

    public JsonArray<ReminderFrequency> getReminderFrequency() {
        return reminderFrequency;
    }

    public JsonObject toJson() {
        JsonObject reminderObject = IModelHelper.toJson(this, true, false);

        reminderObject.put(Field._ID, this.id);
        reminderObject.remove(Field.ID);

        return reminderObject;
    }
}

public static class ReminderTypeFrontEndModel {
    boolean email;
    boolean timeline;

    public ReminderType(JsonObject reminderType) {
        this.email = reminder.getJsonObject(Field.EMAIL, new JsonObject().put(Field.EMAIL, false));
        this.timeline = reminder.getJsonObject(Field.TIMELINE, new JsonObject().put(Field.TIMELINE, false));
    }
}

public static class ReminderFrequencyFrontEndModel {
    boolean hour;
    boolean day;
    boolean week;
    boolean month;

    public ReminderFrequency(JsonObject reminderFrequency) {
        this.hour = reminder.getBoolean(Field.HOUR, new JsonObject().put(Field.HOUR, false));
        this.day = reminder.getBoolean(Field.DAY, new JsonObject().put(Field.DAY, false));
        this.week = reminder.getBoolean(Field.WEEK, new JsonObject().put(Field.WEEK, false));
        this.month = reminder.getBoolean(Field.MONTH, new JsonObject().put(Field.MONTH, false));
    }
}

