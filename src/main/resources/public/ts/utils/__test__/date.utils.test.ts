import {DateUtils} from '../date.utils'
import {moment} from "entcore";
import {FORMAT} from "../../core/const/date-format.ts";

const date = '2019-04-16T18:15:00.000Z'

describe('dateFrFormat', () => {

    test(`Using "${FORMAT.displayFRDate}" which is a date format DD/MM/YYYY, it should returns "16/04/2019"`, () => {
        expect(DateUtils.getSimpleFRDateFormat(date)).toEqual("16/04/2019");
    });
});