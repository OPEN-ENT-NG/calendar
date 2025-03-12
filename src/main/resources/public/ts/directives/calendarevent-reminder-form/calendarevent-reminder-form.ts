import {angular, model, ng} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {IParseService, IScope, ITimeoutService} from "angular";
import {safeApply} from "../../model/Utils";
import {CalendarForm} from "../../model/calendar-form.model";
import {idiom as lang} from "entcore";
import {I18nUtils} from "../../utils/i18n.utils";
import { CalendarEvent } from "../../model";
import {CalendarEventReminder, CalendarEventReminderFrequency, CalendarEventReminderType } from "../../model/reminder.model";


interface ICalendareventReminderFormProps {
    calendarEvent?: CalendarEvent;

    onEventReminderValid?: () => boolean;
}

interface IViewModel extends ng.IController, ICalendareventReminderFormProps {
    i18nUtils?: I18nUtils;

    changeSelection?(field: string): void;
    getTranslate?(key: string, params?: string[]): string;
    hasEmailOrZimbra?(): boolean;
}

interface ICalendareventReminderFormScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {
    calendarEvent: CalendarEvent;

    constructor(private $scope: ICalendareventReminderFormScope) {
    }

    $onInit() {
        this.$scope.vm.i18nUtils = new I18nUtils();
    //     this.$scope.vm.$watch(() => this.calendarEvent.reminders, (newValue) => {
    //         if (newValue) {
    //             safeApply(this.$scope.vm);
    //         }
    //     }, true);
        if (!!this.$scope.vm.calendarEvent.reminders.id) {
            console.log("no reminder yet");
            this.$scope.vm.calendarEvent.reminders = new CalendarEventReminder();
        }
        this.$scope.vm.$watch(() => this.calendarEvent, (newValue) => {
            console.log('Updated reminders:', newValue);
        }, true);
    }


    $onDestroy() {
    }

    getTranslate = (key: string, params?: string[]) => {
        return !!params ? this.$scope.vm.i18nUtils.getWithParams(key, params) : this.$scope.vm.i18nUtils.translate(key);
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
            calendarEvent: '=',
            onEventReminderValid: '&',
        },
        bindToController: true,
        controller: ['$scope', '$parse', '$timeout', Controller],
        link: function ($scope: ICalendareventReminderFormScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
            vm.onCheckEventReminderValid = (): boolean => {
                return $parse($scope.vm.onEventReminderValid())({});
            }
        }
    }
}

export const calendareventReminderForm = ng.directive('calendareventReminderForm', directive);