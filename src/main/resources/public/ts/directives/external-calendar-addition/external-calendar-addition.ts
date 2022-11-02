import {angular, model, ng, toasts} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {Object} from "core-js";
import {safeApply} from "../../model/Utils";
import {ICalendarService} from "../../services";
import {defaultColor} from "../../model/constantes";
import {CalendarForm} from "../../model/calendar-form.model";


interface IViewModel {
    calendar: CalendarForm;

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
        angular.element(document.getElementsByClassName("color grey")).addClass("selected");
        safeApply(this.$scope);
        this.$scope.vm.display.showPanelExternalCalendar = true;
    }

    closeExternalCalendarForm = (): void => {
        angular.element(document.getElementById("external-calendar-form")).scope().vm.resetForm();
        this.$scope.vm.display.showPanelExternalCalendar = false;
    }

    createExternalCalendar = async (calendar): Promise<void> => {
        this.closeExternalCalendarForm();
        safeApply(this.$scope);
        //toasts.info("Ajout du calendrier en cours");
        await this.calendarService.addExternalCalendar(calendar);
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