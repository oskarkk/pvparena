package net.slipcor.pvparena.core;

public class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().length() == 0;
    }

    public static boolean notBlank(String string) {
        return string != null && string.trim().length() > 0;
    }

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static boolean notEmpty(String string) {
        return string != null && string.length() > 0;
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return (str1 == null && str2 == null) || str1 != null && str1.equalsIgnoreCase(str2);
    }

    public static boolean startsWithIgnoreCase(String string, String prefix) {
        return string != null && prefix != null && string.toLowerCase().startsWith(prefix.toLowerCase());
    }

    public static boolean endsWithIgnoreCase(String string, String suffix) {
        return string != null && suffix != null && string.toLowerCase().endsWith(suffix.toLowerCase());
    }
}
