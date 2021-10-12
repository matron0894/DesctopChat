import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class for Server
 *
 * @version 1.0
 */

public class ServerChat extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ServerChat.class.getName());
    private static Selector selector;
    List<SocketChannel> clientList = new ArrayList<>();
    private SelectionKey serverKey;

    public static void main(String[] args) {
        ServerChat server = new ServerChat();
        server.start();
    }

    @Override
    public void run() {

        setSelector();

        try {
            for (; ; ) {
                // printInfo("Сервер ждет подключений.");
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

                    iterateSKeys(key);
                }

                iterator.remove();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setSelector() {
        try {
            //A multiplexor of SelectableChannel objects.
            //Selector — своеобразный слушатель, который сообщает, когда с каналом можно совершить какое-то действие.
            if (selector == null) {
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

                LOGGER.info("The server started successfully!");

            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка в теле setSelector()");
        }
    }

    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
            SocketChannel serverChannel = serverSocket.accept();

            // Adjusts this channel's blocking mode to false
            serverChannel.configureBlocking(false);

            // Operation-set bit for read operations
            serverChannel.register(selector, SelectionKey.OP_READ); //Принимаем подключение у сервера. Тут же его регистрируем в селекторе с OP_READ.
            key.interestOps(SelectionKey.OP_ACCEPT);
            LOGGER.info("Connection Accepted: " + serverChannel.getLocalAddress() + "\n");
            writeMsg("Please enter your nickname and communicate after identifying your identity", serverChannel);
            clientList.add(serverChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iterateSKeys(SelectionKey key) {
        /** Для каждого ключа, с которым вы работаете, вы можете проверить его статус с помощью таких методов
         как isAcceptable или isReadable. Они сообщают вам, какую операцию ждет ключ.*/

//        if (!key.isValid()) {
//            continue;
//        }

        // Tests whether this key's channel is ready to accept a new socket connection
        if (key.isAcceptable()) {  // Эквивалент selectionKey.readyOps () & SelectionKey.OP_ACCEPT,
            accept(key);
        }

        // Tests whether this key's channel is ready for reading
        else if (key.isReadable()) {
            receive(key);
        }

        // Tests whether this key's channel is ready for writing
        else if (key.isWritable()) {
            sendMessage(key);
        }
    }

    private void receive(SelectionKey key) {
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
            // Если клиент закрывает канал, при чтении данных из канала возникнет IOException.
            // После обнаружения исключения закройте канал и отмените ключ
            int length = clientChannel.read(buffer);
            StringBuilder buf = new StringBuilder();
            // Если данные читаются
            if (length > 0) {
                // Позволяем буферу переворачиваться и читать данные в буфере
                buffer.flip();
                buf.append(new String(buffer.array(), 0, length, StandardCharsets.UTF_8));
            }
            String msg = buf.toString();
            printInfo(msg);

            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey sKey = iter.next();
                sKey.attach(msg);
                sKey.interestOps(sKey.interestOps() | SelectionKey.OP_WRITE);
                SocketChannel sc = (SocketChannel) sKey.channel();

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

    private void sendMessage(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            Object attachment = key.attachment();  //Retrieves the current attachment.
            // После получения значения ключа оставьте значение ключа пустым, чтобы не повлиять на следующее использование
            key.attach("");
            channel.write(ByteBuffer.wrap(attachment.toString().getBytes()));
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeMsg(String msg, SocketChannel clientChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(100);
            buffer.clear();
            buffer.put(msg.getBytes());
            buffer.flip();
            clientChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAll(String msg) {

    }

    private void printInfo(String info) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("(HH:mm:ss) ");
        String time = dtf.format(LocalDateTime.now());
        System.out.println(time + info);
    }
}