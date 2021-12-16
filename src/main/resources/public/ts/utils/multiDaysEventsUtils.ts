import { moment } from 'entcore';
import { Moment } from 'moment';
import {CalendarEvent, CalendarEvents} from '../model/CalendarEvent';
import {timeConfig} from "../model/constantes";
import {FORMAT} from "../core/const/date-format";

export class multiDaysEventsUtils {

    /**
     * Takes a multiple day event as a parameter and separates it into one-day events
     * @param multiDayEvent a multiple day event
     * @return dividedEvent array of events created from the original event
     */
    static createMultiDayEventParts = (multiDayEvent : CalendarEvent) : CalendarEvent[] => {
        let dividedEvent : CalendarEvent[] = [];
        let temporaryEventDate : Moment = moment(multiDayEvent.startMoment);

        while (moment(multiDayEvent.endMoment).isSameOrAfter(temporaryEventDate, 'day')){
            let temporaryEvent : CalendarEvent = new CalendarEvent(multiDayEvent);
            //all days except the first day of the event
            if (!multiDaysEventsUtils.isEventMultiDays(moment(multiDayEvent.startMoment), temporaryEventDate)){
                temporaryEvent.startMoment = moment(temporaryEventDate).hours(timeConfig.start_hour).minutes(0).second(0).millisecond(0)
                    .utc().format(FORMAT.formattedISODate);
            }
            //all days except the last day of the event
            if (!multiDaysEventsUtils.isEventMultiDays(moment(multiDayEvent.endMoment), temporaryEventDate)){
                temporaryEvent.endMoment = moment(temporaryEventDate).hours(timeConfig.end_hour).minutes(0).second(0).millisecond(0)
                    .utc().format(FORMAT.formattedISODate);
            }
            temporaryEvent.isMultiDayPart = true;
            dividedEvent.push(temporaryEvent);
            temporaryEventDate = moment(temporaryEventDate).add(1, 'day').valueOf();
        }
        return dividedEvent;
    };

    static isEventMultiDays = (startDate : Moment, endDate : Moment) : boolean => {
        return startDate.isSame(endDate, 'day');
    };


}