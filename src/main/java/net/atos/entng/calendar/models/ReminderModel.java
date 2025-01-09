package net.atos.entng.reminder.models;

import io.vertx.core.json.JsonObject;


import java.util.ArrayList;
import java.util.Date;

public class ReminderModel implements IModel<ReminderModel> {


    private final String id;
    private final String eventId;
    private final JsonObject owner;
    private final JsonArray<JsonObject> reminderType;
    private final JsonArray<Date> reminderFrequency;

    public CalendarModel(JsonObject reminder) {
        this.id = reminder.getString(Field._ID, "");
        this.title = reminder.getString(Field.EVENTID_CAMEL, "");

        this.reminderType = reminder.getList(Field.COLOR, null);
        this.reminderFrequency = reminder.getJsonArray(Field.OWNER, new JsonObject()); //pass through converter
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

    public JsonArray<JsonObject> getReminderType() {
        return reminderType;
    }

    public JsonArray<Date> getReminderFrequency() {
        return reminderFrequency;
    }

    public JsonObject toJson() {
        JsonObject reminderObject = IModelHelper.toJson(this, true, false);

        reminderObject.put(Field._ID, this.id);
        reminderObject.remove(Field.ID);

        return reminderObject;
    }

}

public static class ReminderTypeModel {
    boolean email;
    boolean timeline;

    public ReminderType(JsonObject reminderType) {
        this.email = reminder.getJsonObject(Field.EMAIL, new JsonObject().put(Field.EMAIL, false));
        this.timeline = reminder.getJsonObject(Field.TIMELINE, new JsonObject().put(Field.TIMELINE, false));
    }
}

public static class ReminderFrequencyModel {
    boolean hour;
    boolean day;
    boolean week;
    boolean month;

    public ReminderFrequency(JsonObject reminderFrequency) {
        this.hour = reminder.getJsonArray(Field.HOUR, new JsonObject().put(Field.HOUR, new JsonArray()));
        this.day = reminder.getJsonArray(Field.DAY, new JsonObject().put(Field.DAY, new JsonArray()));
        this.week = reminder.getJsonArray(Field.WEEK, new JsonObject().put(Field.WEEK, new JsonArray()));
        this.month = reminder.getJsonArray(Field.MONTH, new JsonObject().put(Field.MONTH, new JsonArray()));
    }
}
