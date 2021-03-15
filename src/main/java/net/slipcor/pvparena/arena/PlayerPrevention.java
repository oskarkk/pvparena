package net.slipcor.pvparena.arena;

/**
 * PlayerPrevention
 *
 * <pre>
 * BREAK - Block break
 * PLACE - Block placement
 * TNT - TNT usage
 * TNTBREAK - TNT block break
 * DROP - dropping items
 * INVENTORY - accessing inventory
 * PICKUP - picking up stuff
 * CRAFT - crafting stuff
 * </pre>
 */
public enum PlayerPrevention {
    BREAK,
    PLACE,
    TNT,
    TNTBREAK,
    DROP,
    INVENTORY,
    PICKUP,
    CRAFT;

    public static boolean has(int value, PlayerPrevention s) {
        return (((int) Math.pow(2, s.ordinal()) & value) > 0);
    }
}
