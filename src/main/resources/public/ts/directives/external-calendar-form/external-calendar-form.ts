import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {IParseService, IScope, ITimeoutService} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";
import {idiom as lang} from "entcore";
import {I18nUtils} from "../../utils/i18n.utils";


interface IExternalCalendarFormProps {
    calendar?: CalendarForm;
    enableZimbra?: boolean;

    onCreateExternalCalendar?(): (calendar: CalendarForm) => void;
    onCloseExternalCalendarForm?(): () => void;
}

interface IViewModel extends ng.IController, IExternalCalendarFormProps {
    i18nUtils?: I18nUtils;

    // for link method
    resetForm?(calendar: CalendarForm): void;
    isFormComplete?(): boolean;
    changeSelection?(field: string, platform?: string): void;
    translate?(text: string): string;
}

interface IExternalCalendarFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: IExternalCalendarFormScope, private $parse: any, private $timeout: ITimeoutService) {
    }

    $onInit() {
        this.$scope.vm.i18nUtils = new I18nUtils();
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
        return !!this.$scope.vm.calendar && !!this.$scope.vm.calendar.title && (!!this.$scope.vm.calendar.icsLink !== !!this.$scope.vm.calendar.platform);
    }


    changeSelection(field: string, platform?: string): void {
        switch (field) {
            case "icsLink":
                this.$scope.vm.calendar.platform = undefined;
                break;
            case "platform":
                this.$scope.vm.calendar.icsLink = undefined;
                break;
        }
    }

}

function directive($parse) {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}external-calendar-form/external-calendar-form.html`,
        controllerAs: 'vm',
        scope: {
            calendar: '=',
            enableZimbra: "=",
            onCreateExternalCalendar: '&',
            onCloseExternalCalendarForm: '&'
        },
        bindToController: true,
        controller: ['$scope', '$parse', '$timeout', Controller],
        link: function ($scope: IExternalCalendarFormScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            vm.onCreateExternalCalendarAction = (): void => {
                $parse($scope.vm.onCreateExternalCalendar())($scope.vm.calendar);
            },
            vm.onCloseExternalCalendarFormAction = (): void => {
                $parse($scope.vm.onCloseExternalCalendarForm())($scope.vm.calendar);
            }
    }
    }
}

export const externalCalendarForm = ng.directive('externalCalendarForm', directive);