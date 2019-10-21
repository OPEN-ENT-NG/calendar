import http from "axios";
import { moment} from "entcore";
import { timeConfig } from "./constantes";
import { Calendar } from "./";
import { Mix, Selectable, Selection } from "entcore-toolkit";
import {getTime, makerFormatTimeInput, utcTime} from './Utils'

export class CalendarEvent implements Selectable{
    _id: String;
    selected: boolean;
    title: string;
    description: string;
    location: string;
    startMoment: Date;
    endMoment: Date;
    allday: boolean;
    recurrence: object;
    parentId : string;
    isRecurrent: boolean;
    index: number;
    calendar : Calendar;
    startMomentDate:Date;
    startMomentTime:Date;
    endMomentDate:Date;
    endMomentTime:Date;
    is_periodic:boolean;
    locked:boolean = true;
    color:Array<string>;
    shared:any;
    owner:any;
    start_date: Date;
    end_date: Date;
    beginning: Date;
    end: Date;
    startTime: Date;
    endTime: Date;
    noMoreRecurrence: boolean;
    detailToRecurrence: boolean;
    startDateToRecurrence: boolean;

    constructor (calendarEvent? : Object){
        this.allday= false;
        this.isRecurrent= false;
        this.index= 0;
        if(calendarEvent) {
            for (let key in calendarEvent){
                if(typeof calendarEvent[key] !== "function") this[key] = calendarEvent[key];
            }
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
     let {data : {_id : id}} = await http.post('/calendar/' + this.calendar._id + '/events', this.toJSON());
        this._id = id;
    };

    async update(){
        this.editDateBeforeSend(false);
       await http.put('/calendar/' + this.calendar._id + '/event/' + this._id, this.toJSON());
    };

    async delete() {
        await http.delete('/calendar/' + this.calendar._id + '/event/' + this._id);

    };

    editDateBeforeSend(isNewEvent){
        if(isNewEvent){
            const addTime:number = moment(this.startMoment)._i ? 0 : - utcTime();
            this.startMoment= moment.utc(getTime(this.startMoment, this.startTime))
                .add( addTime, 'hours');
            this.endMoment= moment.utc(getTime(this.endMoment, this.endTime))
                .add( addTime, 'hours');
        } else {
            this.startMoment= moment.utc(getTime(this.startMoment, this.startTime));
            this.endMoment= moment.utc(getTime(this.endMoment, this.endTime));
        }
    }

    toJSON(){
        return {
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
        }
    };
}

export class CalendarEvents extends Selection<CalendarEvent> {

    behaviours: string;
    filtered: Array<CalendarEvent>;
    filters: filterCalendarEvent;
    calendar: Calendar;
    all: Array<CalendarEvent>;
    isRecurrent: boolean;

    constructor(calendar:Calendar=new Calendar) {
        super([]);
        this.behaviours = 'calendar';
        this.filters = new filterCalendarEvent();
        this.calendar = calendar;
    }

    async sync (calendar){
        let { data } = await http.get('/calendar/' + calendar._id+ '/events');
        this.all = Mix.castArrayAs(CalendarEvent, data);
        this.all.map(  calendarEvent => {
            calendarEvent.calendar = calendar;
            let startDate = moment(calendarEvent.startMoment).second(0).millisecond(0);
            calendarEvent.startMoment = startDate;
            calendarEvent.startMomentDate = startDate.format('DD/MM/YYYY');
            calendarEvent.startMomentTime = startDate.format('hh:mm');
            calendarEvent.start_date = moment.utc(calendarEvent.startMoment)._d;
            calendarEvent.startTime = makerFormatTimeInput(moment(startDate),moment(startDate));
            let endDate = moment(calendarEvent.endMoment).second(0).millisecond(0);
            calendarEvent.endMoment = endDate;
            calendarEvent.endMomentDate = endDate.format('DD/MM/YYYY');
            calendarEvent.endMomentTime = endDate.format('hh:mm');
            calendarEvent.end_date =  moment.utc(calendarEvent.endMoment)._d;
            calendarEvent.endTime = makerFormatTimeInput(moment(endDate), moment(endDate));
            calendarEvent.is_periodic = false;
            calendarEvent.color = calendar.color;
            return calendarEvent;
        });


    };

    getRecurrenceEvents (calendarEvent) {
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

    applyFilters() {
        if(this.all.length !== 0){
            this.filtered = this.all.filter( event => {
                return moment(event.startMoment).isBefore(moment(this.filters.endMoment).add(1, 'day')) &&
                    moment(event.endMoment).isAfter(this.filters.startMoment);
            });
        }
    }
}

export class filterCalendarEvent {
    calendar: Array<object>;
    startMoment : Date;
    endMoment : Date;

    constructor() {
        this.calendar = [{icon: "mine", filter: "isMine"}, {icon: "pending-action", filter: "unProcessed"}];
    }
}






