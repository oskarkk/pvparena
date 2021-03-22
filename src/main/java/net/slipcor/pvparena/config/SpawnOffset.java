package net.slipcor.pvparena.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class SpawnOffset {
    private static final double DEFAULT_OFFSET = 0.5;
    private final double x;
    private final double y;
    private final double z;

    public SpawnOffset(ConfigurationSection cfg) {
        Optional<ConfigurationSection> optionalCfg = ofNullable(cfg);
        this.x = optionalCfg.map(cs -> cs.getDouble("x", DEFAULT_OFFSET)).orElse(DEFAULT_OFFSET);
        this.y = optionalCfg.map(cs -> cs.getDouble("y", DEFAULT_OFFSET)).orElse(DEFAULT_OFFSET);
        this.z = optionalCfg.map(cs -> cs.getDouble("z", DEFAULT_OFFSET)).orElse(DEFAULT_OFFSET);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }
}
