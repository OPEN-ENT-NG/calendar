//package net.atos.entng.calendar.models;
//
//import net.atos.entng.calendar.core.constants.Field;
//import net.atos.entng.calendar.core.constants.MongoField;
//import net.atos.entng.calendar.helpers.IModelHelper;
//
//import java.util.Date;
//
//public class CalendarEventModel implements IModel<CalendarEventModel> {
//    private final String id;
//    private final String title;
//    private final String updated;
//    private final String color;
//    private final JsonObject owner;
//    private final Boolean isExternal;
//
//    private final String icsLink;
//    private final String platform;
//    private final JsonObject created;
//    private final JsonObject modified;
//    private final Boolean isUpdating;
//
//    public CalendarModel(JsonObject calendarEvent) {
//        this.id = calendarEvent.getString(Field._ID, "");
//        this.title = calendarEvent.getString(Field.TITLE, "");
//
//        Long updateDate = calendarEvent.getJsonObject(Field.UPDATED, new JsonObject()).getLong(MongoField.$DATE, null);
//        this.updated = (updateDate != null) ? new Date(updateDate).toInstant().toString() :  null;
//
//        this.color = calendarEvent.getString(Field.COLOR, null);
//        this.owner = calendarEvent.getJsonObject(Field.OWNER, new JsonObject());
//        this.isExternal = calendarEvent.getBoolean(Field.ISEXTERNAL, false);
//        this.icsLink = calendarEvent.getString(Field.ICSLINK, null);
//        this.platform = calendarEvent.getString(Field.PLATFORM, null);
//        this.created = calendarEvent.getJsonObject(Field.CREATED, new JsonObject());
//        this.modified = calendarEvent.getJsonObject(Field.MODIFIED, new JsonObject());
//        this.isUpdating = calendarEvent.getBoolean(Field.ISUPDATING, null);
//
//    }
//
//    public String id() {
//        return this.id;
//    }
//
//    public String title() {
//        return this.title;
//    }
//
//    public String updated() {
//        return this.updated;
//    }
//
//    public String getColor() {
//        return color;
//    }
//
//    public JsonObject getOwner() {
//        return owner;
//    }
//
//    public Boolean getExternal() {
//        return isExternal;
//    }
//
//    public String getIcsLink() {
//        return icsLink;
//    }
//
//    public String getPlatform() {
//        return platform;
//    }
//
//    public JsonObject getCreated() {
//        return created;
//    }
//
//    public JsonObject getModified() {
//        return modified;
//    }
//
//    public Boolean getIsUpdating() {
//        return isUpdating;
//    }
//
//    public JsonObject toJson() {
//        JsonObject calendarEventObject = IModelHelper.toJson(this, true, false);
//
//        calendarEventObject.put(Field._ID, this.id);
//        calendarEventObject.remove(Field.ID);
//
//        return calendarEventObject;
//    }
//}
