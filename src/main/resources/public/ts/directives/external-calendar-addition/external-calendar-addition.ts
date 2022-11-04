import {angular, model, idiom as lang, ng, toasts} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
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
        try {
            await this.calendarService.addExternalCalendar(calendar);

            let successMessage : string = lang.translate("calendar.the.calendar") + " " + calendar.title + " " + lang.translate("calendar.has.been.added");
            toasts.confirm(successMessage);
            this.$scope.$parent.$eval(this.$scope.vm.onUpdateCalendars)();
        } catch (e) {
            if (e.response.data.message) {
                toasts.info(lang.translate("calendar.external.platform.not.accepted"));
            } else {
                let errorMessage : string = lang.translate("calendar.get.events.error") + " " + calendar.title + ".";
                toasts.warning(errorMessage);
            }

        }
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