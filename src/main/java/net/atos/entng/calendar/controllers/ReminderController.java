package net.atos.entng.calendar.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbControllerHelper;

public class ReminderController  extends MongoDbControllerHelper {
    private final ReminderService reminderService;


    public ReminderController(String collection, ServiceFactory serviceFactory) {
        super(collection);
        this.reminderService = serviceFactory.reminderService();
    }

    @Post("/reminder")
    @ApiDoc("Add a reminder")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.CREATE_REMINDER)
    public void createReminder(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            reminderService.create(body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }

    @Delete("/reminder/:reminderId")
    @ApiDoc("Delete a reminder by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.DELETE_REMINDER)
    public void deleteReminder(HttpServerRequest request) {
        final String id = request.params().get(Field._ID);
        reminderService.delete(id)
                .onSuccess(res -> {
                    Renders.ok(request);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Put("/reminder/:reminderId")
    @ApiDoc("Update a reminder by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.UPDATE_REMINDER)
    public void updateReminder(HttpServerRequest request) {
        final String id = request.params().get(Field._ID);
        RequestUtils.bodyToJson(request, body -> {
            reminderService.update(id, body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }
}
