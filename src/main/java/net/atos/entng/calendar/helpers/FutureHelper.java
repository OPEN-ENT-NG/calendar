package net.atos.entng.calendar.helpers;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;

import java.util.List;

public class FutureHelper {
    private static final Logger log = LoggerFactory.getLogger(FutureHelper.class);


    private FutureHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return CompositeFutureImpl.all(futures.toArray(new Future[0]));
    }

    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[0]));
    }

    public static Handler<Either<String, JsonArray>> handlerJsonObject(Promise<JsonArray> promise) {
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

}
