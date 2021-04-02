package net.slipcor.pvparena.core;

import java.util.Collection;

/**
 * Utility class for collections
 */
public class CollectionUtils {

    private CollectionUtils() {
        // Static class can not be instantiate
    }

    public static boolean isNotEmpty(Collection<?> collection){
        return collection != null && !collection.isEmpty();
    }

    public static boolean containsIgnoreCase(Collection<String> collection, String value) {
        return isNotEmpty(collection) && collection.stream().anyMatch(e -> e.equalsIgnoreCase(value));
    }

}
