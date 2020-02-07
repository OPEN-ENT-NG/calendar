var calendarWidget = model.widgets.findWidget("agenda-widget");

calendarWidget.today = function(format){
    return moment().format(format);
};

calendarWidget.dateToString = function(date) {
    var numberHoursLag = moment(moment(date)
        .format("YYYY MM DD HH:MM"), "YYYY MM DD HH:MM")
        .format('Z')
        .split(':')[0];
    return moment.utc(date).add(numberHoursLag, 'hours').format('DD/MM HH:mm');
};

calendarWidget.userEvents = [];
calendarWidget.numberEvents = 5;

http().get('/calendar/calendars').done(function(calendars){
    if(calendars !== null && calendars !== undefined
        && calendars.length > 0) {
        var filter = '';
        calendars.map(function (calendar) {
            return filter += 'calendarId=' + calendar._id + '&';
        });
        console.log('filter calendarWidget : ' + filter);
        model.widgets.apply();

        http().get('/calendar/events/widget?' + filter + 'nb=' + calendarWidget.numberEvents)
            .done(function (events) {
                calendarWidget.userEvents = events;
                model.widgets.apply();
            });
    } else {
        calendarWidget.userEvents = undefined;
    }
});

model.widgets.apply();
