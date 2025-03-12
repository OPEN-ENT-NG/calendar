package net.atos.entng.calendar.helpers;

import org.entcore.common.user.*;

import java.util.*;

public class WorkflowHelper {

    private WorkflowHelper() {
    }

    public static boolean hasRight(UserInfos user, String action) {
        List<UserInfos.Action> actions = user.getAuthorizedActions();
        return actions.stream().anyMatch(a -> action.equals(a.getDisplayName()));
    }
}

