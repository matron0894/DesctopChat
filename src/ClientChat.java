import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class ClientChat {

    private static final Logger LOGGER = Logger.getLogger(ClientChat.class.getName());
    private static final String HOST = "localhost";
    private static final int PORT = 6001;

    private User user;
    private static ByteBuffer buffer = ByteBuffer.allocate(256);
    private static Selector selector;

    public void init(String username) {
        SocketChannel clientChannel = null;
        try {
            //  selectable channel for stream-oriented connecting sockets
            clientChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
            clientChannel.configureBlocking(false);
            System.out.println("Подключение к серверу по порту 6001...");
            selector = Selector.open();
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("Клиент начал");
            user = new User(username, clientChannel);

            ClientConnect connect = new ClientConnect();
            connect.start();

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Closes this connection.");
            try {
                clientChannel.close();
            } catch (Exception e2) {
                System.out.println("Подключение прошло неудачно...");
                e2.printStackTrace();
            }
        }
    }

    private static void send(SocketChannel sc) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String input = reader.readLine().trim();
            byte[] message = input.getBytes();
            buffer = ByteBuffer.wrap(message);
            sc.write(buffer);
            buffer.clear();
        } catch (Exception e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
        }
    }

    private static void receive(SelectionKey key) {
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


    static class ClientConnect extends Thread {

        @Override
        public void run() {
            try {
                while (selector.select() > 0) {
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


}

