package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;
import net.atos.entng.calendar.helpers.IModelHelper;

import java.util.Date;

public class CalendarModel implements IModel<CalendarModel> {


    private final String id;
    private final String title;
    private final String updated;
    private final String color;
    private final JsonObject owner;
    private final Boolean isExternal;

    private final String icsLink;
    private final String platform;
    private final JsonObject created;
    private final JsonObject modified;
    private final Boolean isUpdating;

    public CalendarModel(JsonObject calendar) {
        this.id = calendar.getString(Field._ID, "");
        this.title = calendar.getString(Field.TITLE, "");

        Long updateDate = calendar.getJsonObject(Field.UPDATED, new JsonObject()).getLong(MongoField.$DATE, null);
        this.updated = (updateDate != null) ? new Date(updateDate).toInstant().toString() :  null;

        this.color = calendar.getString(Field.COLOR, null);
        this.owner = calendar.getJsonObject(Field.OWNER, new JsonObject());
        this.isExternal = calendar.getBoolean(Field.ISEXTERNAL, false);
        this.icsLink = calendar.getString(Field.ICSLINK, null);
        this.platform = calendar.getString(Field.PLATFORM, null);
        this.created = calendar.getJsonObject(Field.CREATED, new JsonObject());
        this.modified = calendar.getJsonObject(Field.MODIFIED, new JsonObject());
        this.isUpdating = calendar.getBoolean(Field.ISUPDATING, null);

    }

    public String id() {
        return this.id;
    }

    public String title() {
        return this.title;
    }

    public String updated() {
        return this.updated;
    }

    public String getColor() {
        return color;
    }

    public JsonObject getOwner() {
        return owner;
    }

    public Boolean getExternal() {
        return isExternal;
    }

    public String getIcsLink() {
        return icsLink;
    }

    public String getPlatform() {
        return platform;
    }

    public JsonObject getCreated() {
        return created;
    }

    public JsonObject getModified() {
        return modified;
    }

    public Boolean getIsUpdating() {
        return isUpdating;
    }

    public JsonObject toJson() {
        JsonObject calendarObject = IModelHelper.toJson(this, true, false);

        calendarObject.put(Field._ID, this.id);
        calendarObject.remove(Field.ID);

        return calendarObject;
    }

}
