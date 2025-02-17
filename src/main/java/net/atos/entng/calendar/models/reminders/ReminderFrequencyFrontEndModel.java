package net.atos.entng.calendar.models.reminders;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;
import net.atos.entng.calendar.models.IModel;

public class ReminderFrequencyFrontEndModel implements IModel<ReminderFrequencyFrontEndModel> {
    private boolean hour;
    private boolean day;
    private boolean week;
    private boolean month;

    public ReminderFrequencyFrontEndModel(JsonObject reminderFrequency) {
        this.hour = reminderFrequency.getBoolean(Field.HOUR, false);
        this.day = reminderFrequency.getBoolean(Field.DAY, false);
        this.week = reminderFrequency.getBoolean(Field.WEEK, false);
        this.month = reminderFrequency.getBoolean(Field.MONTH, false);
    }

    public boolean getDay() {
        return day;
    }

    public void setDay(boolean day) {
        this.day = day;
    }

    public boolean getHour() {
        return hour;
    }

    public void setHour(boolean hour) {
        this.hour = hour;
    }

    public boolean getWeek() {
        return week;
    }

    public void setWeek(boolean week) {
        this.week = week;
    }

    public boolean getMonth() {
        return month;
    }

    public void setMonth(boolean month) {
        this.month = month;
    }

    public JsonObject toJson() {
        JsonObject reminderFrequencyObject = IModelHelper.toJson(this, true, false);

        return reminderFrequencyObject;
    }

}
