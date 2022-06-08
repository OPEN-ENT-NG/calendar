package net.atos.entng.calendar.core.enums;

public enum RbsEventBusActions {
    SAVE_BOOKINGS("save-bookings"),
    DELETE_BOOKINGS("delete-bookings");

    public static final String rbsAddress = "net.atos.entng.rbs";
    private final String eventBusAction;

    RbsEventBusActions(String eventBusAction) {
        this.eventBusAction = eventBusAction;
    }

    public String method() {
        return this.eventBusAction;
    }
}