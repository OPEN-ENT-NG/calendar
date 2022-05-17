import {CalendarEvent} from "../CalendarEvent";
import {_, angular, Behaviours, moment, Rights} from "entcore";
import {Bookings, SavedBooking} from "./booking.model";
import {FORMAT} from "../../core/const/date-format";
import {safeApply} from "../Utils";

export class RbsEmitter {
    scope: any;

    constructor (scope) {
        this.scope = scope;
    }

    emitBookingInfo = (event: string, data?: boolean|CalendarEvent): void => {
        switch (event) {
            case Behaviours.applicationsBehaviours.rbs.eventerRbs.INIT_BOOKING_INFOS:
                this.scope.$broadcast(event, data);
                break;
            case Behaviours.applicationsBehaviours.rbs.eventerRbs.CAN_EDIT_EVENT:
                this.scope.$broadcast(event, data);
                break;
            case Behaviours.applicationsBehaviours.rbs.eventerRbs.CLOSE_BOOKING_INFOS:
                this.scope.$broadcast(event);
                break;
            case Behaviours.applicationsBehaviours.rbs.eventerRbs.UPDATE_BOOKING_INFOS:
                this.scope.$broadcast(event, data);
                break;
        }
    };

    updateRbsSniplet = () : void => {
        this.scope.rbsEmitter.emitBookingInfo(Behaviours.applicationsBehaviours.rbs.eventerRbs.UPDATE_BOOKING_INFOS, this.scope.calendarEvent);
        this.scope.rbsEmitter.checkBookingValidAndSendInfoToSniplet();
    };

    calendarEventHasBookings = (): boolean => {
        return (this.scope.calendarEvent.bookings && (this.scope.calendarEvent.bookings.length > 0));
    };

    calendarEventBookingsSlots = (): Array<string> => {
        let finalSlots: Array<string> = [];
        this.scope.calendarEvent.bookings.forEach((booking: SavedBooking) => {
            let bookingDates: string = "";
            bookingDates += moment(moment.utc(booking.start_date, FORMAT.displayDateTimeShortYear).toDate()).format(FORMAT.formattedFRDateTimeNoSeconds)
                + " - " + moment(moment.utc(booking.end_date, FORMAT.displayDateTimeShortYear).toDate()).format(FORMAT.formattedFRDateTimeNoSeconds);

            finalSlots.push(bookingDates);
        })
        return finalSlots;
    };

    hasAccessToSavedBookings = (): boolean => {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        if (rbsSniplet && rbsSniplet.vm) {
            return rbsSniplet.vm.hasAccessToSavedBookings();
        }
    };

    prepareBookingToSave(calendarEvent: CalendarEvent): void {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        if (rbsSniplet && rbsSniplet.vm) {
            let bookingsContent = rbsSniplet.vm.prepareBookingsToSave();
            if (bookingsContent.length > 0) {
                calendarEvent.bookings = new Bookings();
                calendarEvent.bookings = {
                    all : [...bookingsContent]
                };
            }
        }
    }

    /**
     * Returns true if the calendarEvent is so that a booking is possible and updates the sniplet
     */
    checkBookingValidAndSendInfoToSniplet = (): boolean => {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        if (this.scope.ENABLE_RBS && !!rbsSniplet && !!rbsSniplet.vm) {
            this.scope.calendarEvent.hasBooking = rbsSniplet.vm.hasBooking;
            return (!rbsSniplet.vm.hasBooking || rbsSniplet.vm.isBookingPossible());
        } else {
            return true; // no bookings so no invalid booking form
        }
    };

    newRecurrenceAdded = (): boolean => {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        if (this.scope.ENABLE_RBS && !!rbsSniplet && !!rbsSniplet.vm) {
            return rbsSniplet.vm.hasRecurrenceBeenAdded();
        }
    };


}
