package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;

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

    public JsonObject toJson() {
        JsonObject userObject = new JsonObject();

        userObject.put(Field.USERID, this.id);
        userObject.put(Field.DISPLAYNAME, this.displayName);

        return userObject;
    }

}
