package kingsheep;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Scanner;
import java.util.Arrays;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.URL;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * A server that runs a tournament.
 *
 * The server requires certain files:
 * stats.csv - A CSV file with stats for different matches already played.
 * teams.txt - A list of teams
 * matchesN.txt - A file containing a list of matches to be played
 *                in the Nth step of the ladder.
 */
public class Server implements Runnable {

    /** Teams that are still in the tournament. */
    private List<String> teams = new ArrayList<String>();
    /** A list of lists of matches for each step of the ladder. */
    private List<List<Match>> matches = new ArrayList<List<Match>>();
    /** A Map of statistics objects, one for each team. */
    private Map<String, Statistics> stats = new HashMap<String, Statistics>();
    /** The socket the viewer is connected to. */
    private Socket viewer;
    /** The output stream to send packets to the viewer. */
    private OutputStream viewerOut;

    /** The current step of the ladder. */
    private int ladder = 0;
    /** The current match in the current step of the ladder. */
    private int game = 0;

    /**
     * Creates a server.
     */
    public Server() {
        try {
            Scanner scan = new Scanner(getFile("teams.txt"));
            while (scan.hasNextLine()) {
                String team = scan.nextLine();
                teams.add(team);
                stats.put(team, new Statistics(team));
            }
            scan.close();

            scan = new Scanner(getFile("stats.csv"));
            scan.useDelimiter(";");
            List<Match> currentLadder = loadLadder(ladder++);
            while (scan.hasNextLine()) {
                String team1 = scan.next();
                Statistics stats1 = new Statistics(scan.nextLine(), team1);
                String team2 = scan.next();
                Statistics stats2 = new Statistics(scan.nextLine(), team2);
                if (game == currentLadder.size()) {
                    game = 0;
                    currentLadder = loadLadder(ladder++);
                }
                Match m = currentLadder.get(game++);
                stats.get(team1).add(stats1);
                stats.get(team2).add(stats2);
                m.stats1.add(stats1);
                m.stats2.add(stats2);
            }

            scan.close();
        } catch (Exception e) {
            System.err.printf("An error occurred loading files: %s%n",
                              e.getMessage());
            System.exit(1);
        }

        System.out.println("--- KING SHEEP TOURNAMENT SERVER ---");
        System.out.println();
        viewer = waitForViewer(createServerSocket());
        if (viewer == null) {
            System.err.println("Error creating a server socket.");
            System.exit(3);
        }
    }

    /**
     * Plays every game that's left in the tournament.
     */
    public void run() {
        Scanner scan = new Scanner(System.in);
        List<Match> l = matches.get(ladder-1);
        while (teams.size() > 1) {
            if (l == null)
                break;

            for (; game < l.size(); game++) {
                System.out.println("Press return to play next game:");
                scan.nextLine();

                Match m = l.get(game);
                try {
                    Networking.sendPacket(viewerOut,
                                          new Networking.NewGamePacket(m.team1,
                                                                       m.team2));
                } catch (IOException e) {
                    System.err.printf("Error sending game to client: %s%n",
                                      e.getMessage());
                    System.exit(5);
                }
                System.out.printf("New game on %s: %s vs. %s%n",
                                  m.map, m.team1, m.team2);
                Simulator s = new Simulator(m, viewerOut);
                s.run();

                if (m.stats1.getWins() == 1) {
                    System.out.printf("%s won %d - %d%n",
                                      m.team1, m.p1.score, m.p2.score);
                } else if (m.stats1.getLosses() == 1) {
                    System.out.printf("%s won %d - %d%n",
                                      m.team2, m.p2.score, m.p1.score);
                } else {
                    System.out.printf("It's a draw: %d - %d%n",
                                      m.p2.score, m.p1.score);
                }

                try {
                    m.writeStats(new PrintWriter
                                 (new FileOutputStream
                                  (getFile("stats.csv"), true)));
                } catch (Exception e) {
                    System.err.printf("Error storing stats: %s%n",
                                      e.getMessage());
                    System.exit(6);
                }
                stats.get(m.team1).add(m.stats1);
                stats.get(m.team2).add(m.stats2);
                try {
                    byte[] winner = {Networking.ENDGAME, 0};
                    if (m.stats1.getWins() == 1) {
                        winner[1] = 1;
                    } else if (m.stats1.getLosses() == 1) {
                        winner[1] = 2;
                    }

                    Networking.sendPacket(viewerOut, new Networking.Packet(winner));
                } catch (IOException e) {
                    System.err.printf("Could not send end game packet: %s%n",
                                      e.getMessage());
                }

                if (m.offender != null) {
                    try {
                        Networking.sendPacket(viewerOut,
                                              new Networking.
                                              Packet(Networking.RECONNECT));
                        viewer.close();
                    } catch (IOException e) {
                        System.err.printf("Could not send reconnect: %s%n",
                                          e.getMessage());
                    } finally {
                        System.exit(119);
                    }
                }
            }
            l = loadLadder(ladder++);
            game = 0;
        }
    }

