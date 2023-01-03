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
}
