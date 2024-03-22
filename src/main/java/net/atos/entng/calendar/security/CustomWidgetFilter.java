package net.atos.entng.calendar.security;

import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import org.bson.conversions.Bson;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * Check if user have access to calendars
 */
public class CustomWidgetFilter implements ResourcesProvider {


    protected static final Logger log = LoggerFactory.getLogger(CustomWidgetFilter.class);

    @Override
    public void authorize(final HttpServerRequest request, Binding binding, UserInfos user, final Handler<Boolean> handler) {
        List<String> ids = request.params().getAll("calendarId");
        request.pause();
        if (ids != null && !ids.isEmpty()) {
            log.debug("DEBUG CustomWidgetFilter calendarId : " + ids);
            final Bson queryShared = or(
                    eq("shared.userId",user.getUserId()),
                    in("shared.groupId",user.getGroupsIds())
            );

            final Bson queryOr = or(
                    queryShared,
                    eq("owner.userId",user.getUserId())
            );

            final Bson queryAnd = and(
                    queryOr,
                    in("_id", ids)
            );

            MongoAppFilter.executeCountQuery(request, "calendar",
                    MongoQueryBuilder.build(queryAnd), ids.size(), new Handler<Boolean>() {
                        @Override
                        public void handle(Boolean result) {
                            request.resume();
                            log.debug("DEBUG CustomWidgetFilter result : " + result);
                            handler.handle(result);
                        }
                    });
        } else {
            request.resume();
            handler.handle(false);
        }
    }
}
