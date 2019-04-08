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

export const utcTime = () => {
    return moment(moment()
        .format("YYYY MM DD HH:MM"), "YYYY MM DD HH:MM")
        .format('Z')
        .split(':')[0]
};

export const  isSameAfter = (dateCheck, dateNotPast) => {
    return (moment(dateCheck).isAfter(dateNotPast) || moment(dateCheck).isSame(dateNotPast))
};