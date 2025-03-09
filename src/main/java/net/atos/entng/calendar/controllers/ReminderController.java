package net.atos.entng.calendar.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserUtils;

public class ReminderController  extends MongoDbControllerHelper {
    private final ReminderService reminderService;


    public ReminderController(String collection, ServiceFactory serviceFactory) {
        super(collection);
        this.reminderService = serviceFactory.reminderService();
    }

    @Post("/event/:eventId/reminder")
    @ApiDoc("Add a reminder")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.CREATE_REMINDER)
    public void createReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            RequestUtils.bodyToJson(request, body -> {
                if (Boolean.FALSE.equals(body.containsKey(Field.EVENTID))) {
                    body.put(Field.EVENTID, eventId);
                }
                if (Boolean.FALSE.equals(body.containsKey(Field.OWNER))) {
                    body.put(Field.OWNER, new JsonObject()
                            .put(Field.USERID, user.getUserId())
                            .put(Field.DISPLAYNAME, user.getUsername()));
                }
                reminderService.create(body)
                        .onSuccess(res -> {
                            Renders.ok(request);
                        })
                        .onFailure(err -> {
                            renderError(request);
                        });
            });
        });
    }

    @Put("/event/:eventId/reminder/:reminderId")
    @ApiDoc("Update a reminder by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.UPDATE_REMINDER)
    public void updateReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        final String id = request.params().get(Field.REMINDERID);
        RequestUtils.bodyToJson(request, body -> {
            reminderService.update(eventId, id, body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }

    @Delete("/event/:eventId/reminder/:reminderId")
    @ApiDoc("Delete a reminder by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.DELETE_REMINDER)
    public void deleteReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        final String id = request.params().get(Field.REMINDERID);
        reminderService.delete(eventId, id)
                .onSuccess(res -> {
                    Renders.ok(request);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }
}
