import {CalendarEvent} from "../CalendarEvent";
import {_, angular, Behaviours, model, moment, Rights} from "entcore";
import {Booking, SavedBooking} from "./booking.model";
import {FORMAT} from "../../core/const/date-format";


const rbsViewRight: string = "rbs.view";

export class RbsEmitter {
    scope: any;
    ENABLE_RBS: boolean;

    constructor (scope, ENABLE_RBS) {
        this.scope = scope;
        this.ENABLE_RBS = ENABLE_RBS;
    }

    emitBookingInfo = (event: string, targetSniplet: string, data?: boolean|CalendarEvent): void => {
        if (this.ENABLE_RBS && !!model.me.authorizedActions.find((action) => action.displayName == rbsViewRight)) {
            switch (event) {
                case Behaviours.applicationsBehaviours.rbs.eventerRbs.INIT_BOOKING_INFOS:
                case Behaviours.applicationsBehaviours.rbs.eventerRbs.UPDATE_BOOKING_INFOS:
                    this.scope.$broadcast(targetSniplet + event, data);
                    break;
                case Behaviours.applicationsBehaviours.rbs.eventerRbs.CLOSE_BOOKING_INFOS:
                    this.scope.$broadcast(targetSniplet + event);
                    break;
            }
        }
    };

    updateRbsSniplet = () : void => {
        if (this.ENABLE_RBS) {
            this.scope.rbsEmitter.emitBookingInfo(Behaviours.applicationsBehaviours.rbs.eventerRbs.UPDATE_BOOKING_INFOS, this.scope.calendarEvent);
            this.scope.rbsEmitter.checkBookingValidAndSendInfoToSniplet();
        }
    };

    calendarEventHasBookings = (): boolean => {
        return this.ENABLE_RBS ? (this.scope.calendarEvent.bookings && (this.scope.calendarEvent.bookings.length > 0)) : true;
    };

    calendarEventBookingsSlots = (): Array<string> => {
        if (this.ENABLE_RBS) {
            let finalSlots: Array<string> = [];
            this.scope.calendarEvent.bookings.forEach((booking: SavedBooking) => {
                let bookingDates: string = "";
                bookingDates += moment(moment.utc(booking.start_date, FORMAT.displayDateTimeShortYear).toDate()).format(FORMAT.displayFRDate + " : " + FORMAT.displayTime)
                    + " - " + moment(moment.utc(booking.end_date, FORMAT.displayDateTimeShortYear).toDate()).format(FORMAT.displayTime);

                finalSlots.push(bookingDates);
            })
            return finalSlots;
        } else {
            return [];
        }

    };

    hasAccessToSavedBookings = (): boolean => {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        return (this.ENABLE_RBS && rbsSniplet && rbsSniplet.vm) ? rbsSniplet.vm.hasAccessToSavedBookings() : true;
    };

    prepareBookingToSave(calendarEvent: CalendarEvent): void {
        let rbsSniplet: any = angular.element(document.getElementById("calendar-rbs-booking")).scope();
        if (this.ENABLE_RBS && calendarEvent.hasBooking && rbsSniplet && rbsSniplet.vm) {
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
        if (this.ENABLE_RBS && !!rbsSniplet && !!rbsSniplet.vm) {
            this.scope.calendarEvent.hasBooking = rbsSniplet.vm.hasBooking;
            return (!rbsSniplet.vm.hasBooking || rbsSniplet.vm.isBookingPossible());
        } else {
            return true; // no bookings so no invalid booking form
        }
    };


    bookingExists = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
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
        if (this.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm) {
            return rbsSnipletVm.canViewBooking;
        }
        return false;
    };

    userIsResourceOwner = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all) {
            return rbsSnipletVm.bookings.all.find((b: Booking) => b.resource && (b.resource.owner == model.me.userId));
        }
        return false;
    }

    userIsAdmlForStructure = (): boolean => {
        let rbsSnipletVm: any = this.getRbsSnipletController()['vm'];
        if (this.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
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
        if (this.ENABLE_RBS && !!this.getRbsSnipletController() && !!rbsSnipletVm
            && !!rbsSnipletVm.bookings && !!rbsSnipletVm.bookings.all) {
            return rbsSnipletVm.bookings.all.find((b: Booking) => b.type && b.type.myRights && b.type.myRights.process);
        }
        return false;
    }


}
