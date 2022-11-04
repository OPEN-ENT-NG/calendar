package net.atos.entng.calendar.helpers;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.constants.MongoField;
import net.atos.entng.calendar.ical.ExternalImportICal;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import net.atos.entng.calendar.utils.DateUtils;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;

public class CalendarHelper extends MongoDbControllerHelper {
    protected static final Logger log = LoggerFactory.getLogger(Renders.class);

    private final CalendarService calendarService;
    private final EventServiceMongo eventServiceMongo;
    private EventBus eb;

    public CalendarHelper(String collection, ServiceFactory serviceFactory, EventBus eb, JsonObject config) {
        super(collection, null);
        this.calendarService = serviceFactory.calendarService();
        this.eventServiceMongo = new EventServiceMongoImpl(Field.CALENDAREVENT, eb, serviceFactory);
        this.mongo = MongoDb.getInstance();
        this.eb = eb;
        this.config = config;
    }

    public Future<Void> externalCalendarSync(String calendarId, UserInfos user, String host, String i18nLang, String action) {
        Promise<Void> promise = Promise.promise();
        JsonObject params = new JsonObject();
        calendarService.list(Collections.singletonList(calendarId), true)
                .compose(calendarList -> prepareCalendarAndEventsForUpdate(params, calendarList, action, user))
                .compose(calendarUpdated -> getAndSaveExternalCalendarEvents(user, params.getJsonObject(Field.CALENDAR), host, i18nLang, action))
                .compose(object -> {
                    JsonObject calendar = params.getJsonObject(Field.CALENDAR);
                    return updateExternalCalendar(params, calendar, false);
                })
                .onSuccess(finalCalendar -> promise.complete())
                .onFailure(error -> {
                    if (!params.isEmpty()) { //set the calendar to not updating if there is a fail
                        updateCalendar(params.getJsonObject(Field.CALENDAR).put(Field.ISUPDATING, false));
                    }
                    String message = String.format("[Calendar@%s::externalCalendarSync]:  an error has occurred during " +
                                    "external calendar sync: %s", this.getClass().getSimpleName(), error.getMessage());;
                    log.error(message);
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    private Future<Void> prepareCalendarAndEventsForUpdate(JsonObject params, JsonArray calendarList, String action, UserInfos user) {
        Promise<Void> promise = Promise.promise();

        if(calendarList.isEmpty()) {
            String message = String.format("[Calendar@%s::prepareCalendarAndEventsForUpdate]:  an error has occurred while " +
                            "retrieving external calendar",
                    this.getClass().getSimpleName());
            log.error(message);
            promise.fail(message);
        }

        JsonObject calendar = calendarList.getJsonObject(0);
        params.put(Field.CALENDAR, calendar);
        switch(action) {
            case Field.POST:
                updateExternalCalendar(params, calendar, true)
                        .onSuccess(promise::complete)
                        .onFailure(error -> {
                            String message = String.format("[Calendar@%s::prepareCalendarAndEventsForUpdate]:  an error has occurred while " +
                                            "preparing calendar for first sync: %s",
                                    this.getClass().getSimpleName(), error.getMessage());
                            log.error(message);
                            promise.fail(error.getMessage());
                        });
                break;
            case Field.PUT:
                if(Boolean.FALSE.equals(isTimeToLivePast(calendar))) {
                    String message = String.format("[Calendar@%s::prepareCalendarAndEventsForUpdate]:  last update was too recent",
                            this.getClass().getSimpleName());
                    log.error(message);
                    promise.fail(message);
                    break;
                }

                Timestamp lastUpdateTimestamp = new Timestamp(calendar.getJsonObject(Field.UPDATED).getLong(MongoField.$DATE));
                Date lastUpdateDate = new Date(lastUpdateTimestamp.getTime());
                eventServiceMongo.deleteDatesAfterComparisonDate(calendar.getString(Field._ID), DateUtils.dateToString(lastUpdateDate))
                        .compose(result -> updateExternalCalendar(params, calendar, true))
                        .onSuccess(promise::complete)
                        .onFailure(error -> {
                            String message = String.format("[Calendar@%s::prepareCalendarAndEventsForUpdate]:  an error has occurred while " +
                                            "preparing calendar and event for sync: %s",
                                    this.getClass().getSimpleName(), error.getMessage());
                            log.error(message);
                            promise.fail(message);
                        });
                break;
            default:
                String message = String.format("[Calendar@%s::prepareCalendarAndEventsForUpdate]:  an error has occurred while " +
                                "preparing calendar and event for update",
                        this.getClass().getSimpleName());
                log.error(message);
                promise.fail(message);
                break;
        }
        return promise.future();
    }

    /**
     * Updates the external calendar
     * @param params the variable containing the calendar {@link JsonObject}
     * @param calendar the calendar fetched from the database {@link JsonObject}
     * @param startOfSync whether the updates takes place at the start or the end of the sync {@link Boolean}
     * @return {@link Future<Void>} the update of a calendar
     */
    private Future<Void> updateExternalCalendar(JsonObject params, JsonObject calendar, Boolean startOfSync) {
        calendar.put(Field.ISUPDATING, startOfSync);
        if (Boolean.FALSE.equals(startOfSync)) calendar.put(Field.UPDATED, MongoDb.now());
        params.put(Field.CALENDAR, calendar);
        return updateCalendar(calendar);
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

    private Future<Void> updateCalendar(JsonObject calendar) {
        Promise<Void> promise = Promise.promise();

        calendarService.update(calendar.getString(Field._ID),calendar, true)
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    String message = String.format("[Calendar@%s::updateCalendar]:  an error has occurred while " +
                                    "updating external calendar: %s",
                            this.getClass().getSimpleName(), error.getMessage());
                    log.error(message);
                    promise.fail(error);
                });

        return promise.future();
    }

    /**
     * Checks if the time since the last update is longer than the minimum time (value in config) between two updates
     * Default TTL time : 15 min
     *
     * @param calendar the calendar in which we want to check the update {@link JsonObject}
     * @return {@link Boolean} true if the time since the last update is longer than the minimum time between two updates
     */
    public Boolean isTimeToLivePast(JsonObject calendar) {
        Timestamp lastUpdateTimestamp = new Timestamp(calendar.getJsonObject(Field.UPDATED).getLong(MongoField.$DATE));
        Date lastUpdateDate = new Date(lastUpdateTimestamp.getTime());
        long secondsSinceLastUpdate = (new Date().getTime()-lastUpdateDate.getTime())/1000;

        return secondsSinceLastUpdate - config.getLong(Field.CALENDARSYNCTTL, 900L) > 0;
    }

}
