import {ng} from 'entcore'
import {http, HttpResponse} from "entcore-toolkit";

export interface ICalendarEventService {
    fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<HttpResponse>;
    deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean, isExternal?: boolean): Promise<HttpResponse>;
}

export const calendarEventService: ICalendarEventService = {
    async fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<HttpResponse> {
        let urlParam: string = '';
        if (startDate && endDate) {
            urlParam = `?startDate=${startDate}&endDate=${endDate}`;
        }
        return http.get(`/calendar/${calendarId}/events${urlParam}`);
    },

    async deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean, isExternal?: boolean): Promise<HttpResponse> {
        let urlParam: string = '';
        let allBookingParam: string = '';
        let externalCalendarParam: string = '';
        if (deleteAllBookings || isExternal) {
            urlParam = `?`;
        }
        if (deleteAllBookings) {
            allBookingParam = `&deleteBookings=${deleteAllBookings}`;
        }
        if (isExternal) {
            externalCalendarParam = `&url=${isExternal}`;
        }
        return http.delete(`/calendar/${calendarId}/event/${eventId}${urlParam}${allBookingParam}${externalCalendarParam}`);
    }
};

export const CalendarEventService = ng.service('CalendarEventService', function(): ICalendarEventService { return calendarEventService; });