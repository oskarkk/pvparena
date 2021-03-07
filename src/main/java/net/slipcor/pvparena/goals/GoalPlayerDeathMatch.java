package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "PlayerDeathMatch"
 * </pre>
 * <p/>
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalPlayerDeathMatch extends ArenaGoal {
    public GoalPlayerDeathMatch() {
        super("PlayerDeathMatch");
    }

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getArenaConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getPlayerLifeMap().size();

        return count <= 1; // yep. only one player left. go!
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (!this.arena.isFreeForAll()) {
            return null; // teams are handled somewhere else
        }

        return this.checkForMissingSpawn(list);
    }

    @Override
    public Boolean checkPlayerDeath(Player player) {
        return true;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[PDM] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != Status.FIGHT) {
                    continue;
                }
                ArenaModuleManager.announce(this.arena,
                        Language.parse(this.arena, MSG.PLAYER_HAS_WON, ap.getName()),
                        "END");
                ArenaModuleManager.announce(this.arena,
                        Language.parse(this.arena, MSG.PLAYER_HAS_WON, ap.getName()),
                        "WINNER");

                this.arena.broadcast(Language.parse(this.arena, MSG.PLAYER_HAS_WON, ap.getName()));
            }
            if (ArenaModuleManager.commitEnd(this.arena, team)) {
                return;
            }
        }
        this.endRunner = new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn,
                                  final PlayerDeathEvent event) {

        if (player.getKiller() == null
                || !this.getPlayerLifeMap().containsKey(player.getKiller())
                || player.equals(player.getKiller())) {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerKill:" + player.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastSimpleDeathMessage(player, event);
            }

            final List<ItemStack> returned;

            if (this.arena.getArenaConfig().getBoolean(
                    CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(player);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            WorkflowManager.handleRespawn(this.arena, ArenaPlayer.fromPlayer(player), returned);

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_SUICIDEPUNISH)) {
                for (ArenaPlayer ap : this.arena.getFighters()) {
                    if (player.equals(ap.getPlayer())) {
                        continue;
                    }
                    if (this.increaseScore(ap.getPlayer(), player)) {
                        return;
                    }
                }
            }

            return;
        }
        final Player killer = player.getKiller();
        int iLives = this.getPlayerLifeMap().get(killer);
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerKill:" + killer.getName() + ':' + player.getName(), "playerDeath:" + player.getName());
        Bukkit.getPluginManager().callEvent(gEvent);

        if (this.increaseScore(killer, player)) {
            return;
        }

        if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
            if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_FRAGS, player, event, iLives-1);
            } else {
                this.broadcastSimpleDeathMessage(player, event);
            }
        }

        final List<ItemStack> returned;

        if (this.arena.getArenaConfig().getBoolean(
                CFG.PLAYER_DROPSINVENTORY)) {
            returned = InventoryManager.drop(player);
            event.getDrops().clear();
        } else {
            returned = new ArrayList<>(event.getDrops());
        }

        WorkflowManager.handleRespawn(this.arena, ArenaPlayer.fromPlayer(player), returned);
    }

    private boolean increaseScore(Player killer, Player killed) {
        int iLives = this.getPlayerLifeMap().get(killer);
        debug(this.arena, killer, "kills to go: " + iLives);
        if (iLives <= 1) {
            // player has won!
            final Set<ArenaPlayer> arenaPlayers = new HashSet<>();
            for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
                if (arenaPlayer.getName().equals(killer.getName())) {
                    continue;
                }
                arenaPlayers.add(arenaPlayer);
            }
            for (final ArenaPlayer arenaPlayer : arenaPlayers) {
                this.getPlayerLifeMap().remove(arenaPlayer.getPlayer());

                arenaPlayer.setStatus(Status.LOST);
                arenaPlayer.addLosses();

                if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                    debug(this.arena, arenaPlayer.getPlayer(), "faking player death");
                    PlayerListener.finallyKillPlayer(this.arena, arenaPlayer.getPlayer(), killed.getLastDamageCause());
                }

                if (ArenaManager.checkAndCommit(this.arena, false)) {
                    this.arena.unKillPlayer(killed,
                            killed.getLastDamageCause() != null ?
                            killed.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.VOID, killer);
                    return true;
                }
            }

            if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                debug(this.arena, killed, "faking player death");
                PlayerListener.finallyKillPlayer(this.arena, killed, killed.getLastDamageCause());
            }

            WorkflowManager.handleEnd(this.arena, false);
            return true;
        }
        iLives--;
        this.getPlayerLifeMap().put(killer, iLives);
        return false;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_PDM_LIVES));
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
    }

    @Override
    public boolean hasSpawn(final String string) {

        if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            for (final ArenaClass aClass : this.arena.getClasses()) {
                if (string.toLowerCase().startsWith(
                        aClass.getName().toLowerCase() + "spawn")) {
                    return true;
                }
            }
        }
        return this.arena.isFreeForAll() && string.toLowerCase()
                .startsWith("spawn");
    }

    @Override
    public void initiate(final Player player) {
        this.updateLives(player, this.arena.getArenaConfig().getInt(CFG.GOAL_PDM_LIVES));
    }



    @Override
    public void lateJoin(final Player player) {
        this.initiate(player);
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(player);
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap.getPlayer(), this.arena.getArenaConfig().getInt(CFG.GOAL_PDM_LIVES));
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.arena.getArenaConfig().getInt(CFG.GOAL_PDM_LIVES)
                    - (this.getPlayerLifeMap()
                    .getOrDefault(arenaPlayer.getPlayer(), 0));
            if (scores.containsKey(arenaPlayer.getName())) {
                scores.put(arenaPlayer.getName(), scores.get(arenaPlayer.getName()) + score);
            } else {
                scores.put(arenaPlayer.getName(), score);
            }
        }

        return scores;
    }

    @Override
    public void unload(final Player player) {
        this.getPlayerLifeMap().remove(player);
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }
}
