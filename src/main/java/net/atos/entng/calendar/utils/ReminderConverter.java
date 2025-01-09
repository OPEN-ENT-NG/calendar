import java.util.*;
import java.util.stream.Collectors;

public class ReminderConverter {

    public static ReminderFrontEndModel convertToReminderFrontEndModel(ReminderModel reminderModel, Date eventStartMoment) {
        ReminderFrontEndModel reminderFrontEndModel = new ReminderFrontEndModel();
        reminderFrontEndModel._id = reminderModel._id;
        reminderFrontEndModel.eventId = reminderModel.eventId;
        reminderFrontEndModel.userId = reminderModel.userId;

        // Convertir reminderType
        ReminderFrontEndModel.ReminderType reminderType = new ReminderType();
        for (Map<String, Object> type : reminderModel.reminderType) {
            if (type.containsKey(Field.EMAIL)) {
                reminderType.email = (boolean) type.get(Field.EMAIL);
            }
            if (type.containsKey(Field.TIMELINE)) {
                reminderType.timeline = (boolean) type.get(Field.TIMELINE);
            }
        }
        reminderFrontEndModel.reminderType = reminderType;

        // Convertir reminderFrequency
        ReminderFrontEndModel.ReminderFrequency reminderFrequency = new ReminderFrequency();
        for (Date date : object1.reminderFrequency) {
            long diffHours = (eventStartMoment.getTime() - date.getTime()) / (1000 * 60 * 60);

            if (diffHours < 24) {
                reminderFrequency.hour = true;
            }
            if (diffHours >= 24 && diffHours < 48) {
                reminderFrequency.day = true;
            }
            if (diffHours >= 168 && diffHours < 192) {
                reminderFrequency.week = true;
            }
            if (diffHours >= 720 && diffHours < 744) {
                reminderFrequency.month = true;
            }
        }
        reminder.reminderFrequency = reminderFrequency;

        reminderFrontEndModel = reminder;
        return reminderFrontEndModel;
    }

    public static ReminderModel convertToReminderModel(ReminderFrontEndModel reminderFrontEndModel, Date eventStartMoment, UserInfos user) {
        ReminderModel reminderModel = new ReminderModel();
        reminderModel._id = reminderFrontEndModel._id;
        reminderModel.eventId = reminderFrontEndModel.eventId;
        reminderModel.user = reminderFrontEndModel.user;

        // Convertir reminderType
        List<Map<String, Object>> reminderType = new ReminderType();
        if (reminderFrontEndModel.reminder != null && reminderFrontEndModel.reminderType != null) {
            ReminderType rt = reminderFrontEndModel.reminderType;
            if (rt.email) {
                reminderType.add(Collections.singletonMap(Field.EMAIL, true));
            }
            if (rt.timeline) {
                reminderType.add(Collections.singletonMap(Field.TIMELINE, true));
            }
        }
        reminderModel.reminderType = reminderType;

        // Convertir reminderFrequency
        List<Date> reminderFrequency = new ArrayList<>();
        if (reminderFrontEndModel.reminder != null && reminderFrontEndModel.reminderFrequency != null) {
            ReminderFrontEndModel.ReminderFrequency rf = reminderFrontEndModel.reminderFrequency;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventStartMoment);

            if (rf.day) {
                Calendar dayCal = (Calendar) calendar.clone();
                dayCal.add(Calendar.DAY_OF_YEAR, -1);
                reminderFrequency.add(dayCal.getTime());
            }
            if (rf.week) {
                Calendar weekCal = (Calendar) calendar.clone();
                weekCal.add(Calendar.DAY_OF_YEAR, -7);
                reminderFrequency.add(weekCal.getTime());
            }
            if (rf.month) {
                Calendar monthCal = (Calendar) calendar.clone();
                monthCal.add(Calendar.MONTH, -1);
                reminderFrequency.add(monthCal.getTime());
            }
            if (rf.hour) {
                Calendar hourCal = (Calendar) calendar.clone();
                hourCal.add(Calendar.HOUR_OF_DAY, -1);
                reminderFrequency.add(hourCal.getTime());
            }
        }
        reminderModel.reminderFrequency = reminderFrequency;

        return reminderModel;
    }
}
