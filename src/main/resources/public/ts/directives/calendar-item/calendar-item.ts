import {model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {Subject} from "rxjs";

interface IViewModel {
    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean): void;
    hideOtherCalendarCheckboxes(calendar: Calendar) : void;
}

interface ICalendarItemProps {
    calendar: Calendar;

    onOpenOrCloseClickedCalendar(): (calendar: Calendar, savePreferences: boolean) => void;
    onUncheckOtherCalendarCheckboxes(): (calendar: Calendar) => void;
}

interface ICalendarItemScope extends IScope {
    vm: ICalendarItemProps;
}

class Controller implements ng.IController, IViewModel {

    constructor(private $scope: ICalendarItemScope) {
    }

    $onInit() {
    }

    $onDestroy() {
    }

    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean) : void {
        this.$scope.$parent.$eval(this.$scope.vm.onOpenOrCloseClickedCalendar)(calendar, savePreferences);
    };

    hideOtherCalendarCheckboxes = (calendar: Calendar) : void => {
        this.$scope.$parent.$eval(this.$scope.vm.onUncheckOtherCalendarCheckboxes)(calendar);
    };
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}calendar-item/calendar-item.html`,
        controllerAs: 'vm',
        scope: {
            calendar: '=',
            onOpenOrCloseClickedCalendar: '&',
            onUncheckOtherCalendarCheckboxes: '&',
        },
        bindToController: true,
        controller: ['$scope', Controller]
    }
}

export const calendarItem = ng.directive('calendarItem', directive);