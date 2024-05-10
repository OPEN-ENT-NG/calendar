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
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Either.Right;
import fr.wseduc.webutils.I18n;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.core.constants.Field;
import org.bson.conversions.Bson;
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

import static com.mongodb.client.model.Filters.*;
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
			final List<Bson> groups = new ArrayList<>();
			groups.add(eq("userId",userId));
			for (String gpId: groupIdsLst) {
				groups.add(eq("groupId",gpId));
			}

			final Bson rightsQuery = or(
					eq("visibility",VisibilityFilter.PUBLIC.name()),
					eq("visibility",VisibilityFilter.PROTECTED.name()),
					eq("owner.userId",userId),
					elemMatch("shared", or(groups))
				);

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
				final String idCalendar = j.getJsonArray(Field.CALENDAR, new JsonArray()).size() != 0 ?
						j.getJsonArray(Field.CALENDAR, new JsonArray()).getString(0) : null;
				jr.put(aHeader.get(0), mapIdTitle.get(idCalendar));
				jr.put(aHeader.get(1), formatDescription(locale, j.getString(Field.TITLE), j.getString(Field.DESCRIPTION, ""),
						j.getString(Field.LOCATION, ""), j.getString(Field.STARTMOMENT), j.getString(Field.ENDMOMENT)));
				jr.put(aHeader.get(2), j.getJsonObject(Field.MODIFIED));
				jr.put(aHeader.get(3), j.getJsonObject(Field.OWNER).getString(Field.DISPLAYNAME));
				jr.put(aHeader.get(4), j.getJsonObject(Field.OWNER).getString(Field.USERID));
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

		final String dateFormatRes = "dd/MM/YYYY " + i18n.translate("calendar.search.date.to", I18n.DEFAULT_DOMAIN, locale) + " HH:mm";
		final String sDateRes = new SimpleDateFormat(dateFormatRes).format(sDate);
		final String eDateRes = new SimpleDateFormat(dateFormatRes).format(eDate);

		if (!description.isEmpty() && !location.isEmpty()) {
			descriptionRes = i18n.translate("calendar.search.description.full", Field.DEFAULT_DOMAIN, locale, title, sDateRes,
					eDateRes, location, description);
		} else if (!location.isEmpty()) {
			descriptionRes = i18n.translate("calendar.search.description.location", Field.DEFAULT_DOMAIN, locale, title, sDateRes,
					eDateRes, location);
		} else if (!description.isEmpty()){
			descriptionRes = i18n.translate("calendar.search.description.desc", Field.DEFAULT_DOMAIN, locale, title, sDateRes,
					eDateRes, description);
		} else {
			descriptionRes = i18n.translate("calendar.search.description.min", Field.DEFAULT_DOMAIN, locale, title, sDateRes,
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

		final Bson worldsQuery = text(MongoDbSearchService.textSearchedComposition(searchWords));

		final Bson calendarQuery = in("calendar", mapIdTitle.keySet());
		final Bson query = and(worldsQuery, calendarQuery);

		JsonObject sort = new JsonObject().put("modified", -1);
		final JsonObject projection = new JsonObject();
		for (String field : returnFields) {
			projection.put(field, 1);
		}

		mongo.find(Calendar.CALENDAR_EVENT_COLLECTION, MongoQueryBuilder.build(query), sort,
				projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(handler));
	}
}
