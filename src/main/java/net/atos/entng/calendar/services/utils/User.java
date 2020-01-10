package net.atos.entng.calendar.services.utils;

import io.vertx.core.json.JsonObject;

public class User {

    private String userId;
    private String displayName;

    User(String userId, String displayName){
        this.userId = userId;
        this.displayName = displayName;

    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    JsonObject toJson(){
        return new JsonObject().put("userId",userId).put("displayName",displayName);
    }
}
