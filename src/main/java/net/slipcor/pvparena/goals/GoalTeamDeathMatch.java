package net.slipcor.pvparena.goals;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Arena Goal class "TeamDeathMatch"
 * </pre>
 * <p/>
 * The second Arena Goal. Arena Teams have lives. When every life is lost, the
 * team is teleported to the spectator spawn to watch the rest of the fight.
 *
 * @author slipcor
 */

public class GoalTeamDeathMatch extends AbstractTeamKillGoal {
    public GoalTeamDeathMatch() {
        super("TeamDeathMatch");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean allowsJoinInBattle() {
        return this.arena.getConfig().getBoolean(CFG.PERMS_JOININBATTLE);
    }

    @Override
    protected int getScore(ArenaTeam team) {
        return this.getTeamLivesCfg() - (this.getTeamLifeMap().getOrDefault(team, 0));
    }

    @Override
    protected int getTeamLivesCfg() {
        return this.arena.getConfig().getInt(CFG.GOAL_TDM_LIVES);
    }

    @Override
    public Boolean checkPlayerDeath(Player player) {
        return true;
    }

    @Override
    public void commitPlayerDeath(final Player respawnPlayer, final boolean doesRespawn,
                                  final PlayerDeathEvent event) {

        Player killer = respawnPlayer.getKiller();

        if (killer == null || respawnPlayer.equals(respawnPlayer.getKiller())) {
            if (!this.arena.getConfig().getBoolean(CFG.GOAL_TDM_SUICIDESCORE)) {
                this.broadcastSimpleDeathMessage(respawnPlayer, event);
                this.respawnPlayer(respawnPlayer, event);
                final PAGoalEvent gEvent;
                if (doesRespawn) {
                    gEvent = new PAGoalEvent(this.arena, this, "doesRespawn", "playerDeath:" + respawnPlayer.getName());
                } else {
                    gEvent = new PAGoalEvent(this.arena, this, "playerDeath:" + respawnPlayer.getName());
                }
                Bukkit.getPluginManager().callEvent(gEvent);
                return;
            }
            killer = respawnPlayer;
        }

        final ArenaTeam respawnTeam = ArenaPlayer.fromPlayer(respawnPlayer.getName()).getArenaTeam();
        final ArenaTeam killerTeam = ArenaPlayer.fromPlayer(killer.getName()).getArenaTeam();

        if (killerTeam.equals(respawnTeam)) { // suicide
            for (ArenaTeam newKillerTeam : this.arena.getTeams()) {
                if (!newKillerTeam.equals(respawnTeam) && this.reduceLives(this.arena, newKillerTeam, respawnPlayer, event)) {
                    this.makePlayerLose(respawnPlayer, event);
                    return;
                }
            }
        } else if (this.reduceLives(this.arena, killerTeam, respawnPlayer, event)) {
            this.makePlayerLose(respawnPlayer, event);
            return;
        }

        final PAGoalEvent gEvent = new PAGoalEvent(this.arena, this, String.format("playerDeath:%s", respawnPlayer.getName()),
                String.format("playerKill:%s:%s", respawnPlayer.getName(), killer.getName()));
        Bukkit.getPluginManager().callEvent(gEvent);

        if (this.getTeamLifeMap().get(killerTeam) != null) {
            if (this.arena.getConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                if (killerTeam.equals(respawnTeam) || !this.arena.getConfig().getBoolean(CFG.GENERAL_SHOWREMAININGLIVES)) {
                    this.broadcastSimpleDeathMessage(respawnPlayer, event);
                } else {
                    this.broadcastDeathMessage(MSG.FIGHT_KILLED_BY_REMAINING_TEAM_FRAGS, respawnPlayer, event, this.getTeamLifeMap().get(killerTeam));
                }
            }
            this.respawnPlayer(respawnPlayer, event);
        }

    }

    private void respawnPlayer(Player player, PlayerDeathEvent event) {
        final List<ItemStack> returned;

        if (this.arena.getConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
            returned = InventoryManager.drop(player);
            event.getDrops().clear();
        } else {
            returned = new ArrayList<>(event.getDrops());
        }

        WorkflowManager.handleRespawn(this.arena, ArenaPlayer.fromPlayer(player), returned);
    }

    private void makePlayerLose(Player respawnPlayer, PlayerDeathEvent event) {
        if (this.arena.getConfig().getBoolean(CFG.PLAYER_PREVENTDEATH)) {
            this.respawnPlayer(respawnPlayer, event);
            ArenaPlayer.fromPlayer(respawnPlayer.getName()).setStatus(PlayerStatus.LOST);
        }
    }

    /**
     * @param arena the arena this is happening in
     * @param arenaTeam  the killing team
     * @return true if the player should not respawn but be removed
     */
    private boolean reduceLives(final Arena arena, final ArenaTeam arenaTeam, final Player respawnPlayer, final EntityDeathEvent event) {
        final int iLives = this.getTeamLifeMap().get(arenaTeam);

        if (iLives <= 1) {
            for (final ArenaTeam otherTeam : arena.getNotEmptyTeams()) {
                if (otherTeam.equals(arenaTeam)) {
                    continue;
                }
                this.getTeamLifeMap().remove(otherTeam);
                for (final ArenaPlayer arenaPlayer : otherTeam.getTeamMembers()) {
                    if (arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                        arenaPlayer.setStatus(PlayerStatus.LOST);
                    }
                }
            }
            arena.broadcast(Language.parse(arena,
                    MSG.FIGHT_KILLED_BY,
                    arenaTeam.colorizePlayer(respawnPlayer)
                            + ChatColor.YELLOW, arena.parseDeathCause(
                            respawnPlayer,
                            event.getEntity()
                                    .getLastDamageCause() != null ? event.getEntity()
                                    .getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.VOID, event
                                    .getEntity().getKiller())));
            WorkflowManager.handleEnd(arena, false);
            return true;
        }

        this.getTeamLifeMap().put(arenaTeam, iLives - 1);
        return false;
    }

    @Override
    public void unload(final Player player) {
        if (this.allowsJoinInBattle()) {
            this.arena.hasNotPlayed(ArenaPlayer.fromPlayer(player));
        }
    }
}
