package kingsheep;

/**
 * A King Sheep player with its wolf and sheep.
 */
class Player {

    /** Points accumulated. */
    int score;
    /** The player's sheep. */
    final Creature sheep;
    /** The player's wolf. */
    final Creature wolf;

    /**
     * Creates a new player.
     *
     * @param sheep The player's sheep.
     * @param wolf The player's wolf.
     */
    Player(Creature sheep, Creature wolf) {
        this.score = 0;
        this.sheep = sheep;
        this.wolf = wolf;
    }
}