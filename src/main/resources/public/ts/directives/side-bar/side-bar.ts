import { $, ng} from "entcore";
import {Calendar, Calendars} from "../../model";
import {ROOTS} from "../../core/const/roots";


interface IViewModel {

    calendars: Calendars;
    list: boolean;
    calendar: Calendar;
    showButtonsCalendar: Calendar;

    ownCalendars(): boolean;
    hasSharedCalendars(): boolean;
    isCalendarSharedWithMe(calendar): any;
    hideOtherCalendarCheckboxes(calendar): void;
    isMyCalendar(calendar: Calendar): void;
    isEmpty(): void;

    onShowCalendar(): void;
    onShowList(): void;
    onOpenOrCloseCalendar(calendar: Calendar, savePreferences: boolean): void;

}

export const sideBar = ng.directive('sideBar', () =>{
    return {
        templateUrl: `${ROOTS.directive}side-bar/side-bar.html`,
        scope: {
            ngChange: '&',
            onShowCalendar: '&',
            onShowList: '&',
            onOpenOrCloseCalendar: '&',
            calendar: '='
        },

        restrict: 'E',
        controllerAs: 'vm',
        bindToController: {
            calendars: '=',
            showButtonsCalendar: '=',
            list: "="
        },

        controller: function (){
            const vm: IViewModel = <IViewModel>this;
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

            vm.isEmpty = () : boolean => {
                return vm.calendars
                    && vm.calendars.all
                    && vm.calendars.all.length < 1;
            }

            vm.isMyCalendar = (calendar: Calendar) : boolean => {
                return calendar.owner.userId == $scope.$parent.me.userId;
            }

            vm.hasSharedCalendars = () : boolean => {
                var hasSharedCalendars = vm.calendars.all.some((calendar:Calendar): boolean => vm.isCalendarSharedWithMe(calendar));
                return hasSharedCalendars;
            };

            vm.isCalendarSharedWithMe = (calendar) : boolean => {
                return calendar && calendar.shared && calendar.owner.userId != $scope.$parent.me.userId;
            };

            vm.hideOtherCalendarCheckboxes = (calendar) : void => {
                vm.calendar = calendar;
                vm.showButtonsCalendar = calendar;
                $scope.$parent.calendars.all
                    .filter((item:any):boolean => item._id != calendar._id)
                    .forEach((item:any): boolean => item.showButtons = false);
                $scope.$parent.display.showToggleButtons = calendar.showButtons;
            };
        }
    }
});