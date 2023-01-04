package net.atos.entng.calendar.core.enums;

public enum ExternalPlatformEnum {

    ZIMBRA_ADDRESS("fr.openent.zimbra");

    private final String externalPlatformAddress;

    ExternalPlatformEnum(String externalPlatformAddress) {
        this.externalPlatformAddress = externalPlatformAddress;
    }

    public String getEventBusAddress() {
        return this.externalPlatformAddress;
    }
}
