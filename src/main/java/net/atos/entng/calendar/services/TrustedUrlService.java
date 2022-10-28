package net.atos.entng.calendar.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TrustedUrlService {
    /**
     * Return a trusted url by id
     * @param id The id of the trusted url
     * @return {@link Future<JsonObject>} Future containing the info of the trusted url (Title, regex)
     */
    Future<JsonObject> retrieve(String id);

    /**
     * Return a list of all the trusted urls
     * @return {@link Future<JsonArray>} The trusted urls infos (Title, regex)
     */
    Future<JsonArray> retrieveAll();

    /**
     * Create a trusted url
     * @param body The fields of the new trusted url
     * @return {@link Future<Void>} Future response
     */
    Future<Void> create(JsonObject body);

    /**
     * Update trusted url
     * @param id the id of the trusted url to update
     * @param body the fields to change
     * @return {@link Future<Void>} Future response
     */
    Future<Void> update(String id, JsonObject body);

    /**
     * Delete trusted url
     * @param id the id of the trusted url we want to delete
     * @return {@link Future<Void>} Future response
     */
    Future<Void> delete(String id);
}
