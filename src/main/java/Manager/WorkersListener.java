package Manager;

import Local.S3Controller;
import Local.SQSController;
import Local.TaskProtocol;
import com.google.gson.Gson;
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
        Gson gson = new Gson();
        while (amountOfMessagesPerLocal.size() > 0) {
            List<Message> messages = sqsController.getMessages(sqsUrl);
            for (Message msg : messages) {
//                String[] msg_s;
                if (msg != null) {

                    // TODO: ALON 24.11 23:00 : changed TaskProtocol.toString() to json
//                    msg_s = msg.body().split("\n");
//                    String type = msg_s[0];
//                    String replyUrl = msg_s[3];
                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();
                    String replyUrl = msg_parsed.getReplyURL();

                    switch(type){
                        case "done OCR task":
//                            Pair<String, String> img_identified_text = new Pair<String, String>(msg_s[1], msg_s[2]); // TODO: ALON 24.11 23:00
                            Pair<String, String> img_identified_text = new Pair<String, String>(msg_parsed.getField1(), msg_parsed.getField2());

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
                        case "terminated":
                            //TODO: DELETE LATER (change to handle workers deaths)
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
        File f = HTMLHandler.makeHTMLSummaryFile(imageData);
        String[] bucket_key = s3Controller.putInputInBucket(f != null ? f.getPath() : null, bucket, "summary");

        // TODO: ALON 24.11 23:00
//        sqsController.sendMessage(replyUrl, new TaskProtocol("done task", bucket_key[0], bucket_key[1], "").toString());
        Gson gson = new Gson();
        sqsController.sendMessage(replyUrl, gson.toJson(new TaskProtocol("done task", bucket_key[0], bucket_key[1], "")));
    }
}
