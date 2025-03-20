import {CalendarEvents} from "./CalendarEvent";
import {ICalendarPayload} from "./calendar-form.model";

export interface ICalendarEventReminderPayload {
    _id?: string;
    eventId?: string;
    reminderType: CalendarEventReminderType;
    reminderFrequency: CalendarEventReminderFrequency;
}

export class CalendarEventReminder {
    private _id?: string;
    private _eventId: string;
    private _reminderType: CalendarEventReminderType;
    private _reminderFrequency: CalendarEventReminderFrequency;

    constructor(reminder?: any, eventId?: string) {
        if (!!reminder?.id) this._id = reminder?.id;
        if (!!eventId) {
            this._eventId = eventId;
        } else {
            this._eventId = reminder?.eventId ?? "";
        }
        this._reminderType = reminder?.reminderType ?? new CalendarEventReminderType();
        this._reminderFrequency = reminder?.reminderFrequency ?? new CalendarEventReminderFrequency();
    }

    // Getters and Setters
    public get id(): string | undefined {
        return this._id;
    }

    public set id(value: string | undefined) {
        this._id = value;
    }

    public get eventId(): string {
        return this._eventId;
    }

    public set eventId(value: string) {
        this._eventId = value;
    }

    public get reminderType(): CalendarEventReminderType {
        return this._reminderType;
    }

    public set reminderType(value: CalendarEventReminderType) {
        this._reminderType = {
            email: value.email ?? false,
            timeline: value.timeline ?? false,
        };
    }

    public get reminderFrequency(): CalendarEventReminderFrequency {
        return this._reminderFrequency;
    }

    public set reminderFrequency(value: CalendarEventReminderFrequency) {
        this._reminderFrequency = {
            hour: value.hour ?? false,
            day: value.day ?? false,
            week: value.week ?? false,
            month: value.month ?? false,
        };
    }

    toJSON(): ICalendarEventReminderPayload  {
        let json: ICalendarEventReminderPayload = {
            reminderType: this._reminderType,
            reminderFrequency: this._reminderFrequency,
        };
        if (this._eventId) json.eventId = this._eventId;
        if (this._id) json._id = this._id;

        return json;
    }
}

export class CalendarEventReminderType {
    email: boolean;
    timeline: boolean;
    
    constructor() {
        this.email = false;
        this.timeline = false;
    }
};

export class CalendarEventReminderFrequency {
    hour: boolean;
    day: boolean;
    week: boolean;
    month: boolean;
    
    constructor() {
        this.hour = false;
        this.day = false;
        this.week = false;
        this.month = false;
    }
    
};