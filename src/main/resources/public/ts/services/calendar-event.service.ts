import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";

export interface ICalendarEventService {
    fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse>;
}

export const calendarEventService: ICalendarEventService = {
    async fetchCalendarEvents(calendarId: string, startDate?: string, endDate?: string): Promise<AxiosResponse> {
        let urlParam: string = '';
        if (startDate && endDate) {
            urlParam = `?startDate=${startDate}&endDate=${endDate}`;
        }
        return http.get(`/calendar/${calendarId}/events${urlParam}`);
    }
};

export const CalendarEventService = ng.service('CalendarEventService', (): ICalendarEventService => calendarEventService);