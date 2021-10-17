import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
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
    private static String DEFAULT_HOST = "localhost";
    private static String DEFAULT_PORT = "6001";

    private Selector selector;
    private SocketChannel clientChannel;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final ClientConnect connect = new ClientConnect();

    private User user;

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

        ipField = new JTextField(DEFAULT_HOST);
        portField = new JTextField(DEFAULT_PORT);
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

            Message message = new Message();
            message.setCommand("LOGIN");
            message.setUserName(user.getName());
            message.setContent(user.getName() + " подключается к чату...");

            sendMessage(message);

        }

        if (actionCommand.equals(SEND_TEXT)) {
            String text = chatContentField.getText();

            chatContentField.setText(" ");

            if (text == null || text.equals("")) {
                JOptionPane.showMessageDialog(this, "Вы ничего не ввели", "Ошибка подключения",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Message message = new Message();
            message.setCommand("CONNECTION");
            message.setUserName(user.getName());
            message.setContent(text);

            sendMessage(message);

//            sendText(text, user.getPersonalChannel());
        }

        if (actionCommand.equals(STOP_SERVER)) {
            try {
                System.out.println("Соединение разрывается...");

                Message message = new Message();
                message.setCommand("DISSCONNECT");
                message.setUserName(user.getName());
                message.setContent("покинул чат.");

                sendMessage(message);
//                sendText(loginField.getText() + " покинул чат.", clientChannel);

                selector.close();
                clientChannel.close();
                historyRecordArea.append(" =========   Сеанс завершен. ======= \n");

                initBtnAndTextConnect();

            } catch (IOException ee) {
                System.out.println("Соединение завершено.");
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

                        if (!key.isValid()) {
                            continue;
                        }


                        if (key.isReadable()) {
                            receive(key);
                        }

                        iterator.remove();
                    }
                }

            } catch (IOException e) {
                System.out.println("Ошибка в теле Thread.run()");
            } catch (ClosedSelectorException ce) {
                System.out.println("Селектор закрыт");
            }
        }
    }

    private void init() {
        clientChannel = null;
        try {
            String ipaddr = ipField.getText();
            String port = portField.getText();
            String name = loginField.getText();

            if (ipaddr != null && ipaddr.isEmpty() || ipaddr != null && port.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the server ip and port number!");
                return;
            }

            //  selectable channel for stream-oriented connecting sockets
            System.out.println("Подключение к серверу " + ipaddr + " по порту " + port + "...");
            historyRecordArea.append(String.format("===========> Подключение к серверу %s по порту %s \n", ipaddr, port));

            selector = Selector.open();
            clientChannel = SocketChannel.open(new InetSocketAddress(ipaddr, Integer.parseInt(port)));
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, null);

            user = new User(name, clientChannel);

            System.out.println(user.getName() + " подключился...");

            connect.start();

        } catch (NumberFormatException ne) {
            System.out.println();
        } catch (ConnectException | ClosedChannelException cce) {
            LOGGER.info("Closes this connection.");
            try {
                assert clientChannel != null;
                clientChannel.close();
            } catch (Exception e) {
                System.out.println("Сервер временно недоступен или введены некорректные данные \n");
                historyRecordArea.append("===========> Сервер временно недоступен или введены некорректные данные \n");
            }
        } catch (IOException e) {
            System.out.println("Подключение прошло неудачно...");
            historyRecordArea.append("===========> Подключение прошло неудачно \n");
        }
    }

 /*   private void sendText(String msg, SocketChannel sc) {
        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            String input = reader.readLine().trim();
            buffer.clear();
            byte[] data = msg.getBytes();
            buffer = ByteBuffer.wrap(data);
            sc.write(buffer);
            buffer.clear();
        } catch (Exception e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
            System.exit(0);
            try {
                sc.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }*/

    private void sendMessage(Message msg) {
        try {
            buffer.clear();
            Gson gson = new Gson();
            byte[] data = gson.toJson(msg).getBytes();
            buffer = ByteBuffer.wrap(data);
            clientChannel.write(buffer);
            buffer.clear();
        } catch (Exception e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
            try {
                clientChannel.close();
            } catch (IOException ex) {
                System.out.println("Не удалось закрыть канал...");
            }
        }
    }

    private void receive(SelectionKey key) {
        showBtnAndTextConnectSuccess();
        SocketChannel socketChannel = null;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            socketChannel = (SocketChannel) key.channel();

            int length = socketChannel.read(buffer);
            if (length == -1) {
                key.cancel();
                socketChannel.close();
                return;
            }

            String msg = new String(buffer.array(), 0, length, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Message message = gson.fromJson(msg, Message.class);
            System.out.println(message);
            String username = message.getUserName();
            String command = message.getCommand();
            String content = message.getContent();


            System.out.println(username + ": " + content);
            historyRecordArea.append(username + ": " + content + " " + TimeUtils.getCurrentTime() + "\n");
            buffer.clear();
            key.interestOps(SelectionKey.OP_READ);

        } catch (IOException e) {
            System.out.println("Ошибка в теле receive(). Не удалось получить сообщение. \n");
            key.cancel();
            try {
                socketChannel.close();
            } catch (IOException ex) {
                System.out.println(" Что-то пошло не так при закрытии канала.  \n");
            }

        } catch (JsonSyntaxException jse) {
            System.out.println("Что-то не так с присланным JSON");
        }


    }

}
