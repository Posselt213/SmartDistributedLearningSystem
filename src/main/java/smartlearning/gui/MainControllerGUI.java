/**
 *
 * @author comin
 */
package smartlearning.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * This class is the main GUI controller for the project.
 *  INFO https://docs.oracle.com/javase/8/docs/api/javax/swing/package-summary.html
 * - the main window
 * - a top panel with one button
 * - a large text area to display output
 *
 */
public class MainControllerGUI extends JFrame {

    // Button
    private JButton discoverServicesButton;

    // Text to display responses, logs, and errors
    private JTextArea outputArea;

    /**
     * Constructor.
     *
     * This sets up the main window and all visual components.
     */
    public MainControllerGUI() {
        initializeGUI();
    }

    /**
     * This method creates the window layout and components.
     */
    private void initializeGUI() {

        //  Basic window settings
        setTitle("Smart Distributed Learning Support System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centers the window on screen
        setLayout(new BorderLayout());

        // Top panel for buttons
        JPanel topPanel = new JPanel();

        // Create discover services button
        discoverServicesButton = new JButton("Discover Services");

        // Add button to top panel
        topPanel.add(discoverServicesButton);

        // Add top panel to top area of window
        add(topPanel, BorderLayout.NORTH);

        // Output area in the center
        outputArea = new JTextArea();
        outputArea.setEditable(false); // user should not type here
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Put text area inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Add scroll pane to center of window
        add(scrollPane, BorderLayout.CENTER);

        // Initial message for the user
        outputArea.append("GUI started successfully.\n");
        outputArea.append("Click 'Discover Services' when the Naming Service is running.\n");
    }

    /**
     * Main method used to launch the GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainControllerGUI().setVisible(true);
        });
    }
}