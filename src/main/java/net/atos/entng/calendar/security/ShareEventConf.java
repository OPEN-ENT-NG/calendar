package net.atos.entng.calendar.security;

import com.mongodb.DBObject;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.atos.entng.calendar.Calendar;
import org.bson.conversions.Bson;
import org.entcore.common.http.filter.MongoAppFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

public class ShareEventConf implements ResourcesProvider {

    private MongoDbConf conf = MongoDbConf.getInstance();

    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String id = request.params().get(conf.getResourceIdLabel());
        if (id != null && !id.trim().isEmpty()) {
            final List<Bson> groups = new ArrayList<>();
            String sharedMethod = binding.getServiceMethod().replaceAll("\\.", "-");
            groups.add(and(eq("userId", user.getUserId()), eq(sharedMethod, true)));
            for (String gpId: user.getGroupsIds()) {
                groups.add(and(eq("groupId", gpId), eq(sharedMethod, true)));
            }

            final Bson query = and(
              eq("_id", id),
              or(
                    eq("owner.userId", user.getUserId()),
                    elemMatch("shared", or(groups))
              )
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
