var calendarBehaviours = {
	resources: {
		contrib: {
			right: 'net-atos-entng-calendar-controllers-CalendarController|createCalendar'
		},
		manage: {
			right: 'net-atos-entng-calendar-controllers-CalendarController|updateCalendar'
		},
		share: {
			right: 'net-atos-entng-calendar-controllers-CalendarController|shareCalendar'
		}
	},
	workflow: {
		admin: 'net.atos.entng.forum.controllers.CalendarController|createCalendar'
	},
	viewRights: [ 'net-atos-entng-calendar-controllers-CalendarController|view' ]
};

Behaviours.register('calendar', {
	behaviours:  calendarBehaviours,
	resource: function(resource){
		var rightsContainer = resource;
		if(resource instanceof Event && resource.calendar){
			rightsContainer = resource.calendar;
		}
		if(!resource.myRights){
			resource.myRights = {};
		}

		for(var behaviour in calendarBehaviours.resources){
			if(model.me.hasRight(rightsContainer, calendarBehaviours.resources[behaviour]) 
					|| model.me.userId === resource.owner.userId 
					|| model.me.userId === rightsContainer.owner.userId){
				if(resource.myRights[behaviour] !== undefined){
					resource.myRights[behaviour] = resource.myRights[behaviour] && calendarBehaviours.resources[behaviour];
				}
				else{
					resource.myRights[behaviour] = calendarBehaviours.resources[behaviour];
				}
			}
		}
		return resource;
	},
	resourceRights: function(){
		return ['read', 'manager']
	},
	loadResources: function(callback) {
		http().get('/calendar/list').done(function(calendars){
			this.resources = calendars;
			callback(this.resources);
		}.bind(this));
	}
});