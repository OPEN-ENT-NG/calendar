package net.atos.entng.calendar.core.constants;

public class Actions {

    private Actions() {
        throw new IllegalStateException("Utility class");
    }

    //Trace actions
    public static final String ID = "id";
    public static final String CREATE_CALENDAR = "createCalendar";
    public static final String UPDATE_CALENDAR = "UpdateCalendar";
    public static final String DELETE_CALENDAR = "DeleteCalendar";
    public static final String SHARE_CALENDAR_SUBMIT = "ShareCalendarSubmit";
    public static final String SHARE_CALENDAR_REMOVE = "ShareCalendarRemove";
    public static final String SHARE_CALENDAR = "ShareCalendar";
    public static final String CREATE_EVENT = "CreateEvent";
    public static final String UPDATE_EVENT = "UpdateEvent";
    public static final String DELETE_EVENT = "DeleteEvent";
    public static final String SHARE_EVENT = "ShareEvent";
    public static final String IMPORT_ICL = "ImportIcal";

}
