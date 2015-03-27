model.colors = ['cyan', 'green', 'orange', 'pink', 'yellow', 'purple', 'grey'];
model.defaultColor = 'grey';

model.timeConfig = { // 5min slots from 7h00 to 19h55, default 8h00
	interval: 5, // in minutes
	start_hour: 1,
	end_hour: 23,
	default_hour: 8
};

model.periods = {
	periodicities: [1, 2, 3, 4], // weeks
	days: [
		1, // monday
		2, // tuesday
		3, // wednesday
		4, // thursday
		5, // friday
		6, // saturday
		0 // sunday
	],
	occurrences: [] // loaded by function
};

model.periodsConfig = {
	occurrences: {
		start: 1,
		end: 52,
		interval: 1
	}
};

function CalendarEvent() {

}

CalendarEvent.prototype.save = function(callback){
    if (this.allday) {
        this.startMoment.hours(7);
        this.startMoment.minutes(0);
        this.endMoment.hours(20);
        this.endMoment.minutes(0);
    }
	if(this._id){
		this.update(callback);
	}
	else{
		this.create(callback);
	}
};

CalendarEvent.prototype.create = function(cb){
    var calendarEvent = this;
    http().postJson('/calendar/' + this.calendar._id + '/events', this).done(function(e){
        calendarEvent.updateData(e);
        if(typeof cb === 'function'){
            cb();
        }
    }.bind(this));
};


CalendarEvent.prototype.update = function(cb){
    var calendarEvent = this;
    http().putJson('/calendar/' + this.calendar._id + '/event/' + this._id, this).done(function(e){
        calendarEvent.updateData(e);
        if(typeof cb === 'function'){
            cb();
        }
    }.bind(this));
};

CalendarEvent.prototype.delete = function(callback) {
    http().delete('/calendar/' + this.calendar._id + '/event/' + this._id).done(function() {
        if(typeof callback === 'function'){
            callback();
        }
    }.bind(this));
};

CalendarEvent.prototype.toJSON = function(){

	return {
		title: this.title,
        description: this.description,
        location: this.location,
		startMoment: this.startMoment,
		endMoment: this.endMoment,
        allday: this.allday
	}
};


function Calendar() {
	var calendar = this;

 	this.collection(CalendarEvent, {
		sync: function(callback){
			http().get('/calendar/' + calendar._id + '/events').done(function(calendarEvents){
				_.each(calendarEvents, function(calendarEvent){
					calendarEvent.calendar = calendar;
					calendarEvent.startMoment = moment(calendarEvent.startMoment).utc();
					calendarEvent.endMoment = moment(calendarEvent.endMoment).utc();
                    // ugly patch for event overflow display
                    if (calendarEvent.allday) {
                        calendarEvent.startMoment.hours(7);
                    }
					calendarEvent.is_periodic = false;
                    calendarEvent.locked = true;
                    calendarEvent.color = calendar.color;
				});
				this.load(calendarEvents);
				if(typeof callback === 'function'){
					callback();
				}
			}.bind(this));
		},
		removeSelection: function(callback){
			var counter = this.selection().length;
			this.selection().forEach(function(item){
				http().delete('/calendar/' + calendar._id + '/event/' + item._id).done(function(){
					counter = counter - 1;
					if (counter === 0) {
						Collection.prototype.removeSelection.call(this);
						calendar.calendarEvents.sync();
						if(typeof callback === 'function'){
							callback();
						}
					}
				});
			});
		},
		behaviours: 'calendar'
	});
}

Calendar.prototype.save = function(callback) {
	if (this._id){
		this.update(callback);
	}
	else {
		this.create(callback);
	}
}

Calendar.prototype.create = function(callback){
    var calendar = this;
    http().postJson('/calendar/calendars', this).done(function(e){
        calendar.updateData(e);
        if(typeof callback === 'function'){
            callback();
        }
    }.bind(this));
};

Calendar.prototype.update = function(callback){
    var calendar = this;
    http().putJson('/calendar/' + this._id, this).done(function(e){
        calendar.updateData(e);
        if(typeof callback === 'function'){
            callback();
        }
    }.bind(this));
};

Calendar.prototype.delete = function(callback) {
    http().delete('/calendar/' + this._id).done(function() {
        model.calendars.remove(this);
        if(typeof callback === 'function'){
            callback();
        }
    }.bind(this));
}

Calendar.prototype.toJSON = function(){
	return {
		title: this.title,
        color: this.color
	}
};

Calendar.prototype.open = function(callback){
	this.calendarEvents.one('sync', function(){
		if(typeof callback === 'function'){
			callback();
		}
	}.bind(this));
	this.calendarEvents.sync();
};

model.build = function(){
    loader.loadFile('/calendar/public/js/additional.js');
	this.makeModel(Calendar);
	this.makeModel(CalendarEvent);

	Model.prototype.inherits(CalendarEvent, calendar.ScheduleItem);

	this.collection(Calendar, {
		sync: function(callback){
            var collection = this;
			http().get('/calendar/calendars').done(function(calendars){
				this.load(calendars);
                collection.trigger('sync');
				if(typeof callback === 'function'){
					callback();
				}
			}.bind(this));
		},
		
		behaviours: 'calendar'
	});

    this.collection(CalendarEvent, {
        pushAll: function(datas, trigger) {
            if (datas) {
                this.all = _.union(this.all, datas);
                if (trigger) {
                    this.trigger('sync');
                }
            }
        },
        pullAll: function(datas, trigger) {
            if (datas) {
                this.all = _.difference(this.all, datas);
                if (trigger) {
                    this.trigger('sync');   
                }
            }
        },
        removeCalendarEvents: function(calendar, trigger) {
            if (calendar) {
                var calendarEvents = [];
                this.all.forEach(function(item) {
                    if (item.calendar._id == calendar._id) {
                        calendarEvents.push(item);
                    }
                });
                this.pullAll(calendarEvents, trigger);
            }
        },
        clear: function(trigger) {
            this.all = [];
            if (trigger) {
                this.trigger('sync');   
            }
        },
        applyFilters: function() {
            this.filtered = _.filter(this.all, function(calendarEvent){
                return calendarEvent.startMoment.isBefore(model.calendarEvents.filters.endMoment) && 
                        calendarEvent.endMoment.isAfter(model.calendarEvents.filters.startMoment);
            });
        },
       
        filters: {
            mine: undefined,
            dates: undefined,
            startMoment: undefined,
            endMoment: undefined
        },
        filtered: [],
        behaviours: 'calendar'
    });
}