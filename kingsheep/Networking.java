package kingsheep;

import java.util.Scanner;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Utility class for communication between Viewer and Server.
 */
class Networking {
    /** The port used. */
    public static final int PORT = 63345;

    /** The login packet ("SHEEP"). */
    public static final byte[] LOGIN = {83, 72, 69, 69, 80};
    /** The reconnect packet ("GRASS"). */
    public static final byte[] RECONNECT =  {71, 82, 65, 83, 83};
    /** The disconnect packet ("WOLF"). */
    public static final byte[] DISCONNECT = {87, 79, 76, 70};

    /** Packet types. */
    public static final byte
        MAPROW = 10,
        MOVE = 1,
        ENDTURN = 2,
        NEWGAME = 5,
        ENDGAME = 20;

    /**
     * Sends a packet.
     *
     * @param out The output stream to send the packet over.
     * @param p The packet to send.
     * @throws IOException If something went wrong during sending.
     */
    public static void sendPacket(OutputStream out, Packet p)
        throws IOException {
        out.write(p.data);
        out.flush();
    }

    /**
     * Receives a packet.
     *
     * @param in The input stream to receive the packet from.
     * @throws IOException If something went wrong during sending.
     */
    public static Packet recvPacket(InputStream in) throws IOException {
        byte type = (byte)in.read();
        byte[] data;
        switch (type) {
        case MAPROW:
            data = new byte[20];
            break;
        case MOVE:
            data = new byte[3];
            break;
        case ENDTURN:
            data = new byte[1];
            break;
        case NEWGAME:
            data = new byte[in.read() + 2];
            data[0] = type;
            data[1] = (byte)(data.length - 2);
            in.read(data, 2, data.length - 2);
            return new NewGamePacket(data);
        case ENDGAME:
            data = new byte[2];
            break;
        case 83:
            data = new byte[LOGIN.length];
            break;
        case 71:
            data = new byte[RECONNECT.length];
            break;
        case 87:
            data = new byte[DISCONNECT.length];
            break;
        case -1:
            return new Packet(DISCONNECT);
        default:
            throw new IOException(String.format("Unexpected packet type: %d%n",
                                                type));
        }

        data[0] = type;
        in.read(data, 1, data.length-1);
        switch (type) {
        case MAPROW:
            return new MapPacket(data);
        case MOVE:
            return new MovePacket(data);
        default:
            return new Packet(data);
        }
    }

    /**
     * An ordinary packet with no special data.
     */
    public static class Packet {

        /** The data to be sent/received. */
        public byte[] data;

        /** Creates an empty packet. */
        Packet() {}

        /**
         * Creates a packet.
         *
         * @param data The data contained in the packet.
         */
        Packet(byte[] data) {
            this.data = data;
        }
    }

    /**
     * A packet describing creature movement.
     */ 
    public static class MovePacket extends Packet {

        /** The type of creature that moved. */
        public Type creature;
        /** The move made by the creature. */
        public Creature.Move direction;

        /**
         * Creates a move packet from a string of bytes.
         *
         * @param data The bytes containing the move.
         */
        MovePacket(byte[] data) {
            super(data);
            switch (data[1]) {
            case 0: creature = Type.SHEEP1; break;
            case 1: creature = Type.WOLF1; break;
            case 2: creature = Type.SHEEP2; break;
            case 3: creature = Type.WOLF2; break;
            }

            switch (data[2]) {
            case 0: direction = Creature.Move.WAIT; break;
            case 1: direction = Creature.Move.UP; break;
            case 2: direction = Creature.Move.RIGHT; break;
            case 3: direction = Creature.Move.DOWN; break;
            case 4: direction = Creature.Move.LEFT; break;
            }
        }

        /**
         * Creates a move packet from a creature and direction.
         *
         * @param creature The type of creature that moved.
         * @param direction The direction in which the creature moved. 
         */
        MovePacket(Type creature, Creature.Move direction) {
            this.creature = creature;
            this.direction = direction;
            data = new byte[3];
            data[0] = MOVE;
            byte tmp = 0;

            switch (creature) {
            case SHEEP1: tmp = 0; break;
            case SHEEP2: tmp = 2; break;
            case WOLF1: tmp = 1; break;
            case WOLF2: tmp = 3; break;
            }
            data[1] = tmp;

            switch (direction) {
            case UP: tmp = 1; break;
            case RIGHT: tmp = 2; break;
            case DOWN: tmp = 3; break;
            case LEFT: tmp = 4; break;
            default: tmp = 0;
            }
            data[2] = tmp;
        }
    }

    /**
     * A packet with information about a new game/match.
     */
    public static class NewGamePacket extends Packet {

        /** Team name of player 1. */
        public String team1;
        /** Team name of player 2. */
        public String team2;

        /**
         * Creates a new game packet from a string of bytes.
         *
         * @param data The bytes containing the team names.
         */
        NewGamePacket(byte[] data) {
            super(data);
            Scanner s = new Scanner(new String(data).substring(2));
            s.useDelimiter(";");
            team1 = s.next();
            team2 = s.next();
        }

        /**
         * Creates a new game packet from two team names.
         *
         * @param team1 Player 1's team name.
         * @param team2 Player 2's team name.
         */
        NewGamePacket(String team1, String team2) {
            this.team1 = team1;
            this.team2 = team2;
            byte[] team1B = team1.getBytes(), team2B = team2.getBytes();
            data = new byte[team1B.length + team2B.length + 3];
            data[0] = NEWGAME;
            data[1] = (byte)(data.length - 2);
            System.arraycopy(team1B, 0, data, 2, team1B.length);
            data[team1B.length+2] = 59;
            System.arraycopy(team2B, 0, data, 3 + team1B.length, team2B.length);
        }
    }

    /**
     * A packet containing one row of map data.
     */
    public static class MapPacket extends Packet {

        /** A row in the map. */
        public Type[] row;

        /**
         * Converts from byte to type.
         * This could probably be simplified using {@link Type#getType(char)}.
         *
         * @param b The byte to get the type for.
         */
        Type getType(byte b) {
            switch (b) {
            case 71: return Type.GRASS;
            case 82: return Type.RHUBARB;
            case 49: return Type.SHEEP1;
            case 50: return Type.WOLF1;
            case 51: return Type.SHEEP2;
            case 52: return Type.WOLF2;
            case 35: return Type.FENCE;
            default: return Type.EMPTY;
            }
        }

        /**
         * Converts from type to byte.
         * This could probably be simplified using {@link Type#c}.
         *
         * @param t The type to get the byte for.
         */
        byte getByte(Type t) {
            switch (t) {
            case GRASS: return 71;
            case RHUBARB: return 82;
            case SHEEP1: return 49;
            case WOLF1: return 50;
            case SHEEP2: return 51;
            case WOLF2: return 52;
            case FENCE: return 35;
            default: return 46;
            }
        }

        /**
         * Creates a map packet from a string of bytes.
         *
         * @param data The bytes representing the row in a map.
         */
        MapPacket(byte[] data) {
            super(data);
            row = new Type[19];
            for (int i = 0; i < row.length; i++) {
                row[i] = getType(data[i+1]);
            }
        }

        /**
         * Creates a map packet from a row of types.
         *
         * @param row The row in the map to create a packet for.
         */
        MapPacket(Type[] row) {
            this.row = row;
            data = new byte[row.length + 1];
            data[0] = MAPROW;
            for (int i = 0; i < row.length; i++) {
                data[i+1] = getByte(row[i]);
            }
        }
    }
}