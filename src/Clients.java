import java.io.IOException;

public class Clients {

    public static void main(String[] args) {
        ClientChat client = new ClientChat();
        try {
            client.init("Masha");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
