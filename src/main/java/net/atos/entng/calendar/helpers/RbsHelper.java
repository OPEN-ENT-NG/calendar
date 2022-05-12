package net.atos.entng.calendar.helpers;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import org.entcore.common.user.UserInfos;

import static fr.wseduc.webutils.http.Renders.getHost;
import static net.atos.entng.calendar.helpers.FutureHelper.*;

public class RbsHelper {

    private static String rbsAddress = "net.atos.entng.rbs";

    protected static final Logger log = LoggerFactory.getLogger(Renders.class);

    public static Future<Void> saveBookingsInRbs(HttpServerRequest request, JsonObject object, UserInfos user, JsonObject config, EventBus eb) {
        Promise<Void> promise = Promise.promise();

        Future<JsonArray> eventFuture = Future.succeededFuture();
        if (canEventHaveBooking(object, config)) {
            eventFuture = saveBookings(object, user, eb);
        }
        eventFuture
            .onComplete((res) -> {
                if(res.succeeded()) {
                    if (res.result() != null && !res.result().isEmpty()) {
                        object.put(Field.BOOKINGS, res.result());
                    } else {
                        object.remove(Field.BOOKINGS);
                        object.remove(Field.HASBOOKING);
                    }
                    promise.complete();
                } else {
                    String message = String.format("[Calendar@RbsHelper::saveBookingsInRbs] An error has occured" +
                            " when saving bookings: %s", res.failed());
                    log.error(message,res.failed());
                }
            });

        return promise.future();
    }

    /**
     * Checks if module has RBS booking right and if bookings can be saved
     * @param object the event to be saved
     * @return true if the bookings can be saved
     */
    public static boolean canEventHaveBooking(JsonObject object, JsonObject config) {
        return Boolean.TRUE.equals(config.getBoolean(Field.ENABLE_RBS))
                && Boolean.TRUE.equals(object.getBoolean(Field.HASBOOKING))
                && (object.getJsonObject(Field.BOOKINGS).size() > 0);
    }

    /**
     * Sends bookings to RBS to save them
     * @param object the event
     * @param user the user
     * @return true if the bookings are correctly saved
     */
    public static Future<JsonArray> saveBookings(JsonObject object, UserInfos user, EventBus eb) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject action = new JsonObject()
                .put(Field.ACTION, Field.SAVE_BOOKINGS)
                .put(Field.BOOKINGS, object
                        .getJsonObject(Field.BOOKINGS, null)
                        .getJsonArray(Field.ALL, null))
                .put(Field.userId, user.getUserId());
        eb.request(rbsAddress, action, messageJsonArrayHandler(handlerJsonObject(promise)));

        return promise.future();
    }


}
