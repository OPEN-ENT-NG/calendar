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

package net.atos.entng.calendar.event;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import net.atos.entng.calendar.Calendar;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.service.impl.MongoDbRepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CalendarRepositoryEvents extends MongoDbRepositoryEvents {

    public CalendarRepositoryEvents(Vertx vertx) {
        super(vertx);

        this.collectionNameToImportPrefixMap.put(Calendar.CALENDAR_COLLECTION, "cal_");
        this.collectionNameToImportPrefixMap.put(Calendar.CALENDAR_EVENT_COLLECTION, "ev_");
    }


    @Override
    public void exportResources(JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, String exportId, String userId,
                                JsonArray g, String exportPath, String locale, String host, Handler<Boolean> handler)
    {
        QueryBuilder findByOwner = QueryBuilder.start("owner.userId").is(userId);
        QueryBuilder findByShared = QueryBuilder.start().or(
            QueryBuilder.start("shared.userId").is(userId).get(),
            QueryBuilder.start("shared.groupId").in(g).get()
        );

        QueryBuilder findByAuthorOrOwnerOrShared = exportSharedResources == false ? findByOwner : QueryBuilder.start().or(findByOwner.get(),findByShared.get());

        JsonObject query;

        if(resourcesIds == null)
            query = MongoQueryBuilder.build(findByAuthorOrOwnerOrShared);
        else
        {
            QueryBuilder limitToResources = findByAuthorOrOwnerOrShared.and(
                QueryBuilder.start("_id").in(resourcesIds).get()
            );
            query = MongoQueryBuilder.build(limitToResources);
        }

        final AtomicBoolean exported = new AtomicBoolean(false);

        Map<String, String> prefixMap = this.collectionNameToImportPrefixMap;

        mongo.find(Calendar.CALENDAR_COLLECTION, query, new Handler<Message<JsonObject>>()
        {
            @Override
            public void handle(Message<JsonObject> event)
            {
                JsonArray results = event.body().getJsonArray("results");
                if ("ok".equals(event.body().getString("status")) && results != null)
                {
                    results.forEach(elem ->
                    {
                        JsonObject cal = ((JsonObject) elem);
                        cal.put("title", prefixMap.get(Calendar.CALENDAR_COLLECTION) + cal.getString("title"));
                    });

                    final Set<String> ids = results.stream().map(res -> ((JsonObject)res).getString("_id")).collect(Collectors.toSet());
                    QueryBuilder findByCategoryId = QueryBuilder.start("calendar").in(ids);
                    JsonObject query2 = MongoQueryBuilder.build(findByCategoryId);

                    mongo.find(Calendar.CALENDAR_EVENT_COLLECTION, query2, new Handler<Message<JsonObject>>()
                    {
                        @Override
                        public void handle(Message<JsonObject> event2)
                        {
                            JsonArray results2 = event2.body().getJsonArray("results");
                            if ("ok".equals(event2.body().getString("status")) && results2 != null)
                            {
                                results2.forEach(elem ->
                                {
                                    JsonObject ev = ((JsonObject) elem);
                                    ev.put("title", prefixMap.get(Calendar.CALENDAR_EVENT_COLLECTION) + ev.getString("title"));
                                });

                                createExportDirectory(exportPath, locale, new Handler<String>()
                                {
                                    @Override
                                    public void handle(String path)
                                    {
                                        if (path != null)
                                        {
                                            Handler<Boolean> finish = new Handler<Boolean>()
                                            {
                                                @Override
                                                public void handle(Boolean bool)
                                                {
                                                    if (bool) {
                                                        exportFiles(results, path, new HashSet<String>(), exported, handler);
                                                    } else {
                                                        // Should never happen, export doesn't fail if docs export fail.
                                                        handler.handle(exported.get());
                                                    }
                                                }
                                            };

                                            if(exportDocuments == true)
                                                exportDocumentsDependancies(results.addAll(results2), path, finish);
                                            else
                                                finish.handle(Boolean.TRUE);
                                        }
                                        else
                                        {
                                            handler.handle(exported.get());
                                        }
                                    }
                                });
                            }
                            else
                            {
                                log.error(title + " : Could not proceed query " + query2.encode(), event2.body().getString("message"));
                                handler.handle(exported.get());
                            }
                        }
                    });
                }
                else
                {
                    log.error(title + " : Could not proceed query " + query.encode(), event.body().getString("message"));
                    handler.handle(exported.get());
                }
            }
        });
    }

    @Override
    public void deleteGroups(JsonArray groups) {
        if (groups == null) {
            log.warn("[CalendarRepositoryEvents][deleteGroups] JsonArray groups is null or empty");
            return;
        }

		for(int i = groups.size(); i-- > 0;)
		{
			if(groups.hasNull(i))
				groups.remove(i);
			else if (groups.getJsonObject(i) != null && groups.getJsonObject(i).getString("group") == null)
				groups.remove(i);
		}
        if(groups.size() == 0)
        {
            log.warn("[CalendarRepositoryEvents][deleteGroups] JsonArray groups is null or empty");
            return;
        }

        final String[] groupIds = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            JsonObject j = groups.getJsonObject(i);
            groupIds[i] = j.getString("group");
        }

        final JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("shared.groupId").in(groupIds));

        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.pull("shared", MongoQueryBuilder.build(QueryBuilder.start("groupId").in(groupIds)));
        // remove all the shares with groups
        mongo.update(Calendar.CALENDAR_COLLECTION, matcher, modifier.build(), false, true, MongoDbResult.validActionResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    log.info("[CalendarRepositoryEvents][deleteGroups] All groups shares are removed");
                } else {
                    log.error("[CalendarRepositoryEvents][deleteGroups] Error removing groups shares. Message : " + event.left().getValue());
                }
            }
        }));
    }

    @Override
    public void deleteUsers(JsonArray users) {
        //FIXME: anonymization is not relevant
        if (users == null) {
            log.warn("[CalendarRepositoryEvents][deleteUsers] JsonArray users is null or empty");
            return;
        }
		for(int i = users.size(); i-- > 0;)
		{
			if(users.hasNull(i))
				users.remove(i);
            else if (users.getJsonObject(i) != null && users.getJsonObject(i).getString("id") == null)
                users.remove(i);
		}
		if(users.size() == 0)
		{
            log.warn("[CalendarRepositoryEvents][deleteUsers] JsonArray users is null or empty");
			return;
		}

        final String[] usersIds = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            JsonObject j = users.getJsonObject(i);
            usersIds[i] = j.getString("id");
        }
        /*
         * Clean the database : - First, remove shares of all the categories shared with (usersIds) - then, get the
         * categories identifiers that have no user and no manger, - delete all these categories, - delete all the
         * subjects that do not belong to a category - finally, tag all users as deleted in their own categories
         */

        this.removeSharesCalendars(usersIds);
    }

    /**
     * Remove the shares of categories with a list of users if OK, Call prepareCleanCategories()
     * @param usersIds users identifiers
     */
    private void removeSharesCalendars(final String[] usersIds) {
        final JsonObject criteria = MongoQueryBuilder.build(QueryBuilder.start("shared.userId").in(usersIds));
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.pull("shared", MongoQueryBuilder.build(QueryBuilder.start("userId").in(usersIds)));

        // Remove Categories shares with these users
        mongo.update(Calendar.CALENDAR_COLLECTION, criteria, modifier.build(), false, true, MongoDbResult.validActionResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    log.info("[CalendarRepositoryEvents][removeSharesCalendars] All calendars shares with users are removed");
                    prepareCleanCalendars(usersIds);
                } else {
                    log.error("[CalendarRepositoryEvents][removeSharesCalendars] Error removing calendars shares with users. Message : " + event.left().getValue());
                }
            }
        }));
    }

    /**
     * Prepare a list of categories identifiers if OK, Call cleanCategories()
     * @param usersIds users identifiers
     */
    private void prepareCleanCalendars(final String[] usersIds) {
        DBObject deletedUsers = new BasicDBObject();
        // users currently deleted
        deletedUsers.put("owner.userId", new BasicDBObject("$in", usersIds));
        // users who have already been deleted
        DBObject ownerIsDeleted = new BasicDBObject("owner.deleted", true);
        // no manager found
        JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("shared." + Calendar.MANAGE_RIGHT_ACTION).notEquals(true).or(deletedUsers, ownerIsDeleted));
        // return only calendar identifiers
        JsonObject projection = new JsonObject().put("_id", 1);

        mongo.find(Calendar.CALENDAR_COLLECTION, matcher, null, projection, MongoDbResult.validResultsHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray calendars = event.right().getValue();
                    if (calendars == null || calendars.size() == 0) {
                        log.info("[CalendarRepositoryEvents][prepareCleanCalendars] No calendars to delete");
                        return;
                    }
                    final String[] calendarIds = new String[calendars.size()];
                    for (int i = 0; i < calendars.size(); i++) {
                        JsonObject j = calendars.getJsonObject(i);
                        calendarIds[i] = j.getString("_id");
                    }
                    cleanCalendars(usersIds, calendarIds);
                } else {
                    log.error("[CalendarRepositoryEvents][prepareCleanCalendars] Error retreving the calendars created by users. Message : " + event.left().getValue());
                }
            }
        }));
    }

    /**
     * Delete calendars by identifier if OK, call cleanEvents() and tagUsersAsDeleted()
     * @param usersIds users identifiers, used for tagUsersAsDeleted()
     * @param calendarIds calendars identifiers
     */
    private void cleanCalendars(final String[] usersIds, final String[] calendarIds) {
        JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("_id").in(calendarIds));

        mongo.delete(Calendar.CALENDAR_COLLECTION, matcher, MongoDbResult.validActionResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    log.info("[CalendarRepositoryEvents][cleanCalendars] The calendars created by users are deleted");
                    cleanEvents(calendarIds);
                    tagUsersAsDeleted(usersIds);
                } else {
                    log.error("[CalendarRepositoryEvents][cleanCalendars] Error deleting the calendars created by users. Message : " + event.left().getValue());
                }
            }
        }));
    }

    /**
     * Delete events by calendar identifier
     * @param calendarIds calendars identifiers
     */
    private void cleanEvents(final String[] calendarIds) {
        JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("calendar").in(calendarIds));

        mongo.delete(Calendar.CALENDAR_EVENT_COLLECTION, matcher, MongoDbResult.validActionResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    log.info("[CalendarRepositoryEvents][cleanEvents] The events created by users are deleted");
                } else {
                    log.error("[CalendarRepositoryEvents][cleanEvents] Error deleting the events created by users. Message : " + event.left().getValue());
                }
            }
        }));
    }

    /**
     * Tag as deleted a list of users in their own calendars
     * @param usersIds users identifiers
     */
    private void tagUsersAsDeleted(final String[] usersIds) {
        final JsonObject criteria = MongoQueryBuilder.build(QueryBuilder.start("owner.userId").in(usersIds));
        MongoUpdateBuilder modifier = new MongoUpdateBuilder();
        modifier.set("owner.deleted", true);

        mongo.update(Calendar.CALENDAR_COLLECTION, criteria, modifier.build(), false, true, MongoDbResult.validActionResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    log.info("[CalendarRepositoryEvents][tagUsersAsDeleted] users are tagged as deleted in their own calendars");
                } else {
                    log.error("[CalendarRepositoryEvents][tagUsersAsDeleted] Error tagging as deleted users. Message : " + event.left().getValue());
                }
            }
        }));
    }

}
