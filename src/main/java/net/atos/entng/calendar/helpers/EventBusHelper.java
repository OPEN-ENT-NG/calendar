package net.atos.entng.calendar.helpers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;

public class EventBusHelper {
    private static final Logger log = LoggerFactory.getLogger(EventBusHelper.class);

    public static void eventBusError(String logMessage, String replayErrorMessage, Message<JsonObject> message) {
        log.error(logMessage);
        message.reply(new JsonObject().put(Field.STATUS, Field.ERROR).put(Field.MESSAGE, replayErrorMessage));
    }

    public static Future<JsonObject> requestJsonObject(EventBus eb, String address, JsonObject action) {
        Promise<JsonObject> promise = Promise.promise();
        eb.request(address, action, FutureHelper.messageJsonObjecyHandler(FutureHelper.handlerJsonObject(promise)));
        return promise.future();
    }

    public static Future<JsonArray> requestJsonArray(EventBus eb, String address, JsonObject action) {
        Promise<JsonArray> promise = Promise.promise();
        eb.request(address, action, FutureHelper.messageJsonArrayHandler(FutureHelper.handlerJsonArray(promise)));
        return promise.future();
    }
}
