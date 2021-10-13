import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class UserInterface extends JFrame implements ActionListener {
    private JButton startServerBtn;
    private JButton stopServerBtn;
    private JButton sendBtn;

    private JTextField ipField;
    private JTextField portField;
    private JTextField chatContentField;
    private JTextField loginField;
    private JPasswordField passField;

    private JTextArea historyRecordArea;

    private static final String START_SERVER = "Connect";
    private static final String STOP_SERVER = "Disconnect";
    private static final String SEND_TEXT = "Send";


    private static final Logger LOGGER = Logger.getLogger(ClientChat.class.getName());
    private static String HOST = "localhost";
    private static String PORT = "6001";

    private ByteBuffer buffer = ByteBuffer.allocate(256);
    private Selector selector;
    private SocketChannel clientChannel;

    public static void main(String[] args) {

//        SwingUtilities.invokeLater(CltInf::new);

        new UserInterface();
    }

    public UserInterface() {
        showPanel();
        setupListener();
    }

    private void showPanel() {
        JFrame frame = new JFrame("Chat");
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
        settingPanel.setLayout(new GridLayout(2, 5, 5, 8));

        JLabel ipLabel = new JLabel("Server ip:");
        JLabel portLabel = new JLabel("Server port:");
        JLabel loginLabel = new JLabel("Login:");
        JLabel passLabel = new JLabel("Password:");

        ipField = new JTextField(HOST);
        portField = new JTextField(PORT);
        loginField = new JTextField("masha");
        passField = new JPasswordField("123456");

        startServerBtn = new JButton(START_SERVER);
        stopServerBtn = new JButton(STOP_SERVER);

        settingPanel.add(ipLabel);
        settingPanel.add(ipField);
        settingPanel.add(portLabel);
        settingPanel.add(portField);
        settingPanel.add(startServerBtn);
        settingPanel.add(loginLabel);
        settingPanel.add(loginField);
        settingPanel.add(passLabel);
        settingPanel.add(passField);
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

        initBtnAndTextConnect();

    }

    private void initBtnAndTextConnect() {
        startServerBtn.setEnabled(true);
        loginField.setEnabled(true);
        passField.setEnabled(true);
        ipField.setEnabled(true);
        portField.setEnabled(true);

        stopServerBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        //clearContentBtn.setEnabled(false);
        chatContentField.setEnabled(false);
    }

    private void showBtnAndTextConnectSuccess() {
        startServerBtn.setEnabled(false);
        loginField.setEnabled(false);
        passField.setEnabled(false);
        ipField.setEnabled(false);
        portField.setEnabled(false);

        stopServerBtn.setEnabled(true);
        sendBtn.setEnabled(true);
        //clearContentBtn.setEnabled(true);
        chatContentField.setEnabled(true);
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

        if (actionCommand.equals(START_SERVER)) {
            init();
        }

        if (actionCommand.equals(SEND_TEXT)) {
            String text = chatContentField.getText();
            historyRecordArea.append(TimeUtils.getCurrentTime() +  text + "\n");
            chatContentField.setText("  ");

            send(text, clientChannel);
        }

        if (actionCommand.equals(STOP_SERVER)) {
            try {
                selector.close();
                clientChannel.close();

                System.out.println("Соединение разрывается...");
                historyRecordArea.append(" =========   Сеанс завершен. ======= ");

                initBtnAndTextConnect();

            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
    }


///////////////////////////////////////////////////////////////////////////////

    class ClientConnect extends Thread {
        @Override
        public void run() {
            try {
                for (; ; ) {
                    int count = selector.select();

                    if (count == 0) continue;

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();

                        if (key.isReadable()) {
                            receive(key);
                        }

                        iterator.remove();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void init() {
        clientChannel = null;
        try {
            //  selectable channel for stream-oriented connecting sockets
            System.out.println("Подключение к серверу " + HOST + " по порту " + PORT + "...");
            selector = Selector.open();
            clientChannel = SocketChannel.open(new InetSocketAddress(HOST, Integer.parseInt(PORT)));
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, null);
            System.out.println(loginField.getText() + " подключился...");

            ClientConnect connect = new ClientConnect();
            connect.start();

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Closes this connection.");
            try {
                assert clientChannel != null;
                clientChannel.close();
            } catch (Exception e2) {
                System.out.println("Подключение прошло неудачно...");
                e2.printStackTrace();
            }
        }
    }


    private void send(String msg, SocketChannel sc) {
        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            String input = reader.readLine().trim();
            byte[] message = msg.getBytes();
            buffer = ByteBuffer.wrap(message);
            sc.write(buffer);
            buffer.clear();
        } catch (Exception e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
        }
    }

    private void receive(SelectionKey key) {

        showBtnAndTextConnectSuccess();

        try {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            buffer.clear();
            socketChannel.read(buffer); //read into buffer.
            buffer.flip();  //make buffer ready for read

            System.out.print(TimeUtils.getCurrentTime());
            StringBuilder msg = new StringBuilder();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get()); // read 1 byte at a time
            }
            System.out.println(msg);
            buffer.clear();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.out.println("Что-то пошло не так. Не удалось получить сообщение.");

        }
    }

}
