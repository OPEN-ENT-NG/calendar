import {Calendar, CalendarEvent} from "../model";


export class externalCalendarUtils {

    /**
     * Returns true if the calendar is external
     */
    static isCalendarExternal = (cal: Calendar): boolean => {
        return !!(cal.isExternal && cal.icsLink);
    }
}