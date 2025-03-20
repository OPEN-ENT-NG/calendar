package net.atos.entng.calendar.core.enums;

public enum EventBusAction {
    CONVERSATION_ADDRESS("org.entcore.conversation");

    private final String ebAction;

    EventBusAction(String ebAction) {
        this.ebAction = ebAction;
    }

    public String method() {
        return ebAction;
    }
}
