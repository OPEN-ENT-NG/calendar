package net.atos.entng.calendar.helpers;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.ical.ExternalImportICal;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;

import java.util.Collections;

import static fr.wseduc.webutils.http.Renders.renderError;

public class CalendarHelper extends MongoDbControllerHelper {
    protected static final Logger log = LoggerFactory.getLogger(Renders.class);

    private final CalendarService calendarService;
    private EventBus eb;

    public CalendarHelper(String collection, ServiceFactory serviceFactory, EventBus eb) {
        super(collection, null);
        this.calendarService = serviceFactory.calendarService();
        this.mongo = MongoDb.getInstance();
        this.eb = eb;
    }

    public Future<Void> externalCalendarFirstSync(String calendarId, UserInfos user, String host, String i18nLang, String action) {
        Promise<Void> promise = Promise.promise();
        JsonObject params = new JsonObject();
        calendarService.list(Collections.singletonList(calendarId), true)
                .compose(calendarList -> {
                    if(calendarList.size() != 0) {
                        JsonObject calendar = calendarList.getJsonObject(0).put(Field.ISUPDATING, true);
                        params.put(Field.CALENDAR, calendar);
                        return updateExternalCalendar(calendar);
                    } else {
                        String message = String.format("[Calendar@%s::externalCalendarFirstSync]:  an error has occurred while " +
                                        "retrieving external calendar",
                                this.getClass().getSimpleName());
                        log.error(message);
                        return Future.failedFuture(message);
                    }
                })
                .compose(calendarUpdated -> getAndSaveExternalCalendarEvents(user, params.getJsonObject(Field.CALENDAR), host, i18nLang, action))
                .compose(object -> {
                    JsonObject calendar = params.getJsonObject(Field.CALENDAR);
                    calendar.put(Field.ISUPDATING, false);
                    return updateExternalCalendar(calendar);
                })
                .onSuccess(finalCalendar -> promise.complete())
                .onFailure(error -> {
                    if (!params.isEmpty()) { //set the calendar to not updating if ther is a fail
                        updateExternalCalendar(params.getJsonObject(Field.CALENDAR).put(Field.ISUPDATING, false));
                    }
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    private Future<Void> getAndSaveExternalCalendarEvents(UserInfos user, JsonObject calendar, String host, String i18nLang, String action) {
        Promise<Void> promise = Promise.promise();
        JsonObject requestInfo = new JsonObject().put(Field.DOMAIN, host).put(Field.ACCEPTLANGUAGE, i18nLang);
        JsonObject message = new JsonObject()
                .put(Field.CALENDAR, calendar)
                .put(Field.REQUEST, requestInfo)
                .put(Field.ACTION, action)
                .put(Field.USERID, user.getUserId());
        eb.request(ExternalImportICal.class.getName(), message, event -> {
            if(event.failed()) {
                String errMessage = String.format("[Calendar@%s::getAndSaveExternalCalendarEvents]:  " +
                        "an error has occurred while creating external calendar events: %s",
                        this.getClass().getSimpleName(), event.cause());
                log.error(errMessage);
                promise.fail(errMessage);
            } else {
                promise.complete();
            }
        });
        return promise.future();
    }

    private Future<Void> updateExternalCalendar(JsonObject calendar) {
        Promise<Void> promise = Promise.promise();

        calendar.put(Field.UPDATED, MongoDb.now());
        calendarService.update(calendar.getString(Field._ID),calendar, true)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    String message = String.format("[Calendar@%s::updateExternalCalendar]:  an error has occurred while " +
                                    "updating external calendar: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    promise.fail(error);
                });

        return promise.future();
    }

}
