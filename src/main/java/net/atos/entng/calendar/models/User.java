package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.helpers.IModelHelper;

public class User {

    private final String id;
    private final String userId;
    private final String displayName;

    public User(JsonObject user) {
        this.id = user.getString(Field.ID, "");
        this.userId = user.getString(Field.USERID, "");
        this.displayName = user.getString(Field.DISPLAYNAME, "");
    }

    public String id() {
        return this.id;
    }

    public String userId() {
        return this.userId;
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
