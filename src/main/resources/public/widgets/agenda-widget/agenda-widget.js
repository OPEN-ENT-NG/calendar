(function(){
    var CalendarWidget = model.widgets.findWidget("agenda-widget");

    CalendarWidget.userEvents = [];
    CalendarWidget.dayGroups = [];
    /** Maximum number of events displayed by the widget. */
    CalendarWidget.MAX_EVENTS_DISPLAYED = 5;

    function dateToMoment(date) {
        var numberHoursLag = moment(moment(date).format("YYYY MM DD HH:MM"), 'YYYY MM DD HH:MM')
            .format('Z')
            .split(':')[0];
        return moment.utc(date).add(numberHoursLag, 'hours');
    }

    CalendarWidget.toDisplayedGroup = function(date) {
        return dateToMoment(date).format('dddd D MMMM YYYY'); // FIXME localize the format string with i18n ! See formats at https://momentjs.com/docs/#/displaying/format/
    }

    CalendarWidget.toDisplayedDate = function(date) {
        return dateToMoment(date).format('HH:mm');
    }
    CalendarWidget.today= function(format) {
        return moment().format(format);
    }

    CalendarWidget.loadEvents = function() {
        return entcore.httpPromisy().get('/calendar/calendars')
            .then(calendars => {
            if (angular.isArray(calendars) && calendars.length > 0) {
                let filter = calendars.map(calendar => 'calendarId=' + calendar._id).join('&');
                console.log('filter calendarWidget : ' + filter);
                return filter;
            }
            return null;
        })
        .then(filterOn => {
            return filterOn === null ? [] : entcore.httpPromisy().get('/calendar/events/widget?' + filterOn + '&nb=' + CalendarWidget.MAX_EVENTS_DISPLAYED);
        });
    }
    CalendarWidget.setEvents = function(events) {
        if (angular.isArray(events)) {
            CalendarWidget.dayGroups = [];
            CalendarWidget.userEvents = [];
            let lastGroup = '';
            events.forEach(ev => {
                const displayed = Object.assign({
                    displayedGroup: CalendarWidget.toDisplayedGroup(ev.startMoment),
                    displayedDate: CalendarWidget.toDisplayedDate(ev.startMoment)
                }, ev);
                CalendarWidget.userEvents.push(displayed);
                if (lastGroup !== displayed.displayedGroup) {
                    lastGroup = displayed.displayedGroup;
                    CalendarWidget.dayGroups.push(lastGroup);
                }
            });
        }
    }

    CalendarWidget.loadEvents().then(events => {
        CalendarWidget.setEvents(events);
        model.widgets.apply();
    });

    // Give an opportunity to track some events from outside of this widget.
    CalendarWidget.trackEvent = (e, p) => {
        // var _a, _b;
        // // Allow events to bubble up.
        // if (typeof p.bubbles === "undefined")
        //     p.bubbles = true;
        // let event = null;
        // if (p && ((_a = p.detail) === null || _a === void 0 ? void 0 : _a.open) === 'app') {
        //     event = new CustomEvent(TrackedActionFromWidget.agenda, p);
        // }
        // else if (p && ((_b = p.detail) === null || _b === void 0 ? void 0 : _b.open) === 'event') {
        //     event = new CustomEvent(TrackedActionFromWidget.agenda, p);
        // }
        // if (event && e.currentTarget) {
        //     e.currentTarget.dispatchEvent(event);
        // }
    };
}());