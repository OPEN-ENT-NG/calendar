var calendarWidget = model.widgets.findWidget("agenda-widget");

calendarWidget.today = function(format){
    return moment().format(format);
};

calendarWidget.dateToString = function(date) {
    return moment(date).format('DD/MM HH:mm');
};

calendarWidget.userEvents = [];
calendarWidget.numberEvents = 5;

http().get('/calendar/calendars').done(function(calendars){

    var filter = '';
    calendars.map(function (calendar) {
       return filter += 'calendarId=' + calendar._id + '&';
    });
    filter = filter.slice(0, -1);

    model.widgets.apply();

    http().get('/calendar/widget/events?'+ filter + '&nb=' + calendarWidget.numberEvents)
        .done(function (events) {
            calendarWidget.userEvents = events;
            model.widgets.apply();
        });
});

model.widgets.apply();