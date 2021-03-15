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

}
