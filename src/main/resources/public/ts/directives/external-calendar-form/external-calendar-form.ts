import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {IParseService, IScope, ITimeoutService} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";
import {idiom as lang} from "entcore";


interface IExternalCalendarFormProps {
    calendar?: CalendarForm;

    onCreateExternalCalendar?(): (calendar: CalendarForm) => void;
}

interface IViewModel extends ng.IController, IExternalCalendarFormProps {
    // for link method
    resetForm?(calendar: CalendarForm): void;
    isFormComplete?(): boolean;
    translate?(text: string): string;
}

interface IExternalCalendarFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: IExternalCalendarFormScope, private $parse: any, private $timeout: ITimeoutService) {
    }

    $onInit() {
    }

    $onDestroy() {
    }

    resetForm(): void {
        // clear input fields
        let urlInput: Element = angular.element(document.getElementById("external-calendar-url-input"));
        urlInput[0].value = "";
        let extCalendarForm: IScope = angular.element(document.getElementById("external-calendar-form")).scope();
        extCalendarForm['externalCalendarForm'].$setPristine();

        //clear color
        this.$timeout(() : void => {
            angular.element(document.getElementsByClassName("color grey")).triggerHandler('click');
        })

        safeApply(this.$scope);
    }

    isFormComplete(): boolean {
        return !!this.$scope.vm.calendar && !!this.$scope.vm.calendar.title && !!this.$scope.vm.calendar.icsLink;
    }

    translate(text: string): string {
        return lang.translate(text);
    }

}

function directive($parse) {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}external-calendar-form/external-calendar-form.html`,
        controllerAs: 'vm',
        scope: {
            calendar: '=',
            onCreateExternalCalendar: '&'
        },
        bindToController: true,
        controller: ['$scope', '$parse', '$timeout', Controller],
        link: function ($scope: IExternalCalendarFormScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            vm.onCreateExternalCalendarAction = (): void => {
                $parse($scope.vm.onCreateExternalCalendar())($scope.vm.calendar);
            }
        }
    }
}

export const externalCalendarForm = ng.directive('externalCalendarForm', directive);