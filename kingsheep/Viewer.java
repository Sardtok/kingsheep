package kingsheep;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 * A client to view tournaments/games played on a king sheep server.
 */
public class Viewer implements Runnable {

    /** The socket used for the connection to the server. */
    private Socket sock;
    /** The input stream associated with the socket. */
    private InputStream sin;
    /** The output stream associated with the socket. */
    private OutputStream sout;

    /** The window. */
    private Gfx gfx;
    /** Buffer strategy used for double buffering. */
    private BufferStrategy strategy;
    /** Colours used when displaying strings. */
    private static final Color
        DRAW = new Color(222, 0, 222),
        P1 = new Color(222, 0, 0),
        P2 = new Color(64, 64, 222);

    /** The map that is currently being played. */
    private Type[][] map = new Type[15][];

    /** All the images we need. */
    private ImageIcon imgEmpty, imgSheep1, imgSheep2,
        imgWolf1, imgWolf2, imgGrass, imgRhubarb, imgSkigard;

    /** Indices for the creatures used for coordinate lookups. */
    private static final int SHEEP1 = 0, WOLF1 = 1, SHEEP2 = 2, WOLF2 = 3;

    /** Wait time between frames. */
    private static final int FRAMEWAIT = 150;

    /** String used for fullscreen mode. */
    private static final String FULLSCREEN = "-f";

    /** Coordinates for the creatures. */
    private int[][] coords = new int[4][2];

    /** Player scores. */
    private int[] scores = new int[2];
    /** Current turn. */
    private int turn;
    /** Team names. */
    private String team1, team2;

    /** The server to connect to. */
    private String server = null;

    /** A queue of packets received from the server (buffer). */
    private List<Networking.Packet> packets = new Vector<Networking.Packet>();

