import {ng, toasts} from 'entcore'
import {http, HttpError, HttpResponse} from "entcore-toolkit";
import {Calendar} from "../model";
import {ICalendarPayload} from "../model/calendar-form.model";

export interface ICalendarService {
    fetchCalendars(): Promise<Array<Calendar>>;
    fetchCalendarById(calendar): Promise<Calendar>;
    addExternalCalendar(calendar: Calendar): Promise<HttpResponse>;
    updateExternalCalendar(calendar: Calendar): Promise<HttpResponse>;
    checkExternalCalendarSync(calendar: Calendar): Promise<HttpResponse>;
}

export const calendarService: ICalendarService = {
    fetchCalendars(): Promise<Array<Calendar>> {
        return http.get(`/calendar/calendars`).then((response: HttpResponse) =>
            response.data.map(calendar => new Calendar(calendar)));
    },

    fetchCalendarById(calendar: Calendar): Promise<Calendar> {
        return http.get(`/calendar/calendars/${calendar._id}`)
            .then((response: HttpResponse) => new Calendar(response.data['calendar'][0]))
            .catch((error: HttpError) => {
                console.error(error);
                toasts.warning(error);
                return new Calendar();
            });
    },


    async addExternalCalendar(calendar: ICalendarPayload): Promise<HttpResponse> {
        return http.post(`/calendar/url`, calendar);
    },

    updateExternalCalendar(calendar: Calendar): Promise<HttpResponse> {
        return http.put(`/calendar/${calendar._id}/url`);
    },

    checkExternalCalendarSync(calendar: Calendar): Promise<HttpResponse> {
        return http.get(`/calendar/${calendar._id}/url`);
    }
};

export const CalendarService = ng.service('CalendarService', function(): ICalendarService { return calendarService; });