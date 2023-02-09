package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import net.atos.entng.calendar.services.impl.DefaultUserServiceImpl;
import net.atos.entng.calendar.services.impl.PlatformServiceImpl;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

public class ServiceFactory {
    private final Vertx vertx;
    private final Neo4j neo4j;
    private final Sql sql;
    private final MongoDb mongoDb;
    private WebClient webClient;

    public ServiceFactory(Vertx vertx, Neo4j neo4j, Sql sql, MongoDb mongoDb) {
        this.vertx = vertx;
        this.neo4j = neo4j;
        this.sql = sql;
        this.mongoDb = mongoDb;
        this.webClient = null;
    }

    public ServiceFactory(Vertx vertx, Neo4j neo4j, Sql sql, MongoDb mongoDb, WebClient webClient) {
        this(vertx, neo4j, sql, mongoDb);
        this.webClient = webClient;
    }

    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }

    public UserService userService() {
        return new DefaultUserServiceImpl(neo4j);
    }

    public CalendarService calendarService() {
        return new CalendarServiceImpl(Calendar.CALENDAR_COLLECTION, mongoDb);
    }

    public PlatformService platformService(){
        return new PlatformServiceImpl(Calendar.PLATFORMS_COLLECTION,mongoDb);
    }

    public ServiceFactory setWebClient(WebClient webClient) {
        this.webClient = webClient;
        return this;
    }

    public WebClient webClient() {
        return this.webClient;
    }

    public JsonObject getConfig() {
        return this.vertx.getOrCreateContext().config();
    }
}
