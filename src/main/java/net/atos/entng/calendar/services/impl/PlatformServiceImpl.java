package net.atos.entng.calendar.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.services.PlatformService;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;
import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

public class PlatformServiceImpl implements PlatformService {

    private final MongoDb mongo;
    private final String collection;
    protected static final Logger log = LoggerFactory.getLogger(PlatformServiceImpl.class);

    public PlatformServiceImpl(String collection, MongoDb mongo) {
        this.mongo = mongo;
        this.collection = collection;
    }

    @Override
    public Future<JsonObject> retrieve(String id) {
        Promise<JsonObject> promise = Promise.promise();

        final Bson query = eq(Field._ID, id);


        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::retrieve] An error has occurred while retrieving a platform: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }

    @Override
    public Future<JsonArray> retrieveAll() {
        Promise<JsonArray> promise = Promise.promise();
        mongo.find(this.collection, new JsonObject(), validResultsHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::retrieveAll] An error has occurred while retrieving all platform: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> create(JsonObject body) {
        Promise<Void> promise = Promise.promise();

        mongo.insert(this.collection, body, validResultHandler(event -> {
            if (event.isLeft()){
                String errMessage = String.format("[Calendar@%s::create] An error has occurred while creating a new platform: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> update(String id, JsonObject body) {
        Promise<Void> promise = Promise.promise();

        final Bson query = eq(Field._ID, id);

        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attribute : body.fieldNames()) {
            modifier.set(attribute, body.getValue(attribute));
        }

        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::update] An error has occurred while deleting a platform: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));

        return promise.future();
    }

    @Override
    public Future<Void> delete(String id) {
        Promise<Void> promise = Promise.promise();


        final Bson query = eq(Field._ID, id);

        mongo.delete(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::delete] An error has occurred while deleting a platform: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                log.error(errMessage, event.left().getValue());
                promise.fail(event.left().getValue());
            }else{
                promise.complete();
            }
        }));
        return promise.future();
    }
}
