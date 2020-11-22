package Manager;
import Local.*;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class Manager {
    public static void main(String[] args) {
//        int n = args[0]; // TODO:
        int n = 10; // temp
    // 1. Retrieve sqs url (and create sqs client
        S3Controller s3 = new S3Controller();
        SQSController sqsManager = new SQSController();
        String sqsManagerURL = sqsManager.getQueueURLByName("Manager");

    // 2. Manager listen to his sqs queue
        while (true) {
            List<Message> messages = sqsManager.getMessages(sqsManagerURL);
            for( Message msg : messages) {
                String[] msg_s;
                if(msg != null) {
                    msg_s = msg.toString().split("\n");
                    String type = msg_s[0];

                    switch (type) {
                        case "new task":
                            // retrieve input file from S3
//                        String fileAddress = msg_s[1];
//                            String[] bucket_key = msg_s[1].split(" ");
                            String[] msgArr = s3.getObject(msg_s[1], msg_s[2]);
                            // TODO: send to workers
                            break;
                        case "done OCR task":


                            break;
                        case "termination":

                            break;
                        default:
                            // not suppose to happen
                            break;
                    }
                }
                // 2.1 If the message is that of a new task it:

                    // 2.1.1 The manager should create a worker for every n messages, if there are no running workers.
                    // 2.1.2 If there are k active workers, and the new job requires m workers, then the manager should create m-k new workers, if possible.
                    // 2.1.3 Note that while the manager creates a node for every n messages, it does not delegate messages to specific nodes. All of the worker nodes take their messages from the same SQS queue; so it might be the case that with 2n messages, hence two worker nodes, one node processed n+(n/2) messages, while the other processed only n/2.


                // 2.2 If the message is a termination message, then the manager:

                    // 2.2.1 Does not accept any more input files from local applications.
                    // 2.2.2 Waits for all the workers to finish their job, and then terminates them.
                    // 2.2.3 Creates response messages for the jobs, if needed.
                    // 2.2.4 Terminates
            }

        }
    }

    private static void handleNewTask(String[] msg_s) {

    }
}
