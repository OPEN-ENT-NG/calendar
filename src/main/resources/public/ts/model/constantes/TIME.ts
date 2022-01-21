export const timeConfig  = { // 5min slots from 7h00 to 19h55, default 8h00
    intervalTime: 5, // in minutes
    interval: 15, // in minutes
    start_hour: 7,
    end_hour: 20,
    default_hour: 8,
    min: "07:00:00",
    max: "20:00:00",
    step: 300, //second
};

export const periods = {
    every_day_max: 10,
    every_week_max: 10,
    every_month_max: 10,
    every_year_max: 10,
    periodicities: [1, 2, 3, 4], // weeks
    days: [
        1, // monday
        2, // tuesday
        3, // wednesday
        4, // thursday
        5, // friday
        6, // saturday
        0 // sunday
    ],
    occurrences: [] // loaded by function
};

export const periodsConfig = {
    occurrences: {
        start: 1,
        end: 52,
        interval: 1
    }
};

export const recurrence = {
    week_days: {
        1: false,
        2: false,
        3: false,
        4: false,
        5: false,
        6: false,
        7: false
    },
    dayMap: {
        1: "calendar.recurrence.daymap.mon",
        2: "calendar.recurrence.daymap.tue",
        3: "calendar.recurrence.daymap.wed",
        4: "calendar.recurrence.daymap.thu",
        5: "calendar.recurrence.daymap.fri",
        6: "calendar.recurrence.daymap.sat",
        7: "calendar.recurrence.daymap.sun"
    },
    fullDayMap: {
        1: "calendar.day.monday.lc",
        2: "calendar.day.tuesday.lc",
        3: "calendar.day.wednesday.lc",
        4: "calendar.day.thursday.lc",
        5: "calendar.day.friday.lc",
        6: "calendar.day.saturday.lc",
        0: "calendar.day.sunday.lc"
    }
};

export const LANG_CALENDAR = "fr";
