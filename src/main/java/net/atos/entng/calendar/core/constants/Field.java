package net.atos.entng.calendar.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    // Common

    //id
    public static final String ID = "id";

    //Calendar infos

    public static final String CALENDARID = "calendarId";

    //Event infos

    // event
    public static final String calendarEvent = "calendarEvent";
    public static final String EVENTID = "eventid";

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
    public static final String DELETE_BOOKINGS = "delete-bookings";
    public static final String DELETE_BOOKING_RIGHTS = "delete-booking-rights";
    public static final String DELETEBOOKINGS = "deleteBookings";
    public static final String ISOWNER = "isOwner";

    //Event Bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULT = "result";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";

    //Config
    public static final String ENABLERBS = "enableRbs";
    public static final String ENABLE_RBS = "enable-rbs";

    //Miscellaneous
    public static final String ALL = "all";

    //Actions
    public static final String ACTION = "action";
    public static final String DOMAIN = "domain";
    public static final String ACCEPTLANGUAGE = "acceptLanguage";
    public static final String REQUESTINFO = "requestInfo";

    //ics calendar
    public static final String ICS = "ics";

    //Routes
    public static final String BINDING = "binding";
    public static final String REQUESTMETHOD = "requestMethod";

}