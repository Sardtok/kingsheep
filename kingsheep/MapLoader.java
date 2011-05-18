package kingsheep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A utility class that loads maps.
 */
class MapLoader {

    /**
     * Loads the given map and creates the player objects
     *
     * @param mapName Name of the map file to load.
     * @param p1 Player 1.
     * @param p2 Player 2.
     */
    public static Type[][] loadMap(String mapName, Player p1, Player p2) {

        Type[][] map = new Type[Gfx.YUNIT][Gfx.XUNIT];
        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(MapLoader.class
							  .getClassLoader()
                                                          .getResourceAsStream
                                                          (mapName)));
        } catch (NullPointerException e) {
            System.err.printf("Could not open map file '%s'\n", mapName);
            System.exit(1);
        }

        int next = -1;
        for (int y = 0; y < Gfx.YUNIT; ++y) {
            for (int x = 0; x < Gfx.XUNIT; ++x) {
                try {
                    next = in.read();
                    if (next == 10)
                        next = in.read();  // Skip line break
                } catch (IOException ioe) {
                    System.err.println(ioe.getMessage());
                }

                if (next == -1)
                    break;

                map[y][x] = Type.getType((char)next);

                if (map[y][x] == Type.SHEEP1) {
                    p1.sheep.x = x;
                    p1.sheep.y = y;
                } else if (map[y][x] == Type.SHEEP2) {
                    p2.sheep.x = x;
                    p2.sheep.y = y;
                } else if (map[y][x] == Type.WOLF1) {
                    p1.wolf.x = x;
                    p1.wolf.y = y;
                } else if (map[y][x] == Type.WOLF2) {
                    p2.wolf.x = x;
                    p2.wolf.y = y;
                }
            }
        }

        return map;
    }
}