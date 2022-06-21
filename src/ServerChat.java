import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class for Server
 *
 * @version 1.0
 */

public class ServerChat {

    private static final Logger LOGGER = Logger.getLogger(ServerChat.class.getName());
    private static String HOST = "localhost";
    private static String PORT = "6001";

    private static Selector selector;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private final List<SocketChannel> clientList = new ArrayList<>();
    private final List<String> online = new ArrayList<>();


    public static void main(String[] args) {
        new ServerChat();
    }

    public ServerChat() {
        init();
    }

    private void init() {
        try {
            //A multiplexor of SelectableChannel objects.
            //Selector — своеобразный слушатель, который сообщает, когда с каналом можно совершить какое-то действие.
            if (selector == null) {
                selector = Selector.open();
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                //Binds the channel's socket to a local address and
                // configures the socket to listen for connections.
                serverSocket.bind(new InetSocketAddress(HOST, Integer.parseInt(PORT)));
                //configureBlocking - if false then it will be placed non-blocking mode
                serverSocket.configureBlocking(false);
                //return SelectionKey.OP_ACCEPT
                //OP_ACCEPT используется для ожидания сервером соединения с клиентом
                int supportedOps = serverSocket.validOps();
                //A selectable channel's registration with a selector is represented by a SelectionKey object.
                // A selection key is created each time a channel is registered with a selector.
                //Registers this channel with the given selector, returning a selection key.
                SelectionKey serverKey = serverSocket.register(selector, supportedOps, null);

                LOGGER.info("Сервер успешно запущен!");

                ServerConnect connect = new ServerConnect();
                connect.start();
            }
        } catch (IOException e) {
            System.out.println("Ошибка в теле setSelector()");
        }
    }

    class ServerConnect extends Thread {

        @Override
        public void run() {

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

    }

    private void iterateSKeys(SelectionKey key) {

//        if (!key.isValid()) {
//            continue;
//        }

        // Tests whether this key's channel is ready to accept a new socket connection
        if (key.isAcceptable()) {  // Эквивалент selectionKey.readyOps () & SelectionKey.OP_ACCEPT,
            accept(key);
        }

        // Tests whether this key's channel is ready for reading
        else if (key.isReadable()) {
            read(key);
        }

//        // Tests whether this key's channel is ready for writing
//        else if (key.isWritable()) {
//            sendMessage(key);
//        }
    }

    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverSocket.accept();

            // Adjusts this channel's blocking mode to false
            clientChannel.configureBlocking(false);

            // Operation-set bit for read operations
            clientChannel.register(selector, SelectionKey.OP_READ); //Принимаем подключение у сервера. Тут же его регистрируем в селекторе с OP_READ.
            key.interestOps(SelectionKey.OP_ACCEPT);
            LOGGER.info("Connection Accepted: " + clientChannel.getLocalAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        SocketChannel clientChannel = null;
        try {
            //Returns the channel for which this key was created.
            clientChannel = (SocketChannel) key.channel();
            readBuffer.clear();
            /*ByteBuffer: A byte buffer.
            This class defines six categories of operations upon byte buffers:
            Absolute and relative get and put methods that read and write single bytes;
            Absolute and relative bulk get methods that transfer contiguous sequences
            of bytes from this buffer into an array;
             Cоздаёт буфер в Heap. Можно преобразовать в массив с помощью метода array()
             Устанавливаем буферный буфер
             Если клиент закрывает канал, при чтении данных из канала возникнет IOException.
             После обнаружения исключения закройте канал и отмените ключ*/
            int length = clientChannel.read(readBuffer);

            if (length == -1) {
                key.cancel();
                clientChannel.close();
                clientList.remove(clientChannel);
                return;
            }

            String msg = new String(this.readBuffer.array(), 0, length, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Message message = gson.fromJson(msg, Message.class);
            String username = message.getUserName();
            String command = message.getCommand();
            String content = message.getContent();

            if (command.equals("LOGIN")) {

                online.add(username);

                Message greetingMessage = new Message();
                greetingMessage.setUserName("server");
                greetingMessage.setCommand("LOGIN");
                greetingMessage.setContent("Сервер Приветствуем тебя!");

                String greeting = gson.toJson(greetingMessage, Message.class);
                System.out.println(greeting);

                unicast(greeting, clientChannel);

                clientList.add(clientChannel);

            } else if (command.equals("DISCONNECT")) {
                try {
                    LOGGER.info("Connection close: " + clientChannel.getLocalAddress());

                    Message goodbyeMessage = new Message();
                    goodbyeMessage.setUserName("server");
                    goodbyeMessage.setCommand("DISCONNECT");
                    goodbyeMessage.setContent(username + " покинул чат");

                    String parting = gson.toJson(goodbyeMessage, Message.class);
                    System.out.println(parting);

                    multicast(parting);
                    clientChannel.socket().close();
                    clientChannel.close();
                    clientList.remove(clientChannel);
                } catch (IOException e2) {
                    System.out.println("Что-то пошло не так ...");
                }
            } else {
                System.out.println(username + " " + content + " " + TimeUtils.getCurrentTime());
                readBuffer.clear();
                multicast(msg);
            }
        } catch (IOException e1) {
            // Когда клиент закрывает канал, сервер сообщит об исключении IOException при записи или чтении данных в буфер канала.
            // Решение: перехватить это исключение на сервере и закрыть канал канала на сервере
            key.cancel();
            try {
                System.out.println("Goodbye!");
                LOGGER.info("Connection close: " + clientChannel.getLocalAddress());
                clientChannel.socket().close();
                clientChannel.close();
                clientList.remove(clientChannel);
            } catch (IOException e2) {
                System.out.println("Что-то пошло не так ...");
            }
        } catch (JsonSyntaxException jse) {
            System.out.println("ыла совершена неудачная попытка получения сообщения.");
        }
    }

    private void multicast(String msg) {
        for (SocketChannel sc : clientList) {
            unicast(msg, sc);
        }
    }

    private void unicast(String msg, SocketChannel clientChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.clear();

            buffer.put(msg.getBytes());
            buffer.flip();

            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    private void sendMessage(SelectionKey key) {
//        try {
//            SocketChannel channel = (SocketChannel) key.channel();
//            Object attachment = key.attachment();  //Retrieves the current attachment.
//            // После получения значения ключа оставьте значение ключа пустым, чтобы не повлиять на следующее использование
//            key.attach("");
//            channel.write(ByteBuffer.wrap(attachment.toString().getBytes()));
//            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}