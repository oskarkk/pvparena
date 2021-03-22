package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerState;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.events.PAStartEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.loadables.*;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import net.slipcor.pvparena.runnables.PVPActivateRunnable;
import net.slipcor.pvparena.runnables.SpawnCampRunnable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * Class with static methods to abstractly invoke arena goal and modules respecting their priorities
 */
public class WorkflowManager {

    public static boolean handleCommand(final Arena arena, final CommandSender sender, final String[] args) {
        ArenaGoal goal = arena.getGoal();
        if(goal != null && goal.checkCommand(args[0])) {
            goal.commitCommand(sender, args);
            return true;
        }

        for (final ArenaModule am : arena.getMods()) {
            if (am.checkCommand(args[0].toLowerCase())) {
                am.commitCommand(sender, args);
                return true;
            }
        }
        return false;
    }

    public static boolean handleEnd(final Arena arena, final boolean force) {
        debug(arena, "handleEnd: " + arena.getName() + "; force: " + force);

        try {
            ArenaGoal goal = arena.getGoal();
            if(goal.checkEnd()) {
                debug(arena, "committing end: " + goal.getName());
                goal.commitEnd(force);
                return true;
            }
            debug(arena, "FALSE");
        } catch (GameplayException e) {
            arena.msg(Bukkit.getConsoleSender(), Language.parse(arena, MSG.ERROR_ERROR, e.getMessage()));
        }

        return false;
    }

    public static int handleGetLives(final Arena arena, final ArenaPlayer aPlayer) {

        if (aPlayer.getStatus() == PlayerStatus.LOUNGE || aPlayer.getStatus() == PlayerStatus.WATCH) {
            return 0;
        }

        return arena.getGoal().getLives(aPlayer);
    }

    public static void handleInteract(final Arena arena, final Player player, final PlayerInteractEvent event) {
        ArenaGoal goal = arena.getGoal();
        if (goal != null && goal.checkInteract(player, event)) {
            event.setCancelled(true);
        }
    }

    public static boolean handleJoin(final Arena arena, final Player player, final String[] args) {
        debug(arena, "handleJoin!");

        ArenaModule joinModule = null;

        // Mods are sorted to get most prioritized first
        List<ArenaModule> sortedModules = arena.getMods().stream()
                .sorted(Comparator.comparingInt(ArenaModule::getPriority).reversed())
                .collect(Collectors.toList());

        try {
            for(ArenaModule mod : sortedModules) {
                mod.checkJoin(player);
            }

            for(ArenaModule mod : sortedModules) {
                if(mod.handleJoin(player)) {
                    joinModule = mod;
                    break;
                }
            }

            if(joinModule == null && !ArenaManager.checkJoin(player, arena)) {
                throw new GameplayException(Language.parse(arena, MSG.ERROR_JOIN_REGION));
            }
        } catch (GameplayExceptionNotice e) {
            arena.msg(player, Language.parse(arena, MSG.NOTICE_NOTICE, e.getMessage()));
        } catch (GameplayException e) {
            arena.msg(player, Language.parse(arena, MSG.ERROR_ERROR, e.getMessage()));
        }

        ArenaGoal joinGoal = arena.getGoal();

        try {
            joinGoal.checkJoin(player, args);
        } catch (GameplayException e) {
            arena.msg(player, Language.parse(arena, MSG.ERROR_ERROR, e.getMessage()));
            return false;
        } catch (NullPointerException e) {
            arena.msg(player, Language.parse(arena, MSG.ERROR_NO_GOAL));
            return false;
        }

        final ArenaTeam team;

        if (args.length < 1) {
            // usage: /pa {arenaname} join | join an arena
            team = arena.getTeam(TeamManager.calcFreeTeam(arena));
        } else if(arena.getTeam(args[0]) == null) {
            arena.msg(player, Language.parse(arena, MSG.ERROR_TEAMNOTFOUND, args[0]));
            return false;
        } else {
            team = arena.getTeam(args[0]);
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        ArenaModuleManager.choosePlayerTeam(arena, player, team.getColoredName());

        arena.markPlayedPlayer(player.getName());

        aPlayer.setPublicChatting(!arena.getConfig().getBoolean(CFG.CHAT_DEFAULTTEAM));

        debug(arena, "calling join event");
        final PAJoinEvent event = new PAJoinEvent(arena, player, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug(arena, "! Join event cancelled by plugin !");
            return false;
        }

        if (joinModule == null) {
            // join module null, just put the joiner to some spawn
            if (!arena.tryJoin(player, team)) {
                return false;
            }

            if (arena.isFreeForAll()) {
                arena.msg(player, arena.getConfig().getString(CFG.MSG_YOUJOINED));
                arena.broadcastExcept(player, Language.parse(arena, CFG.MSG_PLAYERJOINED, player.getName()));
            } else {
                arena.msg(player, arena.getConfig().getString(CFG.MSG_YOUJOINEDTEAM).replace("%1%", team.getColoredName() + ChatColor.RESET));
                arena.broadcastExcept(player, Language.parse(arena, CFG.MSG_PLAYERJOINEDTEAM, aPlayer.getName(), team.getColoredName() + ChatColor.RESET));
            }

            ArenaModuleManager.parseJoin(arena, player, team);

            joinGoal.initiate((player));
            ArenaModuleManager.initiate(arena, player);

            if (arena.getFighters().size() >= Math.max(1, arena.getConfig().getInt(CFG.READY_MINPLAYERS))) {
                arena.setFightInProgress(true);

                arena.getTeams().forEach(aTeam -> SpawnManager.distribute(arena, aTeam));
                joinGoal.parseStart();

                arena.getMods().forEach(ArenaModule::parseStart);
            }

            if (aPlayer.getArenaClass() != null && arena.startRunner != null) {
                aPlayer.setStatus(PlayerStatus.READY);
            }

            return true;
        }

        joinModule.commitJoin(player, team);

        ArenaModuleManager.parseJoin(arena, player, team);

        if (aPlayer.getArenaClass() != null && arena.startRunner != null) {
            aPlayer.setStatus(PlayerStatus.READY);
        }

        return true;
    }

    public static void handlePlayerDeath(final Arena arena, final Player player, final PlayerDeathEvent event) {

        ArenaGoal goal = arena.getGoal();
        boolean doesRespawn = true;
        boolean goalHandlesDeath = true;
        if(goal == null || goal.checkPlayerDeath(player) == null) {
            goalHandlesDeath = false;
        } else {
            doesRespawn = goal.checkPlayerDeath(player);
        }

        StatisticsManager.kill(arena, player.getKiller(), player, doesRespawn);
        if (arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES) ||
                arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGESCUSTOM)) {
            event.setDeathMessage("");
        }

