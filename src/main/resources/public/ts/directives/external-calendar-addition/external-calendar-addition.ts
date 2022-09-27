import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {Object} from "core-js";
import {PLATFORM} from "../../core/enum/platform.enum";
import {safeApply} from "../../model/Utils";
import {ICalendarService} from "../../services";
import {defaultColor} from "../../model/constantes";
import {CalendarForm} from "../../model/calendar-form.model";


interface IViewModel {
    calendar: CalendarForm;
    calendarPlatforms: Array<string>;

    openExternalCalendarForm(): void;
    closeExternalCalendarForm(): void;
    createExternalCalendar(calendar: Calendar): Promise<void>;
}

interface IExternalCalendarAdditionProps {
    display: any;

    onUpdateCalendars(): void;
}

interface IExternalCalendarAdditionScope extends IScope {
    vm: IExternalCalendarAdditionProps;
}

class Controller implements ng.IController, IViewModel {
    calendar: CalendarForm;
    calendarPlatforms: Array<string>;

    constructor(private $scope: IExternalCalendarAdditionScope, private calendarService: ICalendarService) {
    }

    $onInit() {
    }

    $onDestroy() {
    }

    openExternalCalendarForm = (): void => {
        this.calendar = new  CalendarForm();
        this.calendar.color = defaultColor;
        this.calendar.isExternal = true;
        this.calendarPlatforms = Object.keys(PLATFORM).filter(key => !isNaN(Number(PLATFORM[key])));
        angular.element(document.getElementsByClassName("color grey")).addClass("selected");
        safeApply(this.$scope);
        this.$scope.vm.display.showPanelExternalCalendar = true;
    }

    closeExternalCalendarForm = (): void => {
        angular.element(document.getElementById("external-calendar-form")).scope().vm.resetForm();
        this.$scope.vm.display.showPanelExternalCalendar = false;
    }

    createExternalCalendar = async (calendar): Promise<void> => {
        await this.calendarService.addExternalCalendar(calendar);
        this.closeExternalCalendarForm();
        this.$scope.$parent.$eval(this.$scope.vm.onUpdateCalendars)();
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}external-calendar-addition/external-calendar-addition.html`,
        controllerAs: 'vm',
        scope: {
            display: '=',
            onUpdateCalendars: '&'
        },
        bindToController: true,
        controller: ['$scope', 'CalendarService', Controller]
    }
}

export const externalCalendarAddition = ng.directive('externalCalendarAddition', directive);