import java.nio.channels.SocketChannel;

public class User {

    private String name;
    private SocketChannel personalChannel;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SocketChannel getPersonalChannel() {
        return personalChannel;
    }

    public void setPersonalChannel(SocketChannel personalChannel) {
        this.personalChannel = personalChannel;
    }

    public User(String name, SocketChannel personalChannel) {
        this.name = name;
        this.personalChannel = personalChannel;
    }


}
