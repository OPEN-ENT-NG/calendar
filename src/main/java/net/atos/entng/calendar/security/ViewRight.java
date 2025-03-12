package net.atos.entng.calendar.security;

import net.atos.entng.calendar.core.constants.*;
import fr.wseduc.webutils.http.*;
import io.vertx.core.*;
import io.vertx.core.http.*;
import net.atos.entng.calendar.helpers.WorkflowHelper;
import org.entcore.common.http.filter.*;
import org.entcore.common.user.*;

public class ViewRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(user, Rights.VIEW));
    }
}