import {CalendarEvents} from "./CalendarEvent";

export class CalendarEventReminder {
    private _id?: string;
    private _eventId: string;
    private _reminderType: CalendarEventReminderType;
    private _reminderFrequency: CalendarEventReminderFrequency;

    constructor(
        eventId: string,
        _id?: string
    ) {        
        if (!!_id) {
            this._id = _id;
        }

        if (!eventId) {
            throw new Error("eventId is required");
        }
        this._eventId = eventId;

        this._reminderType = new CalendarEventReminderType();

        this._reminderFrequency = new CalendarEventReminderFrequency();
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
            hour: value.hour ?? [],
            day: value.day ?? [],
            week: value.week ?? [],
            month: value.month ?? [],
        };
    }

    toJson() {
        return {
            id: this._id,
            eventId: this._eventId,
            reminderType: this._reminderType,
            reminderFrequency: this._reminderFrequency,
        };
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
    hour: number[];
    day: number[];
    week: number[];
    month: number[];
    
    constructor() {
        this.hour = [];
        this.day = [];
        this.week = [];
        this.month = [];
    }
    
};