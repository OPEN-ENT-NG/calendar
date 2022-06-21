import http from "axios";
import {_, Behaviours, moment, Rights, Shareable, Document, angular} from "entcore";
import { timeConfig } from "./constantes";
import {Booking, Calendar, Calendars} from "./";
import { Mix, Selectable, Selection } from "entcore-toolkit";
import {getTime, makerFormatTimeInput, utcTime} from './Utils'
import {multiDaysEventsUtils} from "../utils/multiDaysEventsUtils";
import {FORMAT} from "../core/const/date-format";
import {calendarEventService, CalendarEventService} from "../services/calendar-event.service";
import {SavedBooking} from "./rbs/booking.model";

export class CalendarEvent implements Selectable, Shareable{
    _id: string;
    selected: boolean;
    title: string;
    description: string;
    location: string;
    startMoment: Date;
    endMoment: Date;
    notifStartMoment: Date;
    notifEndMoment: Date;
    allday: boolean;
    recurrence: boolean|CalendarEventRecurrence;
    parentId : string;
    isRecurrent: boolean;
    index: number;
    calendar: Array<Calendar>;
    startMomentDate:Date;
    startMomentTime:Date;
    endMomentDate:Date;
    endMomentTime:Date;
    is_periodic:boolean;
    locked:boolean = true;
    color:Array<string>;
    shared:any;
    owner:any;
    myRights: any;
    start_date: Date;
    end_date: Date;
    beginning: Date;
    end: Date;
    startTime: Date;
    endTime: Date;
    noMoreRecurrent: boolean; //event was part of a recurrence but is not anymore
    noMoreRecurrence: boolean; //all the recurrence the event was part of is deleted
    detailToRecurrence: boolean;
    startDateToRecurrence: boolean;
    endDateToRecurrence: boolean;
    deleteAllRecurrence: boolean;
    sendNotif: boolean;
    editAllRecurrence: boolean;
    isMultiDayPart: boolean;
    attachments: Array<Document>;
    hasBooking: boolean;
    bookings: Array<SavedBooking|Booking>;
    deleteAllBookings: boolean;

    constructor (calendarEvent? : Object) {
        this.myRights = new Rights(this);
        this.allday = false;
        this.isRecurrent = false;
        this.index = 0;

        if(!_.isEmpty(calendarEvent)) {
            for (let key in calendarEvent){
                if(typeof calendarEvent[key] !== "function") this[key] = calendarEvent[key];
            }
            this.myRights.fromBehaviours();
        }
    }

   async save(){
        if (this.allday) {
            this.startTime = moment().hours(timeConfig.start_hour).minutes(0).second(0).millisecond(0)._d;
            this.endTime = moment().hours(timeConfig.end_hour).minutes(0).second(0).millisecond(0)._d;
        }
        if (this._id){
          await this.update();
        } else {
         await this.create();
        }
    };

    async create(){
        this.editDateBeforeSend(true);
        let {data : {_id : id}} = await http.post('/calendar/' + this.calendar[0]._id + '/events', this.toJSON());
        this._id = id;
    };

    async update(){
        this.editDateBeforeSend(false);
       await http.put('/calendar/' + this.calendar[0]._id + '/event/' + this._id, this.toJSON());
    };

    async delete() {
        await calendarEventService.deleteCalendarEventAndBookings(this.calendar[0]._id, this._id, this.deleteAllBookings);
    };

    editDateBeforeSend(isNewEvent){
        this.startMoment = getTime(this.startMoment, this.startTime);
        this.endMoment = getTime(this.endMoment, this.endTime);
        if(isNewEvent) {
            // The date sent by the calendar directive is WRONG
            // It's a timezoned date sent as UTC. We need to calculate the real UTC time when sent by the directive
            // If not sent by the directive, the date is correct
            const addTime: number = moment(this.startMoment)._i ? 0 : -utcTime(this.startMoment);
            this.startMoment = moment(this.startMoment).add(addTime, 'hours');
            this.endMoment = moment(this.endMoment).add(addTime, 'hours');
        }
        const addTimeNotif = utcTime(this.startMoment);
        this.startMoment= moment.utc(this.startMoment);
        this.endMoment= moment.utc(this.endMoment);
        // CO-860 : Notifications must use timezoned dates
        this.notifStartMoment = moment(this.startMoment).add(addTimeNotif, "hours");
        this.notifEndMoment = moment(this.endMoment).add(addTimeNotif, "hours");
    }

