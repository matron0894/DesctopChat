public class Message {

    private String command;
    private String userName;
    private String content;

    public Message() {
    }


    public Message(String name ,String content) {
        this.userName = name;
        this.content = content;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Message: {" +
                "command='" + command + '\'' +
                ", userName='" + userName + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
