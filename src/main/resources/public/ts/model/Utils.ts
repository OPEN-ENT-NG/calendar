import {moment} from "entcore";

export const getTime = (dateUpdate , time) => {
    return moment(dateUpdate).hours(moment(time)
        .format('HH'))
        .minutes(moment(time)
            .format('mm'))
};
export const makerFormatTimeInput = (hours, minutes) => {
    return  new Date(
        1970,
        0,
        1,
        moment(hours).format('HH'),
        moment(minutes).format('mm'),
        0,
    );
};

export const utcTime = (getMomentScopeUtc:Date):number => {
    return moment(moment(getMomentScopeUtc)
        .format("YYYY MM DD HH:MM"), "YYYY MM DD HH:MM")
        .format('Z')
        .split(':')[0]
};

export const  isSameAfter = (dateCheck, dateNotPast) => {
    return (moment(dateCheck).isAfter(dateNotPast) || moment(dateCheck).isSame(dateNotPast))
};

/**
 * Refresh the view
 * @param $scope
 * @returns {any}
 */
export function safeApply($scope: any, fn?): any {
    const phase = $scope.$root.$$phase;
    if (phase == '$apply' || phase == '$digest') {
        if (fn && (typeof (fn) === 'function')) {
            fn();
        }
    } else {
        $scope.$apply(fn);
    }

}