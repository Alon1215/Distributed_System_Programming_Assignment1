package Manager;
import Local.*;

public class Manager {
    public static void main(String[] args) {
//        int n = args[0]; // TODO:
        int n = 10; // temp
    // 1. Retrieve sqs url (and create sqs client

    // 2. Manager listen to his sqs queue

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
