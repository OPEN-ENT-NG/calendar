package net.atos.entng.calendar.utils;

import java.text.SimpleDateFormat;
import java.util.*;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.models.OwnerModel;
import net.atos.entng.calendar.models.User;
import net.atos.entng.calendar.models.reminders.ReminderFrequencyFrontEndModel;
import net.atos.entng.calendar.models.reminders.ReminderFrontEndModel;
import net.atos.entng.calendar.models.reminders.ReminderModel;
import net.atos.entng.calendar.models.reminders.ReminderTypeModel;
import org.entcore.common.user.UserInfos;

public final class ReminderConverter {

    private ReminderConverter()  {}

    public static ReminderFrontEndModel convertToReminderFrontEndModel(ReminderModel reminderModel, Date eventStartMoment) {
        ReminderFrontEndModel reminderFrontEndModel = new ReminderFrontEndModel(new JsonObject());
        reminderFrontEndModel.setId(reminderModel.getId());
        reminderFrontEndModel.setEventId(reminderModel.getEventId());
        reminderFrontEndModel.setReminderType(reminderModel.getReminderType());

        // Convert reminderFrequency
        ReminderFrequencyFrontEndModel frontEndReminderFrequency = new ReminderFrequencyFrontEndModel(new JsonObject());

        reminderModel.getReminderFrequency().stream()
                .map(stringDate -> DateUtils.parseDate(stringDate, DateUtils.DATE_FORMAT_UTC))
                .map(date -> (eventStartMoment.getTime() - date.getTime()) / (1000 * 60 * 60))
                .forEach(diffHours -> {
                    if (diffHours < 24) {
                        frontEndReminderFrequency.setHour(true);
                    }
                    if (diffHours >= 24 && diffHours < 48) {
                        frontEndReminderFrequency.setDay(true);
                    }
                    if (diffHours >= 168 && diffHours < 192) {
                        frontEndReminderFrequency.setWeek(true);
                    }
                    if (diffHours >= 672 && diffHours < 744) { //taking February into account: 28 days = 672h
                        frontEndReminderFrequency.setMonth(true);
                    }
        });
        reminderFrontEndModel.setReminderFrequency(frontEndReminderFrequency);

        return reminderFrontEndModel;
    }

    public static ReminderModel convertToReminderModel(ReminderFrontEndModel reminderFrontEndModel,
                                                       Date eventStartMoment, UserInfos user) {
        ReminderModel reminderModel = new ReminderModel(new JsonObject());
        reminderModel.setId(reminderFrontEndModel.getId());
        reminderModel.setEventId(reminderFrontEndModel.getEventId());
        reminderModel.setReminderType(reminderFrontEndModel.getReminderType());
        reminderModel.setOwner(new OwnerModel(new JsonObject()
                .put(Field.USERID, user.getUserId())
                .put(Field.DISPLAYNAME, user.getUsername())));

        // Convertir reminderFrequency
        List<String> reminderFrequency = new ArrayList<>();
        ReminderFrequencyFrontEndModel reminderFrequencyFront = reminderFrontEndModel.getReminderFrequency();
        if (reminderFrequencyFront != null) {
            if (Boolean.TRUE.equals(reminderFrequencyFront.getMonth())) {
                Date dateMinusMonth = subtractMonths(eventStartMoment, 1);
                reminderFrequency.add(DateUtils.dateToString(dateMinusMonth));
            }
            if (Boolean.TRUE.equals(reminderFrequencyFront.getWeek())) {
                Date dateMinusWeek = new Date(eventStartMoment.getTime() - 7L * 24 * 60 * 60 * 1000); // 7 jours en millisecondes
                reminderFrequency.add(DateUtils.dateToString(dateMinusWeek));
            }
            if (Boolean.TRUE.equals(reminderFrequencyFront.getDay())) {
                Date dateMinusDay = new Date(eventStartMoment.getTime() - 24L * 60 * 60 * 1000); // 1 jour en millisecondes
                reminderFrequency.add(DateUtils.dateToString(dateMinusDay));
            }
            if (Boolean.TRUE.equals(reminderFrequencyFront.getHour())) {
                Date dateMinusHour = new Date(eventStartMoment.getTime() - 60L * 60 * 1000); // 1 heure en millisecondes
                reminderFrequency.add(DateUtils.dateToString(dateMinusHour));
            }
        }
        reminderModel.setReminderFrequency(reminderFrequency);

        return reminderModel;
    }

    // Méthode pour soustraire des mois avec Date
    private static Date subtractMonths(Date date, int months) {
        Date clonedDate = new Date(date.getTime());
        clonedDate.setMonth(clonedDate.getMonth() - months);

        return clonedDate;
    }
}
