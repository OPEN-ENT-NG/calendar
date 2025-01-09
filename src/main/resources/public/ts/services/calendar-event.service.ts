import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";
import {Calendar, CalendarEvent} from "../model";
import { CalendarEventReminder } from '../model/reminder';

export interface ICalendarEventService {
    fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse>;
    deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean, isExternal?: boolean): Promise<AxiosResponse>;
    createCalendarEventReminder(calendarId: string, eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse>;
    updateCalendarEventReminder(calendarId: string, eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse>;
}

export const calendarEventService: ICalendarEventService = {
    async fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (startDate && endDate) {
            urlParam = `?startDate=${startDate}&endDate=${endDate}`;
        }
        return http.get(`/calendar/${calendarId}/events${urlParam}`);
    },

    async deleteCalendarEventAndBookings(calendarId: string, eventId: string, deleteAllBookings?: boolean, isExternal?: boolean): Promise<AxiosResponse> {
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
    },

    async createCalendarEventReminder(calendarId: string, eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse> {
        return http.put(`/calendar/${calendarId}/event/${eventId}/reminder`, reminderData.toJson());
    },

    async updateCalendarEventReminder(calendarId: string, eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse> {
        return http.put(`/calendar/${calendarId}/event/${eventId}/reminder`, reminderData.toJson());
    }
};

export const CalendarEventService = ng.service('CalendarEventService', (): ICalendarEventService => calendarEventService);