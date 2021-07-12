import {Calendar} from "../Calendar";
jest.mock('entcore')
import axios, {AxiosRequestConfig} from 'axios';
import MockAdapter from 'axios-mock-adapter';
describe('Calendar Model', () => {
    const calendar = Object.create(Calendar.prototype, {
        '_id': {value: '5'},
        'myRights': {}
    })
    it('should return data when delete with Calendar is correctly called', () => {
        let mock = new MockAdapter(axios);
        const data = {response: true};
        let correctData;
        let id = '5'
        mock.onDelete('/calendar/' + id).reply(
            (_: AxiosRequestConfig) => new Promise(() => correctData = data)
        );
        calendar.delete().then(response => {
            expect(correctData).toEqual(data);
        });
    });
});