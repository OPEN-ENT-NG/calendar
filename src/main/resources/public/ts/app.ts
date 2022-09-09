import { routes, ng } from "entcore";
import * as controllers from './controllers';
import {colorSelector, formatInputTime, sideBar, rbsBookingDeleteInfo} from './directives';
import {calendarItem} from "./directives/calendar-item/calendar-item";

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

ng.directives.push(colorSelector);
ng.directives.push(formatInputTime);
ng.directives.push(sideBar);
ng.directives.push(rbsBookingDeleteInfo);
ng.directives.push(calendarItem);

routes.define(function($routeProvider) {
    $routeProvider
        .when('/view/:calendarId', {
        action : 'goToCalendar'
    })
        .when('/', {
        action: 'mainPage'
    })
});
