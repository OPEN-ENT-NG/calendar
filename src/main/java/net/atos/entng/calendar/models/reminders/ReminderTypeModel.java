package net.atos.entng.calendar.models.reminders;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;
import net.atos.entng.calendar.models.IModel;

public class ReminderTypeModel implements IModel<ReminderTypeModel> {
    private boolean email;
    private boolean timeline;

    public void ReminderType(JsonObject reminderType) {
        this.email = reminderType.getBoolean(Field.EMAIL, false);
        this.timeline = reminderType.getBoolean(Field.TIMELINE,  false);
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    public boolean isTimeline() {
        return timeline;
    }

    public void setTimeline(boolean timeline) {
        this.timeline = timeline;
    }

    public JsonObject toJson() {
        JsonObject calendarObject = IModelHelper.toJson(this, true, false);

        return calendarObject;
    }
}
