package kingsheep;

import java.util.Scanner;
import java.io.PrintWriter;

/**
 * A class for holding team statistics.
 */
class Statistics implements Comparable<Statistics> {

    /** The team these stats are for. */
    private final String team;
    /** The amount of food eaten. */
    private int grassEaten = 0, rhubarbEaten = 0;
    /** The amount of food destroyed. */
    private int grassCrushed = 0, rhubarbCrushed = 0;
    /** The number of sheep killed. */
    private int sheepEaten = 0;
    /** The number of games won, lost and tied. */
    private int gamesWon = 0, gamesLost = 0, draws = 0;
    /** Number of seconds and nano seconds spent thinking. */
    private int thinkSeconds = 0, thinkNanos = 0;
    /** Number of moves (times thought). */
    private int turns = 0;
    /** The total amount of food encountered in maps. */
    private int grassTotal = 0, rhubarbTotal = 0;

    /**
     * Creates an empty statistics object (everything set to 0).
     *
     * @param team The team the statistics is for.
     */
    public Statistics(String team) {
        this.team = team;
    }

    /**
     * Uses a CSV string to initialize values.
     *
     * @param csv A semicolon separated list of stats.
     * @param team The team the statistics is for.
     */
    public Statistics(String csv, String team) {
        this.team = team;
        Scanner scan =  new Scanner(csv);
        scan.useDelimiter(";");

        gamesWon = scan.nextInt();
        gamesLost = scan.nextInt();
        draws = scan.nextInt();
        grassEaten = scan.nextInt();
        rhubarbEaten = scan.nextInt();
        sheepEaten = scan.nextInt();
        grassCrushed = scan.nextInt();
        rhubarbCrushed = scan.nextInt();
        grassTotal = scan.nextInt();
        rhubarbTotal = scan.nextInt();
        thinkSeconds = scan.nextInt();
        thinkNanos = scan.nextInt();
        turns = scan.nextInt();
    }

    /**
     * Gets the number of wins for this team.
     *
     * @return The number of times this team has won games.
     */
    public int getWins() { return gamesWon; }
    /**
     * Gets the number of losses for this team.
     *
     * @return The number of times this team has lost games.
     */
    public int getLosses() { return gamesLost; }

    /**
     * Gets the number of points accumulated.
     *
     * @return The number of points based on food eaten.
     */
    public int getPoints() {
        return rhubarbEaten * 5 + grassEaten;
    }

    /**
     * Gets the name of the team.
     *
     * @return The name of the team these stats are for.
     */
    public String getTeam() {
        return team;
    }

    /**
     * Adds the given statistics to this statistics object.
     * This is used to add to a total.
     *
     * @param st The statistics object to add to this.
     */
    public void add(Statistics st) {
        grassEaten += st.grassEaten;
        rhubarbEaten += st.rhubarbEaten;
        grassCrushed += st.grassCrushed;
        rhubarbCrushed += st.rhubarbCrushed;
        sheepEaten += st.sheepEaten;
        gamesWon += st.gamesWon;
        gamesLost += st.gamesLost;
        draws += st.draws;
        turns += st.turns;
        grassTotal += st.grassTotal;
        rhubarbTotal += rhubarbTotal;

        thinkSeconds += st.thinkSeconds;
        think(st.thinkNanos);
    }

    /** Increase amount of grass eaten. */
    public void eatGrass() { grassEaten++; }

    /** Increase amount of grass crushed. */
    public void crushGrass() { grassCrushed++; }

    /** Increase amount of rhubarb eaten. */
    public void eatRhubarb() { rhubarbEaten++; }

    /** Increase amount of rhubarb crushed. */
    public void crushRhubarb() { rhubarbCrushed++; }

    /** Increase amount of sheep eaten. */
    public void eatSheep() { sheepEaten++; }

    /** Increase amount of games won. */
    public void win() { gamesWon++; }

    /** Increase amount of games lost. */
    public void lose() { gamesLost++; }

    /** Increase amount of games that ended in a draw. */
    public void draw() { draws++; }

    /**
     * Sets the total counts for grass and rhubarb.
     *
     * @param grass The number of grass tiles in the map.
     * @param rhubarb The number of rhubarb tiles in the map.
     */
    public void setTotals(int grass, int rhubarb) {
        grassTotal = grass;
        rhubarbTotal = rhubarb;
    }

    /**
     * Add to time spent thinking.
     *
     * @param nanos The number of nano seconds spent thinking last round.
     */
    public void think(long nanos) {
        long temp = thinkNanos + nanos;
        while (temp > 1000000000) {
            thinkSeconds++;
            temp -= 1000000000;
        }
        thinkNanos = (int)temp;
        turns++;
    }

    /**
     * Writes out a semicolon separated list of stats to the given writer.
     *
     * @param pw The PrintWriter to print the stats to.
     */
    public void write(PrintWriter pw) {
        pw.printf("%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d%n",
                  gamesWon, gamesLost, draws,
                  grassEaten, rhubarbEaten, sheepEaten,
                  grassCrushed, rhubarbCrushed,
                  grassTotal, rhubarbTotal,
                  thinkSeconds, thinkNanos, turns);
    }

    /**
     * Compares these stats to another set of stats.
     *
     * @param s The other stats to compare this to.
     * @return A negative number if this has a lower rank,
     *         a positive number if this has a higher rank,
     *         or 0 if they are the same rank.
     */
    public int compareTo(Statistics s) {
        int ret = gamesWon - s.getWins();
        if (ret == 0)
            ret = s.getLosses() - gamesLost;

        if (ret == 0)
            ret = getPoints() - s.getPoints();

        if (ret == 0)
            ret = sheepEaten - s.sheepEaten;
			
        return ret;
    }
}