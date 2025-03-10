import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";
import { CalendarEventReminder } from '../model/reminder.model';

export interface IReminderService {
    createCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse>;
    updateCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder, reminderId: string): Promise<AxiosResponse>;
    deleteReminder(eventId: string, reminderId: string): Promise<AxiosResponse>;
}

export const reminderService: IReminderService = {
    async createCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder): Promise<AxiosResponse> {
        return http.post(`/event/${eventId}/reminder`, reminderData.toJSON());
    },

    async updateCalendarEventReminder(eventId: string, reminderData: CalendarEventReminder, reminderId: string): Promise<AxiosResponse> {
        return http.put(`/event/${eventId}/reminder/${reminderId}`, reminderData.toJSON());
    },

    async deleteReminder(eventId: string, reminderId: string): Promise<AxiosResponse> {
        return http.delete(`/event/${eventId}/reminder/${reminderId}`);
    }
};

export const ReminderService = ng.service('ReminderService', (): IReminderService => reminderService);