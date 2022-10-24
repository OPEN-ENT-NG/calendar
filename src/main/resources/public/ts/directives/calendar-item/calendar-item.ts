import {ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {ICalendarService} from "../../services";
import {DateUtils} from "../../utils/date.utils";

interface IViewModel {
    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean): void;
    hideOtherCalendarCheckboxes(calendar: Calendar) : void;
    updateExternalCalendar($event: MouseEvent) : Promise<void>;
    getLastUpdate(format: string) : string;
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

    constructor(private $scope: ICalendarItemScope, private calendarService: ICalendarService) {
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

    updateExternalCalendar = async ($event: MouseEvent): Promise<void> => {
        $event.stopPropagation();
        await this.calendarService.updateExternalCalendar(this.$scope.vm.calendar);
    };

    getLastUpdate = (format: string): string => {
        return DateUtils.getFormattedString(this.$scope.vm.calendar.updated, format)
    }
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
        controller: ['$scope', 'CalendarService', Controller]
    }
}

export const calendarItem = ng.directive('calendarItem', directive);