import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class InterfaceView {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Chat Frame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);


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

        JButton startServerBtn = new JButton("Connect");
        JButton stopServerBtn = new JButton("Diss");

        settingPanel.add(ipLabel);
        settingPanel.add(ipField);
        settingPanel.add(portLabel);
        settingPanel.add(portField);
        settingPanel.add(startServerBtn);
        settingPanel.add(stopServerBtn);

//     ************************************************************

        JTextArea ta = new JTextArea();
        ta.setCaretPosition(0);
        JScrollPane centerScroll = new JScrollPane(ta);
        centerScroll.setBorder(new TitledBorder("Let's chat"));

//     ************************************************************
        JPanel panel = new JPanel(); // the panel is not visible in output
        JLabel time = new JLabel(CorrectDateTime.getDateTime());
        JLabel input = new JLabel("Input: ");
        JTextField text = new JTextField(30); // accepts upto 10 characters
        JButton send = new JButton("Send");
        panel.add(time);
        panel.add(input); // Components Added using Flow Layout
        panel.add(text);
        panel.add(send);
        panel.getCursor();

//     ************************************************************

        DefaultListModel<String> l = new DefaultListModel<>();
        l.addElement("first item");
        l.addElement("second item");
        JList<String> list = new JList< >(l);
//       list.setBounds(100,100,75,75);
        JScrollPane leftScroll = new JScrollPane(list);
        leftScroll.setBorder(new TitledBorder("online users"));

//     ************************************************************

        frame.getContentPane().add(BorderLayout.NORTH, settingPanel);
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, centerScroll);
        frame.getContentPane().add(BorderLayout.WEST,leftScroll);
        frame.setVisible(true);
    }
}
