package net.atos.entng.calendar.core.constants;

public class Actions {

    private Actions() {
        throw new IllegalStateException("Utility class");
    }

    //Trace actions
    public static final String CREATE_CALENDAR = "CREATE_CALENDAR";

    public static final String GET_CALENDAR = "GET_CALENDAR";
    public static final String UPDATE_CALENDAR = "UPDATE_CALENDAR";
    public static final String DELETE_CALENDAR = "DELETE_CALENDAR";
    public static final String SHARE_CALENDAR_SUBMIT = "SHARE_CALENDAR_SUBMIT";
    public static final String SHARE_CALENDAR_REMOVE = "SHARE_CALENDAR_REMOVE";
    public static final String SHARE_CALENDAR = "SHARE_CALENDAR";
    public static final String CREATE_EVENT = "CREATE_EVENT";
    public static final String UPDATE_EVENT = "UPDATE_EVENT";
    public static final String DELETE_EVENT = "DELETE_EVENT";
    public static final String SHARE_EVENT = "SHARE_EVENT";
    public static final String IMPORT_ICAL = "IMPORT_ICAL";
    public static final String SYNC_EXTERNAL_CALENDAR = "SYNC_EXTERNAL_CALENDAR";
    public static final String IMPORT_EXTERNAL_CALENDAR = "IMPORT_EXTERNAL_CALENDAR";
    public static final String CHECK_EXTERNAL_CALENDAR = "CHECK_EXTERNAL_CALENDAR";

    public static final String CREATE_PLATFORM = "CREATE_PLATFORM";
    public static final String DELETE_PLATFORM = "DELETE_PLATFORM";
    public static final String UPDATE_PLATFORM = "UPDATE_PLATFORM";

    public static final String CREATE_REMINDER = "CREATE_REMINDER";
    public static final String DELETE_REMINDER = "DELETE_REMINDER";
    public static final String UPDATE_REMINDER = "UPDATE_REMINDER";
    public static final String UPDATE_ALL_REMINDERS = "UPDATE_ALL_REMINDERS";
    public static final String DELETE_ALL_EVENT_REMINDERS = "DELETE_ALL_EVENT_REMINDERS";
}