    /**
     * Transform the array of calendars in an array of id of calendars for the data base
     */
    getCalendarId = (): String[] => {
        let calendarIds = new Array<String>();
        this.calendar.forEach(function (calendar) {
            calendarIds.push(calendar._id);
        });
        return calendarIds;
    }

    toJSON(){
        let body: any = {
            title: this.title,
            description: this.description,
            location: this.location,
            allday: this.allday,
            recurrence: this.recurrence,
            parentId : this.parentId,
            isRecurrent: this.isRecurrent,
            index: this.index,
            startMoment: this.startMoment,
            endMoment: this.endMoment,
            // Warning : if format() is changed below, it must be changed in net.atos.entng.calendar.helpers.EventHelper.create() too.
            notifStartMoment: this.notifStartMoment.format("DD/MM/YYYY HH:mm"),
            notifEndMoment: this.notifEndMoment.format("DD/MM/YYYY HH:mm"),
            attachments : this.attachments ? this.attachments.map((attachment: Document) => new Document(attachment).toJSON()) : [],
            bookings: this.bookings,
            hasBooking: this.hasBooking
        }
        if (!this._id) {
            body.calendar = this.getCalendarId();
        }
        if (this.sendNotif === false){
            body.sendNotif = this.sendNotif;
        }
        return body;
    };


}

export class CalendarEvents extends Selection<CalendarEvent> {

    behaviours: string;
    filtered: Array<CalendarEvent>;
    filters: filterCalendarEvent;
    calendar: Calendar;
    all: Array<CalendarEvent>;
    isRecurrent: boolean;
    multiDaysEvents: Array<CalendarEvent>;

    constructor(calendar:Calendar=new Calendar) {
        super([]);
        this.behaviours = 'calendar';
        this.filters = new filterCalendarEvent();
        this.calendar = calendar;
    }

    async sync (calendar, calendars, startDate?: string, endDate?: string): Promise<void> {
        let { data } = await calendarEventService.fetchCalendarEvents(calendar._id, startDate, endDate);
        this.all = [];
        this.multiDaysEvents = [];
        data.forEach(event => this.all.push(new CalendarEvent(event)));
        let thisCalendarEvents = this;
        let dividedMultiDaysEvents : Array<any> = [];
        this.all.forEach(calendarEvent => {
            // Let's reconstruct the "calendar" array from found _id(s).
            let newArray = new Array<Calendar>();
            let idCalendars: any = calendarEvent.calendar;
            if( typeof idCalendars.forEach === "function" ) {
                idCalendars.forEach(function (id){
                    newArray.push(thisCalendarEvents.getCalendarFromId(calendars, id));
                });
            } else if( typeof idCalendars === "string" ) { // Old data format (single _id)
                newArray.push(thisCalendarEvents.getCalendarFromId(calendars, idCalendars));
            } else { // Unknown data format
                console.debug( "[CalendarEvent] Unexpected type of idCalendars" );
                newArray.push( idCalendars );
            }
            calendarEvent.calendar = newArray.filter(e => e!= null);

            if(multiDaysEventsUtils.isEventOneDay(moment(calendarEvent.startMoment), moment(calendarEvent.endMoment))){
                this.formatEventDates(calendarEvent, calendar);
            } else { //multiple-days event
                this.divideMultiDaysEvent(calendarEvent, calendar, dividedMultiDaysEvents);
            }
        });
        this.addMultiDaysEventsPartsToCalendarEvents(dividedMultiDaysEvents);
    };

