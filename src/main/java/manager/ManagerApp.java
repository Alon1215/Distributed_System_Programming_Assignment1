package manager;
import com.google.gson.Gson;
import shared.SQSController;
import shared.TaskProtocol;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager logic, and main class of ManagerApp.jar, which runs in the cloud.
 * Manager Listens to sqs queue fed by Local Apps, and receives their input files.
 * Manager is responsible for creating Workers, send them tasks, and parse their output to response for
 * the local request.
 */
public class ManagerApp {

    /**
     * main function of the manager.
     * Listen to locals2Manager queue,
     * If a new task arrive, handle the "new task" process,
     * If a termination message arrive, wait() until all workers finish their job,
     * terminate them, and them finish it's running (terminate itself).
     * @param args arguments received from LocalApp (specifically, n - number of picture per worker)
     */
    public static void main(String[] args) {
        AtomicBoolean terminateSwitch = new AtomicBoolean(false);

        // 1. Retrieve sqs url (and create sqs client
        TaskHandler taskHandler = new TaskHandler(terminateSwitch);
        SQSController sqsManager = new SQSController();
        String sqsManagerURL = sqsManager.getQueueURLByName("Local2Manager");
        String managerInstanceId = "";

        // 2. Manager listen to his sqs queue
        System.out.println("-> Start Listen to queue: " + sqsManagerURL);
        Gson gson = new Gson();
        boolean isDone = false;
        while (!isDone){
            List<Message> messages = sqsManager.getMessages(sqsManagerURL);
            for( Message msg : messages) {
                if(msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();
                    String replyUrl = msg_parsed.getReplyURL();
                    int workersPerImage = msg_parsed.getNPerWorker();
                    System.out.println("Local2Manager: message received! type: " + msg_parsed.getType());

                    switch (type) {
                        case "new task":

                            taskHandler.handleNewTask(msg_parsed, replyUrl, workersPerImage);
                            break;
                        case "terminate":
                            System.out.println("terminateSwitch -> true");
                            terminateSwitch.set(true);

                            managerInstanceId = msg_parsed.getField1();
                            taskHandler.WaitForListenersTermination();
                            isDone = true;
                            break;
                        default:
                            System.err.println("Manager received bad task from local");
                            break;
                    }
                    sqsManager.deleteSingleMessage(sqsManagerURL, msg);
                }
            }
        }
        sqsManager.deleteQueue(sqsManagerURL);


        // Terminate Manager's EC2 and finish
        TaskHandler.terminateEC2ById(managerInstanceId);
    }

}
