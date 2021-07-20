package net.atos.entng.calendar.security;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.atos.entng.calendar.Calendar;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

public class ShareEventConf implements ResourcesProvider {

    private MongoDbConf conf = MongoDbConf.getInstance();

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String id = request.params().get(conf.getResourceIdLabel());
        if (id != null && !id.trim().isEmpty()) {
            List<DBObject> groups = new ArrayList<>();
            String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
            groups.add(QueryBuilder.start("userId").is(user.getUserId())
                    .put(sharedMethod).is(true).get());
            for (String gpId: user.getGroupsIds()) {
                groups.add(QueryBuilder.start("groupId").is(gpId)
                        .put(sharedMethod).is(true).get());
            }

            QueryBuilder query = QueryBuilder.start("_id").is(id).or(
                    QueryBuilder.start("owner.userId").is(user.getUserId()).get(),
                    QueryBuilder.start("shared").elemMatch(
                            new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get()
            );


            MongoAppFilter.executeCountQuery(request, conf.getCollection(), MongoQueryBuilder.build(query), 1, cal-> {
                if (Boolean.TRUE.equals(cal) && cal) {
                    handler.handle(true);
                } else {
                    MongoAppFilter.executeCountQuery(request, Calendar.CALENDAR_EVENT_COLLECTION, MongoQueryBuilder.build(query), 1, handler);
                }
            });

        } else {
                handler.handle(false);
            }
        }
}
