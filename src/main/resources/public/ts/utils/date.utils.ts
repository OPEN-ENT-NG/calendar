import { moment } from 'entcore';
import { FORMAT } from '../core/const/date-format';
import { Moment } from 'moment';

export class DateUtils {

    /**
     * ⚠ Date Utils
     *
     */

    static getSimpleDateFormat = (date: string, parsingFormat?: string): string => {
        let momentDate: Moment = parsingFormat ? moment(date, parsingFormat) : moment(date);
        return momentDate.format(FORMAT.displayDate);
    };

    static getSimpleFRDateFormat = (date: string, parsingFormat?: string): string => {
        let momentDate: Moment = parsingFormat ? moment(date, parsingFormat) : moment(date);
        return momentDate.format(FORMAT.displayFRDate);
    };

}