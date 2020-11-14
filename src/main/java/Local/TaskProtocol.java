package Local;


public class TaskProtocol {
    private String type; // new task / termination
    private String message;
    private String replyURL;

    public TaskProtocol(String type, String message, String replyURL) {
        this.type = type;
        this.message = message;
        this.replyURL = replyURL;
    }

    @Override
    public String toString() {
        return type + "\n" + message + "\n" + replyURL;
//        return "TaskProtocol{" +
//                "type='" + type + '\'' +
//                ", message='" + message + '\'' +
//                ", replyURL='" + replyURL + '\'' +
//                '}';
    }
}
