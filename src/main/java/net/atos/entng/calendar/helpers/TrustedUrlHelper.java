package net.atos.entng.calendar.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.atos.entng.calendar.core.constants.Field;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrustedUrlHelper {

    protected static final Logger log = LoggerFactory.getLogger(TrustedUrlHelper.class);

    private TrustedUrlHelper() {
    }

    /**
     * Create a list of pattern representing the regex of our trusted URLs
     *
     * @param trustedUrlList the list of trusted URLs from which we want our regexes
     * @return {@link List<Pattern>} A list containing all the patterns representing our regexes
     */
    private static List<Pattern> createRegexList(JsonArray trustedUrlList) {
        return trustedUrlList.stream()
                .map(JsonObject.class::cast)
                .map(trustedUrl -> Pattern.compile(trustedUrl.getString(Field.REGEX), Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    /**
     * Check if the given url match one of our regexes obtained from our trusted URLs
     *
     * @param targetUrl the url to check
     * @return Boolean with the result of the check
     */
    public static boolean checkUrlInRegex(String targetUrl, JsonArray trustedUrlList) {
        List<Pattern> regexList = createRegexList(trustedUrlList);
        return regexList.stream().anyMatch(regex -> regex.matcher(targetUrl).matches());
    }
}