    private addMultiDaysEventsPartsToCalendarEvents(dividedMultiDaysEvents: Array<any>) {
        //add sub events to event list and remove original event
        dividedMultiDaysEvents.forEach((item : string|CalendarEvent) => {
            if ((item instanceof CalendarEvent) && (item.hasOwnProperty("_id"))) {
                this.all.push(item);
            } else {
                let multiDayEvent: CalendarEvent = this.all.find((event : CalendarEvent) => (event._id === item
                    && !multiDaysEventsUtils.isEventOneDay(moment(event.startMoment), moment(event.endMoment))));
                this.all.splice(this.all.indexOf(multiDayEvent), 1);
            }
        });
    }

    private divideMultiDaysEvent(calendarEvent: CalendarEvent, calendar : Calendar, dividedMultiDaysEvents: Array<any>) {
        this.multiDaysEvents.push(this.formatEventDates(calendarEvent, calendar));
        //separate multi days event into sub events
        let subEvents: CalendarEvent[] = multiDaysEventsUtils.createMultiDayEventParts(calendarEvent);
        dividedMultiDaysEvents.push(subEvents[0]._id);
        subEvents.forEach((subEvent : CalendarEvent) => {
            dividedMultiDaysEvents.push(this.formatEventDates(subEvent, calendar));
        });
    }

    private formatEventDates(calendarEvent: CalendarEvent, calendar : Calendar) : CalendarEvent {
        // Compute dates
        let startDate = moment(calendarEvent.startMoment).second(0).millisecond(0);
        calendarEvent.startMoment = startDate;
        calendarEvent.startMomentDate = startDate.format(FORMAT.displayFRDate);
        calendarEvent.startMomentTime = startDate.format(FORMAT.displayTime);
        calendarEvent.start_date = moment.utc(calendarEvent.startMoment)._d;
        calendarEvent.startTime = makerFormatTimeInput(moment(startDate), moment(startDate));
        let endDate = moment(calendarEvent.endMoment).second(0).millisecond(0);
        calendarEvent.endMoment = endDate;
        calendarEvent.endMomentDate = endDate.format(FORMAT.displayFRDate);
        calendarEvent.endMomentTime = endDate.format(FORMAT.displayTime);
        calendarEvent.end_date = moment.utc(calendarEvent.endMoment)._d;
        calendarEvent.endTime = makerFormatTimeInput(moment(endDate), moment(endDate));
        calendarEvent.is_periodic = false;
        calendarEvent.color = calendar.color;

        return calendarEvent;
    }

    getRecurrenceEvents (calendarEvent : CalendarEvent) {
        let calendarEvents = [];
        let parentId = calendarEvent.parentId ? calendarEvent.parentId : false;
        this.all.forEach(function (item) {
            if (item.parentId && item.parentId === parentId) {
                calendarEvents.push(item);
            }
        });
        if (calendarEvents.length == 1 && calendarEvents[0]._id == calendarEvent._id) {
            calendarEvents = [];
        }
        return calendarEvents;
    };

    /**
     * Transform an id of a calendar into an object Calendar
     * @param calendars object Calendars
     * @param calendarId id of the calendar
     */
    getCalendarFromId = (calendars: Calendars, calendarId): Calendar => {
        let cal = null;
        calendars.all.forEach(function (calendar) {
            if(calendar._id === calendarId){
                cal = calendar;
            }
        });
        return cal;
    }

    applyFilters() {
        if(this.all.length !== 0){
            this.filtered = this.all.filter( event => {
                return moment(event.startMoment).isBefore(moment(this.filters.endMoment).add(1, 'day')) &&
                    moment(event.endMoment).isAfter(this.filters.startMoment);
            });
        }
    }
}

export type CalendarEventRecurrence = {
    week_days: Array<boolean>;
    start_on: string;
    end_on: string;
    type: string;
    every: number;
    end_type: string;
    end_after: number;
};

export class filterCalendarEvent {
    calendar: Array<object>;
    startMoment : Date;
    endMoment : Date;

    constructor() {
        this.calendar = [{icon: "mine", filter: "isMine"}, {icon: "pending-action", filter: "unProcessed"}];
    }
}






