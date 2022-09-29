package net.atos.entng.calendar.core.constants;

public class Actions {

    private Actions() {
        throw new IllegalStateException("Utility class");
    }

    //Trace actions
    public static final String CREATE_CALENDAR = "CREATE_CALENDAR";
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

}
