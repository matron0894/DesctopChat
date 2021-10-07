import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class ClientChat {

    private static final Logger LOGGER = Logger.getLogger(ClientChat.class.getName());
    private static final String HOST = "localhost";
    private static final int PORT = 6001;

    private static User user;
    private static ByteBuffer buffer;

    public static void main(String[] args) {
//        System.out.println("Введите свое имя: ");
//        Scanner scanner = new Scanner(System.in);
//        String name = scanner.nextLine().trim();

        String name = "User";
        try {
//            SwingUtilities.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    interfaceView = new InterfaceView();
//                }
//            });
            init(name);
        } catch (IOException e) {
            System.out.println("Подключение прошло неудачно...");
        }

    }

    static void init(String username) throws IOException {
        SocketChannel clientSocketChannel = null;
        try {
            //  selectable channel for stream-oriented connecting sockets
            clientSocketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
            user = new User(username, clientSocketChannel);

            System.out.println("Connecting to Server on port 6001...");

            while (true) {

                send(clientSocketChannel);

                receive(clientSocketChannel);
            }

            // close(): Closes this channel.
            // If the channel has already been closed then this method returns immediately.
            // Otherwise it marks the channel as closed and then invokes the implCloseChannel method in order to complete the close operation
        } catch (
                IOException e) {
            e.printStackTrace();
            clientSocketChannel.close();
            LOGGER.info("Closes this connection.");
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
        } catch (IOException e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
        }
    }

    private static void receive(SocketChannel sc) {
        try {
            sc.read(buffer); //read into buffer.
            buffer.flip();  //make buffer ready for read

            System.out.print(TimeUtils.getCurrentTime());
            while (buffer.hasRemaining()) {
                System.out.print((char) buffer.get()); // read 1 byte at a time
            }
            System.out.println();
            buffer.clear(); //make buffer ready for writing
        } catch (IOException e) {
            System.out.println("Что-то пошло не так. Не удалось отправить сообщение.");
        }
    }


}

