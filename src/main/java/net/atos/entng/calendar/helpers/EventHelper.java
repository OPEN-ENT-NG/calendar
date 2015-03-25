package net.atos.entng.calendar.helpers;

import static net.atos.entng.calendar.Calendar.CALENDAR_NAME;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.notEmptyResponseHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.atos.entng.calendar.services.EventService;

import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;

public class EventHelper extends MongoDbControllerHelper {

    private static final String EVENT_CREATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_CREATED";
    private static final String EVENT_UPDATED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_UPDATED";
    private static final String EVENT_DELETED_EVENT_TYPE = CALENDAR_NAME + "_EVENT_DELETED";
    
	private static final String CALENDAR_ID_PARAMETER = "id";
	private static final String ICS_PARAMETER = "id";

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
	
	
	public void getIcal(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                eventService.getIcal(calendarId, user, new Handler<Message<JsonObject>>(){
                    @Override
                    public void handle(Message<JsonObject> reply) {
                        log.warn("Handling response");
                        JsonObject response = reply.body();
                        String content = response.getString("ics");
                        try {
                            File f = File.createTempFile(calendarId, "ics");
                            Files.write(Paths.get(f.getAbsolutePath()), content.getBytes());
                            request.response().putHeader("Content-Type", "text/calendar");
                            request.response().putHeader("Content-disposition", "attachment; filename=\"" + calendarId + ".ics\"");
                            
                            request.response().sendFile(f.getAbsolutePath());
                            
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
	
	
    public void importIcal(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                final String calendarId = request.params().get(CALENDAR_ID_PARAMETER);
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject object) {
                        String icsContent = object.getString("ics");
                        eventService.importIcal(calendarId, icsContent, user, defaultResponseHandler(request));
                    }
                });
                
            }
        });
    }
    
    /**
     * Notify moderators that an event has been created or updated
     */
    private void notifyEventCreatedOrUpdated(final HttpServerRequest request, final UserInfos user, final JsonObject message, final boolean isCreated) {

        final long id = message.getLong("id", 0L);
        final String startDate = message.getString("start_date", null);
        final String endDate = message.getString("end_date", null);

        final String eventType = isCreated ? EVENT_CREATED_EVENT_TYPE : EVENT_UPDATED_EVENT_TYPE;
        final String template = isCreated ? "notify-booking-created.html" : "notify-booking-updated.html";

        if (id == 0L || startDate == null || endDate == null) {
            log.error("Could not get eventId, start_date or end_date from response. Unable to send timeline " + eventType + " notification.");
            return;
        }
        final String eventId = Long.toString(id);

        
        /* eventService.retrieve(calendarId, eventId, user, handler);
        bookingService.getResourceName(bookingId, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight() && event.right().getValue() != null && event.right().getValue().size() > 0) {

                    final String resourceName = event.right().getValue().getString("resource_name");

                    bookingService.getModeratorsIds(bookingId, user, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight() && event.right() != null) {
                                notifyModerators(request, user, event.right().getValue(), bookingId, startDate, endDate, resourceName, eventType, template);
                            } else {
                                log.error("Error when calling service getModeratorsIds. Unable to send timeline " + eventType + " notification.");
                            }
                        }
                    });

                } else {
                    log.error("Error when calling service getResourceName. Unable to send timeline " + eventType + " notification.");
                }
            }
        });*/
    }
}
