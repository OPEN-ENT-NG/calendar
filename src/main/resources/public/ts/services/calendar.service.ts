import {ng, toasts} from 'entcore'
import http, {AxiosError, AxiosResponse} from "axios";
import {Calendar} from "../model";
import {ICalendarPayload} from "../model/calendar-form.model";

export interface ICalendarService {
    fetchCalendars(): Promise<Array<Calendar>>;
    fetchCalendarById(calendar): Promise<Calendar>;
    addExternalCalendar(calendar: Calendar): Promise<AxiosResponse>;
    updateExternalCalendar(calendar: Calendar): Promise<AxiosResponse>;
    checkExternalCalendarSync(calendar: Calendar): Promise<AxiosResponse>;
}

export const calendarService: ICalendarService = {
    fetchCalendars(): Promise<Array<Calendar>> {
        return http.get(`/calendar/calendars`).then((response: AxiosResponse) =>
            response.data.map(calendar => new Calendar(calendar)));
    },

    fetchCalendarById(calendar: Calendar): Promise<Calendar> {
        return http.get(`/calendar/calendars/${calendar._id}`)
            .then((response: AxiosResponse) => new Calendar(response.data['calendar'][0]))
            .catch((error: AxiosError) => {
                console.error(error);
                toasts.warning(error);
                return new Calendar();
            });
    },


    async addExternalCalendar(calendar: ICalendarPayload): Promise<AxiosResponse> {
        return http.post(`/calendar/url`, calendar);
    },

    updateExternalCalendar(calendar: Calendar): Promise<AxiosResponse> {
        return http.put(`/calendar/${calendar._id}/url`);
    },

    checkExternalCalendarSync(calendar: Calendar): Promise<AxiosResponse> {
        return http.get(`/calendar/${calendar._id}/url`);
    }
};

export const CalendarService = ng.service('CalendarService', (): ICalendarService => calendarService);