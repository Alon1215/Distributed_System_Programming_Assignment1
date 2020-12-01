package manager;

import local.S3Controller;
import local.SQSController;
import local.TaskProtocol;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersListener implements Runnable {
    private final SQSController sqsController;
    private final S3Controller s3Controller = new S3Controller();
    private final String W2M_SQSQueURL;
    private final ConcurrentHashMap<String, RequestDetails> localsDetails;
   // private final ConcurrentHashMap<String, Vector<ImageOutput>> identifiedMessages;
    private final Gson gson = new Gson();

    AtomicInteger activeWorkersNumber;

    public WorkersListener(AtomicInteger activeWorkersNumber, String W2M_SQSQueURL, ConcurrentHashMap<String, RequestDetails> localsDetails) {
        this.activeWorkersNumber = activeWorkersNumber;
        this.sqsController = new SQSController();
        this.W2M_SQSQueURL = W2M_SQSQueURL;
        this.localsDetails = localsDetails;
        //this.identifiedMessages = identifiedMessages;
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
                            //Pair<String, String> img_identified_text = new Pair<String, String>(msg_parsed.getField1(), msg_parsed.getField2());
                          //  identifiedMessages.get(replyUrl).add(new ImageOutput(msg_parsed.getField1(), msg_parsed.getField2()));
                        //    identifiedMessages.get(replyUrl).add(img_identified_text);
                            //localsDetails.replace(replyUrl, localsDetails.get(replyUrl) - 1);
                            boolean isDoneTask = localsDetails.get(replyUrl).addImageOutputAndCheckIfDone(new ImageOutput(msg_parsed.getField1(), msg_parsed.getField2()));
                            if (isDoneTask) {
                                doneTask(replyUrl, localsDetails.get(replyUrl).getBucket());
                                localsDetails.remove(replyUrl);
                                if (localsDetails.size() == 0){
                                    localsDetails.notifyAll();
                                }
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
        Vector<ImageOutput> imageData = localsDetails.get(replyUrl).getImageOutputs();
        File f = HTMLHandler.makeHTMLSummaryFile(imageData);
        String[] bucket_key = s3Controller.putInputInBucket(f != null ? f.getPath() : null, bucket, "summary");

        Gson gson = new Gson();
        sqsController.sendMessage(replyUrl, gson.toJson(new TaskProtocol("done task", bucket_key[0], bucket_key[1], "")));
    }
}
