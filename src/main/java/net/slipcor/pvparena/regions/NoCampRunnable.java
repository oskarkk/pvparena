package net.slipcor.pvparena.regions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * Auto-managed runnable used to check and punish camping players in NOCAMP regions
 *
 * The runnable only checks the timestamp of last movement, movement check is done beforehand (in listener).
 * Because listener is triggered only when player moves, the task in this runnable punishes player when their position
 * can't be updated.
 * The runner auto-removes players leaving the arena
 */
public class NoCampRunnable implements Runnable {
    private final int campDamage;
    private final Map<ArenaPlayer, Long> lastMovementMap = new HashMap<>();
    private final Set<ArenaPlayer> waitingRemoval = new HashSet<>();
    private BukkitTask task;

    public NoCampRunnable(int campDamage) {
        this.campDamage = campDamage;
    }

    /**
     * Save last player movement timestamp and starts the ticking task if needed
     * @param player Player who are moving in current NOCAMP region
     * @return true if player has no entry (i.e. player has just entered in the area)
     */
    public boolean updatePlayer(ArenaPlayer player) {
        Long previousValue = this.lastMovementMap.put(player, System.currentTimeMillis());
        this.startIfNeeded();
        return previousValue == null;
    }

    /**
     * Remove a player from ticking task checks
     * @param player Player who has just left current NOCAMP region
     */
    public void removePlayer(ArenaPlayer player) {
        this.lastMovementMap.remove(player);
    }

    @Override
    public void run() {
        // The runnable automatically stops itself if there is no longer players in current NOCAMP region
        if (this.lastMovementMap.isEmpty()) {
            this.stop();
        } else {
            this.lastMovementMap.entrySet().stream()
                    .filter(entry -> System.currentTimeMillis() > entry.getValue() + 500)
                    .forEach(entry -> {
                        ArenaPlayer arenaPlayer = entry.getKey();
                        Player player = arenaPlayer.getPlayer();

                        if(arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                            debug(arenaPlayer, "damaged in NOCAMP region");
                            player.setLastDamageCause(new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, this.campDamage));
                            player.damage(this.campDamage);
                        } else {
                            debug(arenaPlayer, "no longer fighting - removing from NOCAMP task");
                            this.waitingRemoval.add(arenaPlayer);
                        }
                    });

            if(!this.waitingRemoval.isEmpty()) {
                this.waitingRemoval.forEach(this.lastMovementMap::remove);
                this.waitingRemoval.clear();
            }
        }
    }

    public synchronized void stop() {
        if(this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private synchronized void startIfNeeded() {
        if(this.task == null) {
            this.task = Bukkit.getScheduler().runTaskTimer(PVPArena.getInstance(), this, 1, 10);
        }
    }
}
