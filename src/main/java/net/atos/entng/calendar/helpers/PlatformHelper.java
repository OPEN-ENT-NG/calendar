package net.atos.entng.calendar.helpers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.helpers.UserHelper;
import net.atos.entng.calendar.core.constants.Field;
import net.atos.entng.calendar.core.enums.ExternalICalEventBusActions;
import net.atos.entng.calendar.ical.ExternalImportICal;
import net.atos.entng.calendar.services.PlatformService;
import net.atos.entng.calendar.services.ServiceFactory;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class PlatformHelper extends MongoDbControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(PlatformHelper.class);

    private final PlatformService platformService;

    public PlatformHelper(String collection, ServiceFactory serviceFactory) {
        super(collection, null);
        this.platformService = serviceFactory.platformService();
    }

    /**
     * Create a list of pattern representing the regex of our platforms
     *
     * @param platformsList the list of platforms from which we want our regexes
     * @return {@link List<Pattern>} A list containing all the patterns representing our regexes
     */
    private static List<Pattern> createRegexList(JsonArray platformsList) {
        return platformsList.stream()
                .map(JsonObject.class::cast)
                .map(platform -> Pattern.compile(platform.getString(Field.REGEX), Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    /**
     * Check if the given url match one of our regexes obtained from our platforms
     *
     * @param targetUrl the url to check
     * @return Boolean with the result of the check
     */
    public static boolean checkUrlInRegex(String targetUrl, JsonArray platformsList) {
        List<Pattern> regexList = createRegexList(platformsList);
        return regexList.stream().anyMatch(regex -> regex.matcher(targetUrl).matches());
    }

    public Future<Boolean> checkCalendarPlatform(UserInfos user, JsonObject calendar) {
        Promise<Boolean> promise = Promise.promise();

        String url = calendar.getString(Field.ICSLINK, null);
        String platform = calendar.getString(Field.PLATFORM, null);

        if (!StringsHelper.isNullOrEmpty(url) && StringsHelper.isNullOrEmpty(platform)) {
            String finalUrl = url.trim();
            this.platformService.retrieveAll()
                    .onSuccess(platformList -> promise.complete(PlatformHelper.checkUrlInRegex(finalUrl, platformList)))
                    .onFailure(error -> {
                        String message = String.format("[Calendar@%s::checkCalendarPlatform]:  an error has occurred during " +
                                "platform check: %s", this.getClass().getSimpleName(), error.getMessage());
                        log.error(message);
                        promise.fail("calendar.error.during.platform.check");
                    });
        } else if (StringsHelper.isNullOrEmpty(url) && !StringsHelper.isNullOrEmpty(platform)) {
            switch (platform) {
                case Field.ZIMBRA:
                    promise.complete(UserHelper.userHasApp(user, Field.ZIMBRA) && config.getBoolean(Field.ENABLE_ZIMBRA, false));
                    break;
                default:
                    String message = String.format("[Calendar@%s::checkCalendarPlatform]:  an error has occurred during " +
                            "platform check : platform is not valid", this.getClass().getSimpleName());
                    log.error(message);
                    promise.fail("calendar.error.platform.not.valid");
                    break;
            }
        } else {
            String message = String.format("[Calendar@%s::checkCalendarPlatform]: an error has occurred during " +
                    "platform check : object is missing data", this.getClass().getSimpleName());
            promise.fail(message);
        }

        return promise.future();
    }

}