    /**
     * Creates a server object and starts running it.
     */
    public static void main(String[] args) {
        Server s = new Server();
        s.run();
        if (!s.viewer.isClosed()) {
            try {
                Networking.sendPacket(s.viewerOut,
                                      new Networking.Packet(Networking.DISCONNECT));
                s.viewer.close();
            } catch (IOException e) {
                System.err.printf("Could not close socket: %s%n", e.getMessage());
                System.exit(5);
            }
        }
    }

    /**
     * Gets the given file first by getResource in the class loader,
     * then the class and lastly just using File(String).
     *
     * @param name The file name of the file to get.
     * @return The file that was to be retrieved.
     */
    private static File getFile(String name) throws URISyntaxException {
        File f = null;
        URL url = Server.class.getClassLoader().getResource(name);
        if (url == null)
            url = Server.class.getResource(name);

        if (url == null)
            f = new File(name);
        else
            f = new File(url.toURI());

        return f;
    }

    /**
     * Waits for a viewer to connect and returns the viewers socket.
     * This code is pretty dirty.
     *
     * @param srv The server socket used for listening.
     * @return The socket used to communicate with the client.
     */
    private Socket waitForViewer(ServerSocket srv) {
        System.out.println("Waiting for client!");
        if (srv == null) {
            return null;
        }
        while (true) {
            try {
                Socket s = srv.accept();
                InputStream sin = s.getInputStream();
                OutputStream sout = s.getOutputStream();
                Networking.Packet p = Networking.recvPacket(sin);
                if (Arrays.equals(Networking.LOGIN, p.data)) {
                    srv.close();
                    viewerOut = sout;
                    return s;
                }
                s.close();
            } catch (IOException ioe) {
                System.err.printf("Something went wrong while waiting for a client:%n%s%n",
                                  ioe.getMessage());
            }
        }
    }

    /**
     * Creates a server socket and returns it.
     *
     * @return A server socket.
     */
    private ServerSocket createServerSocket() {
        System.out.println("Creating server socket!");
        try {
            return new ServerSocket(Networking.PORT);
        } catch (IOException ioe) {
            System.err.printf("Could not bind socket on port #%d: %n%s%n",
                              Networking.PORT, ioe.getMessage());
            System.exit(3);
        }
        return null;
    }

    /**
     * Selects the teams that are to continue playing
     * in the next step of the ladder.
     *
     * @param keepCount The number of teams to keep in the next step.
     */
    private void selectBestTeams(int keepCount) {
        System.out.printf("Statistics:%n");
        System.out.printf("%4c%4c%4c TEAM%n", 'W', 'L', 'P');
        for (String team : teams) {
            System.out.printf("%4d%4d%4d %s%n",
                              stats.get(team).getWins(),
                              stats.get(team).getLosses(),
                              stats.get(team).getPoints(),
                              team);
        }

        SortedSet<Statistics> rankedStats = new TreeSet<Statistics>();
        rankedStats.addAll(stats.values());

        if (teams.size() > keepCount) {
            System.out.printf("Removing %d teams:%n",
                              teams.size() - keepCount);
        }

        while (teams.size() > keepCount) {
            Iterator<Statistics> it = rankedStats.iterator();
            String remove = it.next().getTeam();
            if (teams.contains(remove)) {
                System.out.printf("- %s%n", remove);
                teams.remove(remove);
            }
            it.remove();
        }
    }

    /**
     * Loads a step on the ladder.
     *
     * @param ladder The number of the ladder to load.
     * @return A list of matches for this part of the ladder
     *         or null if there are no more steps on the ladder.
     */
    private List<Match> loadLadder(int ladder) {
        try {
            Scanner scan = new Scanner(getFile("matches" + ladder + ".txt"));
            List<Match> l = new ArrayList<Match>();
            String map = scan.next();
            selectBestTeams(scan.nextInt());
            while (scan.hasNext()) {
                l.add(new Match(teams.get(scan.nextInt()),
                                teams.get(scan.nextInt()),
                                map));
            }
            matches.add(l);
            scan.close();
            return l;
        } catch (Exception e) {
            selectBestTeams(1);
        }
        return null;
    }
}
