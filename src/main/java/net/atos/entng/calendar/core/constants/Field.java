package net.atos.entng.calendar.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    // Common

    //id
    public static final String id = "_id";

    //Event infos

    // event
    public static final String calendarEvent = "calendarEvent";

    // event start moment
    public static final String STARTMOMENT = "startMoment";
    // event end moment
    public static final String ENDMOMENT = "endMoment";
    // calendars
    public static final String CALENDARS = "calendars";
    // Owner
    public static final String OWNER = "owner";
    //Calendar id
    public static final String _ID = "_id";
    // Event id
    public static final String EVENTID = "eventId";

    // Start and end dates
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    // start date from range
    public static final String STARTDATE = "startDate";
    // end date from range
    public static final String ENDDATE = "endDate";

    // Notification
    public static final String SENDNOTIF = "sendNotif";

    // Attachments
    public static final String ATTACHMENTS = "attachments";

    //Recurrence
    // event is recurrent or not
    public static final String isRecurrent = "isRecurrent";
    // event recurrence information/false if it is not recurrent
    public static final String recurrence = "recurrence";
    // event recurrence type (every day/every week)
    public static final String type = "type";
    // event recurrence type option
    public static final String every_week = "every_week";
    // event recurrence length (1, 2, 3 ... days/week)
    public static final String every = "every";
    // parent id
    public static final String PARENTID = "parentId";


    //Shared Field
    //Shared
    public static final String shared = "shared";
    //Group Id
    public static final String userId = "userId";
    //User Id
    public static final String groupId = "groupId";

    //Document infos
    // document storage id
    public static final String file = "file";
    // document information
    public static final String metadata = "metadata";
    // document name with extension
    public static final String name = "name";

    //Booking infos
    public static final String BOOKINGS = "bookings";
    public static final String HASBOOKING = "hasBooking";
    public static final String SAVE_BOOKINGS = "save-bookings";

    //Event Bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULT = "result";
    public static final String MESSAGE = "message";

    //Config
    public static final String ENABLERBS = "enableRbs";
    public static final String ENABLE_RBS = "enable-rbs";

    //Miscellaneous
    public static final String ALL = "all";

    //Actions
    public static final String ACTION = "action";

}