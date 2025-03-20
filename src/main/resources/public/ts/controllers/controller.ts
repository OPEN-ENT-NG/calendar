import {$, _, moment, ng, template, idiom as lang, notify, toasts, angular, Document} from "entcore";
import {
    Calendar,
    Calendars,
    CalendarEvent,
    CalendarEvents, CalendarEventRecurrence, Preference, CalendarEventReminder,
} from "../model";
import {
    defaultColor,
    periods,
    timeConfig,
    recurrence,
    LANG_CALENDAR,
    rights,
    ACTIONS,
    ActionButtonType, minStartMomentDate, maxEndMomentDate
} from "../model/constantes";
import {
    makerFormatTimeInput,
    utcTime,
    safeApply
} from "../model/Utils";
import {AxiosResponse} from "axios";
import {DateUtils} from "../utils/date.utils";
import {Subject} from "rxjs";
import {Moment} from "moment";
import {FORMAT} from "../core/const/date-format";
import {DAY_OF_WEEK} from "../core/enum/dayOfWeek.enum";
import {attachmentService, calendarEventService} from "../services";
import {PERIODE_TYPE} from "../core/enum/period-type.enum";
import {RbsEmitter} from "../model";
import {IScope} from "angular";
import {RBS_EVENTER} from "../core/enum/rbs/rbs-eventer.enum";
import {RBS_SNIPLET} from "../core/const/rbs-sniplet.const";
import {externalCalendarUtils} from "../utils/externalCalendarUtils";
import {calendarService} from "../services";
import {reminderService} from "../services/reminder.service";

declare var ENABLE_RBS: boolean;
declare var ENABLE_ZIMBRA: boolean;
declare var ENABLE_REMINDER: boolean;
declare let window: any;

