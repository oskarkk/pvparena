package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.runnables.ArenaWarmupRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Module class "WarmupJoin"</pre>
 * <p/>
 * Enables a warmup countdown before joining the arena
 *
 * @author slipcor
 */

public class WarmupJoin extends ArenaModule {

    private static final int PRIORITY = 3;

    private Set<ArenaPlayer> playerSet;

    private boolean announced = false;

    public WarmupJoin() {
        super("WarmupJoin");
    }

    public static boolean didNotAnnounceYet(Arena arena) {
        for (ArenaModule mod : arena.getMods()) {
            if (mod instanceof WarmupJoin) {
                return !((WarmupJoin) mod).announced;
            }
        }
        return true;
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public PACheck checkJoin(final CommandSender sender, final PACheck result, final boolean join) {

        if (result.getPriority() > PRIORITY) {
            return result; // Something already is of higher priority, ignore!
        }

        final Player player = (Player) sender;

        if (this.arena == null) {
            return result; // arena is null - maybe some other mod wants to handle that? ignore!
        }


        if (this.arena.isLocked() && !player.hasPermission("pvparena.admin") && !(player.hasPermission("pvparena.create") && this.arena.getOwner().equals(player.getName()))) {
            result.setError(this, Language.parse(this.arena, MSG.ERROR_DISABLED));
            return result;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sender.getName());

        if (this.getPlayerSet().contains(aPlayer)) {
            return result;
        }

        if (aPlayer.getArena() != null) {
            debug(aPlayer.getArena(), sender, this.getName());
            result.setError(this, Language.parse(this.arena, MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
            return result;
        }
        this.getPlayerSet().add(aPlayer);

        result.setPriority(this, PRIORITY);
        return result;
    }

    @Override
    public void commitJoin(final Player sender, final ArenaTeam team) {
        new ArenaWarmupRunnable(this.arena, ArenaPlayer.parsePlayer(sender.getName()), team.getName(), false, this.arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
        this.announced = true;
    }

    @Override
    public void commitSpectate(final Player sender) {
        new ArenaWarmupRunnable(this.arena, ArenaPlayer.parsePlayer(sender.getName()), null, true, this.arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " +
                this.arena.getArenaConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    private Set<ArenaPlayer> getPlayerSet() {
        if (this.playerSet == null) {
            this.playerSet = new HashSet<>();
        }
        return this.playerSet;
    }

    @Override
    public void reset(final boolean force) {
        this.getPlayerSet().clear();
        this.announced = false;
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        this.getPlayerSet().remove(ArenaPlayer.parsePlayer(player.getName()));
    }
}