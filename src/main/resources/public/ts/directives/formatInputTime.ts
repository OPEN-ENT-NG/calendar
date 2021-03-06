import {moment, ng} from "entcore";
import {
    getTime,
    makerFormatTimeInput,
    isSameAfter,
} from "../model/Utils";

export const formatInputTime = ng.directive('formatInputTime', () => {
    let canCheck = true;
    return {
        require: 'ngModel',
        restrict : "A",
        scope:  {
            startTime: '=startTime',
            endTime: '=endTime',
            startMoment: '=startMoment',
            endMoment: '=endMoment',
        },
        link: function(scope, e, attribute, ctrl) {
            e.bind('focus', function() {
                if (attribute.id === 'time-picker-end-moment')
                    canCheck = false;
            });
            ctrl.$formatters.unshift(time => {

                scope.startMoment = getTime(scope.startMoment, scope.startTime);
                scope.endMoment = getTime(scope.endMoment, scope.endTime);

                if(isSameAfter(scope.startMoment, scope.endMoment )){
                    canCheck = true;
                }


                if (time.endsWith('.000')) {
                    return time.slice(0, -7)
                }
                return time
            })
        }
    }
});