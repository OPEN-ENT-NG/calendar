import { saveAs } from 'file-saver';

var ics = function() {
    'use strict';
    if (navigator.userAgent.indexOf('MSIE') > -1 && navigator.userAgent.indexOf('MSIE 10') == -1) {
        console.log('Unsupported Browser');
        return;
    }
    var SEPARATOR = (navigator.appVersion.indexOf('Win') !== -1) ? '\r\n' : '\n';
    var calendarEvents = [];
    var calendarStart = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0'
    ].join(SEPARATOR);
    var calendarEnd = SEPARATOR + 'END:VCALENDAR';
    return {
        /**
        * Returns events array
        * @return {array} Events
        */
        'events': function() {
            return calendarEvents;
        },
        /**
        * Returns calendar
        * @return {string} Calendar in iCalendar format
        */
        'calendar': function() {
            return calendarStart + SEPARATOR + calendarEvents.join(SEPARATOR) + calendarEnd;
        },
        /**
        * Add event to the calendar
        * @param {string} subject Subject/Title of event
        * @param {string} description Description of event
        * @param {string} location Location of event
        * @param {string} start Beginning date of event (YYYYMMDDTHHmmss format)
        * @param {string} end Ending date of event (YYYYMMDDTHHmmss format)
        */
        'addEvent': function(subject, description, location, start, end) {
            if (typeof subject === 'undefined' ||
                typeof description === 'undefined' ||
                typeof location === 'undefined' ||
                typeof start === 'undefined' ||
                typeof end === 'undefined'
                ) {
                return false;
            };
            var calendarEvent = [
                'BEGIN:VEVENT',
                'CLASS:PUBLIC',
                'DESCRIPTION:' + description,
                'DTSTART:' + start,
                'DTEND:' + end,
                'LOCATION:' + location,
                'SUMMARY;LANGUAGE=en-us:' + subject,
                'TRANSP:TRANSPARENT',
                'END:VEVENT'
            ].join(SEPARATOR);
            calendarEvents.push(calendarEvent);
            return calendarEvent;
        },
        /**
        * Add event to the calendar
        * @param {string} subject Subject/Title of event
        * @param {string} description Description of event
        * @param {string} location Location of event
        * @param {string} start Beginning date of event (YYYYMMDD format)
        * @param {string} end Ending date of event (YYYYMMDD format)
        */
        'addAllDayEvent': function(subject, description, location, start, end) {
            if (typeof subject === 'undefined' ||
                typeof description === 'undefined' ||
                typeof location === 'undefined' ||
                typeof start === 'undefined' ||
                typeof end === 'undefined'
                ) {
                return false;
            };
            var calendarEvent = [
                'BEGIN:VEVENT',
                'CLASS:PUBLIC',
                'DESCRIPTION:' + description,
                'DTSTART;VALUE=DATE:' + start,
                'DTEND;VALUE=DATE:' + end,
                'LOCATION:' + location,
                'SUMMARY;LANGUAGE=en-us:' + subject,
                'TRANSP:TRANSPARENT',
                'END:VEVENT'
            ].join(SEPARATOR);
            calendarEvents.push(calendarEvent);
            return calendarEvent;
        },
        /**
        * Download calendar using the saveAs function from filesave.ts
        * @param {string} filename Filename
        * @param {string} ext Extention
        */
        'download': function(filename, ext) {
            if (calendarEvents.length < 1) {
                return false;
            }
            ext = (typeof ext !== 'undefined') ? ext : '.ics';
            filename = (typeof filename !== 'undefined') ? filename : 'calendar';
            var calendar = calendarStart + SEPARATOR + calendarEvents.join(SEPARATOR) + calendarEnd;
            var blob;
            blob = new Blob([calendar]);
            saveAs(blob, filename + ext);
            return calendar;
        }
    };
};
