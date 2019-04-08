import {moment, ng} from "entcore";
import {
    getTime,
    makerFormatTimeInput,
    isSameAfter,
} from "../model/Utils";

export const formatInputTime = ng.directive('formatInputTime', () => {
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
            ctrl.$formatters.unshift(time => {

                scope.startMoment = getTime(scope.startMoment, scope.startTime);
                scope.endMoment = getTime(scope.endMoment, scope.endTime);

                if(isSameAfter(scope.startMoment, scope.endMoment )){
                    if (attribute.id === 'time-picker-start-moment'){
                        scope.endTime = makerFormatTimeInput(scope.startTime, moment(scope.startTime).add(15, 'minutes'));
                    } else if (attribute.id === 'time-picker-end-moment') {
                        scope.startTime = makerFormatTimeInput(moment(scope.endTime).subtract(1, 'hours'), scope.endTime);
                    }
                }

                if (time.endsWith('.000')) {
                    return time.slice(0, -7)
                }
                return time
            })
        }
    }
});