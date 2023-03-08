package net.atos.entng.calendar.core.enums;

public enum ErrorEnum {

    URL_NOT_AUTHORIZED("URL not authorized"),
    PLATFORM_ALREADY_EXISTS("calendar.platform.already.exists"),
    ZIMBRA_NO_USER("zimbra.no.user"),
    ICAL_EVENTS_CREATED("ical.events.created"),
    CALENDAR_ICAL_EVENT_CREATION_ERROR("calendar.ical.event.creation.error");

    private final String errorEnum;

    ErrorEnum(String errorEnum) {
        this.errorEnum = errorEnum;
    }

    public String method() {
        return this.errorEnum;
    }
}
