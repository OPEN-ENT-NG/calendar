routes.define(function($routeProvider) {
    $routeProvider.when('/view/:calendarId', {
        action : 'goToCalendar'
    }).otherwise({
        action : 'mainPage'
    });
});

function CalendarController($scope, template, model, date, route, $timeout) {
	
    this.initialize = function() {
        $scope.loadCalendarPreferences(function() {
            $scope.template = template;
            $scope.display = {};
            $scope.display.list = false;
            $scope.me = model.me;
            $scope.date = date;
            $scope.calendarEvent = new CalendarEvent();
            $scope.initEventDates(moment().utc(), moment().utc());
            $scope.calendars = model.calendars;
            
            $scope.calendarEvents = model.calendarEvents;
            $scope.periods = model.periods;

            model.calendarEvents.filters.startMoment = moment().startOf('day');
            model.calendarEvents.filters.endMoment = moment().add('month', 2).startOf('day');

            template.open('main', 'main-view');
            template.open('top-menu', 'top-menu');
            template.open('calendar', 'read-calendar');

            
        });
    };

    // Definition of actions
    route({
        goToCalendar : function(params) {
            model.calendars.one('sync', function() {
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
            model.calendars.one('sync', function(){
                if ($scope.calendarPreferences.preference.selectedCalendars) {
                    var toSelectCalendars = _.filter($scope.calendars, function(calendar) {
                        return _.contains($scope.calendarPreferences.selectedCalendars, calendar._id);
                    });
                    toSelectCalendars.forEach(function(calendar) {
                        calendar.selected = true;
                    });
                }
                if ($scope.calendars.selection().length == 0 && !$scope.calendars.isEmpty()) {
                    $scope.openOrCloseCalendar($scope.calendars.all[0]);
                }
            });
        }
    });

    $scope.loadCalendarPreferences = function(callback) {
         if(typeof callback === 'function'){
            callback();
        }
        // http().get('/userbook/preference/calendar').done(function(calendarPreferences){
        //     if (!calendarPreferences) {
        //         calendarPreferences = {};
        //     }
        //     $scope.calendarPreferences = calendarPreferences;
           
        // });
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
                startMoment.add('day', 1);
                endMoment.add('day', 1);
                maxTime.add('day', 1);
            }
        }
        if(endMoment.isBefore(maxTime)){
            event.endMoment = endMoment;
        }
        else{
            event.endMoment = maxTime;
        }

    };

    $scope.openOrCloseCalendar = function(calendar) {
        calendar.selected = !calendar.selected;
        calendar.open(function(){
            if (calendar.selected) {
                $scope.calendar = calendar;
            } 
            $scope.refreshCalendarEventItems();
            if (!$scope.display.list) {
                template.open('calendar', 'read-calendar');
            } else {
                $scope.calendarEvents.applyFilters();
            }
        });
        $scope.calendarPreferences.selectedCalendars = _.map($scope.calendars.selection(), function(calendar) {
            return calendar._id;
        });
        $scope.saveCalendarPreferences();
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
        $scope.display.confirmDeleteCalendarEvent = true;
        event.stopPropagation();
    };

    $scope.confirmRemoveCalendarEvents = function(event){
        $scope.display.confirmDeleteCalendarEvent = true;
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
                    }
                });
            });
        });
        
    };

    $scope.cancelRemoveCalendarEvent = function() {
        $scope.display.confirmDeleteCalendarEvent = undefined;
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
                template.open('calendar', 'share-calendar');
            });
            $scope.calendars.sync();
        }
        template.close('calendar');
    };

    $scope.hasContribRight = function(calendar) {
        var contribRight;
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
        $scope.calendars.forEach(function(cl) {
            cl.showButtons = false;                
        });
        template.close('calendar');
    };

    $scope.confirmRemoveCalendar = function(calendar, event){
        $scope.display.confirmDeleteCalendar = true;
        $scope.calendar = calendar;
        event.stopPropagation();
    };

    $scope.removeCalendar = function() {
        // remove all calendar events from calendar display items
        $scope.calendarEvents.removeCalendarEvents($scope.calendar);

        // remove all calendar events
        $scope.calendar.calendarEvents.forEach(function(calendarEvent) {
            calendarEvent.delete();
        })

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
        $scope.calendarEvent.recurrence.end = 'never';
        $scope.calendarEvent.calendar = $scope.calendars.selection()[$scope.calendars.selection().length - 1];

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
    };

    $scope.importIcsFile = function(calendar) {
        var icsFileInput = $('#icsFile')[0];
        var file = icsFileInput.files[0];
        var reader = new FileReader();
        reader.onloadend = function(e){
            var jsonData = {};
            jsonData.ics = e.target.result;
            http().putJson('/calendar/' + calendar._id + '/ical', jsonData).done(function(e){
                $scope.display.showImportPanel = undefined;
                calendar.calendarEvents.sync(function() {
                    $scope.refreshCalendarEventItems(calendar);
                });
            });
        }
        reader.readAsBinaryString(file);
    };

    $scope.saveCalendarEventEdit = function() { 
        $scope.calendarEvent.save(function(){
            $scope.calendarEvent.calendar.calendarEvents.sync(function() {
                $scope.refreshCalendarEventItems($scope.calendarEvent.calendar);    
            });
        });
        $scope.display.showEventPanel = undefined;
    };


    $scope.cancelEventEdit = function(){
        $scope.display.showEventPanel = undefined;
    };

    $scope.switchSelectAllCalendarEvents = function() {
        if ($scope.display.selectAllCalendarEvents) {
            $scope.calendarEvents.selectAll();
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

    this.initialize();
}