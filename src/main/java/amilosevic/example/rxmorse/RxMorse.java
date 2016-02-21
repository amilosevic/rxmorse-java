package amilosevic.example.rxmorse;

import javax.swing.*;
import java.awt.*;

/**
 * Created by aleksandar on 2/21/16.
 */
public class RxMorse extends JFrame {

    // window dimensions
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 620;

    // window inital position
    private static final int X = 60;
    private static final int Y = 60;

    // title
    private static final String TITLE = "RxMorse";

    /**
     * Constructs a new frame that is initially invisible.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @throws java.awt.HeadlessException if GraphicsEnvironment.isHeadless()
     *                                    returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Component#setSize
     * @see java.awt.Component#setVisible
     * @see javax.swing.JComponent#getDefaultLocale
     */
    public RxMorse() throws HeadlessException {
        super();

        setSize(WIDTH, HEIGHT);
        setLocation(X, Y);
        setResizable(false);
        setTitle(TITLE);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        add(new JPanel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxMorse();
            }
        });
    }
}
