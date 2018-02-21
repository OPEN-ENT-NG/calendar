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

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Either.Right;
import fr.wseduc.webutils.I18n;
import net.atos.entng.calendar.Calendar;
import org.entcore.common.search.SearchingEvents;
import org.entcore.common.service.VisibilityFilter;
import org.entcore.common.service.impl.MongoDbSearchService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.entcore.common.mongodb.MongoDbResult.validResults;
import static org.entcore.common.mongodb.MongoDbResult.validResultsHandler;

public class CalendarSearchingEvents implements SearchingEvents {

	private static final Logger log = LoggerFactory.getLogger(CalendarSearchingEvents.class);
	private final MongoDb mongo;
	private static final I18n i18n = I18n.getInstance();

	public CalendarSearchingEvents() {
		this.mongo = MongoDb.getInstance();
	}

	@Override
	public void searchResource(List<String> appFilters, String userId, JsonArray groupIds, final JsonArray searchWords, final Integer page, final Integer limit,
							   final JsonArray columnsHeader, final String locale, final Handler<Either<String, JsonArray>> handler) {
		if (appFilters.contains(CalendarSearchingEvents.class.getSimpleName())) {

			final List<String> groupIdsLst = groupIds.getList();
			final List<DBObject> groups = new ArrayList<DBObject>();
			groups.add(QueryBuilder.start("userId").is(userId).get());
			for (String gpId: groupIdsLst) {
				groups.add(QueryBuilder.start("groupId").is(gpId).get());
			}

			final QueryBuilder rightsQuery = new QueryBuilder().or(
					QueryBuilder.start("visibility").is(VisibilityFilter.PUBLIC.name()).get(),
					QueryBuilder.start("visibility").is(VisibilityFilter.PROTECTED.name()).get(),
					QueryBuilder.start("owner.userId").is(userId).get(),
					QueryBuilder.start("shared").elemMatch(
							new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()
					).get());

			JsonObject sort = new JsonObject().put("modified", -1);
			final JsonObject projection = new JsonObject();
			projection.put("title", 1);
			//search all calendar of user
			mongo.find(Calendar.CALENDAR_COLLECTION, MongoQueryBuilder.build(rightsQuery), sort,
					projection, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							final Either<String, JsonArray> ei = validResults(event);
							if (ei.isRight()) {
								final JsonArray calendarResult = ei.right().getValue();

								final Map<String, String> mapIdTitle = new HashMap<String, String>();
								for (int i=0;i<calendarResult.size();i++) {
									final JsonObject j = calendarResult.getJsonObject(i);
									mapIdTitle.put(j.getString("_id"), j.getString("title"));
								}

								//search event for the calendars found
								searchEvent(page, limit, searchWords.getList(), mapIdTitle, new Handler<Either<String, JsonArray>>() {
									@Override
									public void handle(Either<String, JsonArray> event) {
										if (event.isRight()) {
											if (log.isDebugEnabled()) {
												log.debug("[CalendarSearchingEvents][searchResource] The resources searched by user are finded");
											}
											final JsonArray res = formatSearchResult(event.right().getValue(), columnsHeader, mapIdTitle, locale);
											handler.handle(new Right<String, JsonArray>(res));
										} else {
											handler.handle(new Either.Left<String, JsonArray>(event.left().getValue()));
										}
									}
								});
							} else {
								handler.handle(new Either.Left<String, JsonArray>(ei.left().getValue()));
							}
						}
					});
		} else {
			handler.handle(new Right<String, JsonArray>(new JsonArray()));
		}
	}

	private JsonArray formatSearchResult(final JsonArray results, final JsonArray columnsHeader, final Map<String,String> mapIdTitle,
										 final String locale) {
		final List<String> aHeader = columnsHeader.getList();
		final JsonArray traity = new JsonArray();

		for (int i=0;i<results.size();i++) {
			final JsonObject j = results.getJsonObject(i);
			final JsonObject jr = new JsonObject();
			if (j != null) {
				final String idCalendar = j.getString("calendar");
				jr.put(aHeader.get(0), mapIdTitle.get(idCalendar));
				jr.put(aHeader.get(1), formatDescription(locale, j.getString("title"), j.getString("description", ""),
						j.getString("location", ""), j.getString("startMoment"), j.getString("endMoment")));
				jr.put(aHeader.get(2), j.getJsonObject("modified"));
				jr.put(aHeader.get(3), j.getJsonObject("owner").getString("displayName"));
				jr.put(aHeader.get(4), j.getJsonObject("owner").getString("userId"));
				jr.put(aHeader.get(5), "/calendar#/view/" + idCalendar);
				traity.add(jr);
			}
		}
		return traity;
	}

	private String formatDescription(final String locale, final String title, final String description,
									 final String location, final String startDate, final String endDate) {
		final String descriptionRes;
		Date sDate = new Date();
		Date eDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		try {
			sDate = dateFormat.parse(startDate);
			eDate = dateFormat.parse(endDate);

		} catch (ParseException e) {
			log.error("can't parse date", e);
		}

		final String dateFormatRes = "EEEEE dd MMMMM yyyy " + i18n.translate("calendar.search.date.to", I18n.DEFAULT_DOMAIN, locale) + " HH:mm";
		final String sDateRes = new SimpleDateFormat(dateFormatRes).format(sDate);
		final String eDateRes = new SimpleDateFormat(dateFormatRes).format(eDate);

		if (!description.isEmpty() && !location.isEmpty()) {
			descriptionRes = i18n.translate("calendar.search.description.full", locale, title, sDateRes,
					eDateRes, location, description);
		} else if (!location.isEmpty()) {
			descriptionRes = i18n.translate("calendar.search.description.location", locale, title, sDateRes,
					eDateRes, location);
		} else if (!description.isEmpty()){
			descriptionRes = i18n.translate("calendar.search.description.desc", locale, title, sDateRes,
					eDateRes, description);
		} else {
			descriptionRes = i18n.translate("calendar.search.description.min", locale, title, sDateRes,
					eDateRes);
		}

		return descriptionRes;
	}

	private void searchEvent(int page, int limit, List<String> searchWords, final Map<String,String> mapIdTitle, Handler<Either<String, JsonArray>> handler) {
		final int skip = (0 == page) ? -1 : page * limit;

		final List<String> returnFields = new ArrayList<String>();
		returnFields.add("title");
		returnFields.add("calendar");
		returnFields.add("description");
		returnFields.add("location");
		returnFields.add("modified");
		returnFields.add("startMoment");
		returnFields.add("endMoment");
		returnFields.add("owner.userId");
		returnFields.add("owner.displayName");

		final QueryBuilder worldsQuery = new QueryBuilder();
		worldsQuery.text(MongoDbSearchService.textSearchedComposition(searchWords));

		final QueryBuilder calendarQuery = new QueryBuilder().start("calendar").in(mapIdTitle.keySet());
		final QueryBuilder query = new QueryBuilder().and(worldsQuery.get(), calendarQuery.get());

		JsonObject sort = new JsonObject().put("modified", -1);
		final JsonObject projection = new JsonObject();
		for (String field : returnFields) {
			projection.put(field, 1);
		}

		mongo.find(Calendar.CALENDAR_EVENT_COLLECTION, MongoQueryBuilder.build(query), sort,
				projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(handler));
	}
}
