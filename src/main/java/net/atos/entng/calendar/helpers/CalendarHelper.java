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
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.core.enums.ExternalPlatformEnum;
import net.atos.entng.calendar.ical.ExternalImportICal;
import net.atos.entng.calendar.models.CalendarModel;
import net.atos.entng.calendar.services.CalendarService;
import net.atos.entng.calendar.services.EventServiceMongo;
import net.atos.entng.calendar.services.ServiceFactory;
import net.atos.entng.calendar.services.impl.EventServiceMongoImpl;
import net.atos.entng.calendar.utils.DateUtils;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
                    if (calendar.getString(Field.PLATFORM, "").equals(Field.ZIMBRA)) {
                        return Future.succeededFuture();
                    }
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

                updateExternalCalendar(params, calendar, true)
                        .compose(result -> {
                            if (calendar.getJsonObject(Field.UPDATED, null) == null) {
                                return Future.succeededFuture();
                            } else {
                                Date date = new Date(calendar.getJsonObject(Field.UPDATED).getLong(MongoField.$DATE));
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.DATE_FORMAT_UTC);
                                simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
                                return eventServiceMongo.deleteDatesAfterComparisonDate(calendar.getString(Field._ID), simpleDateFormat.format(date));
                            }
                        })
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
    public Future<Void> updateExternalCalendar(JsonObject params, JsonObject calendar, Boolean startOfSync) {
        calendar.put(Field.ISUPDATING, startOfSync);
        if (Boolean.FALSE.equals(startOfSync)) calendar.put(Field.UPDATED, MongoDb.now());
        params.put(Field.CALENDAR, calendar);
        return updateCalendar(calendar);
    }

    private Future<Void> getAndSaveExternalCalendarEvents(UserInfos user, JsonObject calendar, String host, String i18nLang, String action) {
        Promise<Void> promise = Promise.promise();
        Future<Void> future = Future.failedFuture("calendar.error.missing.data");

        String url = calendar.getString(Field.ICSLINK, null);
        String platform = calendar.getString(Field.PLATFORM, null);

        if (!StringsHelper.isNullOrEmpty(url) && StringsHelper.isNullOrEmpty(platform)) { //case ics link
            future = callLinkImportEventBus(user, calendar, host, i18nLang, action);
        } else if (StringsHelper.isNullOrEmpty(url) && !StringsHelper.isNullOrEmpty(platform)) { //case external platform
            future = getICalFromExternalPlatform(user, platform, new CalendarModel(calendar), host, i18nLang);
        }

        future
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    String errMessage = String.format("[Calendar@%s::getAndSaveExternalCalendarEvents]: an error has occurred during " +
                            "ics retrieval : %s", this.getClass().getSimpleName(), err.getMessage());
                    log.error(errMessage);
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    private Future<Void> callLinkImportEventBus(UserInfos user, JsonObject calendar, String host, String i18nLang, String action) {
        Promise<Void> promise = Promise.promise();

        JsonObject requestInfo = new JsonObject().put(Field.DOMAIN, host).put(Field.ACCEPTLANGUAGE, i18nLang);
        JsonObject message = new JsonObject()
                .put(Field.CALENDAR, calendar)
                .put(Field.REQUEST, requestInfo)
                .put(Field.ACTION, action)
                .put(Field.USERID, user.getUserId());

        eb.request(ExternalImportICal.class.getName(), message, event -> {
            if(event.failed()) {
                String errMessage = String.format("[Calendar@%s::callLinkImportEventBus]:  " +
                        "an error has occurred while creating external calendar events: %s",
                        this.getClass().getSimpleName(), event.cause().getMessage());
                log.error(errMessage);
                promise.fail(event.cause().getMessage());
            } else {
                promise.complete();
            }
        });

        return promise.future();
    }

    private Future<Void> getICalFromExternalPlatform(UserInfos user, String platform, CalendarModel calendar, String host, String i18nLang) {
        Promise<Void> promise = Promise.promise();

        JsonObject message = new JsonObject()
                .put(Field.ACTION, ExternalICalEventBusActions.GET_PLATFORM_ICS.method())
                .put(Field.userId, user.getUserId());

        String ebAddress = "";
        switch (platform) {
            case Field.ZIMBRA:
                ebAddress = ExternalPlatformEnum.ZIMBRA_ADDRESS.getEventBusAddress();
                break;
            default:
                String errMessage = String.format("[Calendar@%s::getAndSaveExternalCalendarEvents]:  an error has occurred during " +
                        "platform check: platform is not accepted", this.getClass().getSimpleName());
                log.error(errMessage);
                promise.fail("calendar.error.platform.not.accepted");
                return promise.future();
        }

        eb.request(ebAddress, message, FutureHelper.messageJsonObjectHandler(event -> {
            if(event.failed()) {
                String errMessage = String.format("[Calendar@%s::getAndSaveExternalCalendarEvents]:  an error has occurred during " +
                        "ics retrieval: %s", this.getClass().getSimpleName(), event.cause().getMessage());
                log.error(errMessage);
                promise.fail(event.cause().getMessage());
            } else {
                if (Objects.equals(event.result().getString(Field.MESSAGE), "ical.request.ok")) {
                    promise.complete();
                } else {
                    String errMessage = String.format("[Calendar@%s::getAndSaveExternalCalendarEvents]:  an error has occurred during " +
                            "ics retrieval: %s", this.getClass().getSimpleName(), event.cause().getMessage());
                    log.error(errMessage);
                    promise.fail("calendar.platform.ical.status.not.valid");
                }
            }
        }));

        return promise.future();
    }

    private Future<Void> updateCalendar(JsonObject calendar) {
        Promise<Void> promise = Promise.promise();

        calendarService.update(calendar.getString(Field._ID),calendar)
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
        Long calendarUpdateTime = calendar.getJsonObject(Field.UPDATED, new JsonObject()).getLong(MongoField.$DATE, null);
        if (calendarUpdateTime == null) {
            return false;
        }

        Timestamp lastUpdateTimestamp = new Timestamp(calendarUpdateTime);
        Date lastUpdateDate = new Date(lastUpdateTimestamp.getTime());
        long secondsSinceLastUpdate = (new Date().getTime()-lastUpdateDate.getTime())/1000;

        return secondsSinceLastUpdate - config.getLong(Field.CALENDARSYNCTTL, 3600L) > 0;
    }

}
