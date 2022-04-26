import {ng} from 'entcore'
import http, {AxiosResponse} from "axios";
import {Calendar} from "../model";

export interface ICalendarService {
    fetchCalendars(): Promise<Array<Calendar>>;
}

export const calendarService: ICalendarService = {
    fetchCalendars(): Promise<Array<Calendar>> {
        return http.get(`/calendar/calendars`).then((response: AxiosResponse) =>
            response.data.map(calendar => new Calendar(calendar)));
    }
};

export const CalendarService = ng.service('CalendarService', (): ICalendarService => calendarService);