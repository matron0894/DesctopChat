import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;


public class ServerChat extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ServerChat.class.getName());
    private SelectionKey serverKey;
    private static Selector selector;

    public ServerChat() {
        init();
    }

    public static void main(String[] args) throws Exception {
        ServerChat server = new ServerChat();
        server.start();
    }


    private void init() {
        try {
            //A multiplexor of SelectableChannel objects.
            //Selector — своеобразный слушатель, который сообщает, когда с каналом можно совершить какое-то действие.
            selector = Selector.open();

            ServerSocketChannel serverSocket = ServerSocketChannel.open();

            //Binds the channel's socket to a local address and
            // configures the socket to listen for connections.
            serverSocket.bind(new InetSocketAddress("localhost", 6001));
            //configureBlocking - if false then it will be placed non-blocking mode
            serverSocket.configureBlocking(false);

            //return SelectionKey.OP_ACCEPT
            //OP_ACCEPT используется для ожидания сервером соединения с клиентом
            int supportedOps = serverSocket.validOps();

            //A selectable channel's registration with a selector is represented by a SelectionKey object.
            // A selection key is created each time a channel is registered with a selector.
            //Registers this channel with the given selector, returning a selection key.
            serverKey = serverSocket.register(selector, supportedOps, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            do {
                LOGGER.info("I'm a serverSocket and I'm waiting for new connection and buffer select...");

                // Selects a set of keys whose corresponding channels are ready for I/O operations
                // Ждем до того, как появится хотя бы одно событие. Как появятся, выбираем ключи с этими событиями
                int count = selector.select();
                if (count == 0) continue;

                // token representing the registration of a SelectableChannel with a Selector
                //Returns this selector's selected-key set. Все, что регистрирует Selector и что ждет данных,
                //является частью этого набора ключей.
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();

                // После получения набора ключей, последовательно пройдите по каждому ключу, удаляя и обрабатывая его.
                Iterator<SelectionKey> iterator = selectionKeySet.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    /** Для каждого ключа, с которым вы работаете, вы можете проверить его статус с помощью таких методов
                     как isAcceptable или isReadable. Они сообщают вам, какую операцию ждет ключ.*/

                    // Tests whether this key's channel is ready to accept a new socket connection
                    if (key.isValid() && key.isAcceptable()) {  // Эквивалент selectionKey.readyOps () & SelectionKey.OP_ACCEPT,
                        System.out.println(key + ": Получить");
                        iterator.remove(); /// Удаляем ключ из выбранных, так как мы его обработали
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = serverChannel.accept();

                        // Adjusts this channel's blocking mode to false
                        clientChannel.configureBlocking(false);

                        // Operation-set bit for read operations
                        clientChannel.register(selector, SelectionKey.OP_READ); //Принимаем подключение у сервера. Тут же его регистрируем в селекторе с OP_READ.
                        LOGGER.info("Connection Accepted: " + clientChannel.getLocalAddress() + "\n");
                    }

                    // Tests whether this key's channel is ready for reading
                    if (key.isValid() && key.isReadable()) {
                        System.out.println("Получено: " + key);
                        read(key);

                    }

                    // Tests whether this key's channel is ready for reading
                    if (key.isValid() && key.isWritable()) {
                        System.out.println("Отправить: " + key);
                        write(key);
                    }

                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        SocketChannel clientChannel = null;
        try {
            //Returns the channel for which this key was created.
            clientChannel = (SocketChannel) key.channel();

            /*ByteBuffer: A byte buffer.
            This class defines six categories of operations upon byte buffers:
            Absolute and relative get and put methods that read and write single bytes;
            Absolute and relative bulk get methods that transfer contiguous sequences
            of bytes from this buffer into an array;*/
            // Cоздаёт буфер в Heap. Можно преобразовать в массив с помощью метода array()
            // Устанавливаем буферный буфер
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // Если клиент закрывает канал, при чтении данных из канала возникнет IOException.После обнаружения исключения закройте канал и отмените ключ
            int count = clientChannel.read(buffer);
            StringBuilder buf = new StringBuilder();
            // Если данные читаются
            if (count > 0) {
                // Позволяем буферу переворачиваться и читать данные в буфере
                buffer.flip();
                buf.append(new String(buffer.array(), 0, count));
            }
            String msg = buf.toString();
            printInfo(msg);

            if (msg.equals("goodbye")) {
                LOGGER.info("\nServer will keep running. Try running client again to establish new connection");
            }

            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

            while (iter.hasNext()) {
                SelectionKey sKey = iter.next();
                sKey.attach(msg);
                sKey.interestOps(sKey.interestOps() | SelectionKey.OP_WRITE);
            }

            buffer.clear();
        } catch (IOException e1) {
            // Когда клиент закрывает канал, сервер сообщит об исключении IOException при записи или чтении данных в буфер канала.
            // Решение: перехватить это исключение на сервере и закрыть канал канала на сервере
            key.cancel();
            System.out.println("Goodbye client!");
            try {
                clientChannel.socket().close();
                clientChannel.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    private void write(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            Object attachment = key.attachment();
            // После получения значения ключа оставьте значение ключа пустым, чтобы не повлиять на следующее использование
            key.attach("");
            channel.write(ByteBuffer.wrap(attachment.toString().getBytes()));
            key.interestOps(SelectionKey.OP_READ);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String printInfo(String info) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("(HH:mm:ss) ");
        String time = dtf.format(LocalDateTime.now());
        return time + info;
    }
}