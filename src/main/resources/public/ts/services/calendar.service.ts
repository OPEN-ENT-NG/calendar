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
        return http.post(`/calendar/url`, calendar);
    },

    updateExternalCalendar(calendar: Calendar): Promise<AxiosResponse> {
        return http.put(`/calendar/${calendar._id}/url`);
    }
};

export const CalendarService = ng.service('CalendarService', (): ICalendarService => calendarService);