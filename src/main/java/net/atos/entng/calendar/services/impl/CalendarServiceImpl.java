/*
 * Copyright © Région Nord Pas de Calais-Picardie,  Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.atos.entng.calendar.services.impl;
import static fr.wseduc.webutils.http.Renders.getHost;
import static org.entcore.common.mongodb.MongoDbResult.*;

import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.services.CalendarService;

import org.entcore.common.user.UserInfos;
import io.vertx.core.json.JsonObject;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;



public class CalendarServiceImpl implements CalendarService {
    private MongoDb mongo;
    private String collection;
    protected static final Logger log = LoggerFactory.getLogger(CalendarServiceImpl.class);


    public CalendarServiceImpl(String collection, MongoDb mongo) {
        this.collection = collection;
        this.mongo = mongo;
    }

    @Override
    public Future<Boolean> hasDefaultCalendar(UserInfos user) {
        Promise<Boolean> promise = Promise.promise();
        // Query
        QueryBuilder query = QueryBuilder.start("owner.userId").is(user.getUserId());
        query.put("is_default").is(true);

        mongo.findOne(this.collection, MongoQueryBuilder.build(query), validResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::hasDefaultCalendar]: an error has occurred while finding default calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            promise.complete(!result.right().getValue().isEmpty());
        }));
        return promise.future();
    }

    @Override
    public Future<JsonObject> createDefaultCalendar(Boolean exists, UserInfos user, HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();

        if (!exists) {
            JsonObject defaultCalendar = new JsonObject();
            JsonObject now = MongoDb.now();

            String title = I18n.getInstance().translate("calendar.default.title", getHost(request), I18n.acceptLanguage(request));

            defaultCalendar.put("title", title);
            defaultCalendar.put("color", "grey");
            defaultCalendar.put("created", now);
            defaultCalendar.put("modified", now);
            defaultCalendar.put("owner", new JsonObject().put("userId", user.getUserId()).put("displayName", user.getUsername()));
            defaultCalendar.put("is_default", true);


            insertDefaultCalendar(promise, defaultCalendar);
        } else {
            JsonObject response = new JsonObject();
            promise.complete(response);
        }
        return promise.future();
    }

    /**
     * Insert Default Calendar
     *
     * @param promise promise to complete or fail
     * @param defaultCalendar JsonObject containing the data for the default calendar
     */
    private void insertDefaultCalendar(Promise<JsonObject> promise, JsonObject defaultCalendar) {
        mongo.insert(this.collection, defaultCalendar, validActionResultHandler(result -> {
            if(result.isLeft()) {
                log.error("[Calendar@CalendarService::createDefaultCalendar]: an error has occurred while creating default calendar: ",
                        result.left().getValue());
                promise.fail(result.left().getValue());
                return;
            }
            promise.complete(result.right().getValue());
        }));
    }


}
