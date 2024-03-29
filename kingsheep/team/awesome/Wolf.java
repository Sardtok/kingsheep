package kingsheep.team.awesome;

import kingsheep.*;

public class Wolf extends Creature {

    public Wolf(Type type, int playerID, int x, int y) {
        super(type, playerID, x, y);
    }

    protected void think(Type map[][]) {
        int t = (int)(Math.random() * 4);

        try {
            Thread.sleep(1);
        } catch (InterruptedException ie) { }

        switch (t) {
        case 0:
            move = Move.UP;
            break;
        case 1:
            move = Move.DOWN;
            break;
        case 2:
            move = Move.LEFT;
            break;
        case 3:
            move = Move.RIGHT;
            break;
        default:
            move = Move.WAIT;
            break;
        }
    }
}