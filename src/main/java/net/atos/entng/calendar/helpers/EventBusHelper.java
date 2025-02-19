package net.atos.entng.calendar.helpers;

import io.vertx.core.eventbus.Message;
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


}