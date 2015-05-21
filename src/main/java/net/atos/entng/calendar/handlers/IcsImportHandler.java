package net.atos.entng.calendar.handlers;

import net.atos.entng.calendar.services.EventService;

import org.apache.commons.lang.mutable.MutableInt;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class IcsImportHandler implements Handler<Either<String, JsonObject>> {

    private EventService eventService;
    private String calendarId;
    private JsonObject calendarEvent;
    private UserInfos user;
    private Handler<Either<String, JsonObject>> handler;
    final MutableInt i = new MutableInt();
    private JsonObject result;
    
    
    public IcsImportHandler(EventService eventService, String calendarId, JsonObject calendarEvent, UserInfos user, Handler<Either<String, JsonObject>> handler, JsonObject result) {
       this.eventService = eventService;
       this.calendarId = calendarId;
       this.calendarEvent = calendarEvent;
       this.user = user;
       this.handler = handler;
       this.result = result;       
       i.add(result.getNumber("createdEvents"));

    }
    
    @Override
    public void handle(Either<String, JsonObject> event) {
        // No existing event found
        if (event.isRight() && event.right().getValue().size() == 0) {
            eventService.create(calendarId, calendarEvent, user, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    i.subtract(1);
                    // There is no more events to create
                    if (i.toInteger() == 0) {
                        handler.handle(new Either.Right<String, JsonObject>(result));
                    }
                }
            });
        } // Existing event found
        else if (event.isRight() && event.right().getValue().size() > 0) {
            eventService.update(calendarId, event.right().getValue().getString("_id"), calendarEvent, user, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    i.subtract(1);
                    // There is no more events to create
                    if (i.toInteger() == 0) {
                        handler.handle(new Either.Right<String, JsonObject>(result));
                    }
                }
            });
        } // An error occured while retrieving the event
        else {
            i.subtract(1);
            if (i.toInteger() == 0) {
                handler.handle(new Either.Right<String, JsonObject>(result));
            }
        }
    }
}