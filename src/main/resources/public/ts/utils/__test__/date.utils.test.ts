import {DateUtils} from '../date.utils'
import {moment} from "entcore";
import {FORMAT} from "../../core/const/date-format";
import {calendarController} from "../../controllers";

const date = '2019-04-16T18:15:00.000Z'

describe('dateFrFormat', () => {

    test(`Using "${FORMAT.displayFRDate}" which is a date format DD/MM/YYYY, it should return "16/04/2019"`, () => {
        expect(DateUtils.getSimpleFRDateFormat(date)).toEqual("16/04/2019");
    });

    test(`Using "'Mon Sep 05 2022 00:00:00 GMT+0200 (heure d’été d’Europe centrale)'" it should return '05/09/2022 : 00:00'`, () => {
        const dateTest = moment('Mon Sep 05 2022 00:00:00');
        let formatDate = dateTest.format("DD/MM/YYYY" + " : " + "HH:mm");
        expect(formatDate).toEqual('05/09/2022 : 00:00');
    });

    test(`Using "[0, 2, 0, 30]" it should return '2 heures et 30 secondes'`, () => {
        const dateData = [0, 2, 0, 30];
        expect(DateUtils.daysHoursMinutesSecondsToString(dateData)).toEqual('2 calendar.hours.lc calendar.and.lc 30 calendar.seconds.lc');
    });

    test(`Using "[0, 0, 1, 0]" it should return '1 minutes'`, () => {
        const dateData = [0, 0, 1, 0];
        expect(DateUtils.daysHoursMinutesSecondsToString(dateData)).toEqual('1 calendar.minutes.lc');
    });

    test(`Using "[0, 2, null, 30]" it should return '2 heures et 30 secondes'`, () => {
        const dateData = [0, 2, null, 30];
        expect(DateUtils.daysHoursMinutesSecondsToString(dateData)).toEqual('2 calendar.hours.lc calendar.and.lc 30 calendar.seconds.lc');
    });

    test(`Using "[0, 2, 0, 30, 1]" it should return ''`, () => {
        const dateData = [0, 2, 0, 30, 1];
        expect(DateUtils.daysHoursMinutesSecondsToString(dateData)).toEqual('');
    });

    test(`Using "[0, 2, 0]" it should return ''`, () => {
        const dateData = [0, 2, 0];
        expect(DateUtils.daysHoursMinutesSecondsToString(dateData)).toEqual('');
    });
});