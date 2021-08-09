/*
 * Copyright © Région Nord Pas de Calais-Picardie,  Département 91, Région Aquitaine-Limousin-Poitou-Charentes, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package net.atos.entng.calendar.services;


import io.vertx.core.Future;
import net.atos.entng.calendar.models.User;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface UserService {

    /**
     * fetch users from shared data
     *
     * @param ids             list of ids being user or group identifier {@link List<String>}
     * @param user            user info (in this case, we using only its id) {@link UserInfos}
     *
     * @return {@link Future} of {@link List<User>} containing list of user fetched
     */
    Future<List<User>> fetchUser(List<String> ids, UserInfos user);
}

