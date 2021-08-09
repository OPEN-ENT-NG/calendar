package net.atos.entng.calendar.services;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.atos.entng.calendar.Calendar;
import net.atos.entng.calendar.services.impl.CalendarServiceImpl;
import net.atos.entng.calendar.services.impl.DefaultUserServiceImpl;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;

public class ServiceFactory {
    private final Vertx vertx;
    private final Neo4j neo4j;
    private final Sql sql;
    private final MongoDb mongoDb;

    public ServiceFactory(Vertx vertx, Neo4j neo4j, Sql sql, MongoDb mongoDb) {
        this.vertx = vertx;
        this.neo4j = neo4j;
        this.sql = sql;
        this.mongoDb = mongoDb;
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
}
