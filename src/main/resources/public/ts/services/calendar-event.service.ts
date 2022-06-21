import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";

export interface ICalendarEventService {
    fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse>;
    deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean): Promise<AxiosResponse>;
}

export const calendarEventService: ICalendarEventService = {
    async fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (startDate && endDate) {
            urlParam = `?startDate=${startDate}&endDate=${endDate}`;
        }
        return http.get(`/calendar/${calendarId}/events${urlParam}`);
    },

    async deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (deleteAllBookings) {
            urlParam = `?deleteBookings=${deleteAllBookings}`;
        }
        return http.delete(`/calendar/${calendarId}/event/${eventId}${urlParam}`);
    }
};

export const CalendarEventService = ng.service('CalendarEventService', (): ICalendarEventService => calendarEventService);