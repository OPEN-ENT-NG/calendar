routes.define(function($routeProvider) {
    $routeProvider.when('/view/:calendarId', {
        action : 'goToCalendar'
    }).otherwise({
        action : 'mainPage'
    });
});

function CalendarController($scope, template, model, lang, date, route, $timeout) {
	
    this.initialize = function() {
        $scope._ = _;
        $scope.lang = lang;
        $scope.template = template;
        $scope.display = {};
        $scope.display.list = false;
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

        if ($scope.calendarEvent.startMoment > $scope.calendarEvent.endMoment) {
            $scope.calendarEvent.endMoment = moment($scope.calendarEvent.startMoment).add(1, 'hours');
        }
        
    };

    $scope.changeStartMoment = function() {

        if ($scope.calendarEvent.startMoment > $scope.calendarEvent.endMoment) {
            $scope.calendarEvent.startMoment = moment($scope.calendarEvent.endMoment).subtract(1, 'hours');
        }
        
    };

    $scope.toggleIsRecurrent = function() {
        if ($scope.calendarEvent.isRecurrent) {

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
        }
    };

    $scope.changedRecurrenceType = function() {

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

    /* $scope.hasExistingRecurrence = function(calendarEvent) {
        var hasExistingRecurrence = false;
        var recurrentCalendarEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);
        if (recurrentCalendarEvents.length > 1) {
            hasExistingRecurrence = true;
        }
        return hasExistingRecurrence;
    } */

    $scope.handleRecurrence = function(calendarEvent) {
        if (calendarEvent.recurrence) {
            if (calendarEvent.recurrence.type == 'every_day' && calendarEvent.recurrence.every) {
                $scope.handleEveryDayRecurrence(calendarEvent);
            }
            else if (calendarEvent.recurrence.type == 'every_week_day') {
                $scope.handleEveryWeekDayRecurrence(calendarEvent);
            }
            else if (calendarEvent.recurrence.type == 'every_week') {
                $scope.handleEveryWeekRecurrence(calendarEvent);
            }
        }
    };
 
    $scope.handleEveryDayRecurrence = function(calendarEvent) {
        var calendarRecurrentEvent;
        if (calendarEvent.recurrence.end_type == 'after' && calendarEvent.recurrence.end_after) {
            for (i = 1; i < calendarEvent.recurrence.end_after; i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);

                var toAdd = (i + 0) * parseInt(calendarEvent.recurrence.every);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(toAdd, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(toAdd, 'days');

                $scope.saveCalendarEventEdit(calendarRecurrentEvent);
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
            var endOnMoment = moment(calendarEvent.recurrence.end_on);
            var startMoment = calendarEvent.startMoment;

            for (i =0; startMoment.isBefore(endOnMoment); i++) {
                calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);

                var toAdd = (i + 1) * parseInt(calendarEvent.recurrence.every);
                calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).add(toAdd, 'days');
                calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).add(toAdd, 'days');
                if (calendarRecurrentEvent.startMoment.isAfter(endOnMoment)) {
                    break;
                }
                $scope.saveCalendarEventEdit(calendarRecurrentEvent);
                startMoment = calendarRecurrentEvent.startMoment;
            }
        }
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

    $scope.handleEveryWeekRecurrence = function(calendarEvent) {
        var calendarRecurrentEvent;
        var every = calendarEvent.recurrence.every;
        var weekCount = calendarEvent.startMoment.week();

        if (calendarEvent.recurrence.end_type == 'after' && calendarEvent.recurrence.end_after) {
            for (var i = 0; i < calendarEvent.recurrence.end_after;) {
                for (var day in calendarEvent.recurrence.week_days) {
                    if (calendarEvent.recurrence.week_days[day] === true) {
                        if (day > calendarEvent.startMoment.day() || (day < calendarEvent.startMoment.day() && weekCount > calendarEvent.startMoment.week())) {
                            calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                            calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).isoWeekday(1).isoWeekday(7).day(parseInt(day)).week(weekCount);
                            var startMomentDuration = moment.duration(calendarEvent.startMoment.dayOfYear(), 'd');
                            var endMomentDuration = moment.duration(calendarEvent.endMoment.dayOfYear(), 'd');
                            var eventDaysDuration = endMomentDuration.subtract(startMomentDuration).days();
                            calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).isoWeekday(1).isoWeekday(7).day(parseInt(day) + eventDaysDuration).week(weekCount);
                            
                            $scope.saveCalendarEventEdit(calendarRecurrentEvent);
                            i++;
                        }
                    }
                }
                if (i == calendarEvent.recurrence.end_after) {
                    break;
                }
                weekCount += every;
            }
        } else if (calendarEvent.recurrence.end_type == 'on' && calendarEvent.recurrence.end_on) {
            var endOnMoment = moment(calendarEvent.recurrence.end_on);
            var startMoment = calendarEvent.startMoment;

            while (true) {
                for (var day in calendarEvent.recurrence.week_days) {   
                    if (calendarEvent.recurrence.week_days[day] === true) {
                        if (day > calendarEvent.startMoment.day() || (day < calendarEvent.startMoment.day() && weekCount > calendarEvent.startMoment.week())) {
                            calendarRecurrentEvent = $scope.createChildCalendarEvent(calendarEvent);
                            calendarRecurrentEvent.startMoment = moment(calendarEvent.startMoment).isoWeekday(1).isoWeekday(7).day(parseInt(day)).week(weekCount);
                            
                            if (calendarRecurrentEvent.startMoment.isAfter(endOnMoment)) {
                                break;
                            }
                            var startMomentDuration = moment.duration(calendarEvent.startMoment.dayOfYear(), 'd');
                            var endMomentDuration = moment.duration(calendarEvent.endMoment.dayOfYear(), 'd');
                            var eventDaysDuration = endMomentDuration.subtract(startMomentDuration).days();
                            calendarRecurrentEvent.endMoment = moment(calendarEvent.endMoment).isoWeekday(1).isoWeekday(7).day(parseInt(day) + eventDaysDuration).week(weekCount);
                            
                            $scope.saveCalendarEventEdit(calendarRecurrentEvent);
                            i++;
                            startMoment = calendarRecurrentEvent.startMoment;
                        }
                    }

                }
                if (calendarRecurrentEvent.startMoment.isAfter(endOnMoment)) {
                    break;
                }
                weekCount += every;
            }
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
                    $scope.openOrCloseCalendar(calendar);
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
        $scope.calendarEvents.applyFilters();
        template.open('calendar', 'events-list');
    };

    $scope.showCalendar = function() {
        $scope.display.list = false;
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
            event.startMoment = minTime;
            if(startMoment.isAfter(maxTime)){
                startMoment.add(1, 'days');
                endMoment.add(1, 'days');
                maxTime.add(1, 'days');
            }
        }
        if(endMoment.isBefore(maxTime)){
            event.endMoment = endMoment;
        }
        else{
            event.endMoment = maxTime;
        }

    };

    $scope.openOrCloseCalendar = function(calendar, savePreferences) {
        if ($scope.calendars.selection().length > 1 || !calendar.selected) {
            calendar.selected = !calendar.selected;
            calendar.open(function(){
                if (calendar.selected) {
                    $scope.calendar = calendar;
                } 
                $scope.refreshCalendarEventItems();
                if ($scope.display.list) {
                    $scope.calendarEvents.applyFilters();
                } else {
                    template.open('calendar', 'read-calendar');
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
                $scope.calendarEvent.recurrence.week_days = $scope.model.recurrence.week_days;
            }
        }

        template.open('lightbox', 'edit-event');

        $scope.display.showEventPanel = true;
    };

    $scope.closeCalendarEvent = function(calendarEvent) {

        template.close('lightbox');
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
        $scope.calendarEvents.selection().forEach(function(calendarEvent) {
            calendarEvent.delete(function(){
                count--;
                calendarEvent.calendar.calendarEvents.sync(function() {
                    if (count === 0) {
                        $scope.display.confirmDeleteCalendarEvent = undefined;
                        $scope.closeCalendarEvent();
                        $scope.refreshCalendarEventItems();
                        $scope.calendarEvents.applyFilters();
                        if ($scope.display.list && $scope.display.selectAllCalendarEvents) {
                            $scope.display.selectAllCalendarEvents = undefined;
                        }
                    }
                });
            });
        });
        
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
        $scope.calendarEvent.recurrence.week_days = $scope.model.recurrence.week_days;
        $scope.calendarEvent.calendar = $scope.calendars.selection()[$scope.calendars.selection().length - 1];
        $scope.calendarEvent.showDetails = true;

        // dates
        if (model.calendar.newItem !== undefined) {
            $scope.calendarEvent.startMoment = model.calendar.newItem.beginning;
            $scope.calendarEvent.startMoment.minutes(0);
            $scope.calendarEvent.endMoment = model.calendar.newItem.end;
            $scope.calendarEvent.endMoment.minutes(0);
        }
        else {
            $scope.calendarEvent.startMoment = moment();
            $scope.calendarEvent.endMoment = moment();
            $scope.calendarEvent.endMoment.hour($scope.calendarEvent.startMoment.hour() + 1);
        }
        $scope.initEventDates($scope.calendarEvent.startMoment, $scope.calendarEvent.endMoment);
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

    $scope.saveCalendarEventEdit = function(calendarEvent) { 
        if (!calendarEvent) {
            calendarEvent = $scope.calendarEvent;
        }

        var hasExistingRecurrence = false;
        var recurrentCalendarEvents = $scope.calendarEvents.getRecurrenceEvents(calendarEvent);

        if (recurrentCalendarEvents.length > 1) {
            hasExistingRecurrence = true;
        }

        if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
            calendarEvent.recurrence.start_on = moment(calendarEvent.endMoment).hours(0).minutes(0).seconds(0).milliseconds(0);
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
        }

        $scope.display.showEventPanel = undefined;
        
        calendarEvent.save(function(){
            
            if (calendarEvent.isRecurrent && !calendarEvent.parentId && !hasExistingRecurrence) {
                $scope.handleRecurrence(calendarEvent);
            }

            if (calendarEvent.isRecurrent && !calendarEvent.parentId) {
                calendarEvent.parentId = calendarEvent._id;
                calendarEvent.save(function(){});
            } 

            if (calendarEvent.noMoreRecurrent && hasExistingRecurrence && calendarEvent.noMoreRecurrence) {
                recurrentCalendarEvents.forEach(function(cle) {
                    if (cle._id !== calendarEvent._id) {
                        cle.delete(function(){});
                    }
                })
            }

            if ((calendarEvent.detailToRecurrence || calendarEvent.dateToRecurrence) && calendarEvent.parentId) {
                recurrentCalendarEvents.forEach(function(cle) {
                    if (cle._id !== calendarEvent._id) {
                        if (calendarEvent.detailToRecurrence) {
                            cle.title = calendarEvent.title;
                            cle.description = calendarEvent.description;
                            cle.location = calendarEvent.location;
                        }
                        if (calendarEvent.dateToRecurrence) {
                            if (!calendarEvent.allday) {
                                var diff = calendarEvent.endMoment.diff(calendarEvent.startMoment, 'minutes');
                                if (cle.allday) {
                                    cle.startMoment = moment(cle.startMoment).hours(calendarEvent.startMoment.hours()).minutes(calendarEvent.startMoment.minutes());
                                } 
                                cle.endMoment = moment(cle.startMoment).add(diff, 'minutes');
                                
                            }
                            cle.allday = calendarEvent.allday;
                        }
                        cle.save(function(){});   
                    }

                });
            }

            if (calendarEvent.noMoreRecurrent) {
                calendarEvent.noMoreRecurrent = false;
            }

            if (calendarEvent.noMoreRecurrence) {
                calendarEvent.noMoreRecurrence = false;
            }

            if (calendarEvent.detailToRecurrence) {
                calendarEvent.detailToRecurrence = false;
            }

            if (calendarEvent.dateToRecurrence) {
                calendarEvent.dateToRecurrence = false;
            }

            $scope.calendarEvent.calendar.calendarEvents.sync(function() {
                $scope.refreshCalendarEventItems($scope.calendarEvent.calendar); 
                $scope.calendarEvents.applyFilters();   
            });

        });
        //$scope.display.showEventPanel = undefined;
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
}
