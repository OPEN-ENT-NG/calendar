routes.define(function($routeProvider) {
    $routeProvider.when('/calendar/:calendarId', {
        action : 'goToCalendar'
    }).otherwise({
        action : 'mainPage'
    });
});

function CalendarController($scope, template, model, date, route) {
	
    this.initialize = function() {
        $scope.template = template;
        $scope.display = {};
        $scope.me = model.me;
        $scope.date = date;
        $scope.calendars = model.calendars;
        $scope.calendarEvent = new CalendarEvent();
        $scope.initEventDates(moment().utc(), moment().utc());
        $scope.selectedCalendars = [];
        $scope.calendarEvents = model.calendarEvents;
    };

    // Definition of actions
    route({
        goToCalendar : function(params) {
            template.open('main', 'read-calendar');
        },
        mainPage : function(params) {
            template.open('calendars', 'calendars');
        }
    });

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
        calendar.open(function(){
            if ($scope.selectedCalendars.indexOf(calendar) == -1) {
                $scope.calendar = calendar;
                $scope.selectedCalendars.push(calendar);
                $scope.calendarEvents.pushAll($scope.calendar.calendarEvents.all);
                template.open('main', 'read-calendar');
            } else if ($scope.calendarEvents) {
                $scope.calendarEvents.removeCalendarEvents(calendar);
                $scope.selectedCalendars.splice($scope.selectedCalendars.indexOf(calendar), 1);
                if ($scope.selectedCalendars.length == 0) {
                    template.close('main');
                } else {
                    template.open('main', 'read-calendar');
                }
            }
            template.open('calendars', 'calendars');
        });
    };

    $scope.refreshCalendarEventItems = function(calendar) {
        if (calendar) {
            $scope.calendarEvents.removeCalendarEvents(calendar);
            $scope.calendarEvents.pushAll(calendar.calendarEvents.all);
        } else {
            $scope.calendarEvents.clear();
            $scope.selectedCalendars.forEach(function(calendar) {
                $scope.calendarEvents.pushAll(calendar.calendarEvents.all);
            });
        }

    }

    $scope.hideOtherCalendarCheckboxes = function(calendar) {
         $scope.calendars.forEach(function(item) {
            if (item._id != calendar._id) {
                item.showButtons = false;
            }
        });
    }

    $scope.isSelectedCalendar = function(calendar) {
        var isSelected = false;
        $scope.selectedCalendars.forEach(function(item) {
            if (item._id == calendar._id) {
                isSelected = true;
            }
        });
        return isSelected;
    }

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
        template.open('main', 'edit-calendar');
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


    $scope.removeCalendarEvent = function(calendarEvent, event) {
        calendarEvent.delete(function(){
            $scope.calendar.calendarEvents.sync(function() {
                $scope.display.confirmDeleteCalendarEvent = undefined;
                $scope.closeCalendarEvent();
                $scope.refreshCalendarEventItems();
            });
        });
    };

    $scope.cancelRemoveCalendarEvent = function() {
        $scope.display.confirmDeleteCalendarEvent = undefined;
    };

    $scope.editCalendar = function(calendar, event) {
        $scope.calendar = calendar;
        event.stopPropagation();
        template.open('main', 'edit-calendar');
    };

    $scope.saveCalendarEdit = function() {
        if ($scope.calendar._id) {
            $scope.calendar.save(function(){
                $scope.calendar.calendarEvents.sync(function() {
                    $scope.refreshCalendarEventItems($scope.calendar);
                    template.open('main', 'read-calendar');
                });
            });
        }
        else { 
            $scope.calendar.save(function(){
                template.open('main', 'share-calendar');
            });
            $scope.calendars.sync();
        }
        template.close('main');
    };

    $scope.hasContribRight = function(calendar) {
        var contribRight;
        if (calendar) {
           contribRight = calendar.myRights.manage;
        } else {
             var contribRight = $scope.selectedCalendars.some(function(cl) {
                if (cl.myRights.manage) {
                   return true;
                }
            });
        }
        return contribRight;
    }

    $scope.cancelCalendarEdit = function() {
        $scope.calendar = undefined;
        $scope.calendars.forEach(function(cl) {
            cl.showButtons = false;                
        });
        template.close('main');
    };

    $scope.confirmRemoveCalendar = function(calendar, event){
        $scope.calendars.deselectAll();
        calendar.selected = true;
        $scope.display.confirmDeleteCalendar = true;
        event.stopPropagation();
    };

    $scope.removeCalendar = function() {
        $scope.calendar.delete();
        delete $scope.display.confirmDeleteCalendar;
        template.close('main');
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
        $scope.calendarEvent.calendar = $scope.selectedCalendars[$scope.selectedCalendars.length - 1];

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

    $scope.saveCalendarEventEdit = function() { 
        $scope.calendarEvent.save(function(){
            $scope.calendarEvent.calendar.calendarEvents.sync(function() {
                $scope.refreshCalendarEventItems($scope.calendarEvent.calendar);
                template.open('main', 'read-calendar');
            });
        });
        $scope.display.showEventPanel = undefined;
        template.close('main');
    };


    $scope.cancelEventEdit = function(){
        template.open('main', 'read-calendar');
    };

    this.initialize();
}