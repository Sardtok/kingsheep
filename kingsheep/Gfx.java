package kingsheep;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

/**
 * A window used to display the game.
 */
public class Gfx extends JFrame {

    /** Required because JFrame is Serializable. */
    private static final long serialVersionUID = 1L;

    /** Size of a tile. */
    public static final int UNIT   = 48;

    /** Number of horizontal tiles. */
    public static final int XUNIT  = 19;
    /** Number of vertical tiles. */
    public static final int YUNIT  = 15;

    /** Pixel width of the screen. */
    public static final int WIDTH  = 1280;
    /** Pixel height of the screen. */
    public static final int HEIGHT = 720;
    
    /** Width of the window taken up by the map. */
    public static final int MAPWIDTH = XUNIT * UNIT;
    /** Offset for displaying the map. */
    public static final int XSTART = (WIDTH - MAPWIDTH) / 2;

    /** The display mode used prior to opening this window. */
    private DisplayMode dmode;
    /** The graphics device allowing us to change display modes. */
    private GraphicsDevice device;

    /** The return code used when quitting (this window is disposed). */
    private int returnCode = 0;

    /**
     * Creates a new King Sheep window.
     *
     * @param fullscreen Whether to run in fullscreen mode.
     */
    Gfx(boolean fullscreen) {
        super("King Sheep");

        Container pane = this.getContentPane();

        setIconImage((new ImageIcon("gfx/sheep.png")).getImage());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setVisible(true);
        setIgnoreRepaint(true);

        // Try to set the window to fullscreen exclusive mode if requested
        if (fullscreen) {
            device = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(this);
                Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().createImage(""), new Point(), null);
                if (device.isDisplayChangeSupported()) {
                    dmode = device.getDisplayMode();
                    DisplayMode[] modes = device.getDisplayModes();
                    for (DisplayMode mode : modes) {
                        if (mode.getWidth() == WIDTH
                            && mode.getHeight() == HEIGHT) {
                            device.setDisplayMode(mode);
                            break;
                        }
                    }
                }
            }
        }

        createBufferStrategy(2);
    }

    /**
     * Close the window and quit.
     * Uses the return code set as the return code when the window is closed.
     *
     * @see #returnCode
     * @see #setReturnCode(int)
     */
    public void dispose() {
        if (dmode != null) {
            device.setDisplayMode(dmode);
        }
        super.dispose();
        System.exit(returnCode);
    }

    /**
     * Sets the return code.
     *
     * @param returnCode The new return code.
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
