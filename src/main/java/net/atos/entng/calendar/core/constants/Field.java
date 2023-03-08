package net.atos.entng.calendar.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    // Common

    //id
    public static final String ID = "id";
    public static final String CREATED = "created";
    public static final String MODIFIED = "modified";

    public static final String REQUEST = "request";
    public static final String USERID = "userId";
    public static final String DISPLAYNAME = "displayName";
    public static final String DOLLAR_DATE = "$date";

    //Calendar infos

    public static final String CALENDARID = "calendarId";
    public static final String IS_DEFAULT = "is_default";
    public static final String UPDATED = "updated";
    public static final String ISUPDATING = "isUpdating";
    public static final String COLOR = "color";


    //Event infos

    //collection
    public static final String CALENDAREVENT = "calendarevent";
    // event
    public static final String calendarEvent = "calendarEvent";
    public static final String EVENTID = "eventid";

    // event start moment
    public static final String STARTMOMENT = "startMoment";
    // event end moment
    public static final String ENDMOMENT = "endMoment";
    // calendars
    public static final String CALENDARS = "calendars";
    public static final String CALENDAR = "calendar";
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
    // location
    public static final String LOCATION = "location";
    // description
    public static final String DESCRIPTION = "description";

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
    public static final String KO = "ko";
    public static final String STATUS = "status";
    public static final String RESULT = "result";
    public static final String RESULTS = "results";
    public static final String MESSAGE = "message";
    public static final String ERROR = "error";

    //Config
    public static final String ENABLERBS = "enableRbs";
    public static final String ENABLE_RBS = "enable-rbs";
    public static final String CALENDARSYNCTTL = "calendarSyncTTL";
    public static final String ENABLEZIMBRA = "enableZimbra";
    public static final String ENABLE_ZIMBRA = "enable-zimbra";

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

    //Resource infos
    public static final String PROFILURI = "profilUri";
    public static final String USERNAME = "username";
    public static final String CALENDARURI = "calendarUri";
    public static final String RESOURCEURI = "resourceUri";
    public static final String TITLE = "title";
    public static final String BODY = "body";
    public static final String PUSHNOTIF = "pushNotif";

    //External calendars fields
    public static final String URL = "url";
    public static final String ISEXTERNAL = "isExternal";
    public static final String ICSLINK = "icsLink";
    public static final String SYNC = "sync";
    public static final String PLATFORM_ID = "platformId";

    //Platforms
    public static final String PLATFORM = "platform";
    public static final String ZIMBRA = "Zimbra";
    public static final String HAS_PLATFORM_ICS = "hasPlatformIcs";
    public static final String PLATFORM_ICS = "platformIcs";


    //Http Methods
    public static final String POST = "post";
    public static final String PUT = "put";

    //Regex
    public static final String REGEX = "regex";

    //Domain
    public static final String DEFAULT_DOMAIN = "default-domain";
    public static final String PREFERENCES = "preferences";
    public static final String LANGUAGE = "language";



}