export const calendarController = ng.controller('CalendarController',
    ["$location",
        "$scope",
        "$timeout",
        "$compile",
        "$sanitize",
        "$sce",
        "model",
        "route",
        function ($location, $scope, $timeout, $compile, $sanitize, $sce, model, route) {
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
            $scope.calendarAsContribRight = new Array<Calendar>();
            $scope.selectedCalendarInEvent = new Array<Calendar>();
            $scope.rights = rights;
            $scope.buttonAction = ACTIONS;
            $scope.eventSidebar$ = new Subject<void>();
            $scope.ENABLE_RBS = ENABLE_RBS;
            $scope.rbsEmitter = new RbsEmitter($scope, !!$scope.ENABLE_RBS);
            $scope.ENABLE_ZIMBRA = ENABLE_ZIMBRA;
            $scope.ENABLE_REMINDER = ENABLE_REMINDER;
            $scope.minDate = moment(minStartMomentDate);

                template.open('main', 'main-view');
                template.open('top-menu', 'top-menu');

            this.$onInit = () => {
                // Control opening of event lightbox
                model.calendar.eventer.on("calendar.create-item", function (timeSlot) {
                    if (model.calendar.display.mode != "month") {
                        window.bookingState = ACTIONS.create;
                        $scope.createCalendarEvent($scope.calendarEvent, true);
                    }
                });
            };

            this.$onDestroy = () => {
                $scope.eventSidebar$.unsubscribe();
            };

            route({
                goToCalendar: async function (params) {
                    await Promise.all([
                        $scope.calendars.syncCalendars(),
                        $scope.calendars.preference.sync()
                    ]);

                    setCalendarLang();

                    await $scope.resolveSelectedCalendars();

                    let oneWeekLater: Date = model.calendar.firstDay.clone().add(7, 'd');
                    await $scope.calendars.syncSelectedCalendarEvents(model.calendar.firstDay.format(FORMAT.formattedDate), oneWeekLater.format(FORMAT.formattedDate));
                    $scope.loadCalendarEvents();

                    let calendarNotification = $scope.calendars.all.filter(calendarFiltre => calendarFiltre._id === params.calendar)[0];
                    if (calendarNotification === undefined) {
                        $scope.notFound = true;
                        template.open('error', '404');
                    } else {
                        $scope.notFound = false;
                        $scope.openOrCloseCalendar(calendarNotification, true);
                    }
                },
                mainPage: async function () {
                    await Promise.all([
                        $scope.calendars.syncCalendars(),
                        $scope.calendars.preference.sync()
                    ]);

                    $scope.resolveSelectedCalendars();

                    let defaultCalendar: Calendar = $scope.calendars.all.find(cal => cal.is_default == true);
                    if (defaultCalendar) {
                        defaultCalendar.selected = false;
                        $scope.openOrCloseCalendar(defaultCalendar, true);
                    }

                    let oneWeekLater: Date = model.calendar.firstDay.clone().add(7, 'd');
                    await $scope.calendars.syncSelectedCalendarEvents(model.calendar.firstDay.format(FORMAT.formattedDate), oneWeekLater.format(FORMAT.formattedDate));
                    $scope.loadCalendarEvents();
                    //ensure that default calendar is selected


                    $scope.firstOwnedEvent();
                    $scope.initEventDates(moment().utc().second(0).millisecond(0), moment().utc().second(0).millisecond(0).add(1, 'hours'));
                    setCalendarLang();
                    $scope.$apply();
                },
            });

            model.calendar.on('date-change', async () => {
                $scope.calendarEvents.filtered = [];
                await updateChangeDateCalendarSchedule(model.calendar.firstDay);
                $scope.loadCalendarEvents();
            });

            function disableImportFileButton() {
                $scope.$apply(function () {
                    $scope.display.importFileButtonDisabled = false;
                });
            }

            const setCalendarLang = (): void => {
                model.calendar.firstDay = model.calendar.firstDay.lang(LANG_CALENDAR);
            };

            /**
             * Synchronise elements depending on the display
             */
            $scope.syncSelectedCalendars = async function () {
                if (model && model.calendar && model.calendar.display && model.calendar.display.mode) {
                    switch (model.calendar.display.mode) {
                        case PERIODE_TYPE.DAY:
                            let oneDayLater = model.calendar.firstDay.clone().add(1, 'd');
                            await $scope.calendars.syncSelectedCalendarEvents(model.calendar.firstDay.format(FORMAT.formattedDate), oneDayLater.format(FORMAT.formattedDate));
                            break;
                        case PERIODE_TYPE.WEEK:
                            let oneWeekLater = model.calendar.firstDay.clone().add(7, 'd');
                            await $scope.calendars.syncSelectedCalendarEvents(model.calendar.firstDay.format(FORMAT.formattedDate), oneWeekLater.format(FORMAT.formattedDate));
                            break;
                        case PERIODE_TYPE.MONTH:
                            let oneMonthLater = model.calendar.firstDay.clone().add(1, 'M');
                            await $scope.calendars.syncSelectedCalendarEvents(model.calendar.firstDay.format(FORMAT.formattedDate), oneMonthLater.format(FORMAT.formattedDate));
                            break;
                    }
                }
            }


            $scope.isEmpty = () => {
                return $scope.calendars
                    && $scope.calendars.all
                    && $scope.calendars.all.length < 1;
            }

            /*
            ** Fetch all events from selected Calendars
             */
            $scope.loadCalendarEvents = async (calendar?: Calendar) => {
                if ($scope.calendars && $scope.calendars.all) {
                    if (calendar) await $scope.syncSelectedCalendars();
                    if ($scope.calendars.all.length > 0) {
                        $scope.calendarEvents.filtered = $scope.calendars.arr.map((element: Calendar) => element.selected ?
                            element.calendarEvents.all : []).flat();
                        $scope.calendarEvents.all = $scope.calendarEvents.filtered;

                        //add multi days events to multiDaysEvents array
                        $scope.calendarEvents.multiDaysEvents = $scope.calendars.arr.map((element: Calendar) =>
                            (element.calendarEvents && element.calendarEvents.multiDaysEvents) ?
                                element.calendarEvents.multiDaysEvents : []).flat();
                    }
                }
                if ($scope.display.list) {
                    $scope.restoreMultiDaysEvents();
                    $scope.calendarEvents.applyFilters();
                }
                $scope.calendarEvents.filtered = $scope.removeDuplicateCalendarEvent($scope.calendarEvents.filtered);

                /* trigger tooltip to show up */
                let $scheduleItems: JQuery = $('.schedule-items');
                $scheduleItems.mousemove(() => {
                    $timeout(() => safeApply($scope), 600)
                });

                safeApply($scope);
            };

            $scope.restoreMultiDaysEvents = (): void => {
                $scope.calendarEvents.filtered = $scope.calendarEvents.filtered.filter((evt: CalendarEvent) => !evt.isMultiDayPart);
                $scope.calendarEvents.filtered = [...$scope.calendarEvents.filtered, ...$scope.calendarEvents.multiDaysEvents];
                $scope.calendarEvents.all = $scope.calendarEvents.filtered;
            };

            const getHashWithDate = (a: CalendarEvent) => `${a._id}-${a.startMoment ?? ''}-${a.endMoment ?? ''}`;
            const getHashWithoutDate = (a: CalendarEvent) => `${a._id}`;

            /**
             * Remove events which are duplicated
             */
            $scope.removeDuplicateCalendarEvent = (
            events: Array<CalendarEvent>
            ): Array<CalendarEvent> => {
            if ($scope.display.list) {
                //list view
                events = Array.from(
                events
                    .reduce(
                    (
                        filteredEvents: Map<string, CalendarEvent>,
                        item: CalendarEvent
                    ) => {
                        //case where all elements must have a unique _id
                        const hash = getHashWithoutDate(item);
                        if (!filteredEvents.has(hash)) {
                        filteredEvents.set(hash, item);
                        }
                        return filteredEvents;
                    },
                    new Map<string, CalendarEvent>()
                    )
                    .values()
                );
            } else {
                //calendar view
                if (events) {
                events = Array.from(
                    events
                    .reduce(
                        (
                        filteredEvents: Map<string, CalendarEvent>,
                        item: CalendarEvent
                        ) => {
                        //ensures that all events are unique:
                        //a normal event has a unique id
                        //a multi days event part is the only one with this id/startMoment/endMoment combo
                        const hash = getHashWithDate(item);
                        if (!filteredEvents.has(hash)) {
                            filteredEvents.set(hash, item);
                        }
                        return filteredEvents;
                        },
                        new Map<string, CalendarEvent>()
                    )
                    .values()
                );
                }
            }
            return events;
            };

            /** Returns true if the two events represent the same part of a multi-day event (same id, start and end moments)
             *
             * @param event1 first event to be compared
             * @param event2 2nd event of the comparison
             */
            const isSameMultiDayEventPart = (event1: CalendarEvent, event2: CalendarEvent): boolean => {
                return ((event1._id == event2._id) && moment(event1.startMoment).isSame(moment(event2.startMoment)) && moment(event1.endMoment).isSame(moment(event2.endMoment)))
            }


            $scope.someSelectedValue = function (selection) {
                return Object.keys(selection).map(function (val) {
                    return selection[val];
                }).some(function (val) {
                    return val === true;
                });
            };


        $scope.changeStartMoment = () => {
            if (!$scope.isDateValid()) {
                $scope.calendarEvent.endMoment = moment($scope.calendarEvent.startMoment);
            } else {
              $scope.calendarEvent.endMoment = moment($scope.calendarEvent.endMoment).toDate();
            }
            $scope.rbsEmitter.updateRbsSniplet();
        };

        $scope.changeEndMoment = () => {
            if (!$scope.isDateValid()) {
                $scope.calendarEvent.startMoment = moment($scope.calendarEvent.endMoment);
            } else {
               $scope.calendarEvent.startMoment = moment($scope.calendarEvent.startMoment).toDate();
            }
            $scope.rbsEmitter.updateRbsSniplet();
        };

        $scope.retrieveMultiDaysEvent = (id : string) : string => {
            return $scope.calendarEvents.multiDaysEvents.find((event: CalendarEvent) => event._id === id);
        };

        $scope.convertFormat = (item: CalendarEvent,  isStart: boolean) : string => {
            let event : CalendarEvent =  $scope.retrieveMultiDaysEvent(item._id);
            return isStart ? (event.startMoment.format(FORMAT.displayFRDate)) + " : " + (event.startMoment.format(FORMAT.displayTime))
                : (event.endMoment.format(FORMAT.displayFRDate)) + " : " + (event.endMoment.format(FORMAT.displayTime));
        };

        $scope.toggleIsRecurrent = function (calendarEvent) {
            $scope.rbsEmitter.updateRbsSniplet();
            if (calendarEvent.isRecurrent) {
                if (!$scope.calendarEvent.recurrence.end_on) {
                    $scope.calendarEvent.recurrence.end_on = moment($scope.calendarEvent.endMoment).add(1, 'days').hours(0).minutes(0).seconds(0).milliseconds(0);
                }
                if(!$scope.isOneDayEvent()){
                    $scope.calendarEvent.recurrence.start_on =  moment($scope.calendarEvent.startMoment).add(1, 'days').hours(0).minutes(0).seconds(0).milliseconds(0);
                }
                //if the event lasts more than one day, it cannot be recurrent daily => the weekly recurrence becomes default
                if (!$scope.calendarEvent.recurrence.type) {
                    $scope.isOneDayEvent() ? ($scope.calendarEvent.recurrence.type = 'every_day') : ($scope.calendarEvent.recurrence.type = 'every_week');
                }
                if (!$scope.calendarEvent.recurrence.every) {
                    $scope.calendarEvent.recurrence.every = 1;
                }
                if ($scope.calendarEvent.recurrence.type === 'every_week') {
                    $scope.changedRecurrenceType();
                }
                if(!$scope.isOneDayEvent()) $scope.changeStartMoment();
            }
        };

            $scope.toggleDateToRecurrence = function (name) {
                if (name === 'start') {
                    $scope.calendarEvent.endDateToRecurrence = false;
                }
                if (name === 'end') {
                    $scope.calendarEvent.startDateToRecurrence = false;
                }
            };

            $scope.getDate = function (theDate) {
                return moment(theDate).format(FORMAT.formattedDate);
            };

            $scope.changedRecurrenceType = function () {
                $scope.calendarEvent.recurrence.week_days = {
                    1: false,
                    2: false,
                    3: false,
                    4: false,
                    5: false,
                    6: false,
                    7: false
                };
                if ($scope.calendarEvent.recurrence.type === 'every_week') {
                    if (!$scope.someSelectedValue($scope.calendarEvent.recurrence.week_days)) {
                        let dayOfWeek: number = moment($scope.calendarEvent.startMoment).day();
                        if (dayOfWeek === 0) {
                            dayOfWeek = 7;
                        }
                        $scope.calendarEvent.recurrence.week_days[dayOfWeek] = true;
                    }
                }
            };

            $scope.handleRecurrence = function (calendarEvent) {
                if (calendarEvent.recurrence) {
                    if (calendarEvent.recurrence.type == 'every_day' && calendarEvent.recurrence.every) {
                        return $scope.handleEveryDayRecurrence(calendarEvent);
                    } else if (calendarEvent.recurrence.type == 'every_week') {
                        return $scope.handleEveryWeekRecurrence(calendarEvent);
                    }
                } else {
                    return 0;
                }
            };

            $scope.handleEveryDayRecurrence = function (calendarEvent) {
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

                    for (let i = 0; startMoment.isBefore(endOnMoment); i++) {
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

            $scope.handleEveryWeekRecurrence = function (calendarEvent) {
                let weekDays = Object.keys(calendarEvent.recurrence.week_days).filter(val =>
                    calendarEvent.recurrence.week_days[val]
                );
                let dayJump = 7 * calendarEvent.recurrence.every;
                let startOn: Moment = moment(calendarEvent.recurrence.start_on);
                let startDay = calendarEvent.recurrence.start_on.isoWeekday();
                let startHour: Moment = moment(calendarEvent.startTime).hours();
                let startMinute = moment(calendarEvent.startTime).minutes();
                let duration: Moment = $scope.isOneDayEvent()
                    ? moment(calendarEvent.endTime).seconds(0).milliseconds(0).diff(moment(calendarEvent.startTime)
                        .seconds(0).milliseconds(0), 'minutes')
                    : moment(calendarEvent.endMoment).hours(calendarEvent.endTime.getHours()).minutes(calendarEvent.endTime.getMinutes())
                        .seconds(0).milliseconds(0)
                        .diff(moment(calendarEvent.startMoment).hours(calendarEvent.startTime.getHours()).minutes(calendarEvent.startTime.getMinutes())
                            .seconds(0).milliseconds(0), 'minutes');
                let recurrenceDays = weekDays.filter(val => val >= startDay);
                if (recurrenceDays.length === 0) {
                    startOn.isoWeekday(1).add(dayJump, 'days');
                } else {
                    startOn.isoWeekday(parseInt(recurrenceDays[0], 10));
                }
                let endOn: Moment = moment(startOn.toISOString());
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
                        endOn = endOn.isoWeekday(1).add(dayJump, 'days');
                    }
                    endOn = moment(startOn.toISOString());
                    if (recurrenceDays.length) {
                        let previousDay = recurrenceDays[0];
                        recurrenceDays.every((day, idx) => {
                            if (day <= previousDay && idx > 0) {
                                endOn = endOn.isoWeekday(1).add(dayJump, 'days');
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

            $scope.handleEveryWeekDayRecurrence = function (calendarEvent) {
                var calendarRecurrentEvent;
                if (calendarEvent.recurrence.end_type == 'after' && calendarEvent.recurrence.end_after) {
                    for (let i = 0; i < calendarEvent.recurrence.end_after; i++) {
                        calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                        calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(i + 1, 'days');
                        calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(i + 1, 'days');
                        $scope.saveCalendarEventEdit(calendarRecurrentEvent);
                    }
                }
            };

            $scope.cropEventDate = (calendarEventDate: string): string =>
                DateUtils.getSimpleFRDateFormat(calendarEventDate);

            $scope.createChildCalendarEvent = function (calendarEvent) {
                var child = new CalendarEvent();
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
                child.isMultiDayPart = calendarEvent.isMultiDayPart;
                child.attachments = calendarEvent.attachments;
                return child;
            };


            $scope.firstOwnedCalendar = function () {
                return _.find($scope.calendars.all, function (calendar) {
                    return $scope.isMyCalendar(calendar);
                });
            };

            $scope.firstOwnedEvent = function () {
                return _.find($scope.calendarEvents.all, function (calendarEvent) {
                    return $scope.isMyEvent(calendarEvent);
                });
            };

            $scope.updateCalendars = async (isCreation?: boolean) => {
                let updatedCalendar = $scope.calendar;
                await Promise.all([
                    $scope.calendars.sync(),
                    $scope.calendars.preference.sync()
                ]);
                $scope.calendars.selectedElements = $scope.calendars.preference.selectedCalendars;
                $scope.resolveSelectedCalendars();
                $scope.showCalendar();
                $scope.calendar = $scope.calendars.all.find((cl: Calendar) => cl._id == updatedCalendar._id);
                if ($scope.calendar) {
                    $scope.calendar.selected = true;
                    $scope.eventSidebar$.next();
                    $scope.display.showToggleButtons = !isCreation;
                } else {
                    $scope.display.showToggleButtons = false;
                }
                safeApply($scope);
            };

            $scope.updateExternalCalendar = async (calendar: Calendar) : Promise<void> => {
                $scope.display.showToggleButtons = false;

                let updatedCalendar: Calendar = await calendarService.fetchCalendarById(calendar);
                if (updatedCalendar._id) {
                    $scope.calendars.all.find((cl: Calendar) => (cl._id == updatedCalendar._id)).updated = updatedCalendar.updated;
                    await $scope.syncSelectedCalendars();

                    $scope.loadCalendarEvents();
                    safeApply($scope);
                }
            };

            /**
             * Get all selected calendars and the events from these selected calendars
             **/
            $scope.loadSelectedCalendars = (noSelectionUpdate?: boolean): void => {
                if ($scope.calendars.preference && !noSelectionUpdate) {
                    let toSelectCalendars: Calendar[] = $scope.calendars.all.filter(calendar => {
                        return _.contains($scope.calendars.preference.selectedCalendars, calendar._id);
                    });
                    toSelectCalendars.forEach(function (cl) {
                        $scope.openOrCloseCalendar(cl, false);
                    });
                }
                if ($scope.calendars.selected.length === 0 && !$scope.calendars && !noSelectionUpdate) {
                    var calendarToOpen = $scope.firstOwnedCalendar();
                    if (calendarToOpen === undefined) {
                        calendarToOpen = $scope.calendars.all[0];
                    }
                    $scope.openOrCloseCalendar(calendarToOpen, true);
                }
            };

            /**
             * Get all selected calendars and the events from these selected calendars
             **/
            $scope.resolveSelectedCalendars = (): void => {
                if ($scope.calendars.preference) {
                    $scope.calendars.all
                        .filter((calendar: Calendar) => _.contains($scope.calendars.preference.selectedCalendars, calendar._id))
                        .forEach((calendar: Calendar) => calendar.selected = true);
                }
                if ($scope.calendars.selected.length === 0 && !$scope.calendars) {
                    let calendarToOpen: Calendar = $scope.firstOwnedCalendar();
                    if (calendarToOpen == undefined) {
                        calendarToOpen = $scope.calendars.all[0];
                    }
                    calendarToOpen.selected = true;
                }
            };


            $scope.saveCalendarPreferences = async (): Promise<void> => {
                await $scope.calendars.preference.update();
            };

            /**
             * View events in a list
             */
            $scope.showList = function () {
                $scope.display.list = true;
                $scope.display.calendar = false;
                $scope.display.propertyName = 'startMoment';
                $scope.reverse = false;
                $scope.calendarEvents.filters.startMoment = moment().startOf('day');
                template.open('calendar', 'events-list');
            };

            $scope.sortBy = function (propertyName) {
                $scope.reverse = ($scope.propertyName === propertyName) ? !$scope.reverse : false;
                $scope.propertyName = propertyName;
            };

            /**
             * View events in a grid
             */
            $scope.showCalendar = function () {
                if ($scope.calendars.all.length === 0) return;
                $scope.display.list = false;
                $scope.display.calendar = true;
                $scope.loadCalendarEvents();
                template.open('calendar', 'read-calendar');
            };

            $scope.isMyCalendar = function (calendar) {
                return calendar.owner.userId == $scope.me.userId;
            };

            /**
             * Return true if the current user created the event
             * @param calEvent event to check
             */
            $scope.isMyEvent = (calEvent: CalendarEvent): boolean => {
                return calEvent.owner.userId === $scope.me.userId;
            };


            $scope.initEventDates = function (startMoment, endMoment) {
                var event = $scope.calendarEvent;
                var minTime = moment(startMoment);
                minTime.set('hour', timeConfig.start_hour);
                var maxTime = moment(endMoment);
                maxTime.set('hour', timeConfig.end_hour);
                if (startMoment.isAfter(minTime) && startMoment.isBefore(maxTime)) {
                    event.startMoment = startMoment;
                } else {
                    if (startMoment.isAfter(maxTime)) {
                        startMoment.add(1, 'days');
                        endMoment.add(1, 'days');
                        maxTime.add(1, 'days');
                    }
                }
                if (endMoment.isBefore(maxTime)) {
                    event.endMoment = endMoment;
                }
            };

            function handleCalendarDisplay(calendar: Calendar): void {
                calendar.selected = !calendar.selected;
                if (!!calendar) {
                    $scope.calendar = calendar;
                }
                $scope.display.editEventRight = $scope.hasContribRight();
                $scope.calendarEvents.applyFilters();
                if (!$scope.display.list && !$scope.display.calendar) {
                    $scope.showCalendar();
                } else {
                    $scope.loadCalendarEvents();
                }
            }

            async function setPreferences(calendar: Calendar): Promise<void> {
                $scope.calendars.preference.selectedCalendars = $scope.calendars.selectedElements.map(element => element._id);
                if ($scope.calendar && $scope.calendar.selected) {
                    $scope.calendars.preference.selectedCalendars = [...$scope.calendars.preference.selectedCalendars, calendar._id];
                } else {
                    $scope.calendars.preference.selectedCalendars = $scope.calendars.preference.selectedCalendars.filter(element => element !== calendar._id)
                }
                await $scope.saveCalendarPreferences();
                $scope.calendarEvents.all.some((event: CalendarEvent) => event.calendar.find((cal: Calendar) => cal._id == calendar._id)) ?
                    await $scope.loadCalendarEvents() : await $scope.loadCalendarEvents(calendar);
            }

            $scope.openOrCloseCalendar = async function (calendar, savePreferences) {
                if ($scope.calendars.selected.length > 1 || !calendar.selected) {
                    handleCalendarDisplay(calendar);
                    if (savePreferences) {
                        await setPreferences(calendar);
                    }
                }
            };

            $scope.newCalendar = function () {
                $scope.calendarCreationScreen = true;
                $scope.calendar = new Calendar();
                $scope.calendar.color = defaultColor;
                template.open('calendar', 'edit-calendar');
            };

            $scope.resetMultipleDayEventInfo = (originalEvent: CalendarEvent): void => {
                $scope.calendarEvent.startMoment = originalEvent.startMoment;
                $scope.calendarEvent.endMoment = originalEvent.endMoment;

                let startDate: Moment = moment(originalEvent.startMoment).second(0).millisecond(0);
                let endDate: Moment = moment(originalEvent.endMoment).second(0).millisecond(0);
                $scope.calendarEvent.startTime = makerFormatTimeInput(moment(startDate), moment(startDate));
                $scope.calendarEvent.endTime = makerFormatTimeInput(moment(endDate), moment(endDate));
            };

        /**
         *Allows to view an event creation form
         *
         * @param calendarEvent the created event
         * @param isCalendar allows to use the lightbox from the calendar directive
         */
        $scope.viewCalendarEvent = (calendarEvent, isCalendar ? : boolean) => {
            $scope.calendarEvent = new CalendarEvent(calendarEvent);
            if(calendarEvent.isMultiDayPart){
                let originalEvent : CalendarEvent = $scope.calendarEvents.multiDaysEvents.find((item : CalendarEvent) => item._id == calendarEvent._id);
                originalEvent == undefined ? toasts.warning(lang.translate('calendar.event.get.error')) : $scope.resetMultipleDayEventInfo(originalEvent);
            }
            $scope.calendar = calendarEvent.calendar[0];
            $scope.createContentToWatch();
            $scope.calendarEvent.showDetails = true;
             if (!$scope.calendarEvent.parentId) {
                 if (!$scope.calendarEvent.recurrence) {
                     $scope.calendarEvent.recurrence = {};
                     $scope.calendarEvent.recurrence.week_days = recurrence.week_days;
                 }
            }

            if (($scope.hasManageRightOrIsEventOwner(calendarEvent) && $scope.hasRightOnSharedEvent(calendarEvent, rights.resources.updateEvent.right))
                && calendarEvent.editAllRecurrence == undefined
                && calendarEvent.isRecurrent && calendarEvent._id){
                template.open('recurrenceLightbox', 'recurrent-event-edition-popup');
                $scope.display.showRecurrencePanel = true;
            } else if(!calendarEvent._id || (!calendarEvent.isExternal
                && $scope.hasManageRightOrIsEventOwner(calendarEvent)
                && $scope.hasRightOnSharedEvent(calendarEvent, rights.resources.updateEvent.right))) {
                if (calendarEvent.editAllRecurrence){
                    //event content
                    $scope.calendarEvent.detailToRecurrence = true;
                    //event date/length
                    $scope.calendarEvent.startDateToRecurrence = true;
                    $scope.calendarEvent.endDateToRecurrence = true;
                }
                $scope.display.showRecurrencePanel = false;
                template.close('recurrenceLightbox');
                $scope.display.showEditEventPanel = true;
                $scope.rbsEmitter.emitBookingInfo(RBS_EVENTER.INIT_BOOKING_INFOS, RBS_SNIPLET.editEventPanel, $scope.calendarEvent);
            } else {
                $scope.display.showViewEventPanel = true;
                $scope.rbsEmitter.emitBookingInfo(RBS_EVENTER.INIT_BOOKING_INFOS, RBS_SNIPLET.viewEventPanel, $scope.calendarEvent);
            }
        };

        $scope.closeCalendarEvent = () => {
            if(!$scope.calendarEvent.title){
                $scope.eventForm = angular.element(document.getElementById("event-form")).scope();
                $scope.eventForm.editEvent.$setPristine();
                $scope.eventForm.editEvent.$setUntouched();
                $scope.$apply();
            }
            template.close('lightbox');
            $scope.showCalendarEventTimePicker = false;
            $scope.refreshCalendarEvents();
            $scope.display.showEditEventPanel = false;
            $scope.display.showViewEventPanel = false;
            $scope.contentToWatch = "";

            $scope.rbsEmitter.emitBookingInfo(RBS_EVENTER.CLOSE_BOOKING_INFOS, RBS_SNIPLET.editEventPanel);
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

            $scope.confirmRemoveCalendarEvents = function (event) {
                template.open('lightbox');
                $scope.calendarEvents.selected.forEach(calendarEvent => {
                    if (calendarEvent.deleteAllRecurrence) {
                        selectOtherRecurrentEvents(calendarEvent);
                    }
                });
                $scope.display.confirmDeleteCalendarEvent = true;

                if ($scope.listViewSelectedOneCalendarEventWithBooking()) {
                    $scope.calendarEvent = $scope.calendarEvents.selectedElements[0];
                    $scope.viewCalendarEvent($scope.calendarEvent);
                }
            };

            /**
             * Returns true if one element of the list is selected and if it has a booking
             */
            $scope.listViewSelectedOneCalendarEventWithBooking = (): boolean => {
                return $scope.display.list && $scope.calendarEvents && $scope.calendarEvents.selectedElements
                    && $scope.calendarEvents.selectedElements.length == 1 && $scope.calendarEvents.selectedElements[0].bookings
                    && $scope.calendarEvents.selectedElements[0].bookings.length > 0;
            };

            /**
             * Select all recurrent events associated to the current event
             * @param calendarEvent calendar Event
             */
            const selectOtherRecurrentEvents = (calendarEvent: CalendarEvent): void => {
                if (calendarEvent.isMultiDayPart) {
                    $scope.restoreMultiDaysEvents();
                }
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
                if ($scope.calendarEvents.selected.length === 0) {
                    let eventsCalendar = new CalendarEvent($scope.calendarEvent);
                    $scope.deleteCalendarEvent(eventsCalendar);
                    $scope.resetCalendarAfterRemoveEvent(count, countReccurent);
                } else {
                    if ($scope.listViewSelectedOneCalendarEventWithBooking()) {
                        $scope.deleteCalendarEvent($scope.calendarEvent);
                        $scope.closeCalendarEvent();
                    } else {
                        $scope.calendarEvents.selected.forEach(calendarEvent => {
                            $scope.deleteCalendarEvent(calendarEvent);
                            count--;
                            $scope.resetCalendarAfterRemoveEvent(count, countReccurent);
                        });
                    }
                }
                $scope.calendarEvents.deselectAll();
                $scope.display.selectAllCalendarEvents = $scope.display.selectAllCalendarEvents && false;
                if ($scope.calendarEvent.noMoreRecurrence) {
                    $scope.calendarEvent.isRecurrent = false;
                    $scope.calendarEvent.recurrence = false;
                    $scope.calendarEvent.parentId = false;
                    $scope.calendarEvent.index = 0;
                    $scope.calendarEvent.save();
                    $scope.refreshCalendarEvents();
                }
                template.close('lightbox');
                $scope.display.confirmDeleteCalendarEvent = false;
            };

            $scope.deleteCalendarEvent = async (event: CalendarEvent): Promise<void> => {
                try {
                    await event.delete();
                } catch (err) {
                    let error: AxiosResponse = err.response;
                    if (error.status === 400 && event.deleteAllBookings
                        && ($scope.display.calendar || $scope.calendarEvents.selected.length == 1)) {
                        toasts.warning(lang.translate('calendar.rbs.sniplet.error.booking.deletion'));
                    } else {
                        console.error(error.data.error);
                    }
                }
            };

            const addCssOnHtmlElement = (selectorHtml: string, propertyCss: string, valueCss: string): void => {
                $(selectorHtml).css(propertyCss, valueCss);
            }

            $scope.refreshCalendarEvents = async () => {
                template.close('lightbox');
                await $scope.calendars.syncCalendarEvents();
                await $scope.loadCalendarEvents();
                addCssOnHtmlElement("body > portal > div > section", "z-index", "1000");
                $scope.$apply();
            };


            $scope.resetCalendarAfterRemoveEvent = async function (count, countRecurrent) {
                if (count === 0 && countRecurrent === 0) {
                    await $scope.refreshCalendarEvents();
                    $scope.closeCalendarEvent();
                    if ($scope.display.list && $scope.display.selectAllCalendarEvents) {
                        $scope.display.selectAllCalendarEvents = undefined;
                    }
                    $scope.display.confirmDeleteCalendarEvent = false;
                }
            };

            $scope.cancelRemoveCalendarEvent = function () {
                $scope.display.confirmDeleteCalendarEvent = undefined;
                $scope.calendarEvent.deleteAllRecurrence = false;
                $scope.calendarEvent.deleteAllBookings = undefined;
                $scope.calendarEvents.forEach(function (calendarEvent) {
                    calendarEvent.selected = false;
                });
            };

            $scope.editCalendar = function (calendar, event) {
                $scope.calendar = calendar;
                $scope.calendarBeforeEdition = angular.copy(calendar);
                event.stopPropagation();
                template.open('calendar', 'edit-calendar');
                $scope.createContentToWatch();
            };

            $scope.createContentToWatch = function () {
                if ($scope.calendarEvent.title == undefined) {
                    $scope.contentToWatch = "";
                } else {
                    $scope.contentToWatch = $scope.calendarEvent.title;
                }
                if ($scope.calendarEvent.description != undefined) {
                    $scope.contentToWatch += $scope.calendarEvent.description;
                }
                if ($scope.calendarEvent.location != undefined) {
                    $scope.contentToWatch += $scope.calendarEvent.location;
                }
            };

            async function restoreCalendarPreferences(selectedCalendars: string[]): Promise<void> { //restores preferences in scope but needs sync to be applied
                $scope.calendars.preference.selectedCalendars = selectedCalendars.filter((value: string, index: number) => selectedCalendars.indexOf(value) === index);
                await $scope.saveCalendarPreferences();
            }

            $scope.saveCalendarEdit = async () => {
                let selectedCalendars :string[] = $scope.calendars.preference.selectedCalendars;
                if ($scope.calendar._id) {
                    await $scope.calendar.save();
                    await restoreCalendarPreferences(selectedCalendars);
                    await $scope.calendar.calendarEvents.sync($scope.calendar, $scope.calendars);
                    $scope.calendarEvents.applyFilters();

                } else {
                    await $scope.calendar.save();
                    handleCalendarDisplay($scope.calendar);
                    await $scope.updateCalendars();
                    $scope.loadCalendarEvents();
                    $scope.display.showToggleButtons = false;
                }
                safeApply($scope);
                $scope.showCalendar();
                $scope.calendarCreationScreen = false;
            };

            /**
             * Return true if the user as the right to manage the calendar of the event or if he created the event and he can contrib
             * to the calendar
             * @param calEvent event to check
             */
            $scope.hasManageRightOrIsEventOwner = (calEvent: CalendarEvent): boolean => {
                return $scope.smallerRightEvent(calEvent) == "manage" ||
                    ($scope.isMyEvent(calEvent) && $scope.smallerRightEvent(calEvent) == "contrib");
            }

            $scope.hasContribRight = calendar => {
                if (calendar) {
                    return calendar.myRights.contrib;
                } else {
                    return $scope.calendars.all.some(function (cl) {
                        if (cl.myRights.contrib && cl.selected) {
                            return true;
                        }
                    });
                }
            };

            $scope.hasRightOnSharedEvent = (calEvent: CalendarEvent, right: string): boolean => {
                if (!calEvent.shared || calEvent.owner.userId === $scope.me.userId) {
                    return true;
                } else {
                    let numberOfRights: number;
                    numberOfRights = (calEvent.shared
                        .filter(share => share[right]
                            && (share["userId"] === $scope.me.userId
                                || $scope.me.groupsIds.includes(share["groupId"]))).length);

                    return (numberOfRights > 0);
                }
            }

            $scope.hasReadRight = function (calendar) {
                if (calendar) {
                    return calendar.myRights.contrib;
                } else {
                    return $scope.calendars.selected.some(function (cl) {
                        if (cl.myRights.contrib) {
                            return true;
                        }
                    });
                }
            };

            $scope.cancelCalendarEdit = function () {
                if ($scope.isEmpty()) {
                    template.close('calendar');
                } else {
                    $scope.updateCalendars();
                    template.open('calendar', 'read-calendar');
                    $scope.calendar = $scope.calendarBeforeEdition;
                    let calendarElementSideBar = angular.element(document.getElementsByClassName("sidebar")).scope();
                    if (calendarElementSideBar && calendarElementSideBar.vm && calendarElementSideBar.vm.calendar) {
                        calendarElementSideBar.vm.calendar = $scope.calendarBeforeEdition;
                        safeApply($scope);
                    }
                }

            };

            $scope.confirmRemoveCalendar = function (calendar, event) {
                $scope.display.confirmDeleteCalendar = true;
                $scope.calendar = calendar;
                event.stopPropagation();

            };


            $scope.removeCalendar = async () => {
                $scope.calendar.calendarEvents.forEach(function (calendarEvent) {
                    externalCalendarUtils.isCalendarExternal($scope.calendar) ? calendarEvent.delete(true) : calendarEvent.delete();
                });
                let selectedCalendars :Preference = $scope.calendars.preference;
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
                $scope.calendars.preference = selectedCalendars;
                $scope.calendars.selectedElements = selectedCalendars.selectedCalendars;
                await $scope.saveCalendarPreferences();
                if ($scope.calendars.all.length === 1) {
                    template.close("calendar");
                }
                await $scope.calendars.sync();
                $scope.resolveSelectedCalendars();
                template.close('lightbox');
                $scope.display.confirmDeleteCalendar = undefined;
                $scope.eventSidebar$.next();
                $scope.display.showToggleButtons = false;
                $scope.$apply();
            };

            $scope.cancelRemoveCalendar = function () {
                $scope.display.confirmDeleteCalendar = undefined;
            };

            $scope.shareCalendar = function (calendar, event) {
                $scope.calendar = calendar;
                $scope.display.showPanelCalendar = true;
                event.stopPropagation();
            };

            $scope.shareEvent = function (calendarEvent, event) {
                $scope.calendarEvent = calendarEvent;
                $scope.display.showPanelEvent = true;
                event.stopPropagation();
            };

            $scope.saveAndShareEvent = async function (calendarEvent, event) {
                try {
                    $scope.sendNotif = false;
                    await $scope.saveCalendarEventEdit(calendarEvent, event, true);
                    $scope.sendNotif = true;
                } catch (err) {
                    $scope.display.showPanelEvent = false;
                    let error: AxiosResponse = err.response;
                    if (error.status === 401) {
                        toasts.warning(error.data.error);
                    } else {
                        toasts.warning(lang.translate('calendar.event.save.error'));
                    }
                }
                ;
            }

            $scope.nameOfShareButton = (calendarEvent: CalendarEvent, view: "calendar" | "list"): string => {
                if (!calendarEvent || !calendarEvent.calendar) {
                    return "";
                }

                let numberOfSharedCalendars: number = calendarEvent.calendar
                    .filter((calendar: Calendar): boolean => calendar.shared && (calendar.shared.length !== 0))
                    .length;

                const isShared = (): boolean => numberOfSharedCalendars > 0;

                switch (view) {
                    case "calendar":
                        return lang.translate(`calendar.event.save.and.${isShared() ? 'restrict' : 'share'}`);
                    case "list":
                        return lang.translate(`calendar.event.${isShared() ? 'restrict' : 'share'}`);
                    default:
                        return "";
                }
            }

        /**
         * Prepare $scope.calendarEvent to create the event and call the method that will display the calendar creation form
         * @param newItem the event information so far
         * @param isCalendar allows to use the lightbox from the calendar directive
         */
        $scope.createCalendarEvent = (newItem?, isCalendar?: boolean) => {
            $scope.calendarAsContribRight = new Array<Calendar>();
            $scope.selectedCalendarInEvent = new Array<Calendar>();
            $scope.calendarEvent = new CalendarEvent();
            $scope.calendarEvent.recurrence = {};
            $scope.calendarEvent.calendar = new Array<Calendar>();
            $scope.viewCalendarEvent($scope.calendarEvent);
            setListCalendarWithContribFilter();
            $scope.calendarAsContribRight.forEach((calendar : Calendar) => {
                calendar.toString = () => { return calendar.title };
            });
            safeApply($scope);

            if (isCalendar) {
                // dates
                if (model.calendar.newItem !== undefined) {
                    $scope.calendarEvent.startMoment = model.calendar.newItem.beginning;
                    $scope.calendarEvent.startMoment.minutes(0);
                    $scope.calendarEvent.endMoment = model.calendar.newItem.end;
                    $scope.calendarEvent.endMoment.minutes(0);
                } else {
                    $scope.calendarEvent.startMoment = moment();
                    $scope.calendarEvent.endMoment = moment();
                    $scope.calendarEvent.endMoment.hour(
                        $scope.calendarEvent.startMoment.hour() + 1
                    );
                }
                $scope.calendarEvent.startMoment.seconds(0);
                $scope.calendarEvent.endMoment.seconds(0);
            } else {
                if (newItem) {
                    $scope.calendarEvent.startMoment = newItem.beginning;
                    $scope.calendarEvent.startMoment = $scope.calendarEvent.startMoment.minute(0).second(0).millisecond(0);
                    $scope.calendarEvent.endMoment = newItem.end;
                    $scope.calendarEvent.endMoment = $scope.calendarEvent.endMoment.minute(0).second(0).millisecond(0);
                } else {
                    $scope.calendarEvent.startMoment = moment.utc().second(0).millisecond(0).add(utcTime($scope.calendarEvent.startMoment), 'hours');
                    $scope.calendarEvent.endMoment = moment.utc().second(0).millisecond(0).add(5 - utcTime($scope.calendarEvent.endMoment), 'hours');
                }
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
        const unique = function <T>(array: T[]) {
            return array.filter(function (value, index, self) {
                return self.indexOf(value) === index;
            });
        }
        /**
        *   Put the calendars that the user has the right to modify in calendarAsContribRight and tick the first in the list
        */
        const setListCalendarWithContribFilter = (): void => {
            $scope.calendars.arr.forEach(
                function (calendar) {
                    if ($scope.hasContribRight(calendar) != null && !externalCalendarUtils.isCalendarExternal(calendar)) {
                        $scope.calendarAsContribRight.push(calendar);
                    }
                });
            $scope.calendarAsContribRight = unique($scope.calendarAsContribRight);
            let defaultCalendar: Calendar = $scope.calendarAsContribRight.find(cal => cal.is_default == true);
            $scope.selectedCalendarInEvent.push(defaultCalendar ? defaultCalendar : $scope.calendarAsContribRight[0]);
            $scope.selectedCalendarInEvent = unique($scope.selectedCalendarInEvent);
        }

            /**
             *  Verify if there is a element tick in the multi-combo of calendars
             */
            $scope.isCalendarSelectedInEvent = (): boolean => {
                if ($scope.calendarEvent._id) {
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
                    function (selectedCalendars) {
                        $scope.calendars.arr.forEach(
                            function (calendar) {
                                if (selectedCalendars._id === calendar._id) {
                                    $scope.calendarEvent.calendar.push(calendar);
                                }
                            });
                    });
            }

            /**
             * Return the name of the smaller right
             * @param event the calendar event
             */
            $scope.smallerRightEvent = (event: CalendarEvent): string => {
                let right = "manage";
                event.calendar.filter(e => e != null).forEach(
                    function (calendar) {
                        if ($scope.hasContribRight(calendar)) {
                            if (!calendar.myRights.manage && right != "read") {
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
            let right: boolean = false;
            if (event && event.calendar && event.calendar.length > 0) {
                event.calendar.forEach(
                    function (calendar) {
                        if ($scope.hasContribRight(calendar)) {
                            if (calendar.myRights.manage || $scope.isMyEvent(event)) {
                                right = true;
                            }
                        }
                    }
                );
            }
            return right;
        }

            $scope.displayImportIcsPanel = function () {
                $scope.display.showImportPanel = true;
                $scope.display.importFileButtonDisabled = true;
                $scope.newFile.name = '';
            };

            $scope.setFilename = async () => {
                if ($scope.newFile && $scope.newFile.files && $scope.newFile.files.length > 0) {
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
                $scope.calendar = calendar;
                await $scope.calendar.importIcal($scope.jsonData);
                $scope.icsImport = $scope.calendar.icsImport;
                $scope.display.showImportPanel = undefined;
                $scope.display.showImportReport = true;
                $scope.importFileButtonDisabled = true;
                await $scope.calendar.calendarEvents.sync($scope.calendar, $scope.calendars);
                $scope.loadCalendarEvents();
                if ($scope.display.list) {
                    $scope.showList();
                } else {
                    $scope.showCalendar();
                }
            };

            $scope.verifyInputDates = function () {
                if ($scope.calendarEvent.title) {
                    $scope.calendarEvent.showDates = true;
                } else {
                    $scope.calendarEvent.showDetails = true;
                }
            };

            $scope.verifyInputRec = function () {
                if ($scope.calendarEvent.title) {
                    $scope.calendarEvent.showRecurrence = true;
                } else {
                    $scope.calendarEvent.showDetails = true;
                }
            };

            $scope.canICloseLightBox = function () {
                if ($scope.calendarEvent.title == undefined && $scope.calendarEvent.description == undefined && $scope.calendarEvent.location == undefined) {
                    return false;
                } else {
                    let toCompare = "";
                    if ($scope.calendarEvent.title != undefined) {
                        toCompare += $scope.calendarEvent.title;
                    }
                    if ($scope.calendarEvent.description != undefined) {
                        toCompare += $scope.calendarEvent.description;
                    }
                    if ($scope.calendarEvent.location != undefined) {
                        toCompare += $scope.calendarEvent.location;
                    }
                    if ($scope.contentToWatch != toCompare) {
                        return !confirm(lang.translate("calendar.navigation.guard"));
                    } else {
                        return false;
                    }
                }
            };


            $scope.saveCalendarEventEdit = async (calendarEvent = $scope.calendarEvent, event ?, shareOption ?: boolean) => {

                const recurrenceItemsMinimumLength: number = 3;
                /** indicates if the event is being created, in which case its id does not exist yet*/
                const isEventCreated: boolean = !calendarEvent._id;

                async function doItemCalendarEvent(items, count) {
                    /**
                     * Resets elements before closing event saving
                     */
                    function endRecurrenceSave() : void {
                        calendarEvent.noMoreRecurrent = calendarEvent.noMoreRecurrent && false;
                        calendarEvent.noMoreRecurrence = calendarEvent.noMoreRecurrence && false;
                        calendarEvent.detailToRecurrence = calendarEvent.detailToRecurrence && false;
                        calendarEvent.startDateToRecurrence = calendarEvent.startDateToRecurrence && false;
                        calendarEvent.endDateToRecurrence = calendarEvent.endDateToRecurrence && false;
                        $scope.closeCalendarEvent();
                        $scope.calendarEvents.applyFilters();
                        $scope.display.calendar = true;
                        if (shareOption) {
                            if (calendarEvent.isRecurrent && !calendarEvent.created) {
                                if (!$scope.display.showEditEventPanel) {
                                    $scope.shareEvent($scope.recurrentCalendarEventToShare ? $scope.recurrentCalendarEventToShare : {}, event);
                                }
                            } else {
                                $scope.shareEvent($scope.calendarEvent, event);
                            }
                        }
                    }

                    if (!$scope.calendarEvent.isRecurrent || calendarEvent._id || items.length >= recurrenceItemsMinimumLength
                        || !$scope.isDateValid() || !$scope.areRecurrenceAndEventLengthsCompatible()) {
                        if (items.length === count) {
                            endRecurrenceSave();
                        } else {
                            items[count].calEvent.owner = {
                                userId: model.me.userId,
                                displayName: model.me.username
                            };
                            let itemCalendarEvent: any = items[count].calEvent;
                            let action: string = items[count].action;
                            if (action === ACTIONS.save || action === ACTIONS.saveAll) {
                                if (itemCalendarEvent.isRecurrent && count !== 0) {
                                    var parentId: string = items[0].calEvent._id;
                                    if (items[0].calEvent.parentId) {
                                        parentId = items[0].calEvent.parentId;
                                    }
                                    if ($scope.recurrentCalendarEventToShare === null) {
                                        $scope.recurrentCalendarEventToShare = items[count].calEvent;
                                    }
                                    itemCalendarEvent.parentId = parentId;
                                }
                                if (!itemCalendarEvent.created && $scope.sendNotif === false) {
                                    itemCalendarEvent.sendNotif = $scope.sendNotif;
                                }
                                if (isEventCreated && items.length === recurrenceItemsMinimumLength
                                    && ($scope.isOneDayEvent() || itemCalendarEvent.isRecurrent)) {
                                    itemCalendarEvent.isRecurrent = false;
                                    itemCalendarEvent.recurrence = false;
                                    itemCalendarEvent.parentId = false;
                                    itemCalendarEvent.index = 0;
                                }
                                itemCalendarEvent[action === ACTIONS.saveAll? 'saveAll': 'save']()
                                    .then(() => {
                                        items[count].calEvent._id = itemCalendarEvent._id;
                                        count++;
                                        doItemCalendarEvent(items, count);
                                    })
                                    .catch((e) => {
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
                    } else {
                        toasts.warning(lang.translate('calendar.error.date.saving'));
                        endRecurrenceSave();
                    }

                }
                $scope.recurrentCalendarEventToShare = null;
                $scope.rbsEmitter.prepareBookingToSave(calendarEvent);

                $scope.createContentToWatch();
                let items = [];
                calendarEvent.startMoment = moment(calendarEvent.startMoment).seconds(0).milliseconds(0);
                calendarEvent.endMoment = moment(calendarEvent.endMoment).seconds(0).milliseconds(0);
                $scope.display.calendar = false;
                let hasExistingRecurrence : boolean = false;
                if(calendarEvent.isMultiDayPart && calendarEvent.editAllRecurrence) $scope.restoreMultiDaysEvents();
                let recurrentCalendarEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);

                if (recurrentCalendarEvents.length > 1) {
                    hasExistingRecurrence = true;
                }
                if (calendarEvent.isRecurrent && !calendarEvent.parentId) {
                    calendarEvent.recurrence.start_on = moment(calendarEvent.startMoment).hours(0).minutes(0).seconds(0).milliseconds(0);
                    if (calendarEvent.recurrence.end_on) {
                        calendarEvent.recurrence.end_on = moment(calendarEvent.recurrence.end_on).hours(0).minutes(0).seconds(0).milliseconds(0);
                    }
                }
                if (calendarEvent.noMoreRecurrent || calendarEvent.noMoreRecurrence) {
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

                let parentAction: string = ACTIONS.create;
                if (calendarEvent._id) {
                    parentAction = ACTIONS.update;
                }
                let item = {'calEvent': calendarEvent, 'action': (calendarEvent.editAllRecurrence? ACTIONS.saveAll : ACTIONS.save)};
                items.push(item);
                if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
                    Array.prototype.push.apply(items, $scope.handleRecurrence(calendarEvent));
                }
                if (calendarEvent.isRecurrent && !calendarEvent.parentId) {
                    let item = {'calEvent': calendarEvent, 'action': ACTIONS.delete};
                    if (parentAction === ACTIONS.create) {
                        items.push(item);
                    } else if (items.length > 1) {
                        items.push(item);
                    } else {
                        calendarEvent.parentId = calendarEvent._id;
                        item.action = ACTIONS.save;
                        items.push(item);
                    }
                }
                if (calendarEvent.noMoreRecurrent && hasExistingRecurrence && calendarEvent.noMoreRecurrence) {
                    recurrentCalendarEvents.forEach(function (cle) {
                        if (cle._id !== calendarEvent._id) {
                            let item = {'calEvent': cle, 'action': ACTIONS.delete};
                            items.push(item);
                        }
                    });
                }
                await doItemCalendarEvent(items, 0);
            };

            $scope.cancelEventEdit = function(): void {
                $scope.display.showEditEventPanel = undefined;
            };

            $scope.cancelRecurrentEventEdit = (): void => {
                $scope.display.showRecurrencePanel = undefined;
            }

            $scope.switchSelectAllCalendarEvents = function () {
                if ($scope.display.selectAllCalendarEvents) {
                    $scope.calendarEvents.filtered.forEach(function (calendarEvent) {
                        if ($scope.smallerRightEvent(calendarEvent) == "manage") {
                            calendarEvent.selected = true;
                        }
                    });
                } else {
                    $scope.calendarEvents.deselectAll();
                }
            };

            $scope.switchFilterListByDates = function () {
                $scope.calendarEvents.filters.startMoment = moment($scope.calendarEvents.filters.startMoment);
                $scope.calendarEvents.filters.endMoment = moment($scope.calendarEvents.filters.endMoment);
                $scope.calendarEvents.applyFilters();
                $scope.calendarEvents.filtered = $scope.removeDuplicateCalendarEvent($scope.calendarEvents.filtered);
                $scope.$apply();
            };

            $scope.closeRecurrence = function () {
                $scope.display.calendarEventRecurrence = false;
            };

            $scope.nextWeekBookingButton = function () {
                var nextStart = moment(model.calendarEvents.filters.startMoment).add(7, 'day');
                var nextEnd = moment(model.calendarEvents.filters.endMoment).add(7, 'day');
                updateCalendarList(nextStart, nextEnd);
            };

            $scope.previousWeekBookingButton = function () {
                var prevStart = moment(model.calendarEvents.filters.startMoment).subtract(7, 'day');
                var prevEnd = moment(model.calendarEvents.filters.endMoment).subtract(7, 'day');
                updateCalendarList(prevStart, prevEnd);
            };

            $scope.switchCalendarEventTab = function (tab) {
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

            /**
             * Returns true if the event edition form is in the right format:
             * areFieldsInCommonValid is true, it has a title, at least one calendar selected
             * and the event end time is after the event start time.
             * Depending on the case, more conditions can apply.
             * @param actionButton the action of the button calling the method
             */
            $scope.isEventFormValid = (calendarEvent, actionButton: ActionButtonType): boolean => {
                if (!calendarEvent) {
                    return false;
                }

                $scope.eventForm = angular.element(document.getElementById("event-form")).scope();
                /** Ensures that the fields of the form are correctly filled*/
                let areFieldsInCommonValid: boolean = ($scope.rbsEmitter.checkBookingValidAndSendInfoToSniplet() && !$scope.eventForm.editEvent.$invalid && $scope.isCalendarSelectedInEvent()
                    && $scope.isTimeValid() && $scope.isDateValid() &&  $scope.areRecurrenceAndEventLengthsCompatible()
                    && !$scope.isStartDateTooOld() && !$scope.isEndDateTooFar() && $scope.isValidRecurrentEndDate())
                    && (!ENABLE_REMINDER || $scope.isEventReminderValid());

                switch (actionButton) {
                    case ACTIONS.save:
                        /** Recurrent event cannot be saved if the "Delete other events from recurrence" checkbox is checked*/
                        return (areFieldsInCommonValid);
                    case ACTIONS.share:
                        /** Recurrent event can only be shared if the "Remove this event from recurrence" checkbox is checked*/
                        return (areFieldsInCommonValid && !(calendarEvent.isRecurrent && !calendarEvent.noMoreRecurrent)
                            && !calendarEvent.editAllRecurrence);
                    case ACTIONS.delete:
                        /** Recurrent event can only be deleted in one of these cases:
                         * "Edit this event only" and then check "Remove this event from recurrence"
                         * "Edit all occurrences of this recurrence"
                         */
                        return (!calendarEvent.isRecurrent || calendarEvent.editAllRecurrence || (calendarEvent.noMoreRecurrent
                            && !calendarEvent.noMoreRecurrence));
                    default:
                        return false;
                }
            }

            /**
             * Returns true if the event start time is before the event end time or if the event lasts all day
             * or if the event lasts multiple days
             */
            $scope.isTimeValid = (): boolean => (($scope.calendarEvent.startTime && $scope.calendarEvent.endTime
                    && moment($scope.calendarEvent.startTime).isBefore(moment($scope.calendarEvent.endTime)))
                || $scope.calendarEvent.allday || !$scope.isOneDayEvent());

            /**
             * Returns true if the event start date is before or equal to the event end date
             */
            $scope.isDateValid = (): boolean => ($scope.calendarEvent.startMoment && $scope.calendarEvent.endMoment
                && moment($scope.calendarEvent.startMoment).isValid() && moment($scope.calendarEvent.endMoment).isValid()
                && moment($scope.calendarEvent.startMoment).isSameOrBefore(moment($scope.calendarEvent.endMoment), 'day'));

            /**
             * Returns true if startMoment is older than january 1/2000
             */
            $scope.isStartDateTooOld = (): boolean => ($scope.calendarEvent.startMoment < $scope.minDate);

            /**
             * Returns true if endMoment is after 80 years further
             */
            $scope.isEndDateTooFar = (): boolean => $scope.calendarEvent.endMoment > moment($scope.calendarEvent.startMoment).add(maxEndMomentDate, 'years');

            /**
             * Check the end date of recurrence
             * no compute if recurrence type == every_days because the maximum number of years to be reached is 9
             */
            $scope.isValidRecurrentEndDate = (): boolean => {
                const { end_type, end_on, type, every, end_after } = $scope.calendarEvent.recurrence;
                if (end_type === 'on') {
                    const endOnMoment = moment(end_on);
                    return endOnMoment <= moment($scope.calendarEvent.startMoment).add(maxEndMomentDate, 'years')
                        && endOnMoment >= $scope.minDate
                        && endOnMoment > $scope.calendarEvent.endMoment;
                } else if (end_type === 'after' && type === 'every_week') {
                    let weeksToAdd: number = end_after * every;
                    let maxEventEndDate : Date = moment($scope.calendarEvent.startMoment).add(maxEndMomentDate, 'years');
                    let recurrentEndDate : Date = moment($scope.calendarEvent.startMoment).add(weeksToAdd, 'weeks');
                    return recurrentEndDate < maxEventEndDate;
                }
                return true;
            }

            /**
             * Returns true if the event length is shorter than the recurrence length
             */
            $scope.areRecurrenceAndEventLengthsCompatible = (): boolean => ($scope.isOneDayEvent() || !$scope.calendarEvent.isRecurrent
                || ($scope.calendarEvent.recurrence.type == 'every_week'
                    && (moment($scope.calendarEvent.endMoment)
                        .diff(moment($scope.calendarEvent.startMoment), 'days') + 1 <= $scope.calendarEvent.recurrence.every * 7)));

            /**
             * Returns true is the start and end date of the event are the same day or if one of them is not valid
             * Events with invalid dates are treated like one day events
             */
            $scope.isOneDayEvent = (): boolean => (!(moment($scope.calendarEvent.startMoment).isValid())
                || !(moment($scope.calendarEvent.endMoment).isValid())
                || (moment($scope.calendarEvent.startMoment).isSame(moment($scope.calendarEvent.endMoment), 'day')));

            /**
             * Returns the date of the last day of the recurrence
             * @param item an event of the recurrent
             */
            $scope.getEndOfRecurrence = (item: CalendarEvent): String => {
                let recurrenceEndDate: Moment = moment(item.endMoment);
                $scope.calendarEvents.filtered.filter((event: CalendarEvent) => (event.parentId == item.parentId))
                    .forEach((recurrenceEvent: CalendarEvent) => {
                        if (DateUtils.isDateAfter(moment(recurrenceEvent.endMoment), recurrenceEndDate)) {
                            recurrenceEndDate = moment(recurrenceEvent.endMoment);
                        }
                    });
                return recurrenceEndDate.format(FORMAT.displayFRDate);
            }

            /**
             * Returns the day of the week corresponding to the given number
             * @param dayNumber number, the number of the day of the week (0 = sunday, 1 = monday ...)
             */
            $scope.getDayName = (dayNumber: DAY_OF_WEEK): String => {
                return lang.translate(recurrence.fullDayMap[dayNumber]);
            };


            /**
             * Returns a string of the days in which the weekly recurrence takes place
             * @param item CalendarEvent, an event of the recurrence
             */
            $scope.getRecurrenceDays = (item: CalendarEvent): String => {
                let recurrenceDaysList: String = "";
                //add selected days to day list
                Object.keys((<CalendarEventRecurrence>item.recurrence).week_days).forEach((key: string) => {
                    if (((<CalendarEventRecurrence>item.recurrence).week_days[key])) {
                        recurrenceDaysList = recurrenceDaysList.concat($scope.getDayName(Number(key)), ", ");
                    }
                });

                return recurrenceDaysList;
            };

            /**
             * Returns the name of the day of the week for the start or end date of an event.
             * By default it returns the day of the start of the event.
             * @param event CalendarEvent, the event we want the day of
             * @param isMultiDayEvent boolean, if the event is multiple day or not
             * @param isStartOrEnd whether we want the start or end date of the event
             */
            $scope.getDayOfWeek = (event: CalendarEvent, isMultiDayEvent: boolean, isStartOrEnd: "start" | "end"): String => {
                let targetDay: Moment;
                switch (isStartOrEnd) {
                    case "start":
                        targetDay = isMultiDayEvent ?
                            $scope.calendarEvents.multiDaysEvents.find((e: CalendarEvent) => e._id == event._id).startMoment
                            : event.startMoment;
                        return $scope.getDayName(moment(targetDay).day());
                    case "end":
                        targetDay = isMultiDayEvent ?
                            $scope.calendarEvents.multiDaysEvents.find((e: CalendarEvent) => e._id == event._id).endMoment
                            : event.endMoment;
                        return $scope.getDayName(moment(targetDay).day());
                    default:
                        return $scope.getDayName(moment(event.startMoment).day());
                }
            };

            $scope.openAttachmentLightbox = (): void => {
                $scope.display.attachmentLightbox = true;
            };

            /**
             * Adds attachments to document and closes media-library lightbox
             */
            $scope.updateDocument = (): void => {
                $scope.eventDocuments = angular.element(document.getElementsByTagName("media-library")).scope();
                $scope.calendarEvent.attachments = $scope.calendarEvent.attachments ? $scope.calendarEvent.attachments : [];
                if ($scope.eventDocuments.documents) {
                    $scope.calendarEvent.attachments = [...$scope.calendarEvent.attachments, ...$scope.eventDocuments.documents];
                }
                $scope.display.attachmentLightbox = false;
            };

            $scope.removeDocumentFromAttachments = (documentId: String): void => {
                let removedDocument: Document = $scope.calendarEvent.attachments.find((doc: Document) => doc._id == documentId);
                $scope.calendarEvent.attachments.splice($scope.calendarEvent.attachments.indexOf(removedDocument), 1);
            };

            $scope.downloadAttachment = async (calendarEvent: CalendarEvent, attachment: Document): Promise<void> => {
                let isUserAttachmentOwner: boolean = attachment.owner.userId != model.me.userId;
                attachmentService.downloadAttachment(calendarEvent._id, attachment._id, isUserAttachmentOwner);
            };

            $scope.noDeleteOptionChosen = (): boolean => {
                if($scope.calendarEvent.bookings && $scope.calendarEvent.bookings.length > 0) {
                    let bookingDeletionInfo: IScope = angular.element(document.getElementById("booking-deletion-message")).scope();
                    return (bookingDeletionInfo && bookingDeletionInfo['vm'] && bookingDeletionInfo['vm'].canDeleteBooking()
                        && ($scope.calendarEvent.deleteAllBookings == undefined));
                }
                return false;
            };

            $scope.checkExternalCalendarRight = (right: string): boolean => {
                return externalCalendarUtils.checkExternalCalendarRight(right);
            };

            $scope.getDescriptionHTML = (description: string): string => {
                return !!description ? $sce.trustAsHtml(description) : null;
            }

            $scope.isEventReminderValid = (): boolean => {
                //one reminder type + one reminder frequency selected OR nothing selected
                return ((!!$scope.calendarEvent.reminders?.reminderType?.timeline || !!$scope.calendarEvent.reminders?.reminderType?.email)
                    && (!!$scope.calendarEvent.reminders?.reminderFrequency?.hour
                        || !!$scope.calendarEvent.reminders?.reminderFrequency?.day
                        || !!$scope.calendarEvent.reminders?.reminderFrequency?.week
                        || !!$scope.calendarEvent.reminders?.reminderFrequency?.month ))
                    || $scope.isEventReminderFormEmpty();
            }

            $scope.isEventReminderFormEmpty = (): boolean => {
                // nothing selected
                return(!$scope.calendarEvent.reminders?.reminderType?.timeline
                        && !$scope.calendarEvent.reminders?.reminderType?.email
                        && !$scope.calendarEvent.reminders?.reminderFrequency?.hour
                        && !$scope.calendarEvent.reminders?.reminderFrequency?.day
                        && !$scope.calendarEvent.reminders?.reminderFrequency?.week
                        && !$scope.calendarEvent.reminders?.reminderFrequency?.month);
            }

            $scope.saveCalendarEventReminder = async (): Promise<void> => {
                let calendarEventReminders: CalendarEventReminder = new CalendarEventReminder($scope.calendarEvent.reminders);
                calendarEventReminders.eventId = $scope.calendarEvent._id;

                if ($scope.isEventReminderFormEmpty() && !!calendarEventReminders?.id) {
                    await reminderService.deleteReminder($scope.calendarEvent._id, calendarEventReminders.id);
                } else if ($scope.isEventReminderValid(calendarEventReminders) && !!calendarEventReminders?.id) {
                    await reminderService.updateCalendarEventReminder($scope.calendarEvent._id, calendarEventReminders, calendarEventReminders.id);
                } else if ($scope.isEventReminderValid(calendarEventReminders) && !calendarEventReminders?.id) {
                    await reminderService.createCalendarEventReminder($scope.calendarEvent._id, calendarEventReminders);
                }

                $scope.closeCalendarEvent();
            }


            var updateCalendarList = function (start, end) {
                model.calendarEvents.filters.startMoment.date(start.date());
                model.calendarEvents.filters.startMoment.month(start.month());
                model.calendarEvents.filters.startMoment.year(start.year());
                model.calendarEvents.filters.endMoment.date(end.date());
                model.calendarEvents.filters.endMoment.month(end.month());
                model.calendarEvents.filters.endMoment.year(end.year());
                $scope.calendarEvents.applyFilters();

            };

            $scope.$watch(
                function () {
                    return $('.hiddendatepickerform')[0]
                        ? $('.hiddendatepickerform')[0].value
                        : '';
                },
                function (newVal, oldVal) {
                    if (newVal !== oldVal && !$scope.display.list &&
                        newVal &&
                        newVal !== '') {
                        updateChangeDateCalendarSchedule(newVal.startOf('isoweek'));
                    }
                }
            );

            /**
             * Sync elements when startMoment and endMoment are changed in the list view
             */
            $scope.$watchGroup(['calendarEvents.filters.startMoment','calendarEvents.filters.endMoment'], async (newValues, oldValues) => {
                newValues[0] = moment(newValues[0]);
                newValues[1] = moment(newValues[1]);
                await $scope.calendars.syncSelectedCalendarEvents(newValues[0].format(FORMAT.formattedDate), newValues[1].format(FORMAT.formattedDate));
                $scope.loadCalendarEvents();
                template.open('calendar', 'events-list');
            });


            const updateChangeDateCalendarSchedule = async function (newDate): Promise<void> {
                model.calendar.firstDay.date(newDate.date());
                model.calendar.firstDay.month(newDate.month());
                model.calendar.firstDay.year(newDate.year());
                await $scope.syncSelectedCalendars();
                template.open('calendar', 'read-calendar');
            };
        }]);