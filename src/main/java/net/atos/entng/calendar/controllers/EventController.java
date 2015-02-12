package net.atos.entng.calendar.controllers;

import net.atos.entng.calendar.helpers.EventHelper;

import org.entcore.common.service.CrudService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;


public class EventController extends BaseController {
    
    private final EventHelper eventHelper;

    public EventController(String collection, CrudService eventService) {
        eventHelper = new EventHelper(collection, eventService);
    }

    @Get("/:id/events")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getEvents(HttpServerRequest request) {
        eventHelper.list(request);
    }

    @Post("/:id/events")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void createEvent(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                eventHelper.create(request);
            }
        });
    }
    
    @Get("/:id/event/:eventid")
    @SecuredAction(value = "calendar.read", type = ActionType.RESOURCE)
    public void getEvent(HttpServerRequest request) {
        eventHelper.retrieve(request);
    }
    
    @Put("/:id/event/:eventid")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void updateEvent(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                eventHelper.update(request);
            }
        });
    }
    
    @Delete("/:id/event/:eventid")
    @SecuredAction(value = "calendar.contrib", type = ActionType.RESOURCE)
    public void deleteEvent(HttpServerRequest request) {
        eventHelper.delete(request);
    }
}
