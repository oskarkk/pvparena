package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.listeners.PlayerListener;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import net.slipcor.pvparena.runnables.EndRunnable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static net.slipcor.pvparena.config.Debugger.debug;

public abstract class AbstractPlayerLivesGoal extends ArenaGoal {

    protected static final String SPAWN = "spawn";
    protected EndRunnable endRunner;

    public AbstractPlayerLivesGoal(String goalName) {
        super(goalName);
    }

    @Override
    public Boolean checkPlayerDeath(Player player) {
        int pos = this.getPlayerLifeMap().get(player);
        debug(this.arena, player, "lives before death: " + pos);
        return pos > 1;
    }

    protected abstract void broadcastEndMessagesIfNeeded(ArenaTeam teamToCheck);

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
            this.broadcastEndMessagesIfNeeded(arenaTeam);

            if (ArenaModuleManager.commitEnd(this.arena, arenaTeam)) {
                if (this.arena.realEndRunner == null) {
                    this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                            CFG.TIME_ENDCOUNTDOWN));
                }
                return;
            }
        }

        this.endRunner = new EndRunnable(this.arena, this.arena.getConfig().getInt(
                CFG.TIME_ENDCOUNTDOWN));
    }

    @Override
    public void commitPlayerDeath(final Player player, final boolean doesRespawn, final PlayerDeathEvent event) {

        if (!this.getPlayerLifeMap().containsKey(player)) {
            return;
        }

        if (doesRespawn) {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        } else {
            final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + player.getName());
            Bukkit.getPluginManager().callEvent(gEvent);
        }
        final int currentPlayerOrTeamLive = this.getPlayerLifeMap().get(player);

        debug(this.arena, player, "lives before death: " + currentPlayerOrTeamLive);
        if (currentPlayerOrTeamLive <= 1) {
            this.getPlayerLifeMap().remove(player);
            ArenaPlayer.fromPlayer(player).setStatus(PlayerStatus.LOST);
            if (this.arena.getConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
                debug(this.arena, player, "faking player death");
                PlayerListener.finallyKillPlayer(this.arena, player, event);
            }
            // player died => commit death!
            WorkflowManager.handleEnd(this.arena, false);
        } else {
            int nextPlayerOrTeamLive = currentPlayerOrTeamLive - 1;
            this.getPlayerLifeMap().put(player, nextPlayerOrTeamLive);

            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING, player, event, nextPlayerOrTeamLive);
                } else {
                    this.broadcastSimpleDeathMessage(player, event);
                }
            }

            final List<ItemStack> returned;

            if (this.arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
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
                + this.arena.getConfig().getInt(CFG.GOAL_PLIVES_LIVES));
    }

    @Override
    public void initiate(final Player player) {
        this.updateLives(player, this.arena.getConfig().getInt(CFG.GOAL_PLIVES_LIVES));
    }

    @Override
    public void parseLeave(final Player player) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning(this.getName() + ": player NULL");
            return;
        }
        this.getPlayerLifeMap().remove(player);
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                this.updateLives(ap.getPlayer(), this.arena.getConfig().getInt(CFG.GOAL_PLIVES_LIVES));
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.endRunner = null;
        this.getPlayerLifeMap().clear();
    }

    @Override
    public void setDefaults(final YamlConfiguration config) {
    }
}
