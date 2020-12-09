package manager;

import shared.S3Controller;
import shared.SQSController;
import shared.TaskProtocol;
import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Runnable task, to be used by the executorsPool who listen to the workers.
 * Run as a main loop, which waits for all workers to die.
 * Responsible to handle each output from workers.
 */
public class WorkersListener implements Runnable {
    private final SQSController sqsController;
    private final S3Controller s3Controller = new S3Controller();
    private final String W2M_SQSQueURL;
    private final ConcurrentHashMap<String, RequestDetails> localsDetails;
    private final Gson gson = new Gson();
    private final AtomicInteger activeWorkersNumber;
    private final AtomicBoolean terminateSwitch;

    public WorkersListener(AtomicInteger activeWorkersNumber, AtomicBoolean terminateSwitch, String W2M_SQSQueURL, ConcurrentHashMap<String, RequestDetails> localsDetails) {
        this.activeWorkersNumber = activeWorkersNumber;
        this.terminateSwitch = terminateSwitch;
        this.sqsController = new SQSController();
        this.W2M_SQSQueURL = W2M_SQSQueURL;
        this.localsDetails = localsDetails;
    }


    /**
     * The task to be executed by the Worker-listeners
     * Run as a main loop, which waits for all workers to die.
     * Responsible to handle each output from workers.
     */
    @Override
    public void run() {
        System.out.println("-> Start Listen to queue: " + W2M_SQSQueURL);

        boolean isDone = false;
        while (!isDone){
            List<Message> messages = sqsController.getMessages(W2M_SQSQueURL);
            for (Message msg : messages) {
                if (msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();
                    String replyUrl = msg_parsed.getReplyURL();

                    System.out.println("W2M_SQSQueURL: message received! type: " + msg_parsed.getType());

                    switch(type){
                        case "done OCR task":
                            boolean isDoneTask = localsDetails.get(replyUrl).addImageOutputAndCheckIfDone(new ImageOutput(msg_parsed.getField1(), msg_parsed.getField2()));

                            if (isDoneTask) {
                                doneTask(replyUrl, localsDetails.get(replyUrl).getBucket());

                                localsDetails.remove(replyUrl);

                            }
                            break;
                        case "worker died":

                            if (activeWorkersNumber.decrementAndGet() == 0){
                                isDone = true; // finish loop
                            }

                            break;
                        default:
                            // not suppose to happen
                            System.err.println("Bad message from worker");
                            break;
                    }
                    sqsController.deleteSingleMessage(W2M_SQSQueURL, msg);
                }
            }
            if (localsDetails.size()==0 && terminateSwitch.compareAndSet(true, false)){
                System.out.println("terminateSwitch -> false (switch back)");
                TaskHandler.broadcastTerminate2Workers();
            }
        }

        TaskHandler.terminateWorkersAndQueues();
        System.out.println("Worker listener finished run");
    }

    /**
     * If all urls for a given request was processed,
     * Upload the summary file to s3 (the data-strucutre wich contains the results,
     * and send the response back to the Local App.
     * @param replyUrl Local's sqs queue url
     * @param bucket name of the locals bucket, where to put the summary file.
     */
    private void doneTask(String replyUrl, String bucket) {


        RequestDetails requestDetails = localsDetails.get(replyUrl);

        String[] bucket_key = s3Controller.putOutputInBucket(requestDetails, bucket, "summary");
        Gson gson = new Gson();
        sqsController.sendMessage(replyUrl, gson.toJson(new TaskProtocol("done task", bucket_key[0], bucket_key[1], "")));

        System.out.println("Done task: " + bucket + " Pending tasks: " + (localsDetails.size() - 1));
    }
}
