package local;

import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LocalApp {

    public static void main(String[] args) {

        if (args.length < 3){
            System.out.println("To few arguments, program terminate");
            System.exit(1);
        }
        String inputFileName = args[0];
        String outputFileName = args[1];
        int n_input = Integer.parseInt(args[2]);
        boolean isTerminating = (args.length == 4 && args[3].equals("terminate"));

        /*
        String inputFileName = "input1task.txt";
        String outputFileName = "output" + System.currentTimeMillis();
        int n_input = 10; // TODO: delete later
        */


    // 1. Check if manager node is active (if not, initiate)

        ManagerHandler manager = new ManagerHandler(n_input); // create new manager if doesn't exist, else represents current one
        // check which parameters are needed



    // 2. Upload input file to S3
        S3Controller s3 = new S3Controller();
        String bucketName = s3.createNewBucket();
        String[] bucket_key = s3.putInputInBucket(inputFileName,bucketName, "inputFileName");
        if (bucket_key == null){
            // upload failed, exit ungracefully
            System.err.println("upload failed, exit ungracefully");
            System.exit(-1);
        }
        System.out.println(Arrays.toString(bucket_key)); // TODO: delete, test only

    // 3. Sends a message to an SQS queue, stating the location of the file on S3

    // 3.1 create SQS for local2manager & manager2local
        SQSController sqsLocal = new SQSController();
        String sqsLocalURL = sqsLocal.createQueue("local" + new Date().getTime());
        System.out.println("-> sqsLocal created");

    // 3.2 Sends a message to an SQS queue
        Gson gson = new Gson();
        sqsLocal.sendMessage(manager.getQueueURL(), gson.toJson(new TaskProtocol("new task",bucket_key[0], bucket_key[1], sqsLocalURL)));

    // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.
        sqsLocal.getMessages(sqsLocalURL);

        boolean isDone = false;
        while(!isDone){
            List<Message> messages = sqsLocal.getMessages(sqsLocalURL);
            for( Message msg : messages) {
                if (msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();

    // 4.1 Downloads the summary file from S3, and create an html file representing the results.
                    if (type.equals("done task")) {

                        // Download summary file, clean & delete bucket
                        processTaskOutput(outputFileName, s3, bucketName, bucket_key[1], msg_parsed);

    // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.
                        if (isTerminating){
                            sqsLocal.sendMessage(manager.getQueueURL(), gson.toJson(new TaskProtocol("terminate",manager.getInstanceId(), "", "")));
                        }
                        isDone = true;

                    } else {
                        System.err.println("ERROR Occurred, mission didn't accomplished");
                    }
                    sqsLocal.deleteSingleMessage(sqsLocalURL, msg);
                }
            }
        }
        sqsLocal.deleteQueue(sqsLocalURL);

    }

    private static void processTaskOutput(String outputFileName, S3Controller s3, String bucketName, String inputKey1, TaskProtocol msg_parsed) {
        String outputKey = msg_parsed.getField2();
        System.out.println("Summary file received");
        s3.downloadSummaryFile(bucketName, msg_parsed.getField2(), outputFileName);
        s3.emptyObjectFromBucket(bucketName, inputKey1);
        s3.emptyObjectFromBucket(bucketName, outputKey);
        s3.deleteBucket(bucketName);
    }
}
