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
    public static final String startMoment = "startMoment";
    // event end moment
    public static final String endMoment = "endMoment";
    //calendars
    public static final String calendars = "calendars";
    //Owner
    public static final String owner = "owner";
    //Calendar id
    public static final String calendarId = "_id";

    // start date from range
    public static final String startDate = "startDate";
    // end date from range
    public static final String endDate = "endDate";

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

}