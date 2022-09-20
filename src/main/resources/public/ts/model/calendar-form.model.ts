import {CalendarEvents} from "./CalendarEvent";
import {color} from "entcore/types/src/ts/editor/options";
import {defaultColor} from "./constantes";

export interface ICalendarPayload {
    color: string;
    title: string;
    isExternal?: boolean;
    icsLink?: string;
    icsLinkPlatform?: string;
}

export class CalendarForm {
    private _color: string;
    private _title: string;
    private _isExternal?: boolean;
    private _icsLink?: string;
    private _icsLinkPlatform?: string;


    constructor() {
        this._color = defaultColor;
    }

    get color(): string {
        return this._color;
    }

    set color(value: string) {
        this._color = value;
    }

    get title(): string {
        return this._title;
    }

    set title(value: string) {
        this._title = value;
    }

    get isExternal(): boolean {
        return this._isExternal;
    }

    set isExternal(value: boolean) {
        this._isExternal = value;
    }

    get icsLink(): string {
        return this._icsLink;
    }

    set icsLink(value: string) {
        this._icsLink = value;
    }

    get icsLinkPlatform(): string {
        return this._icsLinkPlatform;
    }

    set icsLinkPlatform(value: string) {
        this._icsLinkPlatform = value;
    }
    
    toJSON(): ICalendarPayload {
        let json: ICalendarPayload = {
            color: this._color,
            title: this._title
        };

        if (this.isExternalCalendar) {
            json.isExternal = this._isExternal;
            json.icsLink = this._icsLink;
            json.icsLinkPlatform = this._icsLinkPlatform;
        }
        return json;
    }

    isExternalCalendar(): boolean {
        return (this._isExternal && !!this._icsLink && !!this._icsLinkPlatform);
    }

}