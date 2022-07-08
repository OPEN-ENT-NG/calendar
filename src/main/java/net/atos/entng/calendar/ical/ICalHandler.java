/*
 * Copyright © Région Nord Pas de Calais-Picardie,  Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.atos.entng.calendar.ical;

import fr.wseduc.webutils.I18n;
import io.vertx.core.AbstractVerticle;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.exception.CalendarException;
import net.atos.entng.calendar.exception.UnhandledEventException;
import net.atos.entng.calendar.helpers.FutureHelper;
import net.atos.entng.calendar.utils.DateUtils;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

/**
 * ICal worker to handle ICS parsing and generation
 * @author Atos
 */
public class ICalHandler extends AbstractVerticle implements Handler<Message<JsonObject>> {

    public static final String ICAL_HANDLER_ADDRESS = "ical.handler";
    private static final Logger log = LoggerFactory.getLogger(FutureHelper.class);

    /**
     * Actions handled by worker
     */
    public static final String ACTION_PUT = "put";
    public static final String ACTION_GET = "get";

    /**
     * Simple Date formatter in moment.ts format
     */
    private static final SimpleDateFormat MOMENT_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @Override
    public void start() throws Exception {
        super.start();
        vertx.eventBus().localConsumer(ICAL_HANDLER_ADDRESS, this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "get":
                jsonEventsToIcsContent(message);
                break;
            case "put":
                icsContentToJsonEvents(message);
                break;
        }

    }

