import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";


interface IExternalCalendarFormProps {
    calendar?: CalendarForm;
    calendarPlatforms?: Array<string>;

    onCreateExternalCalendar?(): (calendar: CalendarForm) => void;
}

interface IViewModel extends ng.IController, IExternalCalendarFormProps {
    // for link method
    resetForm?(calendar: CalendarForm): void;
}

interface IExternalCalendarFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: IExternalCalendarFormScope, private $parse: any) {
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
        angular.element(document.getElementsByClassName("color grey")).triggerHandler('click');

        safeApply(this.$scope);
    }
}

function directive($parse) {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}external-calendar-form/external-calendar-form.html`,
        controllerAs: 'vm',
        scope: {
            calendar: '=',
            calendarPlatforms: '=',
            onCreateExternalCalendar: '&'
        },
        bindToController: true,
        controller: ['$scope', '$parse', Controller],
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