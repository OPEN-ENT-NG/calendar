package net.atos.entng.calendar.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PlatformService {
    /**
     * Return a platform by id
     * @param id The id of the platform
     * @return {@link Future<JsonObject>} Future containing the info of the platform (Title, regex)
     */
    Future<JsonObject> retrieve(String id);

    /**
     * Return a list of all the platforms
     * @return {@link Future<JsonArray>} The platforms infos (Title, regex)
     */
    Future<JsonArray> retrieveAll();

    /**
     * Create a platform
     * @param body The fields of the new platform
     * @return {@link Future<Void>} Future response
     */
    Future<Void> create(JsonObject body);

    /**
     * Update platform
     * @param id the id of the platform to update
     * @param body the fields to change
     * @return {@link Future<Void>} Future response
     */
    Future<Void> update(String id, JsonObject body);

    /**
     * Delete platform
     * @param id the id of the platform we want to delete
     * @return {@link Future<Void>} Future response
     */
    Future<Void> delete(String id);
}
