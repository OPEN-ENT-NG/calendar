package net.atos.entng.calendar.services.utils;

import io.vertx.core.json.JsonObject;

import java.util.Date;

public class Course {
    private String title;
    private boolean allDay;
    private JsonObject recurrence;
    private boolean isRecurrent;
    private int index;
    private String startMoment;
    private String endMoment;
    private User owner;
    private String parentId;
    private String calendarId;
    public Course() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public JsonObject getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(JsonObject recurrence) {
        this.recurrence = recurrence;
    }

    public boolean isRecurrent() {
        return isRecurrent;
    }

    public void setRecurrent(boolean recurrent) {
        isRecurrent = recurrent;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getStartMoment() {
        return startMoment;
    }

    public String getEndMoment() {
        return endMoment;
    }

    public void setUser(String userId,String displayName) {
        this.owner = new User(userId,displayName);
    }
    public JsonObject toJson() {
        return new JsonObject()
                .put("title", this.title)
                .put("allDay", allDay)
                .put("recurrence", recurrence)
                .put("isRecurrent", isRecurrent)
                .put("index", index)
                .put("calendar", calendarId)
                .put("startMoment", startMoment)
                .put("endMoment", endMoment)
                .put("owner", owner.toJson());
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setStartMoment(String format) {
        this.startMoment = format;
    }

    public void setEndMoment(String format) {
        this.endMoment = format;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }
}
