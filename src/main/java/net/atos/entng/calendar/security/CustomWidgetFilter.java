package net.atos.entng.calendar.security;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;

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
            log.debug("DEBUG CustomWidgetFilter calendarId : " + ids.toString());
            QueryBuilder queryShared = QueryBuilder.start().or(
                    QueryBuilder.start("shared.userId").is(user.getUserId()).get(),
                    QueryBuilder.start("shared.groupId").in(user.getGroupsIds().toArray(new String[0])).get()
            );

            QueryBuilder queryOr = QueryBuilder.start().or(
                    queryShared.get(),
                    QueryBuilder.start("owner.userId").is(user.getUserId()).get()
            );

            QueryBuilder queryAnd = QueryBuilder.start().and(
                    queryOr.get(),
                    QueryBuilder.start("_id").in(ids.toArray(new String[0])).get()
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
