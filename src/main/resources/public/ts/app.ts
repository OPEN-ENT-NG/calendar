import { routes, ng } from "entcore";
import * as controllers from './controllers';
import {colorSelector, formatInputTime } from './directives';

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

ng.directives.push(colorSelector);
ng.directives.push(formatInputTime);

routes.define(function($routeProvider) {
    $routeProvider
        .when('/view/:calendarId', {
        action : 'goToCalendar'
    })
        .when('/', {
        action: 'mainPage'
    })
});
