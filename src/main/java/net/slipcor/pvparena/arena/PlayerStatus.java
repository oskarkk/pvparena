package net.slipcor.pvparena.arena;

/**
 * Status
 *
 * <pre>
 * - NULL = not part of an arena
 * - WARM = not part of an arena, warmed up
 * - LOUNGE = inside an arena lobby mode
 * - READY = inside an arena lobby mode, readied up
 * - FIGHT = fighting inside an arena
 * - WATCH = watching a fight from the spectator area
 * - DEAD = dead and soon respawning
 * - LOST = lost and thus spectating
 * </pre>
 */
public enum PlayerStatus {
    NULL,
    WARM,
    LOUNGE,
    READY,
    FIGHT,
    WATCH,
    DEAD,
    LOST
}
