import {$, _, moment, ng, template, idiom as lang, notify, toasts} from "entcore";

import {
    Calendar,
    Calendars,
    CalendarEvent,
    CalendarEvents,
} from "../model/index";
import {
    defaultColor,
    periods,
    timeConfig,
    recurrence,
    LANG_CALENDAR,
    rights
} from "../model/constantes";
import {
    isSameAfter,
    makerFormatTimeInput,
    utcTime,
} from "../model/Utils";
import {calendar} from "entcore/types/src/ts/calendar";
import {AxiosResponse} from "axios";
import {DateUtils} from "../utils/date.utils";

export const calendarController =  ng.controller('CalendarController',
    ["$location",
        "$scope",
        "$timeout",
        "$compile",
        "$sanitize",
        "model",
        "route",
        ($location, $scope, $timeout, $compile, $sanitize, model, route) => {
        $scope._ = _;
        $scope.lang = lang;
        $scope.template = template;
        $scope.display = {};
        $scope.display.list = false;
        $scope.display.calendar = false;
        $scope.display.editEventRight = false;
        $scope.model = model;
        $scope.me = model.me;
        $scope.calendarEvent = new CalendarEvent();
        $scope.calendars = new Calendars();
        $scope.display.importFileButtonDisabled = true;
        $scope.calendarEvents= new CalendarEvents() ;
        $scope.periods = periods;
        $scope.newFile = {};
        $scope.propertyName = 'startMoment';
        $scope.reverse = true;
        $scope.calendar = new Calendar();
        $scope.display.selectAllCalendarEvents = false;
        $scope.timeConfig =  timeConfig;
        $scope.recurrence = recurrence;
        $scope.reader = new FileReader();
        $scope.jsonData = {ics: {}};
        $scope.calendarEvents.filters.startMoment = moment().startOf('day');
        $scope.calendarEvents.filters.endMoment = moment().add(2, 'months').startOf('day');
        $scope.contentToWatch = "";
        $scope.calendarCreationScreen = false;
        $scope.calendarAsContribRight = new Array<String>();
        $scope.selectedCalendarInEvent = new Array<String>();
        $scope.rights = rights;

        template.open('main', 'main-view');
        template.open('top-menu', 'top-menu');

        route({
            goToCalendar : async function(params) {
                await Promise.all([
                    $scope.calendars.sync(),
                    $scope.calendars.preference.sync()
                ]);
                setCalendarLang();
                $scope.loadSelectedCalendars();
                let calendarNotification = $scope.calendars.all.filter( calendarFiltre => calendarFiltre._id === params.calendar )[0];
                if (calendarNotification === undefined) {
                    $scope.notFound = true;
                    template.open('error', '404');
                } else {
                    $scope.notFound = false;
                    $scope.openOrCloseCalendar(calendarNotification, true);
                }
            },
            mainPage : async function()  {
                await Promise.all([
                    $scope.calendars.sync(),
                    $scope.calendars.preference.sync()
                ]);
                $scope.loadSelectedCalendars();
                $scope.firstOwnedEvent();
                $scope.initEventDates(moment().utc().second(0).millisecond(0), moment().utc().second(0).millisecond(0).add(1, 'hours'));
                setCalendarLang();
                $scope.$apply();
            },
        });

        model.calendar.on('date-change', () => {
            $scope.calendarEvents.filtered = [];
            $scope.$apply();
            $scope.loadCalendarEvents();
            $scope.$apply();
        });

    function disableImportFileButton() {
        $scope.$apply(function() {
            $scope.display.importFileButtonDisabled = false;
        });
    }

    const setCalendarLang = ():void => {
        model.calendar.firstDay = model.calendar.firstDay.lang(LANG_CALENDAR);
    };

    $scope.isEmpty = () => {
        return $scope.calendars
            && $scope.calendars.all
            && $scope.calendars.all.length < 1;
    }

    $scope.loadCalendarEvents = () =>{
        if($scope.calendars.all.length > 0){
            $scope.calendarEvents.all = $scope.calendarEvents.filtered = $scope.calendars.arr.reduce((accumulator, element)=>{
                    return element.selected? [...accumulator, ...element.calendarEvents.arr] : [...accumulator];
                    },[]
            );
        }
        if ($scope.display.list) $scope.calendarEvents.applyFilters();
        $scope.calendarEvents.filtered = $scope.removeDuplicateCalendarEvent($scope.calendarEvents.filtered);
    };

    /**
     * Remove events which are duplicated
     */
    $scope.removeDuplicateCalendarEvent = (events: Array<CalendarEvent>): Array<CalendarEvent> => {
        events = events.filter((item, index) =>
            index === events.findIndex((t) => t._id === item._id));
        return events;
    };

    $scope.someSelectedValue = function(selection) {
        return Object.keys(selection).map(function(val) { return selection[val]; }).some(function(val) { return val === true;});
    };

    $scope.changeStartMoment = () => {
        $scope.calendarEvent.endMoment = moment($scope.calendarEvent.startMoment);
    };

/*
    $scope.changeEndMoment = () => {
        if (isSameAfter($scope.calendarEvent.startMoment, $scope.calendarEvent.endMoment)) {
            $scope.calendarEvent.startMoment = moment($scope.calendarEvent.endMoment);
                $scope.calendarEvent.startTime = makerFormatTimeInput(moment($scope.calendarEvent.endTime).subtract(1, 'hours'), 0);
        }
    };
*/

    $scope.toggleIsRecurrent = function(calendarEvent) {
        if (calendarEvent.isRecurrent) {
            if (!$scope.calendarEvent.recurrence.end_on) {
                $scope.calendarEvent.recurrence.end_on = moment($scope.calendarEvent.endMoment).add(1, 'days').hours(0).minutes(0).seconds(0).milliseconds(0);
            }
            if (!$scope.calendarEvent.recurrence.type) {
                $scope.calendarEvent.recurrence.type = 'every_day';
            }
            if (!$scope.calendarEvent.recurrence.every) {
                $scope.calendarEvent.recurrence.every = 1;
            }
            if ($scope.calendarEvent.recurrence.type === 'every_week') {
                $scope.changedRecurrenceType();
            }
            $scope.changeStartMoment();
        }
    };

    $scope.toggleDateToRecurrence = function(name) {
        if (name === 'start') {
            $scope.calendarEvent.endDateToRecurrence = false;
        }
        if (name === 'end') {
            $scope.calendarEvent.startDateToRecurrence = false;
        }
    };

    $scope.getDate = function(theDate) {
        return moment(theDate).format('DD/MM/YYYY');
    };

    $scope.changedRecurrenceType = function() {
        $scope.calendarEvent.recurrence.week_days = {1: false,2: false,3: false,4: false,5: false,6: false,7: false};
        if ($scope.calendarEvent.recurrence.type === 'every_week') {
            if (!$scope.someSelectedValue($scope.calendarEvent.recurrence.week_days)) {
                var dayOfWeek = $scope.calendarEvent.startMoment.day();
                if (dayOfWeek === 0) {
                    dayOfWeek = 7;
                }
                $scope.calendarEvent.recurrence.week_days[dayOfWeek] = true;
            }
        }
    };

    $scope.handleRecurrence = function(calendarEvent) {
        if (calendarEvent.recurrence) {
            if (calendarEvent.recurrence.type == 'every_day' && calendarEvent.recurrence.every) {
                return $scope.handleEveryDayRecurrence(calendarEvent);
            }
            else if (calendarEvent.recurrence.type == 'every_week') {
                return $scope.handleEveryWeekRecurrence(calendarEvent);
            }
        } else {
            return 0;
        }
    };

    $scope.handleEveryDayRecurrence = function(calendarEvent) {
        var calendarRecurrentEvent;
        var list = [];
        if (calendarEvent.recurrence.end_type == 'after' && calendarEvent.recurrence.end_after) {
            for (let i = 0; i < calendarEvent.recurrence.end_after; i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.index = i;
                var toAdd = i * parseInt(calendarEvent.recurrence.every);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(toAdd, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(toAdd, 'days');
                var item = {'calEvent': calendarRecurrentEvent, 'action': 'save'};
                list.push(item);
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
            var endOnMoment = moment(calendarEvent.recurrence.end_on);
            var startMoment = calendarEvent.startMoment;

            for (let i =0; startMoment.isBefore(endOnMoment); i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.index = i;
                var toAdd = i * parseInt(calendarEvent.recurrence.every);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(toAdd, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(toAdd, 'days');
                if (calendarRecurrentEvent.startMoment.isAfter(endOnMoment)) {
                    break;
                }
                var item = {'calEvent': calendarRecurrentEvent, 'action': 'save'};
                list.push(item);
                startMoment = calendarRecurrentEvent.startMoment;
            }
        }
        return list;
    };

    $scope.handleEveryWeekRecurrence = function(calendarEvent) {
        let weekDays = Object.keys(calendarEvent.recurrence.week_days).filter(val =>
            calendarEvent.recurrence.week_days[val]
        );
        let dayJump = 7 * calendarEvent.recurrence.every;
        let startOn = moment(calendarEvent.recurrence.start_on);
        let startDay = calendarEvent.recurrence.start_on.isoWeekday();
        let startHour = moment(calendarEvent.startTime).hours();
        let startMinute = moment(calendarEvent.startTime).minutes();
        let duration = moment(calendarEvent.endTime).seconds(0).milliseconds(0).diff(moment(calendarEvent.startTime).seconds(0).milliseconds(0), 'minutes');
        let recurrenceDays = weekDays.filter(val => val >= startDay);
        if (recurrenceDays.length === 0) {
            startOn.isoWeekday(1).add(dayJump, 'days');
        } else {
            startOn.isoWeekday(parseInt(recurrenceDays[0], 10));
        }
        let endOn = moment(startOn);
        let list = [];
        if (calendarEvent.recurrence.end_type == 'after') {
            while (recurrenceDays.length < calendarEvent.recurrence.end_after) {
                recurrenceDays = recurrenceDays.concat(weekDays);
            }
            if (recurrenceDays.length > calendarEvent.recurrence.end_after) {
                recurrenceDays = recurrenceDays.slice(0, calendarEvent.recurrence.end_after);
            }
            let previousDay = recurrenceDays[0];
            recurrenceDays.forEach((day, idx) => {
                if (day <= previousDay && idx > 0) {
                    endOn.isoWeekday(1).add(dayJump, 'days');
                }
                endOn.isoWeekday(parseInt(day, 10));
                let calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.index = idx;
                calendarRecurrentEvent.startMoment = moment(endOn).hours(startHour).minutes(startMinute);
                calendarRecurrentEvent.endMoment = moment(calendarRecurrentEvent.startMoment).add(duration, 'minutes');
                var item = {'calEvent': calendarRecurrentEvent, 'action': 'save'};
                list.push(item);
                previousDay = day;
            });
        } else if (calendarEvent.recurrence.end_type == 'on') {
            while (calendarEvent.recurrence.end_on.diff(endOn, 'days') >= 0) {
                recurrenceDays = recurrenceDays.concat(weekDays);
                endOn.isoWeekday(1).add(dayJump, 'days');
            }
            endOn = moment(startOn);
            if (recurrenceDays.length) {
                let previousDay = recurrenceDays[0];
                recurrenceDays.every((day, idx) => {
                    if (day <= previousDay && idx > 0) {
                        endOn.isoWeekday(1).add(dayJump, 'days');
                    }
                    endOn.isoWeekday(parseInt(day, 10));
                    if (calendarEvent.recurrence.end_on.diff(endOn, 'days') >= 0) {
                        let calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                        calendarRecurrentEvent.index = idx;
                        calendarRecurrentEvent.startMoment = moment(endOn).hours(startHour).minutes(startMinute);
                        calendarRecurrentEvent.endMoment = moment(calendarRecurrentEvent.startMoment).add(duration, 'minutes');
                        var item = {'calEvent': calendarRecurrentEvent, 'action': 'save'};
                        list.push(item);
                        previousDay = day;
                        return true;
                    } else {
                        return false;
                    }
                });
            }
        }
        return list;
    };

    $scope.handleEveryWeekDayRecurrence = function(calendarEvent) {
        var calendarRecurrentEvent;
        if (calendarEvent.recurrence.end_type == 'after' && calendarEvent.recurrence.end_after) {
            for (let i = 0; i < calendarEvent.recurrence.end_after; i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(i + 1, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(i + 1, 'days');
                $scope.saveCalendarEventEdit(calendarRecurrentEvent);
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
        }
    };

    $scope.cropEventDate = (calendarEventDate : string): string =>
        DateUtils.getSimpleFRDateFormat(calendarEventDate);

    $scope.createChildCalendarEvent = function(calendarEvent) {
        var child  = new CalendarEvent();
        child.title = calendarEvent.title;
        child.description = calendarEvent.description;
        child.location = calendarEvent.location;
        child.color = calendarEvent.color;
        child.locked = calendarEvent.locked;
        child.is_periodic = calendarEvent.is_periodic;
        child.calendar = calendarEvent.calendar;
        child.parentId = calendarEvent._id;
        child.allday = calendarEvent.allday;
        child.isRecurrent = calendarEvent.isRecurrent;
        child.recurrence = calendarEvent.recurrence;
        child.startMoment = calendarEvent.startMoment;
        child.endMoment = calendarEvent.endMoment;
        child.startTime = calendarEvent.startTime;
        child.endTime = calendarEvent.endTime;
        return child;
    };



    $scope.firstOwnedCalendar = function() {
        return _.find($scope.calendars.all, function(calendar) {
            return $scope.isMyCalendar(calendar);
        });
    };

    $scope.firstOwnedEvent = function() {
        return _.find($scope.calendarEvents.all, function(calendarEvent) {
            return $scope.isMyEvent(calendarEvent);
        });
    };

    $scope.loadSelectedCalendars = () => {
        if ($scope.calendars.preference) {
            var toSelectCalendars = $scope.calendars.all.filter(calendar => {
                return _.contains($scope.calendars.preference.selectedCalendars, calendar._id);
            });
            toSelectCalendars.forEach(function(cl) {
               $scope.openOrCloseCalendar(cl, false);
            });
        }
        if ($scope.calendars.selected.length === 0 && !$scope.calendars) {
            var calendarToOpen = $scope.firstOwnedCalendar();
            if (calendarToOpen === undefined) {
                calendarToOpen = $scope.calendars.all[0];
            }
            $scope.openOrCloseCalendar(calendarToOpen, true);
        }
    };



    $scope.saveCalendarPreferences = () => {
        $scope.calendars.preference.update();
    };

    /**
     * View events in a list
     */
    $scope.showList = function() {
        $scope.display.list = true;
        $scope.display.calendar = false;
        $scope.display.propertyName = 'startMoment';
        $scope.reverse = false;
        $scope.loadCalendarEvents();
        template.open('calendar', 'events-list');
    };

    $scope.sortBy = function(propertyName) {
        $scope.reverse = ($scope.propertyName === propertyName) ? !$scope.reverse : false;
        $scope.propertyName = propertyName;
    };

    /**
     * View events in a grid
     */
    $scope.showCalendar = function() {
        if($scope.calendars.all.length === 0) return;
        $scope.display.list = false;
        $scope.display.calendar = true;
        $scope.loadCalendarEvents();
        template.open('calendar', 'read-calendar');
    };

    $scope.isMyCalendar = function(calendar) {
        return calendar.owner.userId == $scope.me.userId;
    };

    /**
     * Return true if the current user created the event
     * @param calEvent event to check
     */
    $scope.isMyEvent = (calEvent : CalendarEvent): boolean => {
        return calEvent.owner.userId === $scope.me.userId;
    };


    $scope.initEventDates = function(startMoment, endMoment) {
        var event = $scope.calendarEvent;
        var minTime = moment(startMoment);
        minTime.set('hour', timeConfig.start_hour);
        var maxTime = moment(endMoment);
        maxTime.set('hour', timeConfig.end_hour);
        if(startMoment.isAfter(minTime) && startMoment.isBefore(maxTime)){
            event.startMoment = startMoment;
        }
        else{
            if(startMoment.isAfter(maxTime)){
                startMoment.add(1, 'days');
                endMoment.add(1, 'days');
                maxTime.add(1, 'days');
            }
        }
        if(endMoment.isBefore(maxTime)){
            event.endMoment = endMoment;
        }
    };

    $scope.openOrCloseCalendar = async function(calendar, savePreferences) {
        if ($scope.calendars.selected.length > 1 || !calendar.selected) {
            calendar.selected = !calendar.selected;
            if (calendar.selected) {
                $scope.calendar = calendar;
            }
            $scope.display.editEventRight = $scope.hasContribRight();
            $scope.calendarEvents.applyFilters();
            if (!$scope.display.list && !$scope.display.calendar) {
                $scope.showCalendar();
            } else {
                $scope.loadCalendarEvents();
            }
            if (savePreferences) {
                $scope.calendars.preference.selectedCalendars = $scope.calendars.selectedElements.map(element => element._id);
                if($scope.calendar.selected){
                    $scope.calendars.preference.selectedCalendars = [...$scope.calendars.preference.selectedCalendars, calendar._id] ;
                } else {
                    $scope.calendars.preference.selectedCalendars = $scope.calendars.preference.selectedCalendars.filter(element => element !== calendar._id)
                }
                await $scope.saveCalendarPreferences();
                await $scope.loadCalendarEvents();
            }
        }
    };


    $scope.newCalendar = function() {
        $scope.calendarCreationScreen = true;
        $scope.calendar = new Calendar();
        $scope.calendar.color = defaultColor;
        template.open('calendar', 'edit-calendar');
    };

    /**
     *Allows to view an event creation form
     *
     * @param calendarEvent the created event
     * @param isCalendar allows to use the lightbox from the calendar directive
     */
    $scope.viewCalendarEvent = (calendarEvent, isCalendar ? : boolean) => {
        $scope.calendarEvent = new CalendarEvent(calendarEvent);
        $scope.calendar = calendarEvent.calendar[0];
        $scope.createContentToWatch();
        $scope.calendarEvent.showDetails = true;
         if (!$scope.calendarEvent.parentId) {
             if (!$scope.calendarEvent.recurrence) {
                 $scope.calendarEvent.recurrence = {};
                 $scope.calendarEvent.recurrence.week_days = recurrence.week_days;
             }
        }
         if (!isCalendar){
             if (($scope.hasManageRightOrIsEventOwner(calendarEvent) && $scope.hasRightOnSharedEvent(calendarEvent, rights.resources.updateEvent.right))
                 && calendarEvent.editAllRecurrence == undefined
                 && calendarEvent.isRecurrent && calendarEvent._id){
                 template.open('recurrenceLightbox', 'recurrent-event-edition-popup');
                 $scope.display.showRecurrencePanel = true;
             } else if(!calendarEvent._id || ($scope.hasManageRightOrIsEventOwner(calendarEvent) && $scope.hasRightOnSharedEvent(calendarEvent, rights.resources.updateEvent.right))) {
                 if (calendarEvent.editAllRecurrence){
                     //event content
                     $scope.calendarEvent.detailToRecurrence = true;
                     //event date/length
                     $scope.calendarEvent.startDateToRecurrence = true;
                     $scope.calendarEvent.endDateToRecurrence = true;
                 }
                 $scope.display.showRecurrencePanel = false;
                 template.close('recurrenceLightbox');
                 template.open('lightbox', 'edit-event');
                 // $scope.calendarEvent.editAllRecurrence = undefined;
                 $scope.display.showEventPanel = true;
             } else {
                 template.open('lightbox', 'view-event');
                 $scope.display.showEventPanel = true;
             }

         }
    };

    $scope.closeCalendarEvent = () => {
        template.close('lightbox');
        $scope.showCalendarEventTimePicker = false;
        $scope.display.showEventPanel = false;
        $scope.contentToWatch = "";
    };

    $scope.unselectRecurrenceRemovalCheckbox = (uncheckDeleteAllRecurrence: boolean): void => {
        if (uncheckDeleteAllRecurrence) {
            $scope.calendarEvent.deleteAllRecurrence = false;
        } else {
            $scope.calendarEvent.noMoreRecurrent = false;
            $scope.calendarEvent.noMoreRecurrence = false;
        }
    };

    $scope.confirmRemoveCalendarEvent = (calendarEvent: CalendarEvent, event): void => {
        $scope.calendar.calendarEvents.deselectAll();
        if (calendarEvent.editAllRecurrence) {
            calendarEvent.deleteAllRecurrence = true;
        }
        if (calendarEvent.deleteAllRecurrence) {
            selectOtherRecurrentEvents(calendarEvent);
        }
        if (calendarEvent.noMoreRecurrent && calendarEvent.noMoreRecurrence) {
            selectOtherRecurrentEvents(calendarEvent);
            let clickedEvent: CalendarEvent = $scope.calendarEvents.getRecurrenceEvents(calendarEvent)
                .find((calEvent: CalendarEvent) => calEvent && calEvent._id && calEvent._id === calendarEvent._id);
            if (clickedEvent) clickedEvent.selected = false;
        }
        $scope.display.confirmDeleteCalendarEvent = true;
        event.stopPropagation();
    };

    $scope.confirmRemoveCalendarEvents = function(event){
        template.open('lightbox');
        $scope.calendarEvents.selected.forEach(calendarEvent => {
            if (calendarEvent.deleteAllRecurrence) {
                selectOtherRecurrentEvents(calendarEvent);
            }
        });
        $scope.display.confirmDeleteCalendarEvent = true;
    };

    /**

     * Select all recurrent events associated to the current event
     * @param calendarEvent calendar Event
     */
    const selectOtherRecurrentEvents = (calendarEvent: CalendarEvent): void => {
        let reccurentEvents = $scope.removeDuplicateCalendarEvent($scope.calendarEvents.getRecurrenceEvents(calendarEvent));
        reccurentEvents.forEach(calEvent => {
            calEvent.selected = true;
        });
    };

    /**
     * Allow delete all recurrent events of an event when it is display in list
     * @param calendarEvent calendar event
     */
    $scope.deleteAllRecurrenceList = (calendarEvent: CalendarEvent): void => {
        $scope.calendarEvent = calendarEvent;
        $scope.calendarEvent.deleteAllRecurrence = true;
        $scope.confirmRemoveCalendarEvents(null);

    }

    $scope.removeCalendarEvents = () => {
        let count = $scope.calendarEvents.selected.length;
        let countReccurent = 0;
        if ($scope.calendarEvents.selected.length === 0){
            let eventsCalendar = new CalendarEvent($scope.calendarEvent);
            eventsCalendar.delete();
            $scope.resetCalendarAfterRemoveEvent(count,countReccurent);
        } else {
             $scope.calendarEvents.selected.forEach( calendarEvent => {
                    calendarEvent.delete();
                    count--;
                    $scope.resetCalendarAfterRemoveEvent(count,countReccurent);
            });
        }
        $scope.calendarEvents.deselectAll();
        $scope.display.selectAllCalendarEvents = $scope.display.selectAllCalendarEvents && false;
        template.close('lightbox');
        $scope.display.confirmDeleteCalendarEvent = false;
    };

    const addCssOnHtmlElement = (selectorHtml:string, propertyCss:string, valueCss:string):void => {
        $(selectorHtml).css(propertyCss,valueCss);
    }

    $scope.refreshCalendarEvents = async () => {
        template.close('lightbox');
        await $scope.calendars.syncCalendarEvents();
        await $scope.loadCalendarEvents();
        addCssOnHtmlElement("body > portal > div > section","z-index","9000");
        $scope.$apply();
    };


    $scope.resetCalendarAfterRemoveEvent = async function(count,countRecurrent) {
        if (count === 0 && countRecurrent === 0) {
             await $scope.refreshCalendarEvents();
             $scope.closeCalendarEvent();
            if ($scope.display.list && $scope.display.selectAllCalendarEvents) {
                $scope.display.selectAllCalendarEvents = undefined;
            }
            $scope.display.confirmDeleteCalendarEvent = false;
        }
    };

    $scope.cancelRemoveCalendarEvent = function() {
        $scope.display.confirmDeleteCalendarEvent = undefined;
        $scope.calendarEvent.deleteAllRecurrence = false;
        $scope.calendarEvents.forEach(function(calendarEvent) {
            calendarEvent.selected = false;
        });
    };

    $scope.editCalendar = function(calendar, event) {
        $scope.calendar = calendar;
        event.stopPropagation();
        template.open('calendar', 'edit-calendar');
        $scope.createContentToWatch();
    };

    $scope.createContentToWatch = function(){
        if($scope.calendarEvent.title == undefined){
            $scope.contentToWatch = "";
        }else{
            $scope.contentToWatch = $scope.calendarEvent.title;
        }
        if($scope.calendarEvent.description != undefined){
            $scope.contentToWatch += $scope.calendarEvent.description;
        }
        if($scope.calendarEvent.location != undefined){
            $scope.contentToWatch += $scope.calendarEvent.location;
        }
    };

    $scope.saveCalendarEdit = async () => {
        if ($scope.calendar._id) {
            await $scope.calendar.save();
            await $scope.calendar.calendarEvents.sync($scope.calendar, $scope.calendars);
            $scope.calendarEvents.applyFilters();
        } else {
            await $scope.calendar.save();
            $scope.openOrCloseCalendar($scope.calendar, true);
            await $scope.calendars.sync();
            $scope.loadSelectedCalendars();
            $scope.loadCalendarEvents();
        }
        $scope.showCalendar();
        $scope.calendarCreationScreen = false;
    };

    /**
     * Return true if the user as the right to manage the calendar of the event or if he created the event and he can contrib
     * to the calendar
     * @param calEvent event to check
     */
    $scope.hasManageRightOrIsEventOwner = (calEvent : CalendarEvent): boolean => {
        return $scope.smallerRightEvent(calEvent) == "manage" ||
            ($scope.isMyEvent(calEvent) && $scope.smallerRightEvent(calEvent) == "contrib");
    }

    $scope.hasContribRight = calendar => {
        if (calendar) {
           return calendar.myRights.contrib;
        } else {
             return $scope.calendars.all.some(function(cl) {
                if (cl.myRights.contrib && cl.selected) {
                   return true;
                }
            });
        }
    };

    $scope.hasRightOnSharedEvent = (calEvent : CalendarEvent, right : string): boolean => {
        if (!calEvent.shared || calEvent.owner.userId === $scope.me.userId){
            return true;
        } else {
            let numberOfRights : number;
            numberOfRights = (calEvent.shared
                .filter(share => share[right]
                    && (share["userId"] === $scope.me.userId
                    || $scope.me.groupsIds.includes(share["groupId"]))).length);

            return (numberOfRights>0);
        }
    }

    $scope.hasReadRight = function(calendar) {
        if (calendar) {
            return calendar.myRights.contrib;
        } else {
            return $scope.calendars.selected.some(function(cl) {
                if (cl.myRights.contrib) {
                    return true;
                }
            });
        }
    };

    $scope.cancelCalendarEdit = function() {
        $scope.calendarCreationScreen = false;
        $scope.calendar = undefined;

        if ($scope.isEmpty()) {
            template.close('calendar');
        } else {
            template.open('calendar', 'read-calendar');
        }
    };

    $scope.confirmRemoveCalendar = function(calendar, event){
        $scope.display.confirmDeleteCalendar = true;
        $scope.calendar = calendar;
        event.stopPropagation();

    };


    $scope.removeCalendar = async () => {
        $scope.display.showToggleButtons = false;
        $scope.calendar.calendarEvents.forEach(function(calendarEvent) {
            calendarEvent.delete();
        });

        try {
            await $scope.calendar.delete();
        } catch (err) {
            let error: AxiosResponse = err.response;
            if (error.status === 403) {
                toasts.warning(error.data.error);
            } else {
                toasts.warning(lang.translate('calendar.delete.error'));
            }
        }
        if($scope.calendars.all.length === 1) {
            template.close("calendar");
        }
        await $scope.calendars.sync();
        await $scope.loadSelectedCalendars();
        $scope.loadCalendarEvents();
        template.close('lightbox');
        $scope.display.confirmDeleteCalendar = undefined;
        $scope.$apply();
    };

    $scope.cancelRemoveCalendar = function() {
        $scope.display.confirmDeleteCalendar = undefined;
    };

    $scope.shareCalendar = function(calendar, event) {
        $scope.calendar = calendar;
        $scope.display.showPanelCalendar = true;
        event.stopPropagation();
    };

    $scope.shareEvent = function(calendarEvent, event) {
        $scope.calendarEvent = calendarEvent;
        $scope.display.showPanelEvent = true;
        event.stopPropagation();
    };

    $scope.saveAndShareEvent = async function(calendarEvent, event) {
        try {
            $scope.sendNotif = false;
            await $scope.saveCalendarEventEdit(calendarEvent, event, true);
            $scope.sendNotif = true;
        } catch (err)  {
            $scope.display.showPanelEvent = false;
            let error: AxiosResponse = err.response;
            if (error.status === 401){
                toasts.warning(error.data.error);
            } else {
                toasts.warning(lang.translate('calendar.event.save.error'));
            }
        };
    }

    $scope.nameOfShareButton = (calendarEvent) : string => {
        if(calendarEvent && calendarEvent.calendar){
            let numberOfSharedCalendars = calendarEvent.calendar
                .filter((calendar:Calendar): boolean => calendar.shared && (calendar.shared.length !== 0))
                .length;

            return (numberOfSharedCalendars === 0)?
                lang.translate('calendar.event.save.and.share') :
                lang.translate('calendar.event.save.and.restrict');
        } else {
            return "";
        }
    }

    /**
     * Prepare $scope.calendarEvent to create the event and call the method that will display the calendar creation form
     * @param newItem the event information so far
     * @param isCalendar allows to use the lightbox from the calendar directive
     */
    $scope.createCalendarEvent = (newItem?, isCalendar? :boolean) => {
        $scope.calendarAsContribRight = new Array<String>();
        $scope.selectedCalendarInEvent = new Array<String>();
        $scope.calendarEvent = new CalendarEvent();
        $scope.calendarEvent.recurrence = {};
        $scope.calendarEvent.calendar = new Array<Calendar>();
        isCalendar ? $scope.viewCalendarEvent($scope.calendarEvent, isCalendar)
            : $scope.viewCalendarEvent($scope.calendarEvent);
        setListCalendarWithContribFilter();
        if(newItem){
            $scope.calendarEvent.startMoment = newItem.beginning;
            $scope.calendarEvent.startMoment = $scope.calendarEvent.startMoment.minute(0).second(0).millisecond(0);
            $scope.calendarEvent.endMoment = newItem.end;
            $scope.calendarEvent.endMoment = $scope.calendarEvent.endMoment.minute(0).second(0).millisecond(0);
        } else {
            $scope.calendarEvent.startMoment = moment.utc().second(0).millisecond(0).add(utcTime($scope.calendarEvent.startMoment), 'hours');
            $scope.calendarEvent.endMoment = moment.utc().second(0).millisecond(0).add(5 - utcTime($scope.calendarEvent.endMoment), 'hours');
        }
        $scope.calendarEvent.startTime = makerFormatTimeInput($scope.calendarEvent.startMoment, $scope.calendarEvent.startMoment);
        $scope.calendarEvent.endTime = makerFormatTimeInput($scope.calendarEvent.endMoment, $scope.calendarEvent.startMoment);
        $scope.calendarEvent.recurrence.week_days = recurrence.week_days;
        $scope.calendarEvent.calendar = $scope.calendars.selected[$scope.calendars.selected.length - 1];
        $scope.changeCalendarEventCalendar();
        $scope.calendarEvent.showDetails = true;
        $scope.initEventDates($scope.calendarEvent.startMoment, $scope.calendarEvent.endMoment);
        $scope.showCalendarEventTimePicker = true;
    };
    //unique
    const unique = function<T>(array:T[]) {
        return array.filter(function (value, index, self) {
            return self.indexOf(value) === index;
        });
    }
    /**
    *   Put the calendars that the user has the right to modify in calendarAsContribRight and tick the first in the list
    */
    const setListCalendarWithContribFilter = (): void => {
        $scope.calendars.arr.forEach(
            function(calendar){
                if($scope.hasContribRight(calendar) != null){
                    $scope.calendarAsContribRight.push(calendar.title);
                }
            });
        $scope.calendarAsContribRight = unique($scope.calendarAsContribRight);
        $scope.selectedCalendarInEvent.push($scope.calendarAsContribRight[0]);
        $scope.selectedCalendarInEvent = unique($scope.selectedCalendarInEvent);
    }

    /**
     *  Verify if there is a element tick in the multi-combo of calendars
     */
    $scope.isCalendarSelectedInEvent = (): boolean => {
        if($scope.calendarEvent._id){
            return true;
        } else {
            return $scope.selectedCalendarInEvent.length != 0;
        }
    };

    /**
     * Remove the selected calendar from the list of selected calendars
     * @param calendar name of the selected calendar
     */
    $scope.dropCalendar = (calendar: String): void => {
        $scope.selectedCalendarInEvent = _.without($scope.selectedCalendarInEvent, calendar);
        $scope.changeCalendarEventCalendar();
    };

    /**
     *  Update the calendars of the calendar event
     */
    $scope.changeCalendarEventCalendar = (): void => {
        $scope.calendarEvent.calendar = new Array<Calendar>();

        $scope.selectedCalendarInEvent.forEach(
            function(title){
                $scope.calendars.arr.forEach(
                    function(calendar){
                        if(title === calendar.title){
                            $scope.calendarEvent.calendar.push(calendar);
                        }
                });
        });
    }

    /**
     * Return the name of the smaller right
     * @param event the calendar event
     */
    $scope.smallerRightEvent = (event : CalendarEvent): string => {
        let right = "manage";
        event.calendar.filter(e=>e != null).forEach(
            function(calendar){
                if($scope.hasContribRight(calendar)){
                    if(!calendar.myRights.manage && right != "read"){
                        right = "contrib";
                    }
                } else {
                    right = "read";
                }
            }
        )
        return right;
    }

    /**
     * Verify if one of the calendar of the event as the right of manage or if the event is created by the user and he
     * as one of his calendar with the right of contrib
     * @param event calendar event to check
     */
    $scope.hasACalendarWithRightsOfModifyEvent = (event : CalendarEvent): boolean => {
        let right = false
        event.calendar.forEach(
            function(calendar) {
                if ($scope.hasContribRight(calendar)) {
                    if (calendar.myRights.manage || $scope.isMyEvent(event)) {
                        right = true;
                    }
                }
            }
        );
        return right;
    }

    $scope.displayImportIcsPanel = function() {
        $scope.display.showImportPanel = true;
        $scope.display.importFileButtonDisabled = true;
        $scope.newFile.name = '';
    };

    $scope.setFilename = async () => {
    	if($scope.newFile && $scope.newFile.files && $scope.newFile.files.length > 0) {
        	$scope.newFile.name = $scope.newFile.files[0].name;
            await $scope.reader.readAsText($scope.newFile.files[0], 'UTF-8');
            $scope.reader.onloadend = async () => {
                return $scope.jsonData.ics = $scope.reader.result;
            };
            disableImportFileButton();
    	}
    };
    $scope.importIcsFile = async (calendar, event) => {
        event.currentTarget.disabled = true;
        await $scope.calendar.importIcal($scope.jsonData);
        $scope.icsImport = $scope.calendar.icsImport;
        $scope.display.showImportPanel = undefined;
        $scope.display.showImportReport = true;
        $scope.importFileButtonDisabled = true;
        await $scope.calendar.calendarEvents.sync($scope.calendar, $scope.calendars);
        $scope.loadCalendarEvents();
        if ($scope.display.list) {
            $scope.showList();
        }else {
            $scope.showCalendar();
        }
    };

    $scope.verifyInputDates = function() {
        if($scope.calendarEvent.title){
            $scope.calendarEvent.showDates = true;
        }
        else{
            $scope.calendarEvent.showDetails = true;
        }
    };

        $scope.verifyInputRec = function() {
        if($scope.calendarEvent.title){
            $scope.calendarEvent.showRecurrence = true;
        }
        else{
            $scope.calendarEvent.showDetails = true;
        }
    };

    $scope.canICloseLightBox = function() {
        if($scope.calendarEvent.title == undefined && $scope.calendarEvent.description == undefined && $scope.calendarEvent.location == undefined){
            return false;
        }else {
            let toCompare = "";
            if($scope.calendarEvent.title != undefined){
                toCompare += $scope.calendarEvent.title;
            }
            if($scope.calendarEvent.description != undefined){
                toCompare += $scope.calendarEvent.description;
            }
            if($scope.calendarEvent.location != undefined){
                toCompare += $scope.calendarEvent.location;
            }
            if ($scope.contentToWatch != toCompare) {
                return !confirm(lang.translate("calendar.navigation.guard"));
            } else {
                return false;
            }
        }
    };

    $scope.saveCalendarEventEdit = async (calendarEvent = $scope.calendarEvent, event?, shareOption?: boolean) => {
        async function doItemCalendarEvent(items, count) {
            if (items.length === count) {
                calendarEvent.noMoreRecurrent = calendarEvent.noMoreRecurrent && false;
                calendarEvent.noMoreRecurrence = calendarEvent.noMoreRecurrence && false;
                calendarEvent.detailToRecurrence = calendarEvent.detailToRecurrence && false;
                calendarEvent.startDateToRecurrence = calendarEvent.startDateToRecurrence && false;
                calendarEvent.endDateToRecurrence = calendarEvent.endDateToRecurrence && false;
                $scope.closeCalendarEvent();
                $scope.refreshCalendarEvents();
                $scope.calendarEvents.applyFilters();
                $scope.display.calendar = true;
                if (shareOption) {
                    if (calendarEvent.isRecurrent && !calendarEvent.created){
                        if (!$scope.display.showEventPanel) {
                           $scope.shareEvent($scope.recurrentCalendarEventToShare ? $scope.recurrentCalendarEventToShare : {}, event);
                        }
                    } else {
                        $scope.shareEvent($scope.calendarEvent, event);
                    }
                }
            } else {
                items[count].calEvent.owner = {
                    userId: model.me.userId,
                    displayName: model.me.username
                };
                let itemCalendarEvent = items[count].calEvent;
                let action = items[count].action;
                if (action === 'save') {
                    if (itemCalendarEvent.isRecurrent && count!== 0) {
                        var parentId = items[0].calEvent._id;
                        if (items[0].calEvent.parentId) {
                            parentId = items[0].calEvent.parentId;
                        }
                        if ($scope.recurrentCalendarEventToShare === null) {
                            $scope.recurrentCalendarEventToShare = items[count].calEvent;
                        }
                        itemCalendarEvent.parentId = parentId;
                    }
                    if (!itemCalendarEvent.created && $scope.sendNotif === false){
                        itemCalendarEvent.sendNotif = $scope.sendNotif;
                    }
                    itemCalendarEvent.save()
                        .then(() => {
                            items[count].calEvent._id =  itemCalendarEvent._id;
                            count++;
                            doItemCalendarEvent(items, count);
                        })
                        .catch((e) =>{
                            console.error(e);
                            notify.error(lang.translate('calendar.error.date.saving'));
                            count = items.length;
                            doItemCalendarEvent(items, count);
                        })
                } else {
                    await itemCalendarEvent.delete();
                        count++;
                        doItemCalendarEvent(items, count);
                }
            }
        }
        $scope.recurrentCalendarEventToShare = null;
        $scope.createContentToWatch();
        var items = [];
        calendarEvent.startMoment = moment(calendarEvent.startMoment).seconds(0).milliseconds(0);
        calendarEvent.endMoment = moment(calendarEvent.endMoment).seconds(0).milliseconds(0);
        $scope.display.calendar = false;
        var hasExistingRecurrence = false;
        var recurrentCalendarEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);

        if (recurrentCalendarEvents.length > 1) {
            hasExistingRecurrence = true;
        }
        if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
            calendarEvent.recurrence.start_on = moment(calendarEvent.startMoment).hours(0).minutes(0).seconds(0).milliseconds(0);
            if (calendarEvent.recurrence.end_on) {
                calendarEvent.recurrence.end_on = moment(calendarEvent.recurrence.end_on).hours(0).minutes(0).seconds(0).milliseconds(0);
            }
        }
        if (calendarEvent.noMoreRecurrent) {
            calendarEvent.isRecurrent = false;
        }
        if (!calendarEvent.isRecurrent) {
            if (calendarEvent.recurrence) {
               calendarEvent.recurrence = false;
            }
            if (calendarEvent.parentId) {
               calendarEvent.parentId = false;
            }
            calendarEvent.index = 0;
        }

        var parentAction = 'create';
        if (calendarEvent._id) {
            parentAction = 'update';
        }
        var item = {'calEvent': calendarEvent, 'action': 'save'};
        items.push(item);
        if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
            Array.prototype.push.apply(items, $scope.handleRecurrence(calendarEvent));
        }
        if (calendarEvent.isRecurrent && !calendarEvent.parentId) {
            var item = {'calEvent': calendarEvent, 'action': 'delete'};
            if (parentAction === 'create') {
                items.push(item);
            } else if (items.length > 1) {
                items.push(item);
            } else {
                calendarEvent.parentId = calendarEvent._id;
                item.action = 'save';
                items.push(item);
            }
        }
        if (calendarEvent.noMoreRecurrent && hasExistingRecurrence && calendarEvent.noMoreRecurrence) {
            recurrentCalendarEvents.forEach(function(cle) {
                if (cle._id !== calendarEvent._id) {
                    var item = {'calEvent': cle, 'action': 'delete'};
                    items.push(item);
                }
            });
        }
        if ((calendarEvent.detailToRecurrence ||
            calendarEvent.startDateToRecurrence ||
            calendarEvent.endDateToRecurrence) &&
            calendarEvent.parentId) {
            recurrentCalendarEvents.forEach (function(cle) {
                if (cle._id !== calendarEvent._id) {
                    var save = false;
                    if (calendarEvent.detailToRecurrence) {
                        cle.title = calendarEvent.title;
                        cle.description = calendarEvent.description;
                        cle.location = calendarEvent.location;
                        save = true;
                    }
                    if (calendarEvent.startDateToRecurrence
                    && !calendarEvent.allday && !cle.allday
                    && !moment(cle.startMoment).hours($scope.calendarEvent.startMoment.hours())
                            .minutes($scope.calendarEvent.startMoment.minutes()).isAfter(cle.endMoment, 'minute')) {
                                cle.startMoment = moment(cle.startMoment)
                                    .hours(moment(calendarEvent.startTime).hours())
                                    .minutes(moment(calendarEvent.startTime).minutes());
                                cle.startTime = calendarEvent.startTime;
                                save = true;
                    }
                    if (calendarEvent.endDateToRecurrence
                    && !calendarEvent.allday && !cle.allday
                    && !moment(cle.endMoment).hours($scope.calendarEvent.endMoment.hours())
                            .minutes($scope.calendarEvent.endMoment.minutes()).isBefore(cle.startMoment, 'minute')) {
                        cle.endMoment = moment(cle.endMoment)
                            .hours(moment(calendarEvent.endTime).hours())
                            .minutes(moment(calendarEvent.endTime).minutes());
                        cle.endTime = calendarEvent.endTime;
                        save = true;
                    }
                    if (calendarEvent.allday && calendarEvent.editAllRecurrence) {
                        cle.allday = true;
                        save = true;
                    } else if (!calendarEvent.allday && calendarEvent.editAllRecurrence){
                        cle.allday = false;
                        cle.startTime = calendarEvent.startTime;
                        cle.endTime = calendarEvent.endTime;
                        save = true;
                    }
                    if (save) {
                        var item = {'calEvent': cle, 'action': 'save'};
                        items.push(item);
                    }
                }
            });
        }
        await doItemCalendarEvent(items, 0);
    };

    $scope.cancelEventEdit = function(){
        $scope.display.showEventPanel = undefined;
    };

    $scope.cancelRecurrentEventEdit = (): void => {
        $scope.display.showRecurrencePanel = undefined;
    }

    $scope.switchSelectAllCalendarEvents = function() {
        if ($scope.display.selectAllCalendarEvents) {
            $scope.calendarEvents.filtered.forEach(function(calendarEvent) {
                if ($scope.smallerRightEvent(calendarEvent) == "manage") {
                    calendarEvent.selected = true;
                }
            });
        }
        else {
            $scope.calendarEvents.deselectAll();
        }
    };

    $scope.switchFilterListByDates = function() {
        $scope.calendarEvents.filters.startMoment = moment($scope.calendarEvents.filters.startMoment);
        $scope.calendarEvents.filters.endMoment = moment($scope.calendarEvents.filters.endMoment);
        $scope.calendarEvents.applyFilters();
        $scope.calendarEvents.filtered = $scope.removeDuplicateCalendarEvent($scope.calendarEvents.filtered);
        $scope.$apply();
    };

    $scope.closeRecurrence = function() {
        $scope.display.calendarEventRecurrence = false;
    };

    $scope.nextWeekBookingButton = function() {
        var nextStart = moment(model.calendarEvents.filters.startMoment).add(7, 'day');
        var nextEnd = moment(model.calendarEvents.filters.endMoment).add(7, 'day');
        updateCalendarList(nextStart,nextEnd);
    };

    $scope.previousWeekBookingButton = function() {
        var prevStart = moment(model.calendarEvents.filters.startMoment).subtract(7, 'day');
        var prevEnd = moment(model.calendarEvents.filters.endMoment).subtract(7, 'day');
        updateCalendarList(prevStart,prevEnd);
    };

    $scope.switchCalendarEventTab = function(tab) {
        if (tab === 'dates') {
            $scope.calendarEvent.showRecurrence = false;
            $scope.calendarEvent.showDetails = false;
            $scope.calendarEvent.showDates = true;
            $scope.showCalendarEventTimePicker = true;

        } else if (tab === 'details') {
            $scope.calendarEvent.showRecurrence = false;
            $scope.calendarEvent.showDetails = true;
            $scope.calendarEvent.showDates = false;
            $scope.showCalendarEventTimePicker = false;

        } else if (tab === 'recurrence') {
            $scope.calendarEvent.showRecurrence = true;
            $scope.calendarEvent.showDetails = false;
            $scope.calendarEvent.showDates = false;
            $scope.showCalendarEventTimePicker = false;
        }
    };

    var updateCalendarList = function(start, end){
        model.calendarEvents.filters.startMoment.date(start.date());
        model.calendarEvents.filters.startMoment.month(start.month());
        model.calendarEvents.filters.startMoment.year(start.year());
        model.calendarEvents.filters.endMoment.date(end.date());
        model.calendarEvents.filters.endMoment.month(end.month());
        model.calendarEvents.filters.endMoment.year(end.year());
        $scope.calendarEvents.applyFilters();

    };
    $scope.$watch(
        function() {
            return $('.hiddendatepickerform')[0]
                ? $('.hiddendatepickerform')[0].value
                : '';
        },
        function(newVal, oldVal) {
            if (newVal !== oldVal  &&  !$scope.display.list &&
            newVal &&
            newVal !== '' ){
                updateCalendarSchedule(moment(
                    newVal,
                    'DD/MM/YYYY'
                ).startOf('isoweek'));
            }

        });
    var updateCalendarSchedule = function(newDate){
        model.calendar.firstDay.date(newDate.date());
        model.calendar.firstDay.month(newDate.month());
        model.calendar.firstDay.year(newDate.year());
        $scope.calendar.calendarEvents.sync($scope.calendar, $scope.calendars);
        $scope.calendarEvents.applyFilters();
            template.open('calendar', 'read-calendar');
        $('.hiddendatepickerform').datepicker('setValue', newDate.format("DD/MM/YYYY")).datepicker('update');
        $('.hiddendatepickerform').trigger({type: 'changeDate', date: newDate});

    };
}]);