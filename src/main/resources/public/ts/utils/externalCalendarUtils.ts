import {Calendar} from "../model";
import {model, Rights} from "entcore";


export class externalCalendarUtils {

    /**
     * Returns true if the calendar is external
     */
    static isCalendarExternal = (cal: Calendar): boolean => {
        return !!(cal.isExternal && (cal.icsLink || cal.platform));
    }

    /**
    * Returns true if the user has the parameter right
    */
    static checkExternalCalendarRight  = (right: string): boolean => {
        return !!(model.me.authorizedActions.find(action => action.displayName == right));
    }
}