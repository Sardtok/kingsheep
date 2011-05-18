package kingsheep;

/**
 * Super class for all game creatures.
 */
public abstract class Creature {

    /** Possible moves for a creature. */
    protected enum Move {
        /** Don't move. */ WAIT,
            /** Move up/north. */ UP,
            /** Move down/south. */ DOWN,
            /** Move left/west. */ LEFT,
            /** Move right/east. */ RIGHT }

    /** The type of creature this is. */
    public final Type type;
    /** The player ID (1 or 2). */
    public final int playerID;
    /** The move that was decided during thinking. */
    public Move move;
    /** Horizontal coordinate. */
    public int x;
    /** Vertical coordinate. */
    public int y;
    /** Whether this creature is dead or alive. */
    public boolean alive;

    /**
     * Creates a new creature.
     *
     * @param type The type of creature this is (e.g. SHEEP1).
     * @param playerID The ID of the player controlling the creature.
     * @param x The horizontal start coordinate.
     * @param y The vertical start coordinate.
     */
    protected Creature(Type type, int playerID, int x, int y) {
        this.type = type;
        this.playerID = playerID;
        this.x = x;
        this.y = y;
        alive = true;
    }

    /**
     * Converts a map's creatures, so they look right to this creature.
     * This is used to ensure the player always sees the enemy as player 2.
     *
     * @param map The map to be filtered.
     * @return The map with its creatures converted.
     */
    protected Type[][] filter(Type[][] map) {
        if (playerID == 1)
            return map;

        for (int i = 0; i < map.length; ++i) {
            for (int j = 0; j < map[i].length; ++j) {
                if (map[i][j] == Type.WOLF2)
                    map[i][j] = Type.WOLF1;
                else if (map[i][j] == Type.WOLF1)
                    map[i][j] = Type.WOLF2;
                else if (map[i][j] == Type.SHEEP2)
                    map[i][j] = Type.SHEEP1;
                else if (map[i][j] == Type.SHEEP1)
                    map[i][j] = Type.SHEEP2;
            }
        }
        return map;
    }

    /**
     * Make a movement plan by setting the {@link #move} variable.
     *
     * @param map The current map state.
     * @see Move
     */
    protected abstract void think(Type map[][]);

    /**
     * Tests whether this is a sheep or not.
     *
     * @return true if this is a sheep.
     */
    public boolean isSheep() {
        return type == Type.SHEEP1 || type == Type.SHEEP2;
    }
}
