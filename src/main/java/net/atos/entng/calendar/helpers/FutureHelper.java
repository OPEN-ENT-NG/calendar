package net.atos.entng.calendar.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;

import java.util.ArrayList;
import java.util.List;

public class FutureHelper {
    private static final Logger log = LoggerFactory.getLogger(FutureHelper.class);


    private FutureHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return Future.all(futures);
    }

    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return Future.join(futures);
    }

    public static Handler<Either<String, JsonArray>> handlerJsonArray(Promise<JsonArray> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[Calendar@%s::handlerJsonArray]: %s",
                        EventHelper.class.getSimpleName(), event.left().getValue());
                log.error(message);
                promise.fail(event.left().getValue());
            }
        };
    }

    public static Handler<Either<String, JsonObject>> handlerJsonObject(Promise<JsonObject> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[Calendar@%s::handlerJsonObject]: %s",
                        EventHelper.class.getSimpleName(), event.left().getValue());
                log.error(message);
                promise.fail(event.left().getValue());
            }
        };
    }

    public static Handler<Either<String, List<JsonObject>>> handlerListJsonObject(Promise<List<JsonObject>> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[Calendar@%s::handlerListJsonObject]: %s",
                        EventHelper.class.getSimpleName(), event.left().getValue());
                log.error(message);
                promise.fail(event.left().getValue());
            }
        };
    }

    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonArrayHandler(Handler<Either<String, JsonArray>> handler) {
        return event -> {
            if (event.succeeded()) {
                handler.handle(new Either.Right<>(event.result().body().getJsonArray(Field.RESULT)));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
                return;
            }
        };
    }

//    public static <T> Handler<Either<String, T>> messagePromiseJsonObjectHandler(Promise<T> promise, String errorMessage) {
//        return event -> {
//            if (event.isRight()) {
//                promise.complete(event.right().getValue());
//            } else {
//                String message = String.format("[Calendar@%s::messagePromiseJsonObjectHandler]: %s",
//                        EventHelper.class.getSimpleName(), event.left().getValue());
//                log.error(message);
//                promise.fail(event.left().getValue());
//            }
//        };
//    }

//    public static Future<JsonObject> messagePromiseJsonObjectHandler(Either<String, JsonObject> event, String errorMessage) {
//        Promise<JsonObject> promise = Promise.promise();
//
//        if (event.isRight()) {
//            promise.complete(event.right().getValue());
//        } else {
//            String message = String.format("[Calendar@%s::messagePromiseJsonObjectHandler]: %s",
//                    EventHelper.class.getSimpleName(), event.left().getValue());
//            log.error(message);
//            promise.fail(event.left().getValue());
//        }
//
//        return promise.future();
//    }
    public static Handler<AsyncResult<JsonObject>> messagePromiseJsonObjectHandler(Promise<JsonObject> promise) {
        return event -> {
            if (event.succeeded()) {
                promise.complete(event.result());
            } else {
                promise.fail(event.cause());
            }
        };
    }


//    public static <T> Handler<AsyncResult<JsonObject>> messagePromiseJsonObjectHandler(Promise<JsonObject> promise) {
//        return FutureHelper.messagePromiseJsonObjectHandler(promise, null);
//    }


    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonObjectHandler(Handler<AsyncResult<JsonObject>> handler) {
        return event -> {
            if (event.succeeded()) {
                handler.handle(Future.succeededFuture(event.result().body().getJsonObject(Field.RESULT)));
            } else {
                handler.handle(Future.failedFuture(event.cause().getMessage()));
            }
        };
    }

    public static Handler<AsyncResult<Message<List<JsonObject>>>> messageListJsonObjectHandler(Handler<Either<String, List<JsonObject>>> handler) {
        return event -> {
            if (event.succeeded()) {
                handler.handle(new Either.Right<>(new ArrayList<>(event.result().body())));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        };
    }

}
