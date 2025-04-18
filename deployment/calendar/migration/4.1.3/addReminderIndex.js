db.calendar.reminders.createIndex({ eventId: 1, "owner.userId": 1 });
db.calendar.reminders.createIndex({ reminderFrequency: 1 });