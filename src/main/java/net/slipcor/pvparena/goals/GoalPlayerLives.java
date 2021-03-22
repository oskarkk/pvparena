package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Goal class "PlayerLives"
 * </pre>
 * <p/>
 * The first Arena Goal. Players have lives. When every life is lost, the player
 * is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalPlayerLives extends ArenaGoal {

    public static final String SPAWN = "spawn";

    public GoalPlayerLives() {
        super("PlayerLives");
    }

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean checkEnd() {
        debug(this.arena, "checkEnd - " + this.arena.getName());

        if (!this.arena.isFreeForAll()) {
            debug(this.arena, "TEAMS!");
            final int count = TeamManager.countActiveTeams(this.arena);
            debug(this.arena, "count: " + count);

            return (count <= 1); // yep. only one team left. go!
        }

        debug(this.arena, "lives: " + StringParser.joinSet(this.getPlayerLifeMap().keySet(), "|"));
        final int count = this.getPlayerLifeMap().size();
        return (count <= 1); // yep. only one team left. go!
    }

    @Override
    public Set<String> checkForMissingSpawns(final Set<String> list) {
        if (this.arena.isFreeForAll()) {
            return this.checkForMissingFFASpawn(list);
        }
        return this.checkForMissingTeamSpawn(list);
    }

    @Override
    public Boolean checkPlayerDeath(Player player) {
        int pos;
        if(this.arena.isFreeForAll()) {
            pos = this.getPlayerLifeMap().get(player);
        } else {
            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
            pos = this.getTeamLifeMap().get(arenaPlayer.getArenaTeam());
        }
        debug(this.arena, player, "lives before death: " + pos);
        return pos > 1;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[LIVES] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);

        for (final ArenaTeam arenaTeam : this.arena.getNotEmptyTeams()) {
            for (final ArenaPlayer arenaPlayer : arenaTeam.getTeamMembers()) {
                if (arenaPlayer.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                if (this.arena.isFreeForAll()) {
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                            "END");
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.PLAYER_HAS_WON, arenaPlayer.getName()),
                            "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.PLAYER_HAS_WON,
                            arenaPlayer.getName()));
                } else {
                    ArenaModuleManager.announce(
                            this.arena,
                            Language.parse(this.arena, MSG.TEAM_HAS_WON,
                                    arenaTeam.getColoredName()), "END");
                    ArenaModuleManager.announce(
                            this.arena,
                            Language.parse(this.arena, MSG.TEAM_HAS_WON,
                                    arenaTeam.getColoredName()), "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.TEAM_HAS_WON,
                            arenaTeam.getColoredName()));
                    break;
                }
            }

            if (ArenaModuleManager.commitEnd(this.arena, arenaTeam)) {
                if (this.arena.realEndRunner == null) {
                    this.endRunner = new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                            CFG.TIME_ENDCOUNTDOWN));
                }
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getArenaConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn,
                                  final PlayerDeathEvent event) {

        ArenaTeam arenaTeam = ArenaPlayer.fromPlayer(player).getArenaTeam();
        if(this.arena.isFreeForAll()){
            if (!this.getPlayerLifeMap().containsKey(player)) {
                return;
            }
        } else {
            if (!this.getTeamLifeMap().containsKey(arenaTeam)) {
                return;
            }
        }

        if (doesRespawn) {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        }
        final int currentPlayerOrTeamLive;
        if(this.arena.isFreeForAll()){
            currentPlayerOrTeamLive = this.getPlayerLifeMap().get(player);
        } else {
            currentPlayerOrTeamLive = this.getTeamLifeMap().get(arenaTeam);
        }
        debug(this.arena, player, "lives before death: " + currentPlayerOrTeamLive);
        if (currentPlayerOrTeamLive <= 1) {
            if(this.arena.isFreeForAll()){
                this.getPlayerLifeMap().remove(player);
            } else {
                this.getTeamLifeMap().remove(arenaTeam);
            }
            ArenaPlayer.fromPlayer(player).setStatus(PlayerStatus.LOST);
            if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                debug(this.arena, player, "faking player death");
                PlayerListener.finallyKillPlayer(this.arena, player, event);
            }
            // player died => commit death!
            WorkflowManager.handleEnd(this.arena, false);
        } else {
            int nextPlayerOrTeamLive = currentPlayerOrTeamLive - 1;
            if(this.arena.isFreeForAll()){
                this.getPlayerLifeMap().put(player, nextPlayerOrTeamLive);
            } else {
                this.getTeamLifeMap().put(arenaTeam, nextPlayerOrTeamLive);
            }

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, event, nextPlayerOrTeamLive);
                } else {
                    this.broadcastSimpleDeathMessage(player, event);
                }
            }

            final List<ItemStack> returned;

            if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(player);
                event.getDrops().clear();
            } else {
                returned = new ArrayList<>(event.getDrops());
            }

            WorkflowManager.handleRespawn(this.arena, ArenaPlayer.fromPlayer(player), returned);

        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
    }

    @Override
    public int getLives(ArenaPlayer arenaPlayer) {
        // player lives
        if (this.arena.isFreeForAll()) {
            return this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
        }

        // team lives
        if (this.getTeamLifeMap().containsKey(arenaPlayer.getArenaTeam())) {
            return this.getTeamLifeMap().getOrDefault(arenaPlayer.getArenaTeam(), 0);
        }

        // sum of all team members lives
        return arenaPlayer.getArenaTeam().getTeamMembers().stream()
                .mapToInt(ap -> this.getPlayerLifeMap().getOrDefault(ap.getPlayer(), 0))
                .sum();
    }

    @Override
    public boolean hasSpawn(final String string) {
        if (this.arena.isFreeForAll()) {

            if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                for (final ArenaClass aClass : this.arena.getClasses()) {
                    if (string.toLowerCase().startsWith(
                            aClass.getName().toLowerCase() + SPAWN)) {
                        return true;
                    }
                }
            }
            return string.toLowerCase().startsWith(SPAWN);
        }
        for (final String teamName : this.arena.getTeamNames()) {
            if (string.toLowerCase().startsWith(
                    teamName.toLowerCase() + SPAWN)) {
                return true;
            }
            if (this.arena.getArenaConfig().getBoolean(CFG.GENERAL_CLASSSPAWN)) {
                for (final ArenaClass aClass : this.arena.getClasses()) {
                    if (string.toLowerCase().startsWith(teamName.toLowerCase() +
                            aClass.getName().toLowerCase() + SPAWN)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void initiate(final Player player) {
        this.updateLives(player, this.arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(
                    this.getName() + ": player NULL");
            return;
        }
        if(this.arena.isFreeForAll()) {
            this.getPlayerLifeMap().remove(player);
        } else {
            this.getTeamLifeMap().remove(ArenaPlayer.fromPlayer(player).getArenaTeam());
        }
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap.getPlayer(), this.arena.getArenaConfig().getInt(CFG.GOAL_PLIVES_LIVES));
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
        this.getTeamLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
        if (this.arena.isFreeForAll()) {
            return;
        }

        if (config.get("teams.free") != null) {
            config.set("teams", null);
        }
        if (config.get("teams") == null) {
            debug(this.arena, "no teams defined, adding custom red and blue!");
            config.addDefault("teams.red", ChatColor.RED.name());
            config.addDefault("teams.blue", ChatColor.BLUE.name());
        }
        if (this.arena.getArenaConfig().getBoolean(CFG.GOAL_FLAGS_WOOLFLAGHEAD)
                && config.get("flagColors") == null) {
            debug(this.arena, "no flagheads defined, adding white and black!");
            config.addDefault("flagColors.red", "WHITE");
            config.addDefault("flagColors.blue", "BLACK");
        }
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer ap : this.arena.getFighters()) {
            if (this.arena.isFreeForAll()) {
                double score = this.getPlayerLifeMap().getOrDefault(ap.getPlayer(), 0);
                if (scores.containsKey(ap.getName())) {
                    scores.put(ap.getName(), scores.get(ap.getName()) + score);
                } else {
                    scores.put(ap.getName(), score);
                }
            } else {
                double score = this.getTeamLifeMap().getOrDefault(ap.getArenaTeam(), 0);
                if (ap.getArenaTeam() == null) {
                    continue;
                }
                if (scores.containsKey(ap.getArenaTeam().getName())) {
                    scores.put(ap.getArenaTeam().getName(),
                            scores.get(ap.getName()) + score);
                } else {
                    scores.put(ap.getArenaTeam().getName(), score);
                }
            }
        }

        return scores;
    }
}