        if (player.getKiller() != null) {
            player.getKiller().setFoodLevel(
                    player.getKiller().getFoodLevel()
                            + arena.getConfig().getInt(
                            CFG.PLAYER_FEEDFORKILL));
            if (arena.getConfig().getBoolean(CFG.PLAYER_HEALFORKILL)) {
                PlayerState.playersetHealth(player.getKiller(), (int) player.getKiller().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
            if (arena.getConfig().getBoolean(CFG.PLAYER_REFILLFORKILL)) {
                InventoryManager.clearInventory(player.getKiller());
                ArenaPlayer.fromPlayer(player.getKiller().getName()).getArenaClass().equip(player.getKiller());
            }
            if (arena.getConfig().getItems(CFG.PLAYER_ITEMSONKILL) != null) {
                ItemStack[] items = arena.getConfig().getItems(CFG.PLAYER_ITEMSONKILL);
                for (ItemStack item : items) {
                    if (item != null) {
                        player.getKiller().getInventory().addItem(item.clone());
                    }
                }
            }
            if (arena.getConfig().getBoolean(CFG.USES_TELEPORTONKILL)) {
                SpawnManager.respawn(arena, ArenaPlayer.fromPlayer(player.getKiller().getName()), null);
            }
        }

        if (!goalHandlesDeath) {
            debug(arena, player, "no mod handles player deaths");


            List<ItemStack> returned = null;
            if (arena.getConfig().getBoolean(
                    CFG.PLAYER_DROPSINVENTORY)) {
                returned = InventoryManager.drop(player);
                final int exp = event.getDroppedExp();
                event.getDrops().clear();
                if (arena.getConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                    InventoryManager.dropExp(player, exp);
                } else if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSEXP)) {
                    debug(arena, player, "exp: " + exp);
                    event.setDroppedExp(exp);
                }
            }
            final ArenaTeam respawnTeam = ArenaPlayer.fromPlayer(
                    player.getName()).getArenaTeam();

            if (arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                arena.broadcast(Language.parse(arena, MSG.FIGHT_KILLED_BY,
                        respawnTeam.colorizePlayer(player) + ChatColor.YELLOW,
                        arena.parseDeathCause(player, event.getEntity()
                                .getLastDamageCause().getCause(), event
                                .getEntity().getKiller())));
            }

            ArenaModuleManager.parsePlayerDeath(arena, player, event
                    .getEntity().getLastDamageCause());

            if (returned == null) {
                if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
                    returned = InventoryManager.drop(player);
                } else {
                    returned = new ArrayList<>(event.getDrops());
                }
                event.getDrops().clear();
            }

            handleRespawn(arena, ArenaPlayer.fromPlayer(player), returned);


            arena.getGoal().parsePlayerDeath(player, player.getLastDamageCause());

