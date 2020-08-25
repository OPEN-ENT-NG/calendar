import http from "axios";
import { CalendarEvents } from "./";
import {Rights, notify, Shareable, Behaviours, _, idiom as lang, moment} from 'entcore';
import { Mix, Selectable, Selection } from "entcore-toolkit";

export class Calendar implements Selectable, Shareable{
    _id: string;
    calendarEvents: CalendarEvents;
    selected: boolean;
    color: Array<string>;
    myRights: any;
    shared:any;
    owner:any;
    title: string;
    icsImport: any;
    constructor(calendar?){
        this.calendarEvents = new CalendarEvents(this);
        this.myRights = new Rights(this);
        this.selected= false;
        if (!_.isEmpty(calendar)) {
            this.myRights.fromBehaviours();
            Mix.extend(this,  Behaviours.applicationsBehaviours.calendar.resourceRights(calendar));
        }
    }

    async save () {
        if (this._id){
            await this.update();
        }
        else {
            await this.create();
        }
    };

    async create (){
        let {data} = await http.post('/calendar/calendars', this);
        this._id = data._id;
    };

    async update (){
        await http.put('/calendar/' + this._id, this);
    }

     async delete () {
        await http.delete('/calendar/' + this._id);
    };

    toJSON (){
        return {
            title: this.title,
            color: this.color,
        }
    };
    async importIcal (icalToInput){
        try {
            let {data} = await http.put('/calendar/' + this._id + '/ical', icalToInput);
            this.icsImport = data;
            this.icsImport.invalidEvents.forEach(calendarEvent => {
                calendarEvent.startMoment = moment(calendarEvent.startMoment);
                calendarEvent.endMoment = moment(calendarEvent.endMoment);
            });
        } catch(e){
            notify.error(lang.translate("calendar.notify.icsImportError"));
        }
    }
}

export class Calendars extends Selection<Calendar> {
    behaviours: string;
    preference : Preference;
    all: Array<Calendar>;

    constructor(){
        super([]);
        this.behaviours= 'calendar';
        this.preference = new Preference();
    }

    async sync(){
        let {data} = await http.get('/calendar/calendars');
        this.all = [];
        _.map(data, (calendar)=> {
            let myCalendar = new Calendar(calendar);
            this.all.push(myCalendar);
        });
        await this.syncCalendarEvents();
    }

     async syncCalendarEvents () {
        for( let i = 0 ; i < this.all.length ; i++){
           await this.all[i].calendarEvents.sync(this.all[i], this);
         }
    }

}

export class Preference {
    selectedCalendars : Array<string>;
    async sync () {
        let {data} = await http.get('/userbook/preference/calendar');
        Mix.extend(this,JSON.parse(data.preference)) ;
    };
    async update(){
        http.put('/userbook/preference/calendar', {selectedCalendars: this.selectedCalendars});
    }
}

