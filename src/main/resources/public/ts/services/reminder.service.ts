import {ng} from 'entcore'
import {http, HttpResponse} from "entcore-toolkit";
import { CalendarEventReminder } from '../model/reminder.model';

export interface IReminderService {
    createCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder): Promise<HttpResponse>;
    updateCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder, reminderId: string): Promise<HttpResponse>;
    deleteReminder(eventId: string, reminderId: string): Promise<HttpResponse>;
}

export const reminderService: IReminderService = {
    async createCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder): Promise<HttpResponse> {
        return http.post(`/calendar/event/${eventId}/reminder`, reminderData.toJSON());
    },

    async updateCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder, reminderId: string): Promise<HttpResponse> {
        return http.put(`/calendar/event/${eventId}/reminder/${reminderId}`, reminderData.toJSON());
    },

    async deleteReminder(eventId: string, reminderId: string): Promise<HttpResponse> {
        return http.delete(`/calendar/event/${eventId}/reminder/${reminderId}`);
    }
};

export const ReminderService = ng.service('ReminderService', function(): IReminderService { return reminderService; });