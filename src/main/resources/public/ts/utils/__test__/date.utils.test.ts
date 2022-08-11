import {DateUtils} from '../date.utils'
import {moment} from "entcore";
import {FORMAT} from "../../core/const/date-format";
import {calendarController} from "../../controllers";

const date = '2019-04-16T18:15:00.000Z'

describe('dateFrFormat', () => {

    test(`Using "${FORMAT.displayFRDate}" which is a date format DD/MM/YYYY, it should returns "16/04/2019"`, () => {
        expect(DateUtils.getSimpleFRDateFormat(date)).toEqual("16/04/2019");
    });

    test(`Using "'Tue Aug 23 2022 00:00:00 GMT+0200 (heure d’été d’Europe centrale)'" it should returns '23/08/2022'`, () => {
        const dateTest = new Date ('Tue Aug 23 2022');
        let formatDate = DateUtils.getFRDateFormat(dateTest, "DD/MM/YYYY");
        expect(formatDate).toEqual('23/08/2022');
    });

    test(`Using "'Tue Aug 23 2022 00:00:00 GMT+0200 (heure d’été d’Europe centrale)'" it should returns '23/08/2022'`, () => {
        const dateTest = new Date ('Tue Aug 23 2022');
        let formatDate = DateUtils.getFRDateFormat(dateTest, "HH:mm");
        expect(formatDate).toEqual('00:00');
    });
});