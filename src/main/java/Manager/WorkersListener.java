package Manager;

import Local.SQSController;
import javafx.util.Pair;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkersListener implements Runnable {
    SQSController sqsController;
    String sqsUrl;
    ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal;
    ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages;

    public WorkersListener(String sqsUrl, ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal, ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages) {
        this.sqsController = new SQSController();
        this.sqsUrl = sqsUrl;
        this.amountOfMessagesPerLocal = amountOfMessagesPerLocal;
        this.identifiedMessages = identifiedMessages;
    }

    @Override
    public void run() {

        while (amountOfMessagesPerLocal.size() > 0) {
            List<Message> messages = sqsController.getMessages(sqsUrl);
            for (Message msg : messages) {
                String[] msg_s;
                if (msg != null) {
                    msg_s = msg.toString().split("\n");
                    String type = msg_s[0];
                    String replyUrl = msg_s[3];
                    switch(type){
                        case "done OCR task":
                            Pair<String, String> img_identified_text = new Pair<String, String>(msg_s[1], msg_s[2]);
                            identifiedMessages.get(replyUrl).add(img_identified_text);
                            amountOfMessagesPerLocal.replace(replyUrl, amountOfMessagesPerLocal.get(replyUrl) - 1);
                            if (amountOfMessagesPerLocal.get(replyUrl) <= 0) {
                                // TODO: make a html file and upload it to s3
                                
                                doneTask();
                                amountOfMessagesPerLocal.remove(replyUrl);
                                //TaskProtocol done_task = new TaskProtocol()
                            }
                            //TaskProtocol task = new TaskProtocol("done task", )
                            break;
                        case "termination":
                            //DELETE LATER
                            System.exit(1);
                            break;
                        default:
                            // not suppose to happen
                            break;
                    }
                }
            }
        }
    }

    private void doneTask() {
    }
}
