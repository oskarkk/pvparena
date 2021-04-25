package net.slipcor.pvparena.classes;

import static java.util.Optional.ofNullable;

public class PABlock {
    public static final String PA_BLOCK_TEAM_FORMAT = "%s_%s";

    private final PABlockLocation location;
    private final String name;
    private final String teamName;

    public PABlock(final PABlockLocation loc, final String name, final String teamName) {
        this.location = loc;
        this.name = name;
        this.teamName = teamName;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof PABlock) {
            final PABlock other = (PABlock) o;
            return this.name.equals(other.name) && this.location.equals(other.location);
        }
        return false;
    }

    public PABlockLocation getLocation() {
        return this.location;
    }

    public String getName() {
        return this.name;
    }

    public String getTeamName() {
        return this.teamName;
    }

    /**
     * Returns PaBlock full name (like in the config)
     *
     * @return String with format (team_)<name>
     */
    public String getFullName() {
        return ofNullable(this.teamName)
                .map(teamName -> String.format(PA_BLOCK_TEAM_FORMAT, teamName, this.name))
                .orElse(this.name);
    }

    public String getPrettyName() {
        return ofNullable(this.teamName)
                .map(teamName -> String.join(" ", teamName, this.name))
                .orElse(this.name);
    }
}
