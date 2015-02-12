package net.atos.entng.calendar.controllers;

import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;


public class CalendarController extends MongoDbControllerHelper {
    
    public CalendarController(String collection) {
        super(collection);
    }

    @Get("")
   	@SecuredAction("calendar.view")
	public void view(HttpServerRequest request) {
    	renderView(request);
    }

    @Get("/calendars")
    @SecuredAction("calendar.view")
    public void listCalendars(HttpServerRequest request) {
        list(request);
    }
    
    @Post("/calendars")
    @SecuredAction("calendar.create")
    public void createCalendar(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "calendar", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                create(request);
            }
        });
    }
    
    @Put("/:id")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void updateCalendar(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "calendar", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                update(request);
            }
        });
    }
    
    @Delete("/:id")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void deleteCalendar(HttpServerRequest request) {
        delete(request);
    }
    
    @Get("/share/json/:id")
    @ApiDoc("Share calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareCalendar(final HttpServerRequest request) {
        shareJson(request, false);
    }

    @Put("/share/json/:id")
    @ApiDoc("Share calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void shareCalendarSubmit(final HttpServerRequest request) {
        shareJsonSubmit(request, "notify-timeline-shared.html", false);
    }

    @Put("/share/remove/:id")
    @ApiDoc("Remove calendar by id.")
    @SecuredAction(value = "calendar.manager", type = ActionType.RESOURCE)
    public void removeShareCalendar(final HttpServerRequest request) {
        removeShare(request, false);
    }

}
