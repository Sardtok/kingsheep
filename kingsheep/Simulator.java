package kingsheep;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A class for simulating a King Sheep match.
 */
public class Simulator {

    /** Font size used for the top-window text. */
    private static final int FONTSIZE = 14;

    /** Maximum number of seconds a player is allowed to think. */
    private static final int THINKLIMIT = 1000;

    /** Minimum time to wait between player turns (even if a player used
        less time to think). */
    private static final int WAITMIN = 10;

    /** Number of turns for one game. */
    private static final int TURNS = 100;

    /** 0 if the winner is undecided.
        1 if player 1 has won the game.
        2 if player 2 has won the game.
        -1 if it's a draw. */
    private int playerWon = 0;

    /** The current turn. */
    private int turn;

    /** Holds the map. */
    private Type map[][];

    /** The player turn queue. */
    private LinkedList<Creature> turnQueue;

    /** Holds the players and stats for this match. */
    private Match match;

    /** The output stream*/
    private OutputStream out;

    /**
     * Creates a simulator simulating the given match
     * and sending to the given output stream.
     *
     * @param m The match to simulate.
     * @param out The output stream to send packets over.
     */
    Simulator(Match m, OutputStream out) {

        match = m;
        match.loadTeams();
        this.out = out;

        map = MapLoader.loadMap(match.map, match.p1, match.p2);

        int grass = 0;
        int rhubarb = 0;
        for (Type[] row : map) {
            if (out != null)
                try {
                    Networking.sendPacket(out, new Networking.MapPacket(row));
                } catch (IOException e) {
                    System.err.printf("Could not send map to client: %s%n",
                                      e.getMessage());
                    System.exit(5);
                }
            for (Type t : row) {
                if (t == Type.GRASS)
                    grass++;
                if (t == Type.RHUBARB)
                    rhubarb++;
            }
        }
        match.setTotals(grass, rhubarb);

        turnQueue = new LinkedList<Creature>();

        turnQueue.addFirst(match.p2.wolf);
        turnQueue.addFirst(match.p1.wolf);
        turnQueue.addFirst(match.p2.sheep);
        turnQueue.addFirst(match.p1.sheep);
        turnQueue.addFirst(match.p2.sheep);
        turnQueue.addFirst(match.p1.sheep);
    }

    /**
     * Runs the simulation.
     * Goes through every turn
     * and has every creature think for their moves every turn.
     */
    public void run() {
        for (turn = 0; turn < TURNS && playerWon == 0; ++turn) {
            for (Creature c : turnQueue) {

                if (!c.alive)
                    continue;

                final Creature curCreature = c;

                int oldx = c.x;
                int oldy = c.y;
                final String team = c.playerID == 1 ?
                    match.team1 : match.team2;

                if (map[oldy][oldx] != curCreature.type) {
                    System.out.println(team + " has cheated! DISQUALIFIED!");
                    playerWon = c.playerID == 1 ? 2 : 1;
                    break;
                }

                long startTime = System.nanoTime();

                final Type[][] mapCopy = new Type[map.length][map[0].length];
                for (int i = 0; i < map.length; i++) {
                    System.arraycopy(map[i], 0, mapCopy[i], 0, map[i].length);
                }

                Thread thinker = new Thread(new Runnable() {
                        public void run() {
                            try {
                                curCreature.think(curCreature.
                                                  filter(mapCopy));
                            } catch (RuntimeException e) {
                                System.out.printf("%s - %s:%n%s @ %s%n",
                                                  team,
                                                  e.getMessage(),
                                                  e.getClass().
                                                  getCanonicalName(),
                                                  e.getStackTrace()[0].
                                                  toString());
                                curCreature.move = Creature.Move.WAIT;
                            }
                        }
                    });

                thinker.start();
                int maxWait = (int)((startTime / 1000000) + THINKLIMIT + 100);
                while (true) {
                    try {
                        int wait = maxWait - (int)(System.nanoTime() / 1000000);
                        thinker.join(wait);
                    } catch (InterruptedException e) {
                        System.out.println("OH NO, TEH INTERRUPTION!");
                        continue;
                    }
                    break;
                }

                long elapsedTime = (System.nanoTime() - startTime);
                match.think(c.playerID, elapsedTime);
                elapsedTime /= 1000000;

                if (oldx != c.x || oldy != c.y) {
                    System.out.println(team + " has cheated! DISQUALIFIED!");
                    playerWon = c.playerID == 1 ? 2 : 1;
                }

                if (elapsedTime > THINKLIMIT) {
                    System.out.println(team + " used over one second! "
                                       + "DISQUALIFIED!");
                    playerWon = c.playerID == 1 ? 2 : 1;
                    match.disqualify(c.playerID);
                } else {
                    action(c);
                    if (out != null)
                        try {
                            Networking.sendPacket(out,
                                                  new Networking.MovePacket(c.type,
                                                                            c.move));
                        } catch (IOException e) {
                            System.err.printf("Could not send move: %s%n",
                                              e.getMessage());
                            System.exit(5);
                        }
                }

                checkMap();

                if (playerWon != 0)
                    break;
            }
            if (out != null)
                try {
                    Networking.sendPacket(out,
                                          new Networking.Packet(new byte[]
                                              {Networking.ENDTURN}));
                } catch (IOException e) {
                    System.err.printf("Could not send end turn: %s%n",
                                      e.getMessage());
                    System.exit(5);
                }
        }

        if (playerWon == 0) {
            setWinner();
        }

        match.win(playerWon);
    }

