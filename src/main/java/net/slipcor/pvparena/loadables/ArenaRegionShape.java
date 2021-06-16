package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Arena Region Shape class
 * </pre>
 * <p/>
 * The framework for adding region shapes to an arena
 *
 * @author slipcor
 */

public abstract class ArenaRegionShape {

    private final String name;

    protected ArenaRegionShape(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public abstract boolean contains(PABlockLocation loc);

    public abstract PABlockLocation getCenter();

    public abstract List<PABlockLocation> getContainBlockCheckList();

    public abstract PABlockLocation getMaximumLocation();

    public abstract PABlockLocation getMinimumLocation();

    public abstract boolean overlapsWith(ArenaRegion other);

    public abstract void showBorder(Player player);

    public abstract boolean tooFarAway(int joinRange, Location location);

    public abstract boolean hasVolume();

    public Set<PABlockLocation> getAllBlocks() {
        PABlockLocation min = this.getMinimumLocation();
        PABlockLocation max = this.getMaximumLocation();
        Set<PABlockLocation> result = new HashSet<>();

        for(int x = min.getX(); x <= max.getX(); x++) {
            for(int y = min.getY(); y <= max.getY(); y++) {
                for(int z = min.getZ(); z <= max.getZ(); z++) {
                    PABlockLocation blockLocation =  new PABlockLocation(min.getWorldName(), x, y, z);
                    if(this.contains(blockLocation)) {
                        result.add(blockLocation);
                    }
                }
            }
        }

        return result;
    }

    public void displayInfo(final CommandSender sender) {
    }

    public String getVersion() {
        return "OUTDATED";
    }

    public void onThisLoad() {
    }

    public void toggleActivity() {
        throw new IllegalStateException("Module not up to date: " + this.getName());
    }

    public String version() {
        return "OUTDATED";
    }

    public abstract void move(BlockFace direction, int parseInt);

    public abstract void extend(BlockFace direction, int parseInt);

    public abstract void initialize(ArenaRegion region);
}
