package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;

public class User {

    private final String id;
    private final String displayName;

    public User(JsonObject user) {
        this.id = user.getString("id", "");
        this.displayName = user.getString("displayName", "");
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }
}