    /**
     * Checks if the map has any food left.
     * If not it declares a winner.
     * @see #setWinner()
     */
    private void checkMap() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == Type.GRASS || map[i][j] == Type.RHUBARB) {
                    return;
                }
            }
        }

        setWinner();
    }

    /**
     * Declares a winner based on the team scores.
     */
    private void setWinner() {
        if (match.p1.score > match.p2.score)
            playerWon = 1;
        else if (match.p1.score < match.p2.score)
            playerWon = 2;
        else
            playerWon = -1;  // It's a draw
    }

    /** Determines whether the planned move is legal.
     *
     *  @param x
     *         Planned x position.
     *  @param y
     *         Planned y position.
     *  @param t1
     *         Type of entity being moved.
     *  @return
     *         <code>true</code> of the planned move is legal.
     */
    private boolean legalMove(int x, int y, Type t1) {

        Type t2 = map[y][x];
        if (t2 == Type.FENCE ||
            (t1 == Type.SHEEP1) && (t2 == Type.WOLF1)  ||
            (t1 == Type.SHEEP2) && (t2 == Type.WOLF2)  ||
            (t1 == Type.SHEEP1) && (t2 == Type.WOLF2)  ||
            (t1 == Type.SHEEP2) && (t2 == Type.WOLF1)  ||
            (t1 == Type.SHEEP1) && (t2 == Type.SHEEP2) ||
            (t1 == Type.SHEEP2) && (t2 == Type.SHEEP1) ||
            (t1 == Type.WOLF1)  && (t2 == Type.SHEEP1) ||
            (t1 == Type.WOLF1)  && (t2 == Type.WOLF2)  ||
            (t1 == Type.WOLF2)  && (t2 == Type.WOLF1)  ||
            (t1 == Type.WOLF2)  && (t2 == Type.SHEEP2))
            return false;

        return true;
    }

    /** Attempts to move a creature according to it's plan.
     *
     *  @param c
     *         Creature to move.
     */
    public void action(Creature c) {

        switch (c.move) {
        case RIGHT:
            if ((c.x < Gfx.XUNIT - 1) && legalMove(c.x + 1, c.y, c.type)) {
                map[c.y][c.x] = Type.EMPTY;
                c.x++;
            } else {
                c.move = Creature.Move.WAIT;
                return;
            }
            break;
        case LEFT:
            if ((c.x > 0) && legalMove(c.x - 1, c.y, c.type)) {
                map[c.y][c.x] = Type.EMPTY;
                c.x--;
            } else {
                c.move = Creature.Move.WAIT;
                return;
            }
            break;
        case UP:
            if ((c.y > 0) && legalMove(c.x, c.y - 1, c.type)) {
                map[c.y][c.x] = Type.EMPTY;
                c.y--;
            } else {
                c.move = Creature.Move.WAIT;
                return;
            }
            break;
        case DOWN:
            if ((c.y < Gfx.YUNIT - 1) && legalMove(c.x, c.y + 1, c.type)) {
                map[c.y][c.x] = Type.EMPTY;
                c.y++;
            } else {
                c.move = Creature.Move.WAIT;
                return;
            }
            break;
        default:
            break;
        }

        if (map[c.y][c.x] == Type.GRASS) {
            if (c.isSheep())
                match.eatGrass(c.playerID);
            else
                match.crushGrass(c.playerID);
        } else if (map[c.y][c.x] == Type.RHUBARB) {
            if (c.isSheep())
                match.eatRhubarb(c.playerID);
            else
                match.crushRhubarb(c.playerID);
        } else if (map[c.y][c.x] == Type.SHEEP1 && c.type == Type.WOLF2) {
            match.eatSheep(c.playerID);
        } else if (map[c.y][c.x] == Type.SHEEP2 && c.type == Type.WOLF1) {
            match.eatSheep(c.playerID);
        } else if (map[c.y][c.x] == Type.WOLF2 && c.type == Type.SHEEP1) {
            match.eatSheep(2);
        } else if (map[c.y][c.x] == Type.WOLF1 && c.type == Type.SHEEP2) {
            match.eatSheep(1);
        }

        if (!match.p1.sheep.alive && playerWon == 0 && match.p1.score < match.p2.score)
            playerWon = 2;
        else if (!match.p2.sheep.alive && playerWon == 0
                 && match.p2.score < match.p1.score)
            playerWon = 1;
        else if (!match.p1.sheep.alive && !match.p2.sheep.alive)
            if (match.p1.score < match.p2.score)
                playerWon = 2;
            else if (match.p2.score < match.p1.score)
                playerWon = 1;
            else
                playerWon = -1;
            
        map[c.y][c.x] = c.type;
        
    }
}