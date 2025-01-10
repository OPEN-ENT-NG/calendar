package net.atos.entng.calendar.services.impl;

import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.ReminderService;
import org.entcore.common.user.UserInfos;

import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;

public class ReminderServiceImpl implements ReminderService {
    private final MongoDb mongo;
    private final String collection;
    protected static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);

    public ReminderServiceImpl(String collection, MongoDb mongo) {
        this.mongo = mongo;
        this.collection = collection;
    }

    @Override
    public Future<JsonObject> getEventReminder(String eventId, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        final Bson query = and(
                eq(Field.EVENTID, eventId),
                eq(Field.USERID, user.getUserId())
        );

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::getEventReminder] An error has occurred while retrieving reminder: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }
}
