package net.atos.entng.calendar.core.enums;

public enum ReminderCalendarEventWorkerAction {
    SEND_REMINDERS("sendReminders");


    private final String action;

    ReminderCalendarEventWorkerAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return action;
    }
}
