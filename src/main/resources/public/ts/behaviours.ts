import { Behaviours, model, _ } from 'entcore';
import http from "axios";
import { CalendarEvent } from "./model";
import { rights } from "./model/constantes";

console.log("behaviours");

Behaviours.register('calendar', {
	behaviours: rights,
	resourceRights: function (resource) {
		var rightsContainer = resource;
		 if (resource instanceof CalendarEvent && resource.calendar) {
		 	rightsContainer = resource.calendar;
		}
		if (!resource.myRights) {
			resource.myRights = {};
		}
		for (var behaviour in rights.resources) {
			if (model.me.hasRight(rightsContainer, rights.resources[behaviour]) ||
				model.me.userId === resource.owner.userId ||
				model.me.userId === rightsContainer.owner.userId) {
				if (resource.myRights[behaviour] !== undefined) {
					resource.myRights[behaviour] = resource.myRights[behaviour] && rights.resources[behaviour];
				} else {
					resource.myRights[behaviour] = rights.resources[behaviour];
				}
			}
		}
		return resource;
	},
	workflow: function () {
		var workflow = {};
		var calendarWorkflow = rights.workflow;
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