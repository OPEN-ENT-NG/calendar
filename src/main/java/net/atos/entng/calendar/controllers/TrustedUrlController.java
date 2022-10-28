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
import net.atos.entng.calendar.services.TrustedUrlService;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.mongodb.MongoDbControllerHelper;

public class TrustedUrlController extends MongoDbControllerHelper {

    private final TrustedUrlService trustedUrlService;


    public TrustedUrlController(String collection, ServiceFactory serviceFactory) {
        super(collection);
        this.trustedUrlService = serviceFactory.urlService();
    }

    @Get("/trusted/:urlId")
    @ApiDoc("Retrieve a trusted url by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void retrieveTrustedUrl(HttpServerRequest request) {
        final String id = request.params().get(Field.URLID);
        trustedUrlService.retrieve(id)
                .onSuccess(res -> {
                    renderJson(request, res);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Get("/trusted")
    @ApiDoc("List all trusted url by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void listTrustedUrl(HttpServerRequest request) {
        trustedUrlService.retrieveAll()
                .onSuccess(res -> {
                    renderJson(request, res);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Post("/trusted")
    @ApiDoc("Add a trusted url")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.CREATE_TRUSTED)
    public void createTrustedUrl(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, body -> {
            trustedUrlService.create(body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }

    @Delete("/trusted/:urlId")
    @ApiDoc("Delete a trusted url by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.DELETE_TRUSTED)
    public void deleteTrustedUrl(HttpServerRequest request) {
        final String id = request.params().get(Field.URLID);
        trustedUrlService.delete(id)
                .onSuccess(res -> {
                    Renders.ok(request);
                })
                .onFailure(err -> {
                    renderError(request);
                });
    }

    @Put("/trusted/:urlId")
    @ApiDoc("Update a trusted url by id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @Trace(Actions.UPDATE_TRUSTED)
    public void updateTrustedUrl(HttpServerRequest request) {
        final String id = request.params().get(Field.URLID);
        RequestUtils.bodyToJson(request, body -> {
            trustedUrlService.update(id, body)
                    .onSuccess(res -> {
                        Renders.ok(request);
                    })
                    .onFailure(err -> {
                        renderError(request);
                    });
        });
    }
}
