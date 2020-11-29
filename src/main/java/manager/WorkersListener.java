package manager;

import local.S3Controller;
import local.SQSController;
import local.TaskProtocol;
import com.google.gson.Gson;
import javafx.util.Pair;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersListener implements Runnable {
    SQSController sqsController;
    S3Controller s3Controller = new S3Controller();
    String W2M_SQSQueURL;
    ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal;
    ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages;
    String bucket;
    Gson gson = new Gson();

    AtomicInteger activeWorkersNumber;

    public WorkersListener(AtomicInteger activeWorkersNumber, String W2M_SQSQueURL, ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal, ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages, String bucket) {
        this.activeWorkersNumber = activeWorkersNumber;
        this.bucket = bucket;
        this.sqsController = new SQSController();
        this.W2M_SQSQueURL = W2M_SQSQueURL;
        this.amountOfMessagesPerLocal = amountOfMessagesPerLocal;
        this.identifiedMessages = identifiedMessages;
    }

    @Override
    public void run() {
        while (activeWorkersNumber.get() > 0) {
            List<Message> messages = sqsController.getMessages(W2M_SQSQueURL);
            for (Message msg : messages) {
                if (msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();
                    String replyUrl = msg_parsed.getReplyURL();

                    switch(type){
                        case "done OCR task":
                            Pair<String, String> img_identified_text = new Pair<String, String>(msg_parsed.getField1(), msg_parsed.getField2());

                            identifiedMessages.get(replyUrl).add(img_identified_text);
                            amountOfMessagesPerLocal.replace(replyUrl, amountOfMessagesPerLocal.get(replyUrl) - 1);
                            if (amountOfMessagesPerLocal.get(replyUrl) <= 0) {
                                // TODO: make a html file and upload it to s3
                                
                                doneTask(replyUrl, bucket);
                                amountOfMessagesPerLocal.remove(replyUrl);
                            }
                            break;
                        case "worker died":
                            System.out.println("Terminating thread W2M Listener");
                            activeWorkersNumber.decrementAndGet();
                            break;
                        default:
                            // not suppose to happen
                            break;
                    }
                    sqsController.deleteSingleMessage(W2M_SQSQueURL, msg);
                }
            }
        }
    }

    private void doneTask(String replyUrl, String bucket) {
        Vector<Pair<String, String>> imageData = identifiedMessages.get(replyUrl);
        File f = HTMLHandler.makeHTMLSummaryFile(imageData);
        String[] bucket_key = s3Controller.putInputInBucket(f != null ? f.getPath() : null, bucket, "summary");

        Gson gson = new Gson();
        sqsController.sendMessage(replyUrl, gson.toJson(new TaskProtocol("done task", bucket_key[0], bucket_key[1], "")));
    }
}
