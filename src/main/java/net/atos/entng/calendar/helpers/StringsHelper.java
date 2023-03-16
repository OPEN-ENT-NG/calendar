package net.atos.entng.calendar.helpers;

public class StringsHelper {

    /**
     * Tells whether the given string is either {@code} or consists solely of whitespace characters.
     *
     * @param target string to check
     * @return {@code true} if the target string is null or empty
     */
    public static boolean isNullOrEmpty( String target ) {
        return target == null || target.isEmpty();
    }

    /**
     * Convert a string in CamelCase to snake_case
     */
    public static String camelToSnake(String str)
    {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.replaceAll("\\B([A-Z])", "_$1").toLowerCase();
    }
}
