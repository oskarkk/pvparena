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
}
