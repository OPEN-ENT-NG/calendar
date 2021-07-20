import { Behaviours, model, _ } from 'entcore';
import http from "axios";
import { CalendarEvent } from "./model";

console.log("behaviours");

const calendarBehaviours = {
	resources: {
		contrib: {
			right: 'net-atos-entng-calendar-controllers-EventController|createEvent'
		},
		manage: {
			right: 'net-atos-entng-calendar-controllers-CalendarController|updateCalendar'
		},
		share: {
			right: 'net-atos-entng-calendar-controllers-CalendarController|shareCalendar'
		}
	},
	workflow: {
		admin: 'net.atos.entng.calendar.controllers.CalendarController|createCalendar'
	},
	viewRights: ['net-atos-entng-calendar-controllers-CalendarController|view']
};
Behaviours.register('calendar', {
	behaviours: calendarBehaviours,
	resourceRights: function (resource) {
		var rightsContainer = resource;
		 if (resource instanceof CalendarEvent && resource.calendar) {
		 	rightsContainer = resource.calendar;
		}
		if (!resource.myRights) {
			resource.myRights = {};
		}
		for (var behaviour in calendarBehaviours.resources) {
			if (model.me.hasRight(rightsContainer, calendarBehaviours.resources[behaviour]) ||
				model.me.userId === resource.owner.userId ||
				model.me.userId === rightsContainer.owner.userId) {
				if (resource.myRights[behaviour] !== undefined) {
					resource.myRights[behaviour] = resource.myRights[behaviour] && calendarBehaviours.resources[behaviour];
				} else {
					resource.myRights[behaviour] = calendarBehaviours.resources[behaviour];
				}
			}
		}
		return resource;
	},
	workflow: function () {
		var workflow = {};
		var calendarWorkflow = calendarBehaviours.workflow;
		for (var prop in calendarWorkflow) {
			if (model.me.hasWorkflow(calendarWorkflow[prop])) {
				workflow[prop] = true;
			}
		}
		return workflow;
	},
	loadResources: async function () {
		 let { data } = await http.get('/calendar/list');
		this.resources = data;
	}
});