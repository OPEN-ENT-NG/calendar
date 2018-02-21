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

package net.atos.entng.calendar;

import fr.wseduc.webutils.Server;
import io.vertx.core.DeploymentOptions;
import net.atos.entng.calendar.controllers.CalendarController;
import net.atos.entng.calendar.controllers.EventController;
import net.atos.entng.calendar.event.CalendarRepositoryEvents;
import net.atos.entng.calendar.event.CalendarSearchingEvents;
import net.atos.entng.calendar.ical.ICalHandler;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.ShareAndOwner;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.CrudService;
import io.vertx.core.eventbus.EventBus;


public class Calendar extends BaseServer {
    public final static String CALENDAR_NAME = "CALENDAR";

    public final static String CALENDAR_COLLECTION = "calendar";
    public final static String CALENDAR_EVENT_COLLECTION = "calendarevent";

    public static final String MANAGE_RIGHT_ACTION = "net-atos-entng-calendar-controllers-CalendarController|updateCalendar";

    
    @Override
    public void start() throws Exception {
        super.start();
        CrudService eventService = new EventServiceMongoImpl(CALENDAR_EVENT_COLLECTION, vertx.eventBus());
        
        final MongoDbConf conf = MongoDbConf.getInstance();
        conf.setCollection(CALENDAR_COLLECTION);
        conf.setResourceIdLabel("id");
        
        
        setDefaultResourceFilter(new ShareAndOwner());
        vertx.deployVerticle(ICalHandler.class.getName(), new DeploymentOptions().setWorker(true).setConfig(config));

        setRepositoryEvents(new CalendarRepositoryEvents());

        if (config.getBoolean("searching-event", true)) {
            setSearchingEvents(new CalendarSearchingEvents());
        }
        EventBus eb = Server.getEventBus(vertx);
        final TimelineHelper timelineHelper = new TimelineHelper(vertx, eb, config);
        addController(new CalendarController(CALENDAR_COLLECTION));
        addController(new EventController(CALENDAR_EVENT_COLLECTION, eventService, timelineHelper));
    }
    
}