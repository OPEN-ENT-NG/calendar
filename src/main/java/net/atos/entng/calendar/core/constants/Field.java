package net.atos.entng.calendar.core.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    //Event infos

    // event start moment
    public static final String startMoment = "startMoment";
    // event end moment
    public static final String endMoment = "endMoment";

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
}