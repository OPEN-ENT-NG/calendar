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

    $scope.openCalendar = function(calendar) {
        $scope.calendar = calendar;
        $scope.calendars.forEach(function(cl) {
            if (cl._id != calendar._id) {
                cl.showButtons = false;                
            }
        });
        $scope.calendar.open(function(){
            $scope.calendarEvents = $scope.calendar.calendarEvents.all;
            template.open('calendars', 'calendars');
            template.open('main', 'read-calendar');
            $scope.$apply();
        });
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
                $scope.openCalendar($scope.calendar);
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
                template.open('main', 'read-calendar');
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
        $scope.calendarEvent.calendar = $scope.calendar;

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
            $scope.calendar.calendarEvents.sync(function() {
                $scope.calendarEvents = $scope.calendar.calendarEvents.all;
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