package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;

import java.util.Date;

public class CalendarModel {


    private final String id;
    private final String title;
    private final String updated;
    private final String color;
    private final JsonObject owner;
    private final Boolean isExternal;

    private final String icsLink;
    private final String platform;

    public CalendarModel(JsonObject calendar) {
        this.id = calendar.getString(Field._ID, "");
        this.title = calendar.getString(Field.TITLE, "");
        this.updated = new Date(calendar.getJsonObject(Field.UPDATED, new JsonObject()).getLong(MongoField.$DATE, null))
                .toInstant().toString();
        this.color = calendar.getString(Field.COLOR, null);
        this.owner = calendar.getJsonObject(Field.OWNER, new JsonObject());
        this.isExternal = calendar.getBoolean(Field.ISEXTERNAL, false);
        this.icsLink = calendar.getString(Field.ICSLINK);
        this.platform = calendar.getString(Field.PLATFORM, null);

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
}
