package Local;


public class    TaskProtocol {
    private final String type; // new task / termination
    private final String field1;
    private final String field2;
    private final String replyURL;

    public TaskProtocol(String type, String field1, String field2, String replyURL) {
        this.type = type;
        this.field1 = field1;
        this.field2 = field2;
        this.replyURL = replyURL;
    }

    @Override
    public String toString() {
        return type + "\n" + field1 + "\n" + field2 + "\n" + replyURL;
//        return "TaskProtocol{" +
//                "type='" + type + '\'' +
//                ", message='" + message + '\'' +
//                ", replyURL='" + replyURL + '\'' +
//                '}';
    }
}
