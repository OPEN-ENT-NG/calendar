package net.atos.entng.calendar.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlatformHelper {

    protected static final Logger log = LoggerFactory.getLogger(PlatformHelper.class);

    private PlatformHelper() {
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
}
