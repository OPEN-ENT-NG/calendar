import {ng} from "entcore";
import {IScope} from "angular";
import {I18nUtils} from "../../utils/i18n.utils";
import { CalendarEvent } from "../../model";


interface ICalendareventReminderFormProps {
    calendarEvent?: CalendarEvent;

    onEventReminderValid?: () => boolean;
}

interface IViewModel extends ng.IController, ICalendareventReminderFormProps {
    i18nUtils?: I18nUtils;

    changeSelection?(field: string): void;
    getTranslate?(key: string, params?: string[]): string;
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
    }

    $onDestroy() {
    }

    getTranslate = (key: string, params?: string[]) => {
        return !!params ? this.$scope.vm.i18nUtils.getWithParams(key, params) : this.$scope.vm.i18nUtils.translate(key);
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