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

    static getFormattedDate = (date: Date, parsingFormat?: string): string => {
        return parsingFormat ? moment(date).format(parsingFormat) : moment(date).format(FORMAT.formattedISODate);
    }

    static getFormattedString = (date: string, parsingFormat?: string): string => {
        return parsingFormat ? moment(date).format(parsingFormat) : moment(date).format(FORMAT.formattedISODate);
    }

    static isDateAfter = (date1 : Moment, date2 : Moment) : boolean => {
        return date1.isAfter(date2, 'day');
    };

}