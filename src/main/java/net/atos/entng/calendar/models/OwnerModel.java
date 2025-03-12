package net.atos.entng.calendar.models;

import io.vertx.core.json.JsonObject;
import net.atos.entng.calendar.core.constants.Field;

public class OwnerModel {
    private String userId;
    private String displayName;

    public OwnerModel(JsonObject user) {
        this.userId = user.getString(Field.USERID, "");
        this.displayName = user.getString(Field.DISPLAYNAME, "");
    }

    public String userId() {
        return this.userId;
    }

    public String displayName() {
        return this.displayName;
    }

    public JsonObject toJson() {
        JsonObject userObject = new JsonObject();

        userObject.put(Field.USERID, this.userId);
        userObject.put(Field.DISPLAYNAME, this.displayName);

        return userObject;
    }
}
