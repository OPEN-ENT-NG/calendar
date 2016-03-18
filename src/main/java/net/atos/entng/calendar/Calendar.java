package net.atos.entng.calendar;

import net.atos.entng.calendar.controllers.CalendarController;
import net.atos.entng.calendar.controllers.EventController;
import net.atos.entng.calendar.event.CalendarRepositoryEvents;
import net.atos.entng.calendar.event.CalendarSearchingEvents;
import net.atos.entng.calendar.ical.ICalHandler;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.ShareAndOwner;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.service.CrudService;


public class Calendar extends BaseServer {
    public final static String CALENDAR_NAME = "CALENDAR";

    public final static String CALENDAR_COLLECTION = "calendar";
    public final static String CALENDAR_EVENT_COLLECTION = "calendarevent";

    public static final String MANAGE_RIGHT_ACTION = "net-atos-entng-calendar-controllers-CalendarController|updateCalendar";

    
    @Override
    public void start() {
        super.start();
        CrudService eventService = new EventServiceMongoImpl(CALENDAR_EVENT_COLLECTION, vertx.eventBus());
        
        final MongoDbConf conf = MongoDbConf.getInstance();
        conf.setCollection(CALENDAR_COLLECTION);
        conf.setResourceIdLabel("id");
        
        
        setDefaultResourceFilter(new ShareAndOwner());
        container.deployWorkerVerticle(ICalHandler.class.getName(), config);

        setRepositoryEvents(new CalendarRepositoryEvents());
        setSearchingEvents(new CalendarSearchingEvents());
        
        addController(new CalendarController(CALENDAR_COLLECTION));
        addController(new EventController(CALENDAR_EVENT_COLLECTION, eventService));
    }
    
}