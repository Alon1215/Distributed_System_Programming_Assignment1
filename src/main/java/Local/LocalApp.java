package Local;

import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LocalApp {

    public static void main(String[] args) {
        /*
        if (args.length < 3){
            System.out.println("To few arguments, program terminate");
            System.exit(1);
        }
        Region region;
        String inputFileName = args[0];
        String outputFile = args[1];
        int n = Integer.parseInt(args[2]);
        */
        String inputFileName = "text.images.txt";
        String outputFileName = "output";
        int n_input = 10; // TODO: implement n as input throughout application
        boolean isTerminating = (args.length == 4 && args[3].equals("terminate"));
    // 1. Check if manager node is active (if not, initiate)

        //EC2 newEC2 = new EC2(); // create new manager if doesn't exist, else represents current one
        ManagerHandler manager = new ManagerHandler(); // create new manager if doesn't exist, else represents current one
        // check which parameters are needed



    // 2. Upload input file to S3
        S3Controller s3 = new S3Controller();
        String bucketName = s3.createNewBucket();
        String[] bucket_key = s3.putInputInBucket(inputFileName,bucketName, "inputFileName");
        System.out.println(Arrays.toString(bucket_key)); // TODO: delete, test only

    // 3. Sends a message to an SQS queue, stating the location of the file on S3

        // 3.1 create SQS for local2manager & manager2local
        SQSController sqsLocal = new SQSController();
        String sqsLocalURL = sqsLocal.createQueue("local" + new Date().getTime());
        System.out.println("-> sqsLocal created");
        // 3.2 Sends a message to an SQS queue
        sqsLocal.sendMessage(manager.getQueueURL(), new TaskProtocol("new task",bucket_key[0], bucket_key[1], sqsLocalURL).toString());

        if (isTerminating){
            sqsLocal.sendMessage(manager.getQueueURL(), new TaskProtocol("terminate","", "", "").toString());
        }

        // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.
        sqsLocal.getMessages(sqsLocalURL);

        boolean isDone = false;
        while(!isDone){
            List<Message> messages = sqsLocal.getMessages(sqsLocalURL);
            for( Message msg : messages) {
                String[] msg_s;
                if (msg != null) {
                    msg_s = msg.toString().split("\n");
                    String type = msg_s[0];
                    if (type.equals("done task")) {
                        System.out.println("Summary file received");
                        s3.downloadSummaryFile(msg_s[1], msg_s[2], outputFileName);
                        isDone = true;
                    } else {
                        System.out.println("ERROR Occurred, mission didn't accomplished");
                    }

                }
            }
        }
            // 4.1 Downloads the summary file from S3, and create an html file representing the results.

            // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.


    }
}
