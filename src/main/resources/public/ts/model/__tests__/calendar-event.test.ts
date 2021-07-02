import {CalendarEvent} from "../CalendarEvent";
import axios, {AxiosRequestConfig} from 'axios';
import MockAdapter from 'axios-mock-adapter';

describe('CalendarEvent', () => {

    const calendarEvent = new CalendarEvent();

    it('should return data when delete with calendarEvent is correctly called', () => {
        let mock = new MockAdapter(axios);
        const data = {response: true};
        calendarEvent._id = '_id';
        let calendar = Object.create({}, {'_id': {value: '_id'}})
        calendarEvent.calendar = [calendar];
        let correctData;
        mock.onDelete('/calendar/' + calendarEvent.calendar[0]._id + '/event/' + calendarEvent._id).reply(
            (_: AxiosRequestConfig) => new Promise(() => correctData = data)
        );
        calendarEvent.delete().then(response => {
            expect(response).toEqual(data);
        });
    });

});