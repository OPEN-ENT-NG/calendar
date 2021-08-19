export const rights = {
    resources: {
        shareCalendarSubmit: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|shareCalendarSubmit'
        },
        shareCalendarResource: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|shareResource'
        },
        share: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|shareCalendar'
        },
        manage: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|updateCalendar'
        },
        removeShareCalendar: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|removeShareCalendar'
        },
        deleteCalendar: {
            right: 'net-atos-entng-calendar-controllers-CalendarController|deleteCalendar'
        },
        getIcal: {
            right: 'net-atos-entng-calendar-controllers-EventController|getIcal'
        },
        getEvents: {
            right: 'net-atos-entng-calendar-controllers-EventController|getEvents'
        },
        shareEventResource: {
            right: 'net-atos-entng-calendar-controllers-EventController|shareResource'
        },
        importIcal: {
            right: 'net-atos-entng-calendar-controllers-EventController|importIcal'
        },
        contrib: {
            right: 'net-atos-entng-calendar-controllers-EventController|createEvent'
        },
        deleteEvent: {
            right: 'net-atos-entng-calendar-controllers-EventController|deleteEvent'
        },
        shareEvent: {
            right: 'net-atos-entng-calendar-controllers-EventController|shareEvent'
        },
        updateEvent: {
            right: 'net-atos-entng-calendar-controllers-EventController|updateEvent'
        },
        getEvent: {
            right: 'net-atos-entng-calendar-controllers-EventController|getEvent'
        }
    },
    workflow: {
        admin: 'net.atos.entng.calendar.controllers.CalendarController|createCalendar'
    },
    viewRights: ['net-atos-entng-calendar-controllers-CalendarController|view']
};
