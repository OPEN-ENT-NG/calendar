package net.atos.entng.calendar.services.impl;

import com.mongodb.QueryBuilder;
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
import net.atos.entng.calendar.services.TrustedUrlService;

import static org.entcore.common.mongodb.MongoDbResult.validResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

public class TrustedUrlServiceImpl implements TrustedUrlService {

    private final MongoDb mongo;
    private final String collection;
    protected static final Logger log = LoggerFactory.getLogger(TrustedUrlServiceImpl.class);

    public TrustedUrlServiceImpl(String collection, MongoDb mongo) {
        this.mongo = mongo;
        this.collection = collection;
    }

    @Override
    public Future<JsonObject> retrieve(String id) {
        Promise<JsonObject> promise = Promise.promise();

        QueryBuilder query = QueryBuilder.start(Field._ID).is(id);


        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::retrieve] An error has occurred while retrieving a trusted urls: %s",
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
                String errMessage = String.format("[Calendar@%s::retrieveAll] An error has occurred while retrieving all trusted url: %s",
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
                String errMessage = String.format("[Calendar@%s::create] An error has occurred while creating a new trusted url: %s",
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

        QueryBuilder query = QueryBuilder.start(Field._ID).is(id);

        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        for (String attribute : body.fieldNames()) {
            modifier.set(attribute, body.getValue(attribute));
        }

        mongo.update(this.collection, MongoQueryBuilder.build(query), modifier.build(), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::update] An error has occurred while deleting a trusted url: %s",
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


        QueryBuilder query = QueryBuilder.start(Field._ID).is(id);

        mongo.delete(this.collection, MongoQueryBuilder.build(query), validResultHandler(event -> {
            if(event.isLeft()){
                String errMessage = String.format("[Calendar@%s::delete] An error has occurred while deleting a trusted url: %s",
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
