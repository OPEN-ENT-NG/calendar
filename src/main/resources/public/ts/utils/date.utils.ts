import { moment, idiom as lang } from 'entcore';
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

    static secondsToDaysHoursMinutesSeconds = (seconds : number, toString? : boolean) : string | Array<number> => {
        let days = Math.floor(seconds / (3600*24));
        let hours = Math.floor(seconds % (3600*24) / 3600);
        let minutes = Math.floor(seconds % 3600 / 60);
        let sec = Math.floor(seconds % 60);

        let result = [days, hours, minutes, sec];

        return toString ? DateUtils.daysHoursMinutesSecondsToString(result) : result;
    }

    /**
     * Returns days / hours / minutes / seconds.
     * Examples :
     * "2 hours et 30 seconds"
     * "1 hours, 2 minutes"
     * "3 minutes and 2 seconds"
     * @param timeArray Array<number> [days, hours, minutes, seconds]
     */
    static daysHoursMinutesSecondsToString = (timeArray : Array<number>) : string => {
        if (timeArray.length == 4) {
            let days = (!timeArray[0] || timeArray[0] == 0) ? "" : timeArray[0] + " " + lang.translate("calendar.recurrence.days")
                + (!!timeArray[1] && timeArray[1] != 0 || !!timeArray[2] && timeArray[2] != 0 ? ", "
                    : (!!timeArray[3] || timeArray[3] != 0 ? " " + lang.translate("calendar.and.lc") + " " : "" ));
            let hours = (!timeArray[1] || timeArray[1] == 0) ? "" : timeArray[1] + " " + lang.translate("calendar.hours.lc")
                + (!!timeArray[2] && timeArray[2] != 0 ? ", "
                    : (!!timeArray[3] || timeArray[3] != 0 ? " " + lang.translate("calendar.and.lc") + " " : "" ));
            let minutes = (!timeArray[2] || timeArray[2] == 0) ? "" : timeArray[2] + " " + lang.translate("calendar.minutes.lc")
                + (!!timeArray[3] || timeArray[3] == 0 ? "" : " " + lang.translate("calendar.and.lc") + " ");
            let seconds = (!timeArray[3] || timeArray[3] == 0) ? "" : timeArray[3] + " " + lang.translate("calendar.seconds.lc");

            return days + hours + minutes + seconds;


        } else {
            return "";
        }

    }

}