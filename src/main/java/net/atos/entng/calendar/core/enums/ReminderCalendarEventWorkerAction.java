package net.atos.entng.calendar.core.enums;

public enum ReminderCalendarEventWorkerAction {
    SEND_REMINDERS("sendReminders");


    private final String eventAction;

    ReminderCalendarEventWorkerAction(String eventAction) {
        this.eventAction = eventAction;
    }

    public String method() {
        return eventAction;
    }
}


