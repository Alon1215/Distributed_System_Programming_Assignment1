package shared;

/**
 * Task / message protocol of the communication in our program.
 * contain:
 * message  header
 * 2 fields of information,
 * and reply url.
 *
 * Mainly will be parsed to/ from Json,
 * and give us unified communication throughout the program.
 */
public class TaskProtocol {
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

    public String getType() {
        return type;
    }

    public String getField1() {
        return field1;
    }

    public String getField2() {
        return field2;
    }

    public String getReplyURL() {
        return replyURL;
    }

    @Override
    public String toString() {
        return type + "\n" + field1 + "\n" + field2 + "\n" + replyURL;
    }
}
