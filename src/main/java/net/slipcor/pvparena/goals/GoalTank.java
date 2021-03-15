package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.events.PATeamChangeEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

public class GoalTank extends ArenaGoal {
    public GoalTank() {
        super("Tank");
    }

    private static final Map<Arena, ArenaPlayer> tanks = new HashMap<>();

    private EndRunnable endRunner;

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean checkEnd() {
        final int count = this.getPlayerLifeMap().size();
        return (count <= 1 || tanks.get(this.arena).getStatus() != PlayerStatus.FIGHT);
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (!this.arena.isFreeForAll()) {
            return null; // teams are handled somewhere else
        }

        if (!list.contains("tank")) {
            return "tank";
        }

        return this.checkForMissingSpawn(list);
    }

    @Override
    public Boolean checkPlayerDeath(final Player player) {
        if (this.getPlayerLifeMap().containsKey(player)) {
            final int iLives = this.getPlayerLifeMap().get(player);
            debug(this.arena, player, "lives before death: " + iLives);
            return iLives > 1 && !tanks.get(this.arena).equals(player.getName());
        }
        return true;
    }

    @Override
    public boolean overridesStart() {
        return true;
    }

    @Override
    public void commitEnd(final boolean force) {
        if (this.endRunner != null) {
            return;
        }
        if (this.arena.realEndRunner != null) {
            debug(this.arena, "[TANK] already ending");
            return;
        }
        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "");
        Bukkit.getPluginManager().callEvent(gEvent);
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (ap.getStatus() != PlayerStatus.FIGHT) {
                    continue;
                }
                if (tanks.containsValue(ap.getName())) {
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_TANK_TANKWON, ap.getName()), "END");
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_TANK_TANKWON, ap.getName()), "WINNER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_TANK_TANKWON, ap.getName()));
                } else {

                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_TANK_TANKDOWN), "END");
                    ArenaModuleManager.announce(this.arena,
                            Language.parse(this.arena, MSG.GOAL_TANK_TANKDOWN), "LOSER");

                    this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_TANK_TANKDOWN));
                }
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
        if (!this.getPlayerLifeMap().containsKey(player)) {
            return;
        }
        int iLives = this.getPlayerLifeMap().get(player);
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        debug(this.arena, player, "lives before death: " + iLives);
        if (iLives <= 1 || tanks.get(this.arena).equals(arenaPlayer)) {

            if (tanks.get(this.arena).equals(arenaPlayer)) {

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "tank", "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            } else if (doesRespawn) {

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            } else {

                final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
                Bukkit.getPluginManager().callEvent(gEvent);
            }


            this.getPlayerLifeMap().remove(player);
            if (this.arena.getArenaConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                debug(this.arena, player, "faking player death");
                PlayerListener.finallyKillPlayer(this.arena, player, event);
            }

            ArenaPlayer.fromPlayer(player).setStatus(PlayerStatus.LOST);
            // player died => commit death!
            WorkflowManager.handleEnd(this.arena, false);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
            iLives--;
            this.getPlayerLifeMap().put(player, iLives);

            if (this.arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, event, iLives);
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
    }

    @Override
    public void commitStart() {
        this.parseStart(); // hack the team in before spawning, derp!
        for (final ArenaTeam team : this.arena.getTeams()) {
            SpawnManager.distribute(this.arena, team);
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("lives: "
                + this.arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
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
                .startsWith("spawn") || "tank".equals(string);
    }

    @Override
    public void initiate(final Player player) {
        this.getPlayerLifeMap().put(player, this.arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
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
        if (this.arena.getTeam("tank") != null) {
            return;
        }
        ArenaPlayer tank = null;
        final Random random = new Random();
        for (final ArenaTeam team : this.arena.getTeams()) {
            int pos = random.nextInt(team.getTeamMembers().size());
            debug(this.arena, "team " + team.getName() + " random " + pos);
            for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                debug(this.arena, arenaPlayer.getPlayer(), "#" + pos + ": " + arenaPlayer);
                if (pos-- == 0) {
                    tank = arenaPlayer;
                }
                this.getPlayerLifeMap().put(arenaPlayer.getPlayer(), this.arena.getArenaConfig().getInt(CFG.GOAL_TANK_LIVES));
            }
        }
        final ArenaTeam tankTeam = new ArenaTeam("tank", "PINK");

        assert tank != null;
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (team.getTeamMembers().contains(tank)) {
                final PATeamChangeEvent tcEvent = new PATeamChangeEvent(this.arena, tank.getPlayer(), team, tankTeam);
                Bukkit.getPluginManager().callEvent(tcEvent);
                this.arena.updateScoreboardTeam(tank.getPlayer(), team, tankTeam);
                team.remove(tank);
            }
        }
        tankTeam.add(tank);
        tanks.put(this.arena, tank);

        final ArenaClass tankClass = this.arena.getClass("%tank%");
        if (tankClass != null) {
            tank.setArenaClass(tankClass);
            InventoryManager.clearInventory(tank.getPlayer());
            tankClass.equip(tank.getPlayer());
            for (final ArenaModule mod : this.arena.getMods()) {
                mod.parseRespawn(tank.getPlayer(), tankTeam, DamageCause.CUSTOM,
                        tank.getPlayer());
            }
        }

        this.arena.broadcast(Language.parse(this.arena, MSG.GOAL_TANK_TANKMODE, tank.getName()));

        final Set<PASpawn> spawns = new HashSet<>(SpawnManager.getPASpawnsStartingWith(this.arena, "tank"));

        int pos = spawns.size();

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.arena.tpPlayerToCoordName(tank, spawn.getName());
                break;
            }
        }

        this.arena.getTeams().add(tankTeam);
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
        tanks.remove(this.arena);
        this.arena.getTeams().remove(this.arena.getTeam("tank"));
    }

    @Override
    public Map<String, Double> timedEnd(final Map<String, Double> scores) {

        for (final ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            double score = this.getPlayerLifeMap().getOrDefault(arenaPlayer.getPlayer(), 0);
            if (tanks.containsValue(arenaPlayer)) {
                score *= this.arena.getFighters().size();
            }
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
    }
}
