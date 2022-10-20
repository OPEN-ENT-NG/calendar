package net.atos.entng.calendar.core.enums;

public enum ExternalICalEventBusActions {
    POST("post"),
    PUT("put"),
    SYNC("sync"),
    GET("get");

    private final String eventBusAction;

    ExternalICalEventBusActions(String eventBusAction) {
        this.eventBusAction = eventBusAction;
    }

    public String method() {
        return this.eventBusAction;
    }
}