import {angular, model, idiom as lang, ng, toasts} from "entcore";
import {ROOTS} from "../../core/const/roots";
import {Calendar} from "../../model";
import {IScope} from "angular";
import {safeApply} from "../../model/Utils";
import {ICalendarService} from "../../services";
import {defaultColor} from "../../model/constantes";
import {CalendarForm} from "../../model/calendar-form.model";
import {FORMAT} from "../../core/const/date-format";
import {DateUtils} from "../../utils/date.utils";
import {I18nUtils} from "../../utils/i18n.utils";


interface IViewModel {
    calendar: CalendarForm;

    openExternalCalendarForm(): void;
    closeExternalCalendarForm(): void;
    createExternalCalendar(calendar: Calendar): Promise<void>;
}

interface IExternalCalendarAdditionProps {
    display: any;
    enableZimbra: boolean;

    onUpdateCalendars(): void;
}

interface IExternalCalendarAdditionScope extends IScope {
    vm: IExternalCalendarAdditionProps;
}

class Controller implements ng.IController, IViewModel {
    calendar: CalendarForm;
    i18nUtils: I18nUtils;

    constructor(private $scope: IExternalCalendarAdditionScope, private calendarService: ICalendarService) {
    }

    $onInit() {
        this.i18nUtils = new I18nUtils();
    }

    $onDestroy() {
    }

    openExternalCalendarForm = (): void => {
        this.calendar = new  CalendarForm();
        this.calendar.color = defaultColor;
        this.calendar.isExternal = true;
        angular.element(document.getElementsByClassName("color grey")).addClass("selected");
        safeApply(this.$scope);
        this.$scope.vm.display.showPanelExternalCalendar = true;
    }

    closeExternalCalendarForm = (): void => {
        angular.element(document.getElementById("external-calendar-form")).scope().vm.resetForm();
        this.$scope.vm.display.showPanelExternalCalendar = false;
    }

    createExternalCalendar = async (calendar): Promise<void> => {
        this.closeExternalCalendarForm();
        safeApply(this.$scope);
        try {
            await this.calendarService.addExternalCalendar(calendar);

            let successMessage : string = lang.translate("calendar.the.calendar") + " " + calendar.title + " " + lang.translate("calendar.has.been.added");
            toasts.confirm(successMessage);
            this.$scope.$parent.$eval(this.$scope.vm.onUpdateCalendars)(true);
        } catch (e) {
            if (e.response.status == 401 && e.response.data.error == "URL not authorized") {
                toasts.info(lang.translate("calendar.external.platform.not.accepted"));
            } else if (e.response.status == 401 && e.response.data.error == "calendar.platform.already.exists") {
                let platformMessage : string = this.i18nUtils.getWithParam("calendar.external.platform.already.exists", calendar.platform);
                toasts.info(platformMessage);
            } else {
                let errorMessage : string = lang.translate("calendar.get.events.error") + " " + calendar.title + ".";
                toasts.warning(errorMessage);
            }

        }
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}external-calendar-addition/external-calendar-addition.html`,
        controllerAs: 'vm',
        scope: {
            display: '=',
            onUpdateCalendars: '&',
            enableZimbra: "="
        },
        bindToController: true,
        controller: ['$scope', 'CalendarService', Controller]
    }
}

export const externalCalendarAddition = ng.directive('externalCalendarAddition', directive);