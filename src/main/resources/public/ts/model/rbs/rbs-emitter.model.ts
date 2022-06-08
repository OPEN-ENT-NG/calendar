import {CalendarEvent} from "../CalendarEvent";
import {_, angular, Behaviours, model, moment, Rights} from "entcore";
import {Booking, Bookings, SavedBooking} from "./booking.model";
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
                calendarEvent.bookings = [...bookingsContent];
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


    bookingExists = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.scope.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all) {
            return (rbsSnipletVm.bookings.all.filter((booking: Booking) => booking.created != undefined).length > 0);
        }
        return false;
    };

    getRbsSnipletController(): ng.IScope {
        return angular.element(document.getElementById("calendar-rbs-booking")).scope();
    }

    canViewBooking = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.scope.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm) {
            return rbsSnipletVm.canViewBooking;
        }
        return false;
    };

    userIsResourceOwner = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.scope.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all) {
            return rbsSnipletVm.bookings.all.find((b: Booking) => b.resource && (b.resource.owner == model.me.userId));
        }
        return false;
    }

    userIsAdmlForStructure = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.scope.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all && model.me.functions.ADMIN_LOCAL) {
            let resourceTypeStructures: Array<string> = [];
            rbsSnipletVm.bookings.all
                .filter((b: Booking) => b.type && b.type.schoolId)
                .forEach((book: Booking) => resourceTypeStructures = [...resourceTypeStructures, book.type.schoolId]);

            return model.me.functions.ADMIN_LOCAL.scope.find(schoolId => (resourceTypeStructures.indexOf(schoolId) != -1));
        }
        return false;
    }

    hasManageShareRight = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.scope.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all) {
            return rbsSnipletVm.bookings.all.find((b: Booking) => b.type && b.type.myRights && b.type.myRights.process);
        }
        return false;
    }


}
