package net.atos.entng.calendar.helpers;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;
import net.atos.entng.calendar.services.EventService;

import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

public class EventHelper extends MongoDbControllerHelper {

	private static final String CALENDAR_ID_PARAMETER = "id";
	private static final String EVENT_ID_PARAMETER = "eventid";

	private final EventService eventService;

	
	public EventHelper(String collection, CrudService eventService) {
		super(collection, null);
		this.eventService = (EventService) eventService;
		this.crudService = eventService;
	}

	@Override
	public void list(final HttpServerRequest request) {
		
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
				eventService.list(calendarId, user, arrayResponseHandler(request));
			}
		});

	}

	@Override
	public void create(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {		
			@Override
			public void handle(final UserInfos user) {
				final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject object) {
							eventService.create(calendarId, object, user, notEmptyResponseHandler(request));
						}
					});
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
				}
			}
		});
	}

	@Override
	public void update(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject object) {
							String id = request.params().get(EVENT_ID_PARAMETER);
							crudService.update(id, object, user, notEmptyResponseHandler(request));
						}
					});
				} else {
					log.debug("User not found in session.");
					Renders.unauthorized(request);
				}
			}
		});
	}

	@Override
	public void retrieve(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
				String eventId = request.params().get(EVENT_ID_PARAMETER);

				eventService.retrieve(calendarId, eventId, user, notEmptyResponseHandler(request));
			}
		});
	}

	@Override
	public void delete(final HttpServerRequest request) {

		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
				final String eventId = request.params().get(EVENT_ID_PARAMETER);

				eventService.delete(calendarId, eventId, user, defaultResponseHandler(request));
			}
		});
	}
}
