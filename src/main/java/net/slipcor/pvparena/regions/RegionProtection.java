package net.slipcor.pvparena.regions;

/**
 * RegionProtection
 * <p/>
 * <pre>
 * BREAK - Block break
 * FIRE - Fire
 * MOBS - Mob spawning
 * NATURE - Environment changes (leaves, shrooms, water, lava)
 * PAINTING - Painting placement/destruction
 * PISTON - Piston triggering
 * PLACE - Block placement
 * TNT - TNT usage
 * TNTBREAK - TNT block break
 * DROP - Player dropping items
 * INVENTORY - Player accessing inventory
 * PICKUP - Player picking up stuff
 * CRAFT - Player crafting stuff
 * TELEPORT - Player teleporting
 * </pre>
 */
public enum RegionProtection {
    BREAK, FIRE, MOBS, NATURE, PAINTING, PISTON, PLACE, TNT, TNTBREAK, DROP, INVENTORY, PICKUP, CRAFT, TELEPORT
}
