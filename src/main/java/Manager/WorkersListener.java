package Manager;

import Local.S3Controller;
import Local.SQSController;
import Local.TaskProtocol;
import com.sun.istack.internal.NotNull;
import javafx.util.Pair;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkersListener implements Runnable {
    SQSController sqsController;
    S3Controller s3Controller = new S3Controller();
    String sqsUrl;
    ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal;
    ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages;
    String bucket;
    public WorkersListener(String sqsUrl, ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal, ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages, String bucket) {
        this.bucket = bucket;
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
                                
                                doneTask(replyUrl, bucket);
                                amountOfMessagesPerLocal.remove(replyUrl);
                                //TaskProtocol done_task = new TaskProtocol()
                            }
                            //TaskProtocol task = new TaskProtocol("done task", )
                            break;
                        case "termination":
                            //TODO: DELETE LATER
                            System.out.println("Terminating thread W2M Listener");
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

    private void doneTask(String replyUrl, String bucket) {
        Vector<Pair<String, String>> imageData = identifiedMessages.get(replyUrl);
        File f = HTMLHandler.parseListOfUrlAndTextToHTML(imageData, replyUrl);
        String[] bucket_key = s3Controller.putInputInBucket(f != null ? f.getAbsolutePath() : null, bucket, "summary" + System.currentTimeMillis());
        sqsController.sendMessage(replyUrl, new TaskProtocol("done task", bucket_key[0], bucket_key[1], "").toString());
    }
}
