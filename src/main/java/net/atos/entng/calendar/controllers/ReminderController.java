package net.atos.entng.calendar.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.ReminderHelper;
import net.atos.entng.calendar.models.reminders.ReminderFrontEndModel;
import net.atos.entng.calendar.security.ViewRight;
import net.atos.entng.calendar.services.ReminderService;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.PlatformServiceImpl;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

public class ReminderController extends MongoDbControllerHelper {
    private final ReminderService reminderService;
    private final ReminderHelper reminderHelper;
    protected static final Logger log = LoggerFactory.getLogger(PlatformServiceImpl.class);

    public ReminderController(String collection, ServiceFactory serviceFactory, EventBus eb) {
        super(collection);
        this.reminderService = serviceFactory.reminderService();
        this.reminderHelper = new ReminderHelper(serviceFactory, eb);
    }

    @Post("/event/:eventId/reminder")
    @ApiDoc("Add a reminder")
    @ResourceFilter(ViewRight.class)
    @Trace(Actions.CREATE_REMINDER)
    public void createReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            RequestUtils.bodyToJson(request, body -> {
//                reminderHelper.getFormattedReminder(body, eventId, user)
//                        .compose(reminder ->  reminderService.create(reminder))
                reminderHelper.remindersEventFormActions(Actions.CREATE_REMINDER, eventId, user, body)
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
    @ResourceFilter(ViewRight.class)
    @Trace(Actions.UPDATE_REMINDER)
    public void updateReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        final String id = request.params().get(Field.REMINDERID);
        UserUtils.getAuthenticatedUserInfos(eb, request).onSuccess(user -> {
            RequestUtils.bodyToJson(request, body -> {
//                reminderHelper.getFormattedReminder(body, eventId, user)
//                        .compose(reminder ->  reminderService.update(eventId, id, reminder))
                reminderHelper.remindersEventFormActions(Actions.UPDATE_REMINDER, eventId, user, body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
            });
        });
    }

    @Delete("/event/:eventId/reminder/:reminderId")
    @ApiDoc("Delete a reminder by id")
    @ResourceFilter(ViewRight.class)
    @Trace(Actions.DELETE_REMINDER)
    public void deleteReminder(HttpServerRequest request) {
        final String eventId =  request.params().get(Field.EVENTID);
        final String id = request.params().get(Field.REMINDERID);
//        reminderService.delete(eventId, id)
        reminderHelper.remindersEventFormActions(Actions.DELETE_REMINDER, eventId, id)
                .onSuccess(res -> {
                    Renders.ok(request);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }
}
