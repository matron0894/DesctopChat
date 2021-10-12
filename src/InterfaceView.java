import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InterfaceView extends JFrame implements ActionListener {

    private JButton startServerBtn;
    private JButton stopServerBtn;
    private JButton sendBtn;
    private JTextField chatContentField;
    private JTextArea historyRecordArea;

    private static final String START_SERVER = "Connect";
    private static final String STOP_SERVER = "Disconnect";
    private static final String SEND_TEXT = "Send";

    public InterfaceView() {
        showPanel();
    }

    private void showPanel() {
        JFrame frame = new JFrame("Chat Frame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setVisible(true);

        JMenu menu = new JMenu("Settings");
        JMenuBar bar = new JMenuBar();
        JMenuItem m11 = new JMenuItem("New...");
        menu.add(m11);
        bar.add(menu);
        frame.setJMenuBar(bar);
//     ************************************************************

        JPanel settingPanel = new JPanel();
        settingPanel.setBorder(new TitledBorder("Server Configuration"));
        settingPanel.setLayout(new GridLayout(1, 6, 5, 10));

        JTextField ipField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField("6001");

        JLabel ipLabel = new JLabel("Server ip:");
        JLabel portLabel = new JLabel("Server port:");

        startServerBtn = new JButton(START_SERVER);
        stopServerBtn = new JButton(STOP_SERVER);

        settingPanel.add(ipLabel);
        settingPanel.add(ipField);
        settingPanel.add(portLabel);
        settingPanel.add(portField);
        settingPanel.add(startServerBtn);
        settingPanel.add(stopServerBtn);
//     ************************************************************

        historyRecordArea = new JTextArea();
        historyRecordArea.setForeground(Color.blue);
        historyRecordArea.setEditable(false);
        JScrollPane centerScroll = new JScrollPane(historyRecordArea);
        centerScroll.setBorder(new TitledBorder("Let's chat"));
//     ************************************************************

        JPanel panel = new JPanel(); // the panel is not visible in output
        JLabel time = new JLabel(TimeUtils.getDateTime());
        JLabel input = new JLabel("Input: ");
        chatContentField = new JTextField(30); // accepts upto 10 characters
        chatContentField.setEditable(true);
        sendBtn = new JButton(SEND_TEXT);

        panel.add(time);
        panel.add(input); // Components Added using Flow Layout
        panel.add(chatContentField);
        panel.add(sendBtn);
        panel.getCursor();
//     ************************************************************

        DefaultListModel<String> l = new DefaultListModel<>();
        l.addElement("first item");
        l.addElement("second item");
        JList<String> list = new JList<>(l);
//       list.setBounds(100,100,75,75);
        JScrollPane leftScroll = new JScrollPane(list);
        leftScroll.setBorder(new TitledBorder("online users"));
//     ************************************************************

        frame.getContentPane().add(BorderLayout.NORTH, settingPanel);
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, centerScroll);
        frame.getContentPane().add(BorderLayout.WEST, leftScroll);
        frame.setVisible(true);

       setupListener();


    }

    // Set the listening event of the button
    private void setupListener() {
        startServerBtn.addActionListener(this);
        stopServerBtn.addActionListener(this);
        sendBtn.addActionListener(this);
        //clearContentBtn.addActionListener(this);
        // The carriage return event of the text box to send the message
        chatContentField.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (actionCommand.equals(SEND_TEXT)) {
            String text = chatContentField.getText();
            historyRecordArea.append(text + "\n");
            chatContentField.setText("  ");
        }
    }


    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new InterfaceView());

    }


}
