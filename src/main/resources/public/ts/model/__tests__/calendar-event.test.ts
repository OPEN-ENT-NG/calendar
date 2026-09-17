import {CalendarEvent} from "../CalendarEvent";
jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));
import {http} from 'entcore-toolkit';

describe('CalendarEvent', () => {


    const calendarEvent = Object.create(CalendarEvent.prototype, {
        'myRights': {}
    })

    it('should return data when delete with calendarEvent is correctly called', () => {
        const data = {response: true};
        calendarEvent._id = '_id';
        let calendar = Object.create({}, {'_id': {value: '_id'}})
        calendarEvent.calendar = [calendar];
        let correctData;
        (http.delete as jest.Mock).mockImplementationOnce(() => new Promise(() => correctData = data));
        calendarEvent.delete().then(response => {
            expect(response).toEqual(data);
        });
        expect(http.delete).toHaveBeenCalledWith('/calendar/' + calendarEvent.calendar[0]._id + '/event/' + calendarEvent._id);
    });

});