            return;
        }

        debug(arena, player, "handled by: " + goal.getName());
        final int exp = event.getDroppedExp();

        goal.commitPlayerDeath(player, doesRespawn, event);
        debug(arena, player, "parsing death: " + goal.getName());
        goal.parsePlayerDeath(player, player.getLastDamageCause());

        ArenaModuleManager.parsePlayerDeath(arena, player,
                player.getLastDamageCause());

        if (!arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY) || !ArenaPlayer.fromPlayer(player).mayDropInventory()) {
            event.getDrops().clear();
        }
        if (doesRespawn
                || arena.getConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
            InventoryManager.dropExp(player, exp);
        } else if (arena.getConfig().getBoolean(CFG.PLAYER_DROPSEXP)) {
            event.setDroppedExp(exp);
            debug(arena, player, "exp: " + exp);
        }
    }

    public static void handleRespawn(final Arena arena, final ArenaPlayer aPlayer, final List<ItemStack> drops) {

        for (final ArenaModule mod : arena.getMods()) {
            if (mod.tryDeathOverride(aPlayer, drops)) {
                return;
            }
        }
        debug(arena, aPlayer.getPlayer(), "handleRespawn!");
        new InventoryRefillRunnable(arena, aPlayer.getPlayer(), drops);
        SpawnManager.respawn(arena, aPlayer, null);
        EntityDamageEvent.DamageCause damageCause = ofNullable(aPlayer.getPlayer().getLastDamageCause())
                .map(EntityDamageEvent::getCause)
                .orElse(null);
        arena.unKillPlayer(aPlayer.getPlayer(), damageCause, aPlayer.getPlayer().getKiller());

    }

    /**
     * try to set a flag
     *
     * @param player the player trying to set
     * @param block  the block being set
     * @return true if the handling is successful and if the event should be
     * cancelled
     */
    public static boolean handleSetFlag(final Player player, final Block block) {
        final Arena arena = PAA_Region.activeSelections.get(player.getName());

        if (arena == null) {
            return false;
        }

        ArenaGoal goal = arena.getGoal();
        if (goal.checkSetBlock(player, block)) {
            return goal.commitSetFlag(player, block);
        }

        return false;
    }

    public static boolean handleSpectate(final Arena arena, final Player player) {
        debug(arena, player, "handling spectator");

        ArenaModule spectateModule = null;

        // Mods are sorted to get most prioritized first
        List<ArenaModule> sortedModules = arena.getMods().stream()
                .sorted(Comparator.comparingInt(ArenaModule::getPriority).reversed())
                .collect(Collectors.toList());

        try {
            for(ArenaModule mod : sortedModules) {
                mod.checkSpectate(player);
            }

            for(ArenaModule mod : sortedModules) {
                if(mod.handleSpectate(player)) {
                    spectateModule = mod;
                    break;
                }
            }
        } catch (GameplayException e) {
            arena.msg(player, Language.parse(arena, MSG.ERROR_ERROR, e.getMessage()));
            return false;
        }

        if(spectateModule == null) {
            debug(arena, player, "commit null");
            return false;
        }

        final PAJoinEvent event = new PAJoinEvent(arena, player, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug(arena, "! Spectate event cancelled by plugin !");
            return false;
        }

        spectateModule.commitSpectate(player);
        return true;
    }

    public static Boolean handleStart(final Arena arena, final CommandSender sender, final boolean force) {
        debug(arena, "handling start!");

        if (!force && arena.getFighters().size() < Math.min(2, arena.getConfig().getInt(CFG.READY_MINPLAYERS))) {
            debug(arena, "not forcing and we have less than minplayers");
            return null;
        }

        final PAStartEvent event = new PAStartEvent(arena);
        Bukkit.getPluginManager().callEvent(event);
        if (!force && event.isCancelled()) {
            debug(arena, "not forcing and cancelled by other plugin");
            return false;
        }

        debug(arena, sender, "teleporting all players to their spawns");

        ArenaGoal goal = arena.getGoal();
        if (goal.overridesStart()) {
            goal.commitStart(); // override spawning
        } else {
            for (final ArenaTeam team : arena.getTeams()) {
                SpawnManager.distribute(arena, team);
            }
        }

        debug(arena, sender, "teleported everyone!");

        arena.broadcast(Language.parse(arena, MSG.FIGHT_BEGINS));
        arena.setFightInProgress(true);

        goal.parseStart();

        for (final ArenaModule x : arena.getMods()) {
            x.parseStart();
        }

        final SpawnCampRunnable scr = new SpawnCampRunnable(arena);
        scr.runTaskTimer(PVPArena.getInstance(), 100L, arena.getConfig().getInt(CFG.TIME_REGIONTIMER));

        if (arena.getConfig().getInt(CFG.TIME_PVP) > 0) {
            arena.pvpRunner = new PVPActivateRunnable(arena, arena.getConfig().getInt(CFG.TIME_PVP));
        }

        arena.setStartingTime();
        arena.getScoreboard().refresh();
        return true;
    }
}
