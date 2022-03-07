import {ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {CalendarEvent, RbsEmitter} from "../../model";
import {IScope} from "angular";

interface IViewModel {
    enableRbs: boolean;
    rbsEmitter: RbsEmitter;
    calendarEvent: CalendarEvent;
}

class Controller implements ng.IController, IViewModel {
    enableRbs: boolean;
    rbsEmitter: RbsEmitter;
    calendarEvent: CalendarEvent;

    constructor(private $scope: IScope){
    }

    $onInit() {
    }

    $onDestroy() {
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
            calendarEvent: '='
        },
        bindToController: true,
        controller: ['$scope', Controller]
    }
}

export const rbsBookingDeleteInfo = ng.directive('rbsBookingDeleteInfo', directive);