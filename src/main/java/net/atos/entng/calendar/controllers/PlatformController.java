package net.atos.entng.calendar.controllers;

import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import net.atos.entng.calendar.core.constants.Actions;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.PlatformService;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbControllerHelper;

public class PlatformController extends MongoDbControllerHelper {

    private final PlatformService platformService;


    public PlatformController(String collection, ServiceFactory serviceFactory) {
        super(collection);
        this.platformService = serviceFactory.platformService();
    }

    @Get("/platforms/:platformId")
    @ApiDoc("Retrieve a platform by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void retrievePlatform(HttpServerRequest request) {
        final String id = request.params().get(Field.PLATFORM_ID);
        platformService.retrieve(id)
                .onSuccess(res -> {
                    renderJson(request, res);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Get("/platforms")
    @ApiDoc("List all platform")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void listPlatforms(HttpServerRequest request) {
        platformService.retrieveAll()
                .onSuccess(res -> {
                    renderJson(request, res);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Post("/platforms")
    @ApiDoc("Add a platform")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.CREATE_PLATFORM)
    public void createPlatforms(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            platformService.create(body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }

    @Delete("/platforms/:platformId")
    @ApiDoc("Delete a platform by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.DELETE_PLATFORM)
    public void deletePlatforms(HttpServerRequest request) {
        final String id = request.params().get(Field.PLATFORM_ID);
        platformService.delete(id)
                .onSuccess(res -> {
                    Renders.ok(request);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Put("/platforms/:platformId")
    @ApiDoc("Update a platform by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.UPDATE_PLATFORM)
    public void updatePlatforms(HttpServerRequest request) {
        final String id = request.params().get(Field.PLATFORM_ID);
        RequestUtils.bodyToJson(request, body -> {
            platformService.update(id, body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }
}
