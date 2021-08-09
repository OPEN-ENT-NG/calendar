package net.atos.entng.calendar.helpers;

import io.vertx.core.json.JsonArray;
import net.atos.entng.calendar.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserHelper {

    private UserHelper() { throw new IllegalStateException("Helper class"); }

    /**
     * "convert" Array widgets into list User {@link User}
     *
     * using "for loop" instead of .stream() since this JsonArray is filled with LinkedHashMap instead of JsonObject
     * else .stream() might encounter "LinkedHashMap" cannot cast to JsonObject
     *
     * @param usersArray                List of user {@link JsonArray}
     * @return  {@link List<User>}      List of user with model
     */
    public static List<User> usersList(JsonArray usersArray) {
        List<User> usersList = new ArrayList<>();
        for (int i = 0; i < usersArray.size(); i++) {
            User user = new User(usersArray.getJsonObject(i));
            usersList.add(user);
        }
        return usersList;
    }

}
