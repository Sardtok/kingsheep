package kingsheep;

/**
 * Types of tiles on a map.
 */
public enum Type {
    /** Ordinary tiles. */ EMPTY('.'),
        /** Grass - 1 point tile. */ GRASS('g'),
        /** Fence - impassable tile. */ FENCE('#'),
        /** Rhubarb - 5 point tile. */ RHUBARB('r'),
        /** Player 1's sheep (self as seen by AI). */ SHEEP1('1'),
        /** Player 2's sheep (enemy as seen by AI). */ SHEEP2('3'),
        /** Player 1's wolf (self as seen by AI). */ WOLF1('2'),
        /** Player 2's wolf (enemy as seen by AI). */ WOLF2('4');

    /** The character representation of a tile (used in map files). */
    public char c;

    /**
     * Creates a new type with the given character representation.
     *
     * @param c This type's character representation.
     */
    private Type(char c) {
        this.c = c;
    }

    /**
     * Gets the type matching the given character.
     *
     * @param c The character whose type to look for.
     * @return The type matching the character or null if there's no such type.
     */
    public static Type getType(char c) {
        for (Type t : values())
            if (t.c == c)
                return t;
        return null;
    }
}