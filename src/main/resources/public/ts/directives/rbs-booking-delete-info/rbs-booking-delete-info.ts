import {model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {CalendarEvent, CalendarEvents, RbsEmitter} from "../../model";
import {IScope} from "angular";

interface IViewModel {
    enableRbs: boolean;
    rbsEmitter: RbsEmitter;
    calendarEvent: CalendarEvent;
    calendarEvents: CalendarEvents;

    hasBookingsToDeleteCalendarView(): boolean;

    hasBookingsToDeleteListView(): boolean;

    canDeleteBooking(): boolean;

    listViewSelectedCalendarEvents(): Array<CalendarEvent>;

    listViewSelectedCalendarEventsWithBooking(): Array<CalendarEvent>;

    hasListViewSelectedCalendarEventsWithBooking(): boolean;

    hasOneListViewSelectedCalendarEventsWithBooking(): boolean;
}

class Controller implements ng.IController, IViewModel {
    enableRbs: boolean;
    rbsEmitter: RbsEmitter;
    calendarEvent: CalendarEvent;
    calendarEvents: CalendarEvents;
    display: any;

    constructor(private $scope: IScope){
    }

    $onInit() {
    }

    $onDestroy() {
    }

    /**
     * Returns true if a deletion has been initiated for an event with a booking on Calendar View
     */
    hasBookingsToDeleteCalendarView = (): boolean => {
        return this.rbsEmitter.calendarEventHasBookings() && this.rbsEmitter.canViewBooking();
    }

    /**
     * Returns true if a deletion has been initiated for events including a booking on List View
     */
    hasBookingsToDeleteListView = (): boolean => {
        return this.display.list && this.hasListViewSelectedCalendarEventsWithBooking();
    }

    /**
     * Returns true if the user has the right to delete a booking
     */
    canDeleteBooking = (): boolean => {
        let userIsBookingOwner:boolean = this.calendarEvent.owner.userId == model.me.userId;
        let userIsResourceOwner:boolean = this.rbsEmitter.userIsResourceOwner();
        let userIsAdmlForStructure:boolean = this.rbsEmitter.userIsAdmlForStructure();
        let userHasManageShareRight:boolean = this.rbsEmitter.hasManageShareRight();

        return userIsBookingOwner || userIsResourceOwner || userIsAdmlForStructure || userHasManageShareRight;
    }

    /**
     * Returns the events selected in the list view
     */
    listViewSelectedCalendarEvents = (): Array<CalendarEvent> => {
        return this.calendarEvents.filtered.filter((event: CalendarEvent) => event.selected == true);
    }

    /**
     * Returns the events with booking that are selected in the list view
     */
    listViewSelectedCalendarEventsWithBooking = (): Array<CalendarEvent> => {
        return this.listViewSelectedCalendarEvents().filter((event: CalendarEvent) =>  event.bookings && event.bookings.length > 0);
    }

    /**
     * Returns true if events with booking are selected in the list view
     */
    hasListViewSelectedCalendarEventsWithBooking = (): boolean => {
        return (this.listViewSelectedCalendarEventsWithBooking().length > 0);
    }

    /**
     * Returns true if only one eventis selected in the list view and this event has a booking
     */
    hasOneListViewSelectedCalendarEventsWithBooking = (): boolean => {
        return (this.listViewSelectedCalendarEventsWithBooking().length == 1 && this.listViewSelectedCalendarEvents().length == 1);
    }


}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}rbs-booking-delete-info/rbs-booking-delete-info.html`,
        controllerAs: 'vm',
        scope: {
            enableRbs: "=",
            rbsEmitter: '=',
            calendarEvent: '=',
            calendarEvents: '=',
            display: '='
        },
        bindToController: true,
        controller: ['$scope', Controller]
    }
}

export const rbsBookingDeleteInfo = ng.directive('rbsBookingDeleteInfo', directive);