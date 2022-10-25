import {ng, toasts} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IIntervalService, IPromise, IScope, ITimeoutService} from "angular";
import {ICalendarService} from "../../services";
import {DateUtils} from "../../utils/date.utils";
import {AxiosResponse} from "axios";
import {safeApply} from "../../model/Utils";
import {idiom as lang} from "entcore/types/src/ts/idiom";

interface IViewModel {
    loading: boolean;

    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean): void;
    hideOtherCalendarCheckboxes(calendar: Calendar) : void;
    updateExternalCalendar($event?: MouseEvent) : Promise<void>;
    handleUpdateInterval() : Promise<void>;
    getLastUpdate(format: string) : string;
}

interface ICalendarItemProps {
    calendar: Calendar;

    onOpenOrCloseClickedCalendar(): (calendar: Calendar, savePreferences: boolean) => void;
    onUncheckOtherCalendarCheckboxes(): (calendar: Calendar) => void;
}

interface ICalendarItemScope extends IScope {
    vm: ICalendarItemProps;
}

class Controller implements ng.IController, IViewModel {
    loading: boolean;

    constructor(private $scope: ICalendarItemScope,
                private calendarService: ICalendarService,
                private $timeout: ITimeoutService,
                private $interval: IIntervalService) {
    }

    $onInit() {
        this.updateExternalCalendar();
    }

    $onDestroy() {
    }

    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean) : void {
        this.$scope.$parent.$eval(this.$scope.vm.onOpenOrCloseClickedCalendar)(calendar, savePreferences);
    };

    hideOtherCalendarCheckboxes = (calendar: Calendar) : void => {
        this.$scope.$parent.$eval(this.$scope.vm.onUncheckOtherCalendarCheckboxes)(calendar);
    };

    updateExternalCalendar = async ($event?: MouseEvent): Promise<void> => {
        if($event) $event.stopPropagation();
        this.loading = true;
        safeApply(this.$scope);
        this.calendarService.updateExternalCalendar(this.$scope.vm.calendar);
        this.$timeout(() : IPromise<void> => {
            this.calendarService.checkExternalCalendarSync(this.$scope.vm.calendar)
                .then((r:AxiosResponse) => {
                    if(r.data['isUpdating'] == false) {
                        this.loading = false;
                        safeApply(this.$scope);
                        return;
                    }
                    return this.handleUpdateInterval();
                })
                .catch((e) => {
                    this.loading = false;
                    safeApply(this.$scope);
                    let error: AxiosResponse = e.response;
                    toasts.warning(error);
                });
            return;
        }, 15000, false)
    };

    handleUpdateInterval = async (): Promise<void> => {
        this.$interval(() : IPromise<void> => {
            this.calendarService.checkExternalCalendarSync(this.$scope.vm.calendar)
                .then((r: AxiosResponse) => {
                    if(r.data['isUpdating'] == false) {
                        this.loading = false;
                        safeApply(this.$scope);
                        return;
                    }
                })
                .catch((e) => {
                    this.loading = false;
                    safeApply(this.$scope);
                    let error: AxiosResponse = e.response;
                    toasts.warning(error);
                    return;
                });
            return;
            }, 60000, 0, false);
    }

    getLastUpdate = (format: string): string => {
        return DateUtils.getFormattedString(this.$scope.vm.calendar.updated, format)
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}calendar-item/calendar-item.html`,
        controllerAs: 'vm',
        scope: {
            calendar: '=',
            onOpenOrCloseClickedCalendar: '&',
            onUncheckOtherCalendarCheckboxes: '&',
        },
        bindToController: true,
        controller: ['$scope', 'CalendarService', "$timeout", "$interval", Controller]
    }
}

export const calendarItem = ng.directive('calendarItem', directive);