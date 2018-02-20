routes.define(function($routeProvider) {
    $routeProvider.when('/view/:calendarId', {
        action : 'goToCalendar'
    }).otherwise({
        action : 'mainPage'
    });
});

function CalendarController($scope, template, model, lang, date, route, $timeout, $q) {
	
    this.initialize = function() {
        $scope._ = _;
        $scope.lang = lang;
        $scope.template = template;
        $scope.display = {};
        $scope.display.list = false;
        $scope.display.calendar = false;
        $scope.display.editEventRight = false;
        $scope.model = model;
        $scope.me = model.me;
        $scope.date = date;
        $scope.calendarEvent = new CalendarEvent();
        $scope.initEventDates(moment().utc().second(0).millisecond(0), moment().utc().second(0).millisecond(0).add(1, 'hours'));
        $scope.calendars = model.calendars;
        $scope.display.importFileButtonDisabled = true;
        $scope.calendarEvents = model.calendarEvents;
        $scope.periods = model.periods;
        $scope.newFile = {};
        $scope.propertyName = 'startMoment';
        $scope.reverse = true;

        model.calendarEvents.filters.startMoment = moment().startOf('day');
        model.calendarEvents.filters.endMoment = moment().add(2, 'months').startOf('day');

        template.open('main', 'main-view');
        template.open('top-menu', 'top-menu');

    };

    function disableImportFileButton() {
        $scope.$apply(function() {
            $scope.display.importFileButtonDisabled = false;
        });
    }

    $scope.someSelectedValue = function(selection) {
        return Object.keys(selection).map(function(val, idx, arr) { return selection[val]; }).some(function(val, idx, arr) { return val === true;});
    };

    $scope.changeEndMoment = function() {

        if ($scope.calendarEvent.startMoment.isAfter($scope.calendarEvent.endMoment)) {
            $scope.calendarEvent.endMoment = moment($scope.calendarEvent.startMoment).add(1, 'hours');
        }
        
    };

    $scope.changeStartMoment = function() {

        if($scope.calendarEvent.isRecurrent){
            $scope.calendarEvent.endMoment.years($scope.calendarEvent.startMoment.years()).months($scope.calendarEvent.startMoment.months()).days($scope.calendarEvent.startMoment.days());
        }

       
        if ($scope.calendarEvent.startMoment.isAfter($scope.calendarEvent.endMoment)) {
            $scope.calendarEvent.startMoment = moment($scope.calendarEvent.endMoment).subtract(1, 'hours');
        }
        
    };

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
            for (i = 0; i < calendarEvent.recurrence.end_after; i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.index = i;
                var toAdd = (i + 0) * parseInt(calendarEvent.recurrence.every);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(toAdd, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(toAdd, 'days');
                var item = {'calEvent': calendarRecurrentEvent, 'action': 'save'};
                list.push(item);
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
            var endOnMoment = moment(calendarEvent.recurrence.end_on);
            var startMoment = calendarEvent.startMoment;

            for (i =0; startMoment.isBefore(endOnMoment); i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                calendarRecurrentEvent.index = i;
                var toAdd = (i + 0) * parseInt(calendarEvent.recurrence.every);
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

        var weekDays = Object.keys(calendarEvent.recurrence.week_days).filter(function(val, idx, arr){ if (calendarEvent.recurrence.week_days[val]) return val;});
        var dayJump = 7 * calendarEvent.recurrence.every;
        var startDay = calendarEvent.recurrence.start_on.isoWeekday();
        var startOn = moment(calendarEvent.recurrence.start_on);
        var startHour = calendarEvent.startMoment.hours();
        var startMinute = calendarEvent.startMoment.minutes();
        var duration = moment(calendarEvent.endMoment).seconds(0).milliseconds(0).diff(moment(calendarEvent.startMoment).seconds(0).milliseconds(0), 'minutes');
        var recurrenceDays = weekDays.filter(function(val, idx, arr) { if (val >= startDay) return val;});
        if (recurrenceDays.length == 0) {
            startOn.isoWeekday(1).add(dayJump, 'days');
        } else {
            startOn.isoWeekday(recurrenceDays[0]);
        }
        var endOn = moment(startOn);
        var list = [];
        if (calendarEvent.recurrence.end_type == 'after') {
            while (recurrenceDays.length < calendarEvent.recurrence.end_after) {
                recurrenceDays = recurrenceDays.concat(weekDays);
            }
            if (recurrenceDays.length > calendarEvent.recurrence.end_after) {
                recurrenceDays = recurrenceDays.slice(0, calendarEvent.recurrence.end_after);
            }
            var previousDay = recurrenceDays[0];
            recurrenceDays.forEach(function(day, idx, arr) {
                if (day <= previousDay && idx > 0) {
                    endOn.isoWeekday(1).add(dayJump, 'days');
                }
                endOn.isoWeekday(day);
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
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
                var previousDay = recurrenceDays[0];
                recurrenceDays.every(function(day, idx, arr) {
                    if (day <= previousDay && idx > 0) {
                        endOn.isoWeekday(1).add(dayJump, 'days');
                    }
                    endOn.isoWeekday(day);
                    if (calendarEvent.recurrence.end_on.diff(endOn, 'days') >= 0) {
                        calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
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
            for (i = 0; i < calendarEvent.recurrence.end_after; i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);

                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(i + 1, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(i + 1, 'days');

                $scope.saveCalendarEventEdit(calendarRecurrentEvent);
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
            
        }
    };

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
        return child;
    };

    // Definition of actions
    route({
        goToCalendar : function(params) {
            $scope.calendars.one('sync', function() {
               var calendar = model.calendars.find(function(cl) {
                    return cl._id === params.calendarId;
                });
                if (calendar === undefined) {
                    $scope.notFound = true;

                    template.open('error', '404');
                } else {
                    $scope.notFound = false;
                    $scope.openOrCloseCalendar(calendar, true);
                }
            });
        },
        mainPage : function(params) {
            $scope.calendars.one('sync', function(){
                $scope.loadCalendarPreferences(function() {
                    $scope.loadSelectedCalendars();
                });
            });
        }
    });

    $scope.firstOwnedCalendar = function() {
        return _.find($scope.calendars.all, function(calendar) {
            return $scope.isMyCalendar(calendar);
        });
    };

    $scope.loadSelectedCalendars = function() {
        if ($scope.calendarPreferences.selectedCalendars) {
            var toSelectCalendars = _.filter($scope.calendars.all, function(calendar) {
                return _.contains($scope.calendarPreferences.selectedCalendars, calendar._id);
            });
            toSelectCalendars.forEach(function(cl) {
               $scope.openOrCloseCalendar(cl, false);
            });
        }
        
        if ($scope.calendars.selection().length === 0 && !$scope.calendars.isEmpty()) {
            var calendarToOpen = $scope.firstOwnedCalendar();
            if (calendarToOpen === undefined) {
                calendarToOpen = $scope.calendars.all[0];
            }
            $scope.openOrCloseCalendar(calendarToOpen, true);
        }
    };

    $scope.loadCalendarPreferences = function(callback) {
       
        http().get('/userbook/preference/calendar').done(function(calendarPreferences){
            if (!calendarPreferences || !calendarPreferences.preference) {
                $scope.calendarPreferences = {};
            } else {
                $scope.calendarPreferences = JSON.parse(calendarPreferences.preference);
            }
            if (typeof callback === 'function') {
                callback();
            }
        });
    };

    $scope.saveCalendarPreferences = function() {
        http().putJson('/userbook/preference/calendar', $scope.calendarPreferences);
    };

    $scope.showList = function() {
        $scope.display.list = true;
        $scope.display.calendar = false;
        $scope.display.propertyName = 'startMoment';
        $scope.reverse = false;
        $scope.calendarEvents.applyFilters();
        template.open('calendar', 'events-list');
    };

    $scope.sortBy = function(propertyName) {
        $scope.reverse = ($scope.propertyName === propertyName) ? !$scope.reverse : false;
        $scope.propertyName = propertyName;
    };

    $scope.showCalendar = function() {
        $scope.display.list = false;
        $scope.display.calendar = true;
        template.open('calendar', 'read-calendar');
    };

    $scope.ownCalendars = function() {
        var ownCalendars =  $scope.calendars.some(function(calendar) {
            if ($scope.isMyCalendar(calendar)) {
                return true;
            }
            return false;
        });
        return ownCalendars;
    };

    $scope.isMyCalendar = function(calendar) {
        return calendar.owner.userId == $scope.me.userId;
    };

    $scope.isCalendarSharedWithMe = function(calendar) {
        return calendar.shared && calendar.owner.userId != $scope.me.userId;
    };

    $scope.hasSharedCalendars = function() {
        var hasSharedCalendars = $scope.calendars.some(function(calendar) {
            if ($scope.isCalendarSharedWithMe(calendar)) {
                return true;
            }
            return false;
        });
        return hasSharedCalendars;
    };

    $scope.initEventDates = function(startMoment, endMoment) {
        var event = $scope.calendarEvent;

        // hours minutes management
        var minTime = moment(startMoment);
        minTime.set('hour', model.timeConfig.start_hour);
        var maxTime = moment(endMoment);
        maxTime.set('hour', model.timeConfig.end_hour);
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

    $scope.openOrCloseCalendar = function(calendar, savePreferences) {
        if ($scope.calendars.selection().length > 1 || !calendar.selected) {
            $scope.display.calendar = false;
            calendar.selected = !calendar.selected;
            calendar.open(function(){
                if (calendar.selected) {
                    $scope.calendar = calendar;
                } 
                $scope.display.editEventRight = $scope.hasContribRight();
                $scope.refreshCalendarEventItems();

                $scope.calendarEvents.applyFilters();
                if (!$scope.display.list && !$scope.display.calendar) {
                    $scope.showCalendar();
                    //template.open('calendar', 'read-calendar');
                    //$scope.display.calendar = true;
                } 
            });
            if (savePreferences) {
                $scope.calendarPreferences.selectedCalendars = _.map($scope.calendars.selection(), function(calendar) {
                    return calendar._id;
                });
                $scope.saveCalendarPreferences();
            }
        }
    };

    $scope.refreshCalendarEventItems = function(calendar) {
        if (calendar) {
            $scope.calendarEvents.removeCalendarEvents(calendar);
            $scope.calendarEvents.pushAll(calendar.calendarEvents.all);
        } else {
            $scope.calendarEvents.clear(true);
            $scope.calendars.selection().forEach(function(cl) {
                $scope.calendarEvents.pushAll(cl.calendarEvents.all);
            });
        }
        $scope.calendarEvents.applyFilters();
    };

    $scope.hideOtherCalendarCheckboxes = function(calendar) {
        $scope.showButtonsCalendar = calendar;
        $scope.calendars.forEach(function(item) {
            if (item._id != calendar._id) {
                item.showButtons = false;
            }
        });
        $scope.display.showToggleButtons = calendar.showButtons;
    };

   
    $scope.openMainPage = function(){
        delete $scope.calendar;
        $scope.calendars.forEach(function(cl) {
            cl.showButtons = false;
        });
        template.close('main');
    };

    $scope.newCalendar = function() {
        $scope.calendar = new Calendar();
        $scope.calendar.color = model.defaultColor;
        template.open('calendar', 'edit-calendar');
    };

    $scope.viewCalendarEvent = function(calendarEvent) {
        $scope.calendarEvent = calendarEvent;
        $scope.calendarEvent.showDetails = true;
        if (!$scope.calendarEvent.parentId) {
            if (!$scope.calendarEvent.recurrence) {
                $scope.calendarEvent.recurrence = {};
                //$scope.calendarEvent.recurrence.week_days = $scope.model.recurrence.week_days;
                $scope.calendarEvent.recurrence.week_days = model.recurrence.week_days;
            }
        }

        if ($scope.hasContribRight(calendarEvent.calendar)){
            template.open('lightbox', 'edit-event');
        } else {
            template.open('lightbox', 'view-event');
        }

        $scope.display.showEventPanel = true;
    };

    $scope.closeCalendarEvent = function(calendarEvent) {

        template.close('lightbox');
        $scope.showCalendarEventTimePicker = false;
        $scope.display.showEventPanel = false;
    };

     $scope.confirmRemoveCalendarEvent = function(calendarEvent, event){
        $scope.calendar.calendarEvents.deselectAll();
        calendarEvent.selected = true;
        if (!$scope.calendarEvent.parentId) {
            $scope.calendarEvents.getRecurrenceEvents(calendarEvent).forEach(function(calEvent) {
                calEvent.selected = true;
            });
        }
        $scope.display.confirmDeleteCalendarEvent = true;
        event.stopPropagation();
    };

    $scope.confirmRemoveCalendarEvents = function(event){
        $scope.display.confirmDeleteCalendarEvent = true;
        $scope.calendarEvents.selection().forEach(function(calendarEvent) {
            $scope.calendarEvents.getRecurrenceEvents(calendarEvent).forEach(function(calEvent) {
                calEvent.selected = true;
            });
        });
        event.stopPropagation();
    };

    $scope.removeCalendarEvents = function(event) {
        var count = $scope.calendarEvents.selection().length;
        var countReccurent = 0;
        $scope.calendarEvents.selection().forEach(function(calendarEvent) {
            if(calendarEvent.detailToRecurrence === true && calendarEvent.parentId !== undefined){
                var recurrentEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);
                countReccurent = recurrentEvents.length;
                count--;
                recurrentEvents.forEach(function(calEventRecurrent) {
                    calEventRecurrent.delete(function(){
                        countReccurent--;
                        $scope.resetCalendarAfterRemoveEvent(count,countReccurent,calendarEvent);
                    });
                });
            }else {
                calendarEvent.delete(function(){
                    count--;
                    $scope.resetCalendarAfterRemoveEvent(count,countReccurent,calendarEvent);
                });
            }
        });
    };

    $scope.resetCalendarAfterRemoveEvent = function(count,countRecurrent,calendarEvent) {
        if (count === 0 && countRecurrent === 0) {
            calendarEvent.calendar.calendarEvents.sync(function () {
                $scope.display.confirmDeleteCalendarEvent = undefined;
                $scope.closeCalendarEvent();
                $scope.refreshCalendarEventItems();
                $scope.calendarEvents.applyFilters();
                if ($scope.display.list && $scope.display.selectAllCalendarEvents) {
                    $scope.display.selectAllCalendarEvents = undefined;
                }
            });
        }
    };

    $scope.cancelRemoveCalendarEvent = function() {
        $scope.display.confirmDeleteCalendarEvent = undefined;
        $scope.calendarEvents.forEach(function(calendarEvent) {
            calendarEvent.selected = false;
        });
    };

    $scope.editCalendar = function(calendar, event) {
        $scope.calendar = calendar;
        event.stopPropagation();
        template.open('calendar', 'edit-calendar');
    };

    $scope.saveCalendarEdit = function() {
        if ($scope.calendar._id) {
            $scope.calendar.save(function(){
                $scope.calendar.calendarEvents.sync(function() {
                    $scope.refreshCalendarEventItems($scope.calendar);
                    template.open('calendar', 'read-calendar');
                });
            });
        }
        else { 
            $scope.calendar.save(function(){
                $scope.calendars.sync(function() {
                    $scope.loadCalendarPreferences(function() {
                        $scope.loadSelectedCalendars();
                    });
                });
            });
        }
        template.close('calendar');
    };

    $scope.hasContribRight = function(calendar) {
        var contribRight = false;
        if (calendar) {
           contribRight = calendar.myRights.contrib;
        } else {
             var contribRight = $scope.calendars.selection().some(function(cl) {
                if (cl.myRights.contrib) {
                   return true;
                }
            });
        }
        return contribRight;
    };

    $scope.hasReadRight = function(calendar) {
        var readRight = false;
        if (calendar) {
            readRight = calendar.myRights.contrib;
        } else {
            var readRight = $scope.calendars.selection().some(function(cl) {
                if (cl.myRights.contrib) {
                    return true;
                }
            });
        }
        return contribRight;
    };

    $scope.cancelCalendarEdit = function() {
        $scope.calendar = undefined;
        /* $scope.calendars.forEach(function(cl) {
            cl.showButtons = false;                
        }); */
        template.open('calendar', 'read-calendar');
    };

    $scope.confirmRemoveCalendar = function(calendar, event){
        $scope.display.confirmDeleteCalendar = true;
        $scope.calendar = calendar;
        event.stopPropagation();
    };

    $scope.removeCalendar = function() {
        // remove toggle buttons
        if ($scope.showButtonsCalendar._id == $scope.calendar._id) {
            $scope.showButtonsCalendar = undefined;
            $scope.showToggleButtons = undefined;
        }
        $scope.display.showToggleButtons = false;
        
        // remove all calendar events from calendar display items
        $scope.calendarEvents.removeCalendarEvents($scope.calendar);

        // remove all calendar events
        $scope.calendar.calendarEvents.forEach(function(calendarEvent) {
            calendarEvent.delete();
        });

        // remove calendar
        $scope.calendar.delete();
        $scope.display.confirmDeleteCalendar = undefined;


    };

    $scope.cancelRemoveCalendar = function() {
        $scope.display.confirmDeleteCalendar = undefined;
    };

    $scope.shareCalendar = function(calendar, event) {
        $scope.calendar = calendar;
        $scope.display.showPanel = true;
        event.stopPropagation();
    };

    $scope.createCalendarEvent = function() {
        $scope.calendarEvent = new CalendarEvent();
        $scope.calendarEvent.recurrence = {};
        //$scope.calendarEvent.recurrence.week_days = $scope.model.recurrence.week_days;
        $scope.calendarEvent.recurrence.week_days = model.recurrence.week_days;
        $scope.calendarEvent.calendar = $scope.calendars.selection()[$scope.calendars.selection().length - 1];
        $scope.calendarEvent.showDetails = true;

        // dates
        if (model.calendar.newItem !== undefined) {
            $scope.calendarEvent.startMoment = moment.utc(
                model.calendar.newItem.beginning.format('YYYY-MM-DD HH'));
            $scope.calendarEvent.startMoment.minutes(0);
            $scope.calendarEvent.endMoment = moment.utc(
                model.calendar.newItem.end.format('YYYY-MM-DD HH'));
            $scope.calendarEvent.endMoment.minutes(0);
        }
        else {
            $scope.calendarEvent.startMoment = moment().utc();
            $scope.calendarEvent.endMoment = moment().utc();
            $scope.calendarEvent.endMoment.hour($scope.calendarEvent.startMoment.hour() + 1);
        }
        $scope.initEventDates($scope.calendarEvent.startMoment, $scope.calendarEvent.endMoment);
        $scope.showCalendarEventTimePicker = true;
    };

    $scope.displayImportIcsPanel = function() {
        $scope.display.showImportPanel = true;
        $scope.display.importFileButtonDisabled = true;
        $scope.newFile.name = '';
    };
    
    $scope.setFilename = function() {
    	if($scope.newFile && $scope.newFile.files && $scope.newFile.files.length > 0) {
    		disableImportFileButton();
        	$scope.newFile.name = $scope.newFile.files[0].name;
    	}
    };

    $scope.importIcsFile = function(calendar, e) {
        var importButton = e.currentTarget;
        importButton.disabled = true;
        var file = $scope.newFile.files[0];
        var reader = new FileReader();
        reader.onloadend = function(e){
            var jsonData = {};
            jsonData.ics = e.target.result;

            $.ajax('/calendar/' + calendar._id + '/ical', {
                type: "PUT",
                traditional: true,
                data: angular.toJson(jsonData)
            }).done(function(data){
                $scope.display.showImportPanel = undefined;
                $scope.icsImport = data;
                $scope.icsImport.invalidEvents.forEach(function(calendarEvent) {
                    calendarEvent.startMoment = moment(calendarEvent.startMoment);
                    calendarEvent.endMoment = moment(calendarEvent.endMoment);
                });
                $scope.display.showImportReport = true;
                $scope.importFileButtonDisabled = true;
                calendar.calendarEvents.sync(function() {
                    $scope.refreshCalendarEventItems(calendar);
                    if ($scope.display.list) {
                        $scope.calendarEvents.applyFilters();
                    }
                });
            }).fail(function(){
                importButton.disabled = false;
                notify.error(lang.translate("calendar.notify.icsImportError"));
            });
        };
        reader.readAsBinaryString(file);
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

    $scope.saveCalendarEventEdit = function(calendarEvent) {

        function doItemCalendarEvent(items, count) {
            if (items.length === count) {
                if (calendarEvent.noMoreRecurrent) {
                    calendarEvent.noMoreRecurrent = false;
                }

                if (calendarEvent.noMoreRecurrence) {
                    calendarEvent.noMoreRecurrence = false;
                }

                if (calendarEvent.detailToRecurrence) {
                    calendarEvent.detailToRecurrence = false;
                }

                if (calendarEvent.startDateToRecurrence) {
                    calendarEvent.startDateToRecurrence = false;
                }

                if (calendarEvent.endDateToRecurrence) {
                    calendarEvent.endDateToRecurrence = false;
                }

                if (calendarEvent.durationToRecurrence) {
                    calendarEvent.durationToRecurrence = false;
                }

                if (calendarEvent.alldayToRecurrence) {
                    calendarEvent.alldayToRecurrence = false;
                }

                $scope.calendarEvent.calendar.calendarEvents.sync(function() {
                    $scope.refreshCalendarEventItems($scope.calendarEvent.calendar); 
                    $scope.calendarEvents.applyFilters(); 
                    $scope.display.calendar = true;
                });
            } else {

                var itemCalendarEvent = items[count].calEvent;
                var action = items[count].action;

                if (action === 'save') {
                    if (itemCalendarEvent.isRecurrent && count!== 0) {
                        var parentId = items[0].calEvent._id;
                        if (items[0].calEvent.parentId) {
                            parentId = items[0].calEvent.parentId;
                        }
                        itemCalendarEvent.parentId = parentId;            
                    }
                    itemCalendarEvent.save(function () {
                        count++;
                        doItemCalendarEvent(items, count);
                    });

                } else {
                    itemCalendarEvent.delete(function () {
                        count++;
                        doItemCalendarEvent(items, count);
                    });
                }
            }
        }

        var items = [];
        
        if (!calendarEvent) {
            calendarEvent = $scope.calendarEvent;
        }

        $scope.display.calendar = false;

        var parentId = false;
        if (calendarEvent.parentId) {
            parentId = calendarEvent.parentId;
        } else if (calendarEvent._id) {
            parentId = calendarEvent._id;
        }

        var hasExistingRecurrence = false;
        var recurrentCalendarEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);

        if (recurrentCalendarEvents.length > 1) {
            hasExistingRecurrence = true;
        }

        if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
            calendarEvent.recurrence.start_on = moment(calendarEvent.startMoment).hours(0).minutes(0).seconds(0).milliseconds(0);
            if (calendarEvent.recurrence.end_on) {
                calendarEvent.recurrence.end_on.hours(0).minutes(0).seconds(0).milliseconds(0);
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

        calendarEvent.startMoment.seconds(0).milliseconds(0);
        calendarEvent.endMoment.seconds(0).milliseconds(0);

        $scope.closeCalendarEvent(calendarEvent);

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

        if ((calendarEvent.detailToRecurrence || calendarEvent.durationToRecurrence || calendarEvent.startDateToRecurrence || calendarEvent.endDateToRecurrence) && calendarEvent.parentId) {
            recurrentCalendarEvents.forEach (function(cle) {
                if (cle._id !== calendarEvent._id) {
                    var save = false;
                    if (calendarEvent.detailToRecurrence) {
                        cle.title = calendarEvent.title;
                        cle.description = calendarEvent.description;
                        cle.location = calendarEvent.location;
                        save = true;
                    }
                    if (calendarEvent.durationToRecurrence) {
                        if (!calendarEvent.allday) {
                            var diff = calendarEvent.endMoment.diff(calendarEvent.startMoment, 'minutes'); 
                            if (calendarEvent.startDateToRecurrence) {
                                cle.startMoment = moment(cle.startMoment).hours(calendarEvent.startMoment.hours()).minutes(calendarEvent.startMoment.minutes());
                                cle.endMoment = moment(cle.startMoment).add(diff, 'minutes');
                                cle.allday = false;
                            } else if (calendarEvent.endDateToRecurrence) {
                                cle.endMoment = moment(cle.endMoment).hours(calendarEvent.endMoment.hours()).minutes(calendarEvent.endMoment.minutes());
                                cle.startMoment = moment(cle.endMoment).subtract(diff, 'minutes'); 
                                cle.allday = false;  
                            } else if (!cle.allday) {
                                cle.endMoment = moment(cle.startMoment).add(diff, 'minutes'); 
                            }
                            if (!cle.allday) {
                                save = true;
                            }   
                        }
                    } else {
                        if (calendarEvent.startDateToRecurrence) {
                            if (!calendarEvent.allday && !cle.allday) {
                                if (!moment(cle.startMoment).hours(calendarEvent.startMoment.hours()).minutes(calendarEvent.startMoment.minutes()).isAfter(cle.endMoment, 'minute')) {
                                    cle.startMoment = moment(cle.startMoment).hours(calendarEvent.startMoment.hours()).minutes(calendarEvent.startMoment.minutes());
                                    save = true;
                                }
                            }
                        }
                        if (calendarEvent.endDateToRecurrence) {
                            if (!calendarEvent.allday && !cle.allday) {
                                if (!moment(cle.endMoment).hours(calendarEvent.endMoment.hours()).minutes(calendarEvent.endMoment.minutes()).isBefore(cle.startMoment, 'minute')) {
                                    cle.endMoment = moment(cle.endMoment).hours(calendarEvent.endMoment.hours()).minutes(calendarEvent.endMoment.minutes());
                                    save = true;
                                }
                            }
                        }
                    }
                    if (calendarEvent.alldayToRecurrence && calendarEvent.allday) {
                        cle.allday = true;
                        save = true;
                    } 
                    if (save) {
                        var item = {'calEvent': cle, 'action': 'save'};
                        items.push(item);
                    }   
                }

            });
        }

        doItemCalendarEvent(items, 0);
    };


    $scope.cancelEventEdit = function(){
        $scope.display.showEventPanel = undefined;
    };

    $scope.switchSelectAllCalendarEvents = function() {
        if ($scope.display.selectAllCalendarEvents) {
            $scope.calendarEvents.forEach(function(calendarEvent) {
                if (calendarEvent.myRights.contrib) {
                    calendarEvent.selected = true;
                }
            });
        }
        else {
            $scope.calendarEvents.deselectAll();
        }
    };

    $scope.switchFilterListByDates = function(filter) {
        if ($scope.calendarEvents.filters.dates !== true || filter === true) {
            $scope.calendarEvents.filters.startMoment = moment($scope.calendarEvents.filters.startDate);
            $scope.calendarEvents.filters.endMoment = moment($scope.calendarEvents.filters.endDate);
            $scope.calendarEvents.filters.dates = true;
        }
        else {
            $scope.calendarEvents.filters.dates = undefined;
        }
        $scope.calendarEvents.applyFilters();
        $scope.calendarEvents.trigger('change');
    };

    $scope.closeRecurrence = function() {
        $scope.display.calendarEventRecurrence = false;
    };


    $scope.nextWeekButton = function() {
        var next = moment(model.calendar.firstDay).add(7, 'day');
        updateCalendarSchedule(next);
    };
 
    $scope.previousWeekButton = function() {
        var prev = moment(model.calendar.firstDay).subtract(7, 'day');
        updateCalendarSchedule(prev);
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
    }

    var updateCalendarList = function(start, end){
        model.calendarEvents.filters.startMoment.date(start.date());
        model.calendarEvents.filters.startMoment.month(start.month());
        model.calendarEvents.filters.startMoment.year(start.year());
        
        model.calendarEvents.filters.endMoment.date(end.date());
        model.calendarEvents.filters.endMoment.month(end.month());
        model.calendarEvents.filters.endMoment.year(end.year());
 
        $scope.calendarEvents.applyFilters();
 
    };
 
    var updateCalendarSchedule = function(newDate){
        model.calendar.firstDay.date(newDate.date());
        model.calendar.firstDay.month(newDate.month());
        model.calendar.firstDay.year(newDate.year());
 
        $('.hiddendatepickerform').datepicker('setValue', newDate.format("DD/MM/YYYY")).datepicker('update');
        $('.hiddendatepickerform').trigger({type: 'changeDate', date: newDate});

    };


    this.initialize();
    model.calendarEvents.on('refresh', function () {
        template.close('calendar');
        $scope.calendar.calendarEvents.sync(function() {
            $scope.refreshCalendarEventItems($scope.calendar);
            template.open('calendar', 'read-calendar');
        });
        $scope.viewCalendarEvent(model.currentEvent);
    });
}
