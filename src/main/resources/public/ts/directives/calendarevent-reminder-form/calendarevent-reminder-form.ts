import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {IParseService, IScope, ITimeoutService} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";
import {idiom as lang} from "entcore";
import {I18nUtils} from "../../utils/i18n.utils";
import { CalendarEvent } from "../../model";
import {CalendarEventReminder, CalendarEventReminderFrequency, CalendarEventReminderType } from "../../model/reminder";


interface ICalendareventReminderFormProps {
    calendarEvent?: CalendarEvent;
    // enableCalendarReminder?: boolean;

    // onCreateExternalCalendar?(): (calendar: CalendarForm) => void;
    // onCloseExternalCalendarForm?(): () => void;
}

interface IViewModel extends ng.IController, ICalendareventReminderFormProps {
    i18nUtils?: I18nUtils;

    // resetForm?(calendar: CalendarForm): void;
    // isFormComplete?(): boolean;
    changeSelection?(field: string): void;
    getTranslate?(key: string, params?: string[]): string;
    hasEmailOrZimbra?(): boolean;
}

interface ICalendareventReminderFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: ICalendareventReminderFormScope, private $parse: any, private $timeout: ITimeoutService) {
        console.log("constr", this.$scope.vm.calendarEvent);
    }

    $onInit() {
        this.$scope.vm.i18nUtils = new I18nUtils();
        console.log("init", this.$scope.vm.calendarEvent);
        if (!this.$scope.vm.calendarEvent.reminders?.id)
        this.$scope.vm.calendarEvent.reminders = new CalendarEventReminder(this.$scope.vm.calendarEvent._id, new CalendarEventReminderType(), new CalendarEventReminderFrequency());
    }

    $onDestroy() {
        //reset form
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
    // isEventReminderValid = (): boolean => {
    //     return !!(this.$scope.vm.calendarEvent.reminders.reminderType.timeline || this.$scope.vm.calendarEvent.reminders.reminderType.email) 
    //         && (!!this.$scope.vm.calendarEvent.reminders.reminderFrequency.hour.length 
    //             || !!this.$scope.vm.calendarEvent.reminders.reminderFrequency.day.length 
    //             || !!this.$scope.vm.calendarEvent.reminders.reminderFrequency.week.length 
    //             || !!this.$scope.vm.calendarEvent.reminders.reminderFrequency.month.length );
    // }


    changeSelection(field: string): void {
        console.log(this.$scope.vm.calendarEvent);

        switch (field) {
            case "hour":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminders.reminderFrequency.hour);
                break;
            case "day":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminders.reminderFrequency.day);
                break;
            case "week":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminders.reminderFrequency.week);
                break;
            case "month":
                this.addOrRemoveReminder(this.$scope.vm.calendarEvent.reminders.reminderFrequency.month);
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
        templateUrl: `/calendar/public/ts/directives/calendarevent-reminder-form/calendarevent-reminder-form.html`,
        controllerAs: 'vm',
        scope: {
            calendarEvent: '='
            // enableCalendarReminder: "=",
            // onCreateExternalCalendar: '&',
            // onCloseExternalCalendarForm: '&'
        },
        bindToController: true,
        controller: ['$scope', '$parse', '$timeout', Controller],
        link: function ($scope: ICalendareventReminderFormScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            console.log("debut", $scope.vm.calendarEvent);
            // vm.onCreateExternalCalendarAction = (): void => {
            //     $parse($scope.vm.onCreateExternalCalendar())($scope.vm.calendar);
            // },
            //     vm.onCloseExternalCalendarFormAction = (): void => {
            //         $parse($scope.vm.onCloseExternalCalendarForm())($scope.vm.calendar);
            //     }
        }
    }
}

export const calendareventReminderForm = ng.directive('calendareventReminderForm', directive);