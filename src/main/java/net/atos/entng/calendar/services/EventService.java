package net.atos.entng.calendar.services;

import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public interface EventService {

    void list(String calendarId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    void create(String calendarId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void retrieve(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void update(String calendarId, String eventId, JsonObject body, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void delete(String calendarId, String eventId, UserInfos user, Handler<Either<String, JsonObject>> handler);

}
