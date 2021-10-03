import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientChat {

    private static final Logger LOGGER = Logger.getLogger(ClientChat.class.getName());
    private static User user;


    public static void main(String[] args) {
        System.out.println("Введите свое имя: ");
        Scanner scanner = new Scanner(System.in);
        String name = scanner.next();
        try {
            connect(name);
        } catch (IOException e) {
            System.out.println("Подключение прошло неудачно...");
        }

    }


    public static void connect(String username) throws IOException {
        SocketChannel clientSocketChannel = null;
        try {
            //  selectable channel for stream-oriented connecting sockets
            clientSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 6001));
            user = new User(username, clientSocketChannel);
            System.out.println("Connecting to Server on port 6001...");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            ByteBuffer buffer;
            do {
                System.out.println("Напиши что нибудь: ");
                String input = reader.readLine();
                byte[] message = input.getBytes();
                buffer = ByteBuffer.wrap(message);
                clientSocketChannel.write(buffer);
                buffer.clear();

                int bytesRead = clientSocketChannel.read(buffer); //read into buffer.

                while (bytesRead != -1) {
                    buffer.flip();  //make buffer ready for read
                    while (buffer.hasRemaining()) {
                        System.out.print((char) buffer.get()); // read 1 byte at a time
                    }
                    buffer.clear(); //make buffer ready for writing
                    bytesRead = clientSocketChannel.read(buffer);
                }
            } while (true);

            // close(): Closes this channel.
            // If the channel has already been closed then this method returns immediately.
            // Otherwise it marks the channel as closed and then invokes the implCloseChannel method in order to complete the close operation
        } catch (IOException e) {
            e.printStackTrace();
            clientSocketChannel.close();
            LOGGER.info("Closes this connection.");
        }
    }
}

