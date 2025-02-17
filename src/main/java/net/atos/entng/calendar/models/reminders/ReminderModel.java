package net.atos.entng.calendar.models.reminders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;
import net.atos.entng.calendar.models.IModel;
import net.atos.entng.calendar.models.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderModel implements IModel<ReminderModel> {

    private String id;
    private String eventId;
    private User owner;
    private ReminderTypeModel reminderType;
    private List<String> reminderFrequency;

    public ReminderModel(JsonObject reminder) {
        this.id = reminder.getString(Field._ID, "");
        this.eventId = reminder.getString(Field.EVENTID_CAMEL, "");
        this.owner =  new User((JsonObject) reminder.getValue(Field.OWNER, new JsonObject()));
        this.reminderType = new ReminderTypeModel((JsonObject) reminder.getValue(Field.REMINDERTYPE, new JsonObject()));
        this.reminderFrequency = reminder.getJsonArray(Field.REMINDERFREQUENCY, new JsonArray()).getList();;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public ReminderTypeModel getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderTypeModel reminderType) {
        this.reminderType = reminderType;
    }

    public List<String> getReminderFrequency() {
        return reminderFrequency;
    }

    public void setReminderFrequency(List<String> reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }

    public JsonObject toJson() {
        JsonObject reminderObject = IModelHelper.toJson(this, true, false);

        return reminderObject;
    }
}


