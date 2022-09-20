import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";
import {Calendar} from "../model";
import {ICalendarPayload} from "../model/calendar-form.model";

export interface ICalendarService {
    fetchCalendars(): Promise<Array<Calendar>>;
    addExternalCalendar(calendar: Calendar): Promise<AxiosResponse>;
    updateExternalCalendar(calendar: Calendar): Promise<AxiosResponse>;
}

export const calendarService: ICalendarService = {
    fetchCalendars(): Promise<Array<Calendar>> {
        return http.get(`/calendar/calendars`).then((response: AxiosResponse) =>
            response.data.map(calendar => new Calendar(calendar)));
    },

    async addExternalCalendar(calendar: ICalendarPayload): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (calendar.icsLink) {
            urlParam = `?url=${calendar.icsLink}`;
        }
        return http.post(`/calendar/calendars${urlParam}`, calendar);
    },

    updateExternalCalendar(calendar: Calendar): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (calendar.icsLink) {
            urlParam = `?url=${calendar.icsLink}`;
        }
        return http.put(`/calendar/${calendar._id}/ical${urlParam}`, calendar);
    }
};

export const CalendarService = ng.service('CalendarService', (): ICalendarService => calendarService);