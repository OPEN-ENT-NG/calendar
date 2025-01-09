import java.util.*;
import java.util.stream.Collectors;

public class ReminderConverter {
    
    public static class ReminderFrontEndModel {
        String _id;
        String eventId;
        String userId;
        Reminder reminder;

        public static class Reminder {
            String _id;
            ReminderType reminderType;
            ReminderFrequency reminderFrequency;

            public static class ReminderType {
                boolean email;
                boolean timeline;
            }

            public static class ReminderFrequency {
                boolean hour;
                boolean day;
                boolean week;
                boolean month;
            }
        }

        // Constructeur, getters et setters
    }

    public static ReminderFrontEndModel convertToReminderFrontEndModel(ReminderModel reminderModel, Date eventStartMoment) {
        ReminderFrontEndModel reminderFrontEndModel = new ReminderFrontEndModel();
        reminderFrontEndModel._id = reminderModel._id;
        reminderFrontEndModel.eventId = reminderModel.eventId;
        reminderFrontEndModel.userId = reminderModel.userId;

        ReminderFrontEndModel.Reminder reminder = new ReminderFrontEndModel.Reminder();
        reminder._id = reminderModel._id; // Hypothèse : _id du reminder correspond à _id de l'objet

        // Convertir reminderType
        ReminderFrontEndModel.Reminder.ReminderType reminderType = new ReminderFrontEndModel.Reminder.ReminderType();
        for (Map<String, Object> type : reminderModel.reminderType) {
            if (type.containsKey("email")) {
                reminderType.email = (boolean) type.get("email");
            }
            if (type.containsKey("timeline")) {
                reminderType.timeline = (boolean) type.get("timeline");
            }
        }
        reminder.reminderType = reminderType;

        // Convertir reminderFrequency
        ReminderFrontEndModel.Reminder.ReminderFrequency reminderFrequency = new ReminderFrontEndModel.Reminder.ReminderFrequency();
        for (Date date : reminderModel.reminderFrequency) {
            long diff = eventStartMoment.getTime() - date.getTime();
            long diffDays = diff / (1000 * 60 * 60 * 24);

            if (diffDays == 1) {
                reminderFrequency.day = true;
            } else if (diffDays == 7) {
                reminderFrequency.week = true;
            } else if (diffDays == 30) {
                reminderFrequency.month = true;
            } else if (diffDays == 0) {
                reminderFrequency.hour = true;
            }
        }
        reminder.reminderFrequency = reminderFrequency;

        reminderFrontEndModel.reminder = reminder;
        return reminderFrontEndModel;
    }

    public static ReminderModel convertToReminderModel(ReminderFrontEndModel reminderFrontEndModel, Date eventStartMoment) {
        ReminderModel reminderModel = new ReminderModel();
        reminderModel._id = reminderFrontEndModel._id;
        reminderModel.eventId = reminderFrontEndModel.eventId;
        reminderModel.userId = reminderFrontEndModel.userId;

        // Convertir reminderType
        List<Map<String, Object>> reminderType = new ArrayList<>();
        if (reminderFrontEndModel.reminder != null && reminderFrontEndModel.reminder.reminderType != null) {
            ReminderFrontEndModel.Reminder.ReminderType rt = reminderFrontEndModel.reminder.reminderType;
            if (rt.email) {
                reminderType.add(Collections.singletonMap("email", true));
            }
            if (rt.timeline) {
                reminderType.add(Collections.singletonMap("timeline", true));
            }
        }
        reminderModel.reminderType = reminderType;

        // Convertir reminderFrequency
        List<Date> reminderFrequency = new ArrayList<>();
        if (reminderFrontEndModel.reminder != null && reminderFrontEndModel.reminder.reminderFrequency != null) {
            ReminderFrontEndModel.Reminder.ReminderFrequency rf = reminderFrontEndModel.reminder.reminderFrequency;
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
