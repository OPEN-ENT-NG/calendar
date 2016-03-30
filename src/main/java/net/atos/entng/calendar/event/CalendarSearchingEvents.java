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
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

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

			final List<String> groupIdsLst = groupIds.toList();
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

			JsonObject sort = new JsonObject().putNumber("modified", -1);
			final JsonObject projection = new JsonObject();
			projection.putNumber("title", 1);
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
									final JsonObject j = calendarResult.get(i);
									mapIdTitle.put(j.getString("_id"), j.getString("title"));
								}

								//search event for the calendars found
								searchEvent(page, limit, searchWords.toList(), mapIdTitle, new Handler<Either<String, JsonArray>>() {
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
		final List<String> aHeader = columnsHeader.toList();
		final JsonArray traity = new JsonArray();

		for (int i=0;i<results.size();i++) {
			final JsonObject j = results.get(i);
			final JsonObject jr = new JsonObject();
			if (j != null) {
				final String idCalendar = j.getString("calendar");
				jr.putString(aHeader.get(0), mapIdTitle.get(idCalendar));
				jr.putString(aHeader.get(1), formatDescription(locale, j.getString("title"), j.getString("description", ""),
						j.getString("location", ""), j.getString("startMoment"), j.getString("endMoment")));
				jr.putObject(aHeader.get(2), j.getObject("modified"));
				jr.putString(aHeader.get(3), j.getObject("owner").getString("displayName"));
				jr.putString(aHeader.get(4), j.getObject("owner").getString("userId"));
				jr.putString(aHeader.get(5), "/calendar#/view/" + idCalendar);
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

		final String dateFormatRes = "EEEEE dd MMMMM yyyy " + i18n.translate("calendar.search.date.to", locale) + " HH:mm";
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

		final List<String> searchFields = new ArrayList<String>();
		searchFields.add("title");
		searchFields.add("description");
		searchFields.add("location");

		final Map<String,List<DBObject>> wordsMap = new HashMap<String, List<DBObject>>();
		for (String field : searchFields) {
			final List<DBObject> listDb = new ArrayList<DBObject>();
			for (String word : searchWords) {
				listDb.add(QueryBuilder.start(field).regex(Pattern.compile(".*" + word + ".*", Pattern.CASE_INSENSITIVE)).get());
			}
			wordsMap.put(field, listDb);
		}

		final QueryBuilder worldsOrQuery = new QueryBuilder();
		for (final List<DBObject> words : wordsMap.values()) {
			worldsOrQuery.or(new QueryBuilder().and(words.toArray(new DBObject[words.size()])).get());
		}

		final QueryBuilder calendarQuery = new QueryBuilder().start("calendar").in(mapIdTitle.keySet());

		final QueryBuilder query = new QueryBuilder().and(worldsOrQuery.get(), calendarQuery.get());

		JsonObject sort = new JsonObject().putNumber("modified", -1);
		final JsonObject projection = new JsonObject();
		for (String field : returnFields) {
			projection.putNumber(field, 1);
		}

		mongo.find(Calendar.CALENDAR_EVENT_COLLECTION, MongoQueryBuilder.build(query), sort,
				projection, skip, limit, Integer.MAX_VALUE, validResultsHandler(handler));
	}
}
