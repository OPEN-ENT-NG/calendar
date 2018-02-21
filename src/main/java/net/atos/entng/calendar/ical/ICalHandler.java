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

import io.vertx.core.AbstractVerticle;
import net.atos.entng.calendar.exception.CalendarException;
import net.atos.entng.calendar.exception.UnhandledEventException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * ICal worker to handle ICS parsing and generation
 * @author Atos
 */
public class ICalHandler extends AbstractVerticle implements Handler<Message<JsonObject>> {

    public static final String ICAL_HANDLER_ADDRESS = "ical.handler";

    /**
     * Actions handled by worker
     */
    public static final String ACTION_PUT = "put";
    public static final String ACTION_GET = "get";

    /**
     * Simple Date formatter in moment.js format
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
                java.util.Date startDate = MOMENT_FORMAT.parse(startMoment);
                java.util.Date endDate = MOMENT_FORMAT.parse(endMoment);
                if (allDay) {
                    addAllDayEvent(calendar, startDate, title, icsUid, location, description);
                } else {
                    addEvent(calendar, startDate, endDate, title, icsUid, location, description);
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
        String icsContent = message.body().getString("ics");
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
                        setEventProperties(event, jsonEvent);
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
    private void setEventProperties(VEvent event, JsonObject jsonEvent) {

        String title = encodeString(event.getSummary().getValue());
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
        Charset iso88591charset = Charset.forName("ISO-8859-1");

        ByteBuffer inputBuffer = ByteBuffer.wrap(toEncode.getBytes());

        // decode UTF-8
        CharBuffer data = utf8charset.decode(inputBuffer);

        // encode ISO-8559-1
        ByteBuffer outputBuffer = iso88591charset.encode(data);
        byte[] outputData = outputBuffer.array();
        return new String(outputData);
    }

    /**
     * Set ical4j event dates to JsonObject with moment.js formatting
     * @param event ical4j filled event
     * @param jsonEvent JsonObject event to fill
     */
    private void setEventDates(VEvent event, JsonObject jsonEvent) {
        // get DTSTART;VALUE parameter
        String dtStartValue = event.getStartDate().getParameter(Parameter.VALUE) != null ? event.getStartDate().getParameter(Parameter.VALUE).getValue() : "";
        // check if DTSTART;VALUE=DATE
        boolean allDay = dtStartValue.equals("DATE");
        Date startDate = event.getStartDate().getDate();
        Date endDate = event.getEndDate().getDate();
        String startMoment = MOMENT_FORMAT.format(startDate);
        String endMoment = MOMENT_FORMAT.format(endDate);

        // If allDay, set Hours to 0 instead of 1
        if (allDay) {
            java.util.Calendar calendar = new GregorianCalendar();
            // Start Date
            calendar.setTime(startDate);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            startMoment = MOMENT_FORMAT.format(calendar.getTime());
            // End Date
            calendar.setTime(endDate);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
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
     * @param date Event date
     * @param title Event title
     * @throws CalendarException
     */
    private void addAllDayEvent(Calendar calendar, java.util.Date date, String title, String icsUid, String location, String description) throws CalendarException {
        VEvent event = new VEvent(new Date(date.getTime()), title);
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
    private void addEvent(Calendar calendar, java.util.Date startDate, java.util.Date endDate, String title, String icsUid, String location, String description) throws CalendarException {
        DateTime startDateTime = new DateTime(startDate);
        DateTime endDateTime = new DateTime(endDate);
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

}