    /**
     * Get message containing a JsonArray filled with events and reply by ICS generated data
     * @param message Contains JsonArray filled with events
     */
    private void jsonEventsToIcsContent(Message<JsonObject> message) {
        JsonObject results = new JsonObject();
        JsonObject body = message.body();
        Calendar calendar = new Calendar();
        initCalendarProperties(calendar);
        JsonArray calendarEvents = body.getJsonArray("events");
        for (Object calendarEvent : calendarEvents) {
            JsonObject ce = (JsonObject) calendarEvent;
            String startMoment = ce.getString("startMoment");
            String endMoment = ce.getString("endMoment");
            String title = ce.getString("title");
            String icsUid = ce.getString("icsUid");
            String location = ce.getString("location");
            String description = ce.getString("description");
            boolean allDay = ce.containsKey("allday") && ce.getBoolean("allday");
            try {
                if (allDay) {
                    java.util.Date startDate = MOMENT_FORMAT.parse(startMoment);
                    java.util.Date oldEndDate = MOMENT_FORMAT.parse(endMoment);
                    //add 1 day to end date so that N-days events ends on day N+1 at 00:00
                    java.util.Date endDate = new java.util.Date(oldEndDate.getTime() + TimeUnit.DAYS.toMillis(1));
                    addAllDayEvent(calendar, startDate, endDate, title, icsUid, location, description);
                } else {
                    addEvent(calendar, startMoment, endMoment, title, icsUid, location, description);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        results.put("ics", calendar.toString());
        results.put("status", 200);
        message.reply(results);
    }

    /**
     * Get message containg ICS data and reply by a JsonArray containg all calendar events in Json format
     * @param message Contains ICS data
     */
    private void icsContentToJsonEvents(Message<JsonObject> message) {
        String icsContent = message.body().getString(Field.ICS);
        JsonObject requestInfo = message.body().getJsonObject(Field.REQUESTINFO);
        InputStream inputStream = new ByteArrayInputStream(icsContent.getBytes());
        JsonObject results = new JsonObject();

        try {
            // Reader r = new InputStreamReader(inputStream, "ISO-8859-15");
            CalendarBuilder calendarBuilder = new CalendarBuilder();
            Calendar calendar = calendarBuilder.build(inputStream);
            JsonArray events = new JsonArray();
            JsonArray invalidEvents = new JsonArray();
            ComponentList components = calendar.getComponents();
            for (Object component : components) {
                if (component instanceof VEvent) {
                    JsonObject jsonEvent = new JsonObject();
                    try {
                        VEvent event = (VEvent) component;
                        setEventDates(event, jsonEvent);
                        setEventProperties(event, jsonEvent, requestInfo);
                        validateEvent(event);
                        events.add(jsonEvent);
                    } catch (UnhandledEventException uee) {
                        jsonEvent.put("errorCause", uee.getMessage());
                        invalidEvents.add(jsonEvent);
                    }
                }
            }
            results.put("events", events);
            results.put("invalidEvents", invalidEvents);
            message.reply(results);
        } catch (IOException | ParserException e) {
            e.printStackTrace();
            results.put("status", "ko");
            message.reply(results);
        }
    }

    private void validateEvent(VEvent event) throws UnhandledEventException {
        if (event.getProperty("RRULE") != null) {
            System.out.println(event.getProperty("RRULE").getValue());
            throw new UnhandledEventException("Les évènements récurrents ne sont pas pris en charge");
        }
    }

    /**
     * Set ical4j event properties to JsonObject
     * @param event ical4j filled event
     * @param jsonEvent JsonObject event to fill
     */
    private void setEventProperties(VEvent event, JsonObject jsonEvent, JsonObject requestInfo) {

        String title = event.getSummary() == null ? encodeString(I18n.getInstance().translate("calendar.ical.event.no.title",
                requestInfo.getString(Field.DOMAIN), requestInfo.getString(Field.ACCEPTLANGUAGE))) : encodeString(event.getSummary().getValue());
        String location = event.getLocation() != null ? encodeString(event.getLocation().getValue()) : "";
        String description = event.getDescription() != null ? encodeString(event.getDescription().getValue()) : "";
        String uid = event.getUid() != null ? event.getUid().getValue() : "";

        if (!title.isEmpty()) {
            jsonEvent.put("title", title);
        }
        if (!location.isEmpty()) {
            jsonEvent.put("location", location);
        }
        if (!description.isEmpty()) {
            jsonEvent.put("description", description);
        }
        if (!uid.isEmpty()) {
            jsonEvent.put("icsUid", uid);
        }

    }

    private String encodeString(String toEncode) {
        Charset utf8charset = Charset.forName("UTF-8");

        ByteBuffer inputBuffer = ByteBuffer.wrap(toEncode.getBytes());

        // decode UTF-8
        CharBuffer data = utf8charset.decode(inputBuffer);
        return data.toString();
    }

    /**
     * Set ical4j event dates to JsonObject with moment.ts formatting
     * @param event ical4j filled event
     * @param jsonEvent JsonObject event to fill
     */
    private void setEventDates(VEvent event, JsonObject jsonEvent) throws UnhandledEventException {
        // get DTSTART;VALUE parameter
        String dtStartValue = event.getStartDate().getParameter(Parameter.VALUE) != null ? event.getStartDate().getParameter(Parameter.VALUE).getValue() : "";
        // check if DTSTART;VALUE=DATE
        boolean allDay = dtStartValue.equals("DATE");
        Date startDate = event.getStartDate().getDate();
        Date endDate = event.getEndDate().getDate();

        String startMoment = formatIcsDate(startDate);
        String endMoment = formatIcsDate(endDate);

        // If allDay, set Hours to 0 instead of 1
        if (allDay) {
            java.util.Calendar calendar = new GregorianCalendar();
            // Start Date
            calendar.setTime(startDate);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 5);
            startMoment = MOMENT_FORMAT.format(calendar.getTime());
            // End Date
            calendar.setTime(endDate);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 18);
            calendar.add(java.util.Calendar.DATE, -1);
            endMoment = MOMENT_FORMAT.format(calendar.getTime());
            jsonEvent.put("allday", allDay);
        }
        // Put dates to jsonEvent
        jsonEvent.put("startMoment", startMoment);
        jsonEvent.put("endMoment", endMoment);
    }

    /**
     * Init ical4j calendar properties (ProdId, Version, CalScale)
     * @param calendar Ical4j calendar
     */
    private void initCalendarProperties(Calendar calendar) {
        calendar.getProperties().add(new ProdId("-//OpenENT Calendar 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
    }

    /**
     * Add an all day event to ical4j calendar
     * @param calendar Ical4j calendar
     * @param startDate Event start date
     * @param endDate Event end date
     * @param title Event title
     * @throws CalendarException
     */
    private void addAllDayEvent(Calendar calendar, java.util.Date startDate, java.util.Date endDate, String title, String icsUid,
                                String location, String description) throws CalendarException {
        VEvent event = new VEvent(new Date(startDate.getTime()), new Date(endDate.getTime()), title);
        try {
            Uid uid = new Uid();
            uid.setValue(icsUid);
            event.getProperties().add(uid);
            if( location != null )  {
                event.getProperties().add(new Location(location));
            }
            if( description != null ) {
                event.getProperties().add(new Description(description));
            }
            calendar.getComponents().add(event);
        } catch (Exception e) {
            throw new CalendarException(e);
        }
    }

    /**
     * Add an event to ical4j calendar
     * @param calendar Ical4j calendar
     * @param startDate Event start date
     * @param endDate Event end date
     * @param title Event title
     * @throws CalendarException
     */
    private void addEvent(Calendar calendar, String startDate, String endDate, String title, String icsUid, String location, String description) throws CalendarException {
        DateTime startDateTime = null;
        DateTime endDateTime = null;
        try {
            startDateTime = new DateTime(startDate, DateUtils.DATE_FORMAT_UTC, true);
            endDateTime = new DateTime(endDate, DateUtils.DATE_FORMAT_UTC, true);
        } catch (ParseException e) {
            String message = String.format("[Calendar@%s::addEvent] An error has occured during date formatting: %s",
                    this.getClass().getSimpleName(), e.getMessage());
            log.error(message, e.getMessage());
            throw new CalendarException(e);
        }
        VEvent event = new VEvent(startDateTime, endDateTime, title);

        try {
            Uid uid = new Uid();
            uid.setValue(icsUid);
            event.getProperties().add(uid);
            if( location != null )  {
                event.getProperties().add(new Location(location));
            }
            if( description != null ) {
                event.getProperties().add(new Description(description));
            }
            calendar.getComponents().add(event);
        } catch (Exception e) {
            throw new CalendarException(e);
        }
    }

    /**
     * Format date depending on format "yyyyMMdd'T'HHmmss'Z'"
     * @param date the date {@link java.util.Date}
     * @return {@link String} the date in the right format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
     * @throws UnhandledEventException
     */
    private static String formatIcsDate (java.util.Date date) throws UnhandledEventException {
        java.util.Date newStartDate;
        try {
            newStartDate = new SimpleDateFormat(DateUtils.ICAL_DATE_FORMAT).parse(date.toString());
        } catch (ParseException e) {
            newStartDate = formatIcsAlldayDate(date, e);
        }
        return new SimpleDateFormat(DateUtils.DATE_FORMAT_UTC).format(newStartDate);
    }

    /**
     * Format date depending on format "yyyyMMdd"
     * @param date the date {@link java.util.Date}
     * @param e1 {@link ParseException} the exception from the previous parse error
     * @return {@link java.util.Date} the date
     * @throws UnhandledEventException
     */
    private static java.util.Date formatIcsAlldayDate(java.util.Date date, ParseException e1) throws UnhandledEventException {
        java.util.Date newStartDate;
        try {
            newStartDate = new SimpleDateFormat(DateUtils.ICAL_ALLDAY_FORMAT).parse(date.toString());
        } catch (ParseException e2) {
            String errorMessage = String.format("[Calendar@%s::formatIcsAlldayDate] Errors have occured during ical import: %s %s",
                    ICalHandler.class.getSimpleName(), e1.getMessage(), e2.getMessage());
            log.error(errorMessage);
            throw new UnhandledEventException("calendar.ical.event.slot.problem");
        }
        return newStartDate;
    }

}
