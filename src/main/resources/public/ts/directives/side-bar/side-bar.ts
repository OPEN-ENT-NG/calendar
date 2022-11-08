import {$, ng, toasts} from "entcore";
import {Calendar, Calendars} from "../../model";
import {ROOTS} from "../../core/const/roots";
import {Subject} from "rxjs";
import {calendar} from "entcore/types/src/ts/calendar";
import {calendarService} from "../../services";
import {AxiosResponse} from "axios";
import {safeApply} from "../../model/Utils";

interface IViewModel {
    $onInit(): any;

    // props
    calendars: Calendars;
    list: boolean;
    showButtonsCalendar: Calendar;
    onEventUpdateCheckbox: Subject<void>;


    ownCalendars(): boolean;
    hasSharedCalendars(): boolean;
    isCalendarSharedWithMe(calendar): boolean;
    hideOtherCalendarCheckboxes(calendar): void;
    isMyCalendar(calendar: Calendar): boolean;
    isExternalCalendar(calendar: Calendar): boolean;
    hasExternalCalendars(): boolean;
    isEmpty(): boolean;

    //scope
    calendar: Calendar;

    onShowCalendar(): void;
    onShowList(): void;
    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean): void;
    onUpdateCalendarList(calendar : Calendar) : void;
    onCheckExternalCalendarRight(right: string) : boolean;

}

export const sideBar = ng.directive('sideBar', () =>{
    return {
        templateUrl: `${ROOTS.directive}side-bar/side-bar.html`,
        scope: {
            ngChange: '&',
            onShowCalendar: '&',
            onShowList: '&',
            onOpenOrCloseCalendar: '&',
            calendar: '=',
            onUpdateCalendarList: '&',
            onCheckExternalCalendarRight: '&'
        },

        restrict: 'E',
        controllerAs: 'vm',
        bindToController: {
            calendars: '=',
            showButtonsCalendar: '=',
            list: "=",
            onEventUpdateCheckbox: '<'
        },

        controller: function (){
            const vm: IViewModel = <IViewModel>this;

            vm.$onInit = () => {
                if (vm.onEventUpdateCheckbox) {
                    vm.onEventUpdateCheckbox.asObservable().subscribe(() => {
                        vm.hideOtherCalendarCheckboxes(vm.calendar);
                    });
                }
            };

        },

        link: function ($scope) {
            const vm: IViewModel = $scope.vm;

            vm.onShowCalendar = () : void => {
                $scope.$eval($scope.onShowCalendar);
            };

            vm.onShowList = () : void => {
                $scope.$eval($scope.onShowList);
            };

            vm.onOpenOrCloseCalendar = (calendar: Calendar, savePreferences: boolean) : void => {
                $scope.$eval($scope.onOpenOrCloseCalendar)(calendar, savePreferences);
            };

            vm.onCheckExternalCalendarRight = (right: string) : boolean => {
                return $scope.$eval($scope.onCheckExternalCalendarRight)(right);
            };

            vm.isEmpty = () : boolean => {
                return vm.calendars
                    && vm.calendars.all
                    && vm.calendars.all.length < 1;
            }

            vm.isMyCalendar = (calendar: Calendar) : boolean => {
                return (calendar.owner.userId == $scope.$parent.me.userId) && !vm.isExternalCalendar(calendar);
            }

            vm.hasSharedCalendars = () : boolean => {
                var hasSharedCalendars = vm.calendars.all.some((calendar:Calendar): boolean => vm.isCalendarSharedWithMe(calendar));
                return hasSharedCalendars;
            };

            vm.isCalendarSharedWithMe = (calendar) : boolean => {
                return calendar && calendar.shared && calendar.owner.userId != $scope.$parent.me.userId;
            };

            vm.isExternalCalendar = (calendar: Calendar) : boolean => {
                return calendar.isExternal;
            }

            vm.hasExternalCalendars = () : boolean => {
                return vm.calendars.all.some((calendar:Calendar): boolean => vm.isExternalCalendar(calendar));
            }

            vm.hideOtherCalendarCheckboxes = (calendar) : void => {
                vm.calendar = calendar;
                vm.showButtonsCalendar = calendar;
                $scope.$parent.calendars.all
                    .filter((item:any):boolean => item._id != calendar._id)
                    .forEach((item:any): boolean => item.showButtons = false);
                $scope.$parent.display.showToggleButtons = calendar.showButtons;
            };

            vm.onUpdateCalendarList = (calendar : Calendar) : void => {
                $scope.$eval($scope.onUpdateCalendarList)(calendar);
            }
        }
    }
});