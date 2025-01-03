import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {IParseService, IScope, ITimeoutService} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";
import {idiom as lang} from "entcore";
import {I18nUtils} from "../../utils/i18n.utils";
import { CalendarEvent } from "../../model";


interface ICalendarReminderFormProps {
    calendarEvent?: CalendarEvent;
    enableCalendarReminder?: boolean;

    onCreateExternalCalendar?(): (calendar: CalendarForm) => void;
    onCloseExternalCalendarForm?(): () => void;
}

interface IViewModel extends ng.IController, ICalendarReminderFormProps {
    i18nUtils?: I18nUtils;

    // resetForm?(calendar: CalendarForm): void;
    // isFormComplete?(): boolean;
    changeSelection?(field: string): void;
    getTranslate?(key: string, params?: string[]): string;
    hasEmailOrZimbra?(): boolean;
}

interface ICalendarReminderFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: ICalendarReminderFormScope, private $parse: any, private $timeout: ITimeoutService) {
    }

    $onInit() {
        this.$scope.vm.i18nUtils = new I18nUtils();
    }

    $onDestroy() {
    }
    
    getTranslate = (key: string, params?: string[]) => {
        return !!params ? this.$scope.vm.i18nUtils.getWithParams(key, params) : this.$scope.vm.i18nUtils.translate(key);
    }

    //todo
    // resetForm(): void {
    //     // clear input fields
    //     let urlInput: Element = angular.element(document.getElementById("external-calendar-url-input"));
    //     urlInput[0].value = "";
    //     let extCalendarForm: IScope = angular.element(document.getElementById("external-calendar-form")).scope();
    //     extCalendarForm['externalCalendarForm'].$setPristine();
    //
    //     //clear color
    //     this.$timeout(() : void => {
    //         angular.element(document.getElementsByClassName("color grey")).triggerHandler('click');
    //     })
    //
    //     safeApply(this.$scope);
    // }

    //todo
    // isFormComplete(): boolean {
    //     return !!this.$scope.vm.calendar && !!this.$scope.vm.calendar.title && (!!this.$scope.vm.calendar.icsLink !== !!this.$scope.vm.calendar.platform);
    // }


    changeSelection(field: string): void {
        switch (field) {
            case "hour":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminderFrequency.hour);
                break;
            case "day":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminderFrequency.day);
                break;
            case "week":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminderFrequency.week);
                break;
            case "month":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminderFrequency.month);
                break;
        }
    }

    private addOrRemoveReminder = (timeMeasurement: number[]): void => {
        if (!!timeMeasurement.length) {
            timeMeasurement = [];
        } else {
            timeMeasurement = [1];
        }
    }

    hasEmailOrZimbra(): boolean {
        return !!model.me.email || !!(model.me.authorizedActions.find(action => action.displayName == "zimbra.view"));
    }

}

function directive($parse) {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}calendarevent-reminder/calendarevent-reminder.html`,
        controllerAs: 'vm',
        scope: {
            calendarEvent: '=',
            // enableCalendarReminder: "=",
            // onCreateExternalCalendar: '&',
            // onCloseExternalCalendarForm: '&'
        },
        bindToController: true,
        controller: ['$scope', '$parse', '$timeout', Controller],
        link: function ($scope: ICalendarReminderFormScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            // vm.onCreateExternalCalendarAction = (): void => {
            //     $parse($scope.vm.onCreateExternalCalendar())($scope.vm.calendar);
            // },
            //     vm.onCloseExternalCalendarFormAction = (): void => {
            //         $parse($scope.vm.onCloseExternalCalendarForm())($scope.vm.calendar);
            //     }
        }
    }
}

export const CalendarReminderForm = ng.directive('CalendarReminderForm', directive);