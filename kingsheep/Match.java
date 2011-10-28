package kingsheep;

import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;

/**
 * A match in a tournament.
 */
class Match {

    /** Team names. */
    String team1, team2;
    /** Players. */
    Player p1, p2;
    /** Points to disqualified players (requires restart). */
    Player offender;
    /** Team statistics. */
    Statistics stats1, stats2;
    /** Name of the map file. */
    String map;

    /**
     * Creates a match between two players.
     *
     * @param team1 The name of the first team.
     * @param team2 The name of the second team.
     * @param map The name of the file containing the map.
     */
    public Match(String team1, String team2, String map) {
        this.team1 = team1;
        this.stats1 = new Statistics(team1);
        this.team2 = team2;
        this.stats2 = new Statistics(team2);
        this.map = map;
    }

    /**
     * Updates the stats at the end of a game.
     *
     * @param playerID The ID of the player that won,
     *                 or a non-player ID if it was a draw.
     */
    public void win(int playerID) {
        switch(playerID) {
        case 1:
            stats1.win();
            stats2.lose();
            break;
        case 2:
            stats1.lose();
            stats2.win();
            break;
        default:
            stats1.draw();
            stats2.draw();
            break;
        }

        // Hopefully this should clean up the player objects.
        p1 = null;
        p2 = null;
    }

    /**
     * Disqualify a player.
     *
     * @param playerID The ID of the player that violated the time rule.
     */
    public void disqualify(int playerID) {
        if (playerID == 1) {
            offender = p1;
        } else {
            offender = p2;
        }
    }

    /**
     * Update think time stats.
     *
     * @param playerID The ID of the player that's done thinking.
     * @param nanos The time spent thinking.
     */
    public void think(int playerID, long nanos) {
        if (playerID == 1) {
            stats1.think(nanos);
        } else {
            stats2.think(nanos);
        }
    }

    /**
     * Sets the amount of food available in a map.
     *
     * @param grass Amount of grass in the map.
     * @param rhubarb Amount of rhubarb in the map.
     */
    public void setTotals(int grass, int rhubarb) {
        stats1.setTotals(grass, rhubarb);
        stats2. setTotals(grass, rhubarb);
    }

    /**
     * The given player eats the opposite team's sheep.
     *
     * @param playerID The ID of the player that ate the sheep.
     */
    public void eatSheep(int playerID) {
        if (playerID == 1) {
            stats1.eatSheep();
            p2.sheep.alive = false;
        } else {
            stats2.eatSheep();
            p1.sheep.alive = false;
        }
    }

    /**
     * Eat grass.
     *
     * @param playerID The ID of the player that ate the grass.
     */
    public void eatGrass(int playerID) {
        if (playerID == 1) {
            p1.score++;
            stats1.eatGrass();
        } else {
            p2.score++;
            stats2.eatGrass();
        }
    }

    /**
     * Eat rhubarb.
     *
     * @param playerID The ID of the player that ate the rhubarb.
     */
    public void eatRhubarb(int playerID) {
        if (playerID == 1) {
            p1.score += 5;
            stats1.eatRhubarb();
        } else {
            p2.score += 5;
            stats2.eatRhubarb();
        }
    }

    /**
     * Crush grass.
     *
     * @param playerID The ID of the player that crushed the grass.
     */
    public void crushGrass(int playerID) {
        if (playerID == 1) {
            stats1.crushGrass();
        } else {
            stats2.crushGrass();
        }
    }

    /**
     * Crush rhubarb.
     *
     * @param playerID The ID of the player that crushed the rhubarb.
     */
    public void crushRhubarb(int playerID) {
        if (playerID == 1) {
            stats1.crushRhubarb();
        } else {
            stats2.crushRhubarb();
        }
    }

    /**
     * Writes the statistics for a game using the given PrintWriter.
     *
     * @param pw The PrintWriter to print the stats to.
     */
    public void writeStats(PrintWriter pw) {
        pw.print(team1);
        pw.print(";");
        stats1.write(pw);
        pw.print(team2);
        pw.print(";");
        stats2.write(pw);
        pw.close();
    }

    /**
     * Loads the teams for this match.
     * This must be called prior to playing the match.
     */
    public void loadTeams() {
        try {
            p1 = loadTeam(team1, 1);
            p2 = loadTeam(team2, 2);
        } catch (Exception e) {
            System.err.printf("Could not load teams: %s%n",
                              e.getMessage());
            System.exit(5);
        }
    }

    /**
     * Dynamically loads classes based on the team's name.
     *
     * @param teamName The name of the team (sub-package).
     * @param playerID 1 for player 1 and 2 for player 2.
     */
    Player loadTeam(String teamName, int playerID)
        throws ClassNotFoundException, InstantiationException,
               NoSuchMethodException, IllegalAccessException,
               InvocationTargetException
    {
        ClassLoader loader = getClass().getClassLoader();
        String pack = "kingsheep.team." + teamName + ".";

        Creature sheep = (Creature)loader.loadClass(pack + "Sheep")
            .getConstructors()[0]
            .newInstance(playerID == 1 ? Type.SHEEP1 : Type.SHEEP2,
                         playerID, -1, -1);

        Creature wolf = (Creature)loader.loadClass(pack + "Wolf")
            .getConstructors()[0]
            .newInstance(playerID == 1 ? Type.WOLF1 : Type.WOLF2,
                         playerID, -1, -1);

        Player p = new Player(sheep, wolf);

        return p;
    }
}