    /**
     * Creates a new Viewer.
     *
     * @param server The address of the server to connect to.
     * @param fullscreen Whether to run in fullscreen exclusive mode or not.
     */
    Viewer(String server, final boolean fullscreen) {
        this.server = server;
        connect();

        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    loadImages();
                    gfx = new Gfx(fullscreen);
                    gfx.setBackground(Color.BLACK);
                    strategy = gfx.getBufferStrategy();
                    Graphics g = strategy.getDrawGraphics();
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, Gfx.WIDTH, Gfx.HEIGHT);
                    g.dispose();
                    strategy.show();
                }
            });
    }

    /**
     * Entry point, creates a client and connects to the server.
     */
    public static void main(String[] args) {
        boolean fullscreen = false;
        String server = null;
        if (args.length == 2 && args[0].equals(FULLSCREEN)) {
            fullscreen = true;
            server = args[1];
        } else if (args.length > 0) {
            server = args[0];
        }
        Viewer v = new Viewer(server, fullscreen);
        v.run();
    }

    /**
     * Reads packets from the server until it receives a disconnect packet.
     */
    public void run() {
        while(true) {
            Networking.Packet p = getPacket();
            if (p instanceof Networking.NewGamePacket) {
                createNewGame((Networking.NewGamePacket) p);
            } else if (p instanceof Networking.MovePacket) {
                move((Networking.MovePacket) p);
            } else if (p.data[0] == Networking.ENDGAME) {
                endGame(p.data[1]);
            } else if (Arrays.equals(p.data, Networking.RECONNECT)) {
                connect(); // Reconnect to the server
            } else if (Arrays.equals(p.data, Networking.DISCONNECT)) {
                disconnect(); // Disconnect
            } else if (p.data[0] == Networking.ENDTURN) {
                turn++;
            } else {
                System.err.println("Invalid packet received!"
                                   + "Possibly out of sequence.");
            }
        }
    }

    /**
     * Tries forever to get a packet from the list of packets.
     */
    private Networking.Packet getPacket() {
        while (packets.size() == 0) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {}
        }
        return packets.remove(0);
    }

    /** Displays the screen.
     *
     *  @param c The creature whose turn it is to move.
     */
    private Graphics display(byte c) {
        Graphics2D g = (Graphics2D) strategy.getDrawGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        drawMap(g);
        drawStats(g, c);
        
        return g;
    }

    /**
     * Draws the tiles on the map.
     *
     * @param g The graphics to draw to.
     */
    private void drawMap(Graphics g) {
        ImageIcon drawMe = null;
        g.setColor(Color.BLACK);
        g.fillRect(0,0,Gfx.WIDTH,Gfx.HEIGHT);
        g.setColor(Color.GREEN);
        g.fillRect(Gfx.XSTART, 0, Gfx.MAPWIDTH, Gfx.HEIGHT);

        for (int i = 0; i < Gfx.YUNIT; ++i) {
            for (int j = 0; j < Gfx.XUNIT; ++j) {
                switch (map[i][j]) {
                case EMPTY:
                    drawMe = imgEmpty;
                    break;
                case GRASS:
                    drawMe = imgGrass;
                    break;
                case FENCE:
                    drawMe = imgSkigard;
                    break;
                case RHUBARB:
                    drawMe = imgRhubarb;
                    break;
                case SHEEP1:
                    drawMe = imgSheep1;
                    break;
                case SHEEP2:
                    drawMe = imgSheep2;
                    break;
                case WOLF1:
                    drawMe = imgWolf1;
                    break;
                case WOLF2:
                    drawMe = imgWolf2;
                    break;
                default:
                    break;
                }

                g.drawImage(drawMe.getImage(), j * Gfx.UNIT + Gfx.XSTART, i * Gfx.UNIT,
                            null, null);
            }
        }
    }

    /**
     * Draws game stats around the map (team names, points, turn).
     *
     * @param g The graphics to draw to.
     * @param c The team whose turn it is
     *          (formerly used to display who's thinking).
     */
    private void drawStats(Graphics g, byte c) {
        Font f = new Font(Font.MONOSPACED, Font.BOLD, 32);
        FontMetrics fm = g.getFontMetrics(f);
        int y = fm.getAscent();
        g.setFont(f);
        g.setColor(P1);
        g.drawString(team1, 0, y);
        g.setColor(P2);
        g.drawString(team2, Gfx.WIDTH - fm.stringWidth(team2), y);

        f = new Font(Font.MONOSPACED, Font.BOLD, 96);
        fm = g.getFontMetrics(f);
        y = fm.getDescent();
        g.setFont(f);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("#" + turn,
                     Gfx.WIDTH - fm.stringWidth("#" + turn),
                     Gfx.HEIGHT - y);
        g.setColor(P1);
        g.drawString("" + scores[0],
                     (Gfx.XSTART - fm.stringWidth("" + scores[0])) / 2,
                     Gfx.HEIGHT / 2);
        g.setColor(P2);
        g.drawString("" + scores[1],
                     Gfx.WIDTH - (fm.stringWidth("" + scores[1])
                                  + Gfx.XSTART) / 2,
                     Gfx.HEIGHT / 2);
    }

    /**
     * Moves a creature on the map.
     *
     * @param p The packet describing the move.
     */
    private void move(final Networking.MovePacket p) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    display(p.data[1]).dispose();
                    strategy.show();
                }
            });

        int[] coords = this.coords[p.data[1]];

        switch(p.direction) {
        case UP:
            map[coords[0]][coords[1]] = Type.EMPTY;
            coords[0]--;
            break;
        case DOWN:
            map[coords[0]][coords[1]] = Type.EMPTY;
            coords[0]++;
            break;
        case LEFT:
            map[coords[0]][coords[1]] = Type.EMPTY;
            coords[1]--;
            break;
        case RIGHT:
            map[coords[0]][coords[1]] = Type.EMPTY;
            coords[1]++;
            break;
        }

        // Count points on the client side
        if (p.creature == Type.SHEEP1 || p.creature == Type.SHEEP2) {
            if (map[coords[0]][coords[1]] == Type.GRASS) {
                scores[p.data[1] / 2]++;
            } else if (map[coords[0]][coords[1]] == Type.RHUBARB) {
                scores[p.data[1] / 2] += 5;
            }
        }
            
        map[coords[0]][coords[1]] = p.creature;

        try {
            Thread.sleep(FRAMEWAIT);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Creates a new game.
     *
     * @param p The packet containing the team names.
     */
    private void createNewGame(final Networking.NewGamePacket p) {
        team1 = p.team1;
        team2 = p.team2;

        // Display team names on the screen
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                       RenderingHints.
                                       VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING,
                                       RenderingHints.VALUE_RENDER_QUALITY);

                    Font f = new Font(Font.MONOSPACED, Font.BOLD, 128);
                    FontMetrics fm = g.getFontMetrics(f);
                    int y = fm.getAscent();
                    g.setFont(f);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, Gfx.WIDTH, Gfx.HEIGHT);
                    g.setColor(P1);
                    int center = Gfx.WIDTH / 2;
                    g.drawString("vs.",
                                 center - (fm.stringWidth("vs.") / 2),
                                 Gfx.HEIGHT / 2);
                    g.drawString(p.team1,
                                 center - (fm.stringWidth(p.team1) / 2),
                                 Gfx.HEIGHT / 2 - y);
                    g.drawString(p.team2,
                                 center - (fm.stringWidth(p.team2) / 2),
                                 Gfx.HEIGHT / 2 + y);
                    g.dispose();
                    strategy.show();
                }
            });

        // Load map
        try {
            for (int i = 0; i < map.length; i++) {
                Networking.Packet mp = getPacket();
                if (!(mp instanceof Networking.MapPacket)) {
                    throw new IOException("Not a map packet!");
                }
                map[i] = ((Networking.MapPacket)mp).row;

                for (int j = 0; j < map[i].length; j++) {
                    int index = -1;
                    if (map[i][j] == Type.SHEEP1) {
                        index = SHEEP1;
                    } else if(map[i][j] == Type.SHEEP2) {
                        index = SHEEP2;
                    } else if(map[i][j] == Type.WOLF1) {
                        index = WOLF1;
                    } else if(map[i][j] == Type.WOLF2) {
                        index = WOLF2;
                    }

                    if (index > -1) {
                        coords[index][0] = i;
                        coords[index][1] = j;
                    }
                }
            }
            turn = 0;
            scores[0] = 0;
            scores[1] = 0;
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        } catch (IOException ioe) {
            System.err.printf("Networking error receiving map: %s%n",
                              ioe.getMessage());
            gfx.setReturnCode(2);
            SwingUtilities.invokeLater(new Disposer());
        }
    }

    /**
     * Draws the end game screen.
     *
     * @param winner The player that won the game (1 or 2).
     */
    private void endGame(final byte winner) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Graphics g = display((byte)-1);
                    Font f = new Font(Font.MONOSPACED, Font.BOLD, 100);
                    FontMetrics fm = g.getFontMetrics(f);
                    int y = fm.getAscent();
                    g.setColor(Color.BLACK);
                    g.setFont(f);
                    String toDraw;
                    Color textColor;
                    if (winner != 1 && winner != 2) {
                        textColor = DRAW;
                        toDraw = "It's a draw!";
                    } else {
                        textColor = (winner == 1 ? P1 : P2);
                        toDraw = (winner == 1 ? team1 : team2) + " won!"; 
                    }

                    int x = fm.stringWidth(toDraw);
                    g.fillRect((Gfx.WIDTH - x) / 2 - 15,
                               (Gfx.HEIGHT / 2) - y - 5,
                               x + 20, y + fm.getDescent() + 10);
                    g.setColor(textColor);
                    drawBox(g, (Gfx.WIDTH - x) / 2 - 10,
                            (Gfx.HEIGHT / 2) - y,
                            x + 10, y + fm.getDescent(), 5);
                    g.drawString(toDraw,
                                 (Gfx.WIDTH - x) / 2,
                                 Gfx.HEIGHT / 2);
                    g.dispose();
                    strategy.show();
                }
            });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}
    }

    /**
     * Draws a bordered box around the given area
     * with the given pixel thickness.
     *
     * @param g The Graphics to draw to.
     * @param x The horizontal start position of the box.
     * @param y The vertical start position of the box.
     * @param width The width of the box.
     * @param height The height of the box.
     * @param thickness How many pixels wide the line should be.
     */
    private void drawBox(Graphics g, int x, int y,
                            int width, int height, int thickness) {
        for (int i = 0; i < thickness; i++) {
            g.drawRect(x + i, y + i,
                       width - i - i, height - i - i);
        }
    }

    /**
     * Tries to connect to the server for one minute.
     * If it can connect, it starts a thread to buffer network data.
     * This means the server may disregard the client and start games before
     * the client is done showing the game without filling the buffer.
     */
    private void connect() {
        // Try to connect for up to one minute in ten second intervals.
        for (int i = 0; i < 6; i++) {
            System.out.printf("Trying to connect to %s:%d.%n",
                              server, Networking.PORT);
            try {
                sock = new Socket(server, Networking.PORT);
                sin = sock.getInputStream();
                sout = sock.getOutputStream();
                Networking.sendPacket(sout,
                                      new Networking.Packet(Networking.LOGIN));
                break;
            } catch (IOException ioe) {
                System.err.printf("Could not connect to server: %s%n",
                                  server);
                if (i == 6)
                    System.exit(5);
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }

        // A thread reading packets.
        new Thread(new Runnable() {
                public void run() {
                    while(true) {
                        try {
                            Networking.Packet pack = Networking.recvPacket(sin);
                            packets.add(pack);
                            if (Arrays.equals(pack.data, Networking.DISCONNECT)
                                || Arrays.equals(pack.data,
                                                 Networking.RECONNECT))
                                break;
                        } catch (IOException e) {
                            System.err.printf("Networking error occurred: %s%n",
                                              e.getMessage());
                            gfx.setReturnCode(2);
                            SwingUtilities.invokeLater(new Disposer());
                        }
                    }
                }
            }).start();
        
    }

    /**
     * Disconnects from the server and closes the program.
     */
    private void disconnect() {
        try {
            System.out.println("Good bye!");
            sock.close();
            SwingUtilities.invokeLater(new Disposer());
        } catch (IOException e) {
            System.err.printf("Error closing socket: %s%n", e.getMessage());
            gfx.setReturnCode(10);
            SwingUtilities.invokeLater(new Disposer());
        }
    }

    /** Loads the necessary image files. */
    private void loadImages() {
        imgEmpty   = loadImage("gfx/empty.png");
        imgSheep1  = loadImage("gfx/sheep1.png");
        imgSheep2  = loadImage("gfx/sheep2.png");
        imgWolf1   = loadImage("gfx/wolf1.png");
        imgWolf2   = loadImage("gfx/wolf2.png");
        imgGrass   = loadImage("gfx/grass.png");
        imgRhubarb = loadImage("gfx/rhubarb.png");
        imgSkigard = loadImage("gfx/skigard.png");
    }

    /**
     * Loads an image through the resource system.
     *
     * @param fileName The name of the file to load.
     * @return An ImageIcon containing the loaded image.
     */
    private ImageIcon loadImage(String fileName) {
        ImageIcon ret = null;
        try {
            ret = new ImageIcon(getClass().getClassLoader()
                                .getResource(fileName));
        } catch (Exception e) {
            System.err.printf("Could not load image (%s) - %s%n",
                              fileName,
                              e.getMessage());
            System.exit(1);
        }
        return ret;
    }

    /**
     * A simple Runnable that quits the program by closing the window.
     * This is used in several different places,
     * so it's better to have a normal inner class than an anonymous one.
     */
    private class Disposer implements Runnable {

        /** Closes the window (quitting the client). */
        public void run() {
            gfx.dispose();
        }
    }
}