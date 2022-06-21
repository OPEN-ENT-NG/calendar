package net.atos.entng.calendar.helpers;

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
import net.atos.entng.calendar.core.enums.RbsEventBusActions;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.atos.entng.calendar.helpers.FutureHelper.*;

public class RbsHelper {
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
                && (object.getJsonArray(Field.BOOKINGS).size() > 0);
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
                .put(Field.ACTION, RbsEventBusActions.SAVE_BOOKINGS.method())
                .put(Field.BOOKINGS, object
                        .getJsonArray(Field.BOOKINGS, null))
                .put(Field.userId, user.getUserId());
        eb.request(RbsEventBusActions.rbsAddress, action, messageJsonArrayHandler(handlerJsonArray(promise)));

        return promise.future();
    }

    public static Future<JsonArray> checkAndDeleteBookingRights(UserInfos user, JsonObject event, EventBus eb) {
        Promise<JsonArray> promise = Promise.promise();
        List<Integer> bookingIds = event
                .getJsonArray(Field.BOOKINGS, new JsonArray())
                .stream()
                .map(booking -> ((JsonObject)booking).getInteger(Field.ID, null))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        JsonObject action = new JsonObject()
                .put(Field.ACTION, RbsEventBusActions.DELETE_BOOKINGS.method())
                .put(Field.BOOKINGS, bookingIds)
                .put(Field.userId, user.getUserId());

        eb.request(RbsEventBusActions.rbsAddress, action, messageJsonArrayHandler((handlerJsonArray(promise))));
        return promise.future();
    }
}
