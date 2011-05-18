package kingsheep;

import java.io.PrintWriter;

/**
 * Class that runs a single match.
 */
class KingSheep {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: KingSheep map ai1 ai2");
            System.exit(1);
        }

        Match m = new Match(args[1], args[2], args[0]);
        new Simulator(m, null).run();
        if (m.stats1.getWins() == 1) {
            System.out.printf("%s won %d - %d.%n", m.team1, m.p1.score, m.p2.score);

        } else if (m.stats1.getLosses() == 1) {
            System.out.printf("%s won %d - %d.%n", m.team2, m.p2.score, m.p1.score);
        } else {
            System.out.printf("It's a draw %d - %d.%n", m.p1.score, m.p2.score);
        }
        m.writeStats(new PrintWriter(System.out));
    }
}
