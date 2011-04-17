package kingsheep.team.dumb;

import kingsheep.*;

public class Sheep extends Creature {

    public Sheep(Type type, int playerID, int x, int y) {
        super(type, playerID, x, y);
    }

    protected void think(Type map[][]) {
        move = Move.WAIT;
    }
}