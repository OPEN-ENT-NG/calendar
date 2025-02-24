package net.atos.entng.calendar.enums;

import java.util.Arrays;

public enum I18nKeys {
    CALENDAR_REMINDER_PUSH_NOTIF("calendar.reminder.push.notif.body");

    private final String value;

    I18nKeys(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static I18nKeys getI18nKey(String value) {
        return Arrays.stream(I18nKeys.values())
                .filter(i18nKey -> i18nKey.getValue().equals(value))
                .findFirst()
                .orElse(null);
    }
}