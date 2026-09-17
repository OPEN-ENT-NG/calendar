import {Calendar} from "../Calendar";
jest.mock('entcore')
jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));
import {http} from 'entcore-toolkit';
describe('Calendar Model', () => {
    const calendar = Object.create(Calendar.prototype, {
        '_id': {value: '5'},
        'myRights': {}
    })
    it('should return data when delete with Calendar is correctly called', () => {
        const data = {response: true};
        let correctData;
        let id = '5';
        (http.delete as jest.Mock).mockImplementationOnce(() => new Promise(() => correctData = data));
        calendar.delete().then(response => {
            expect(correctData).toEqual(data);
        });
        expect(http.delete).toHaveBeenCalledWith('/calendar/' + id);
    });
});