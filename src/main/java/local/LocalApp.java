package local;

import com.google.gson.Gson;
import shared.S3Controller;
import shared.SQSController;
import shared.TaskProtocol;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Date;
import java.util.List;

/**
 * Local application logic, and main class of LocalApp.jar, which is the local application of our assignment.
 * to run the local application: java -jar LocalApp <inputFileName> <outputFileName> <n> ?<terminate>
 * such as file names are input/ file names, n is number of pictures per worker, and add termination if local responsible for manager termination.
 */
public class LocalApp {

    /**
     * Main function and logic of the LocalApp.
     * LocalApp upload input file to S3, and send manager a new task mission (with the specified n).
     * Afterwards listen to it's queue for result.
     * After receiving the output summary file, if needed, send 'terminate' message,
     * process summary file to HTML and finish run.
     * @param args =  <inputFileName> <outputFileName> <n> ?<terminate>
     */
    public static void main(String[] args) {

        if (args.length < 3){
            System.out.println("To few arguments, program terminate");
            System.exit(-1);
        }
        String inputFileName = args[0];
        String outputFileName = args[1];
        int n_input = Integer.parseInt(args[2]);
        boolean isTerminating = (args.length == 4 && args[3].equals("terminate"));
        System.out.println("Start LocalApp -> n = " + n_input+ ", isTerminate = " + isTerminating);
        // 1. Check if manager node is active (if not, initiate)

        ManagerHandler manager = new ManagerHandler(); // create new manager if doesn't exist, else represents current one
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

        // 3. Sends a message to an SQS queue, stating the location of the file on S3

        // 3.1 create SQS for local2manager & manager2local
        SQSController sqsLocal = new SQSController();
        String sqsLocalURL = sqsLocal.createQueue("local" + new Date().getTime());
        System.out.println("-> sqsLocal created");

        // 3.2 Sends a message to an SQS queue
        Gson gson = new Gson();
        sqsLocal.sendMessage(manager.getQueueURL(), gson.toJson(new TaskProtocol("new task",bucket_key[0], bucket_key[1], sqsLocalURL, n_input)));

        // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.
        sqsLocal.getMessages(sqsLocalURL);
        System.out.println("-> Start Listen to queue: " + sqsLocalURL);

        boolean isDone = false;
        while(!isDone){
            List<Message> messages = sqsLocal.getMessages(sqsLocalURL);
            for( Message msg : messages) {
                if (msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();

                    System.out.println("message received! type: " + msg_parsed.getType());
                    // 4.1 Downloads the summary file from S3, and create an html file representing the results.
                    if (type.equals("done task")) {

                        // Download summary file, clean & delete bucket
                        processTaskOutput(outputFileName, s3, bucketName, bucket_key[1], msg_parsed);

                        // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.
                        if (isTerminating){
                            System.out.println("Send terminate to manager");
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

    /**
     * Processes output received from the manager.
     * @param outputFileName name of the output name (given as an argument).
     * @param s3 S3 client
     * @param bucketName bucket name where summary file was uploaded.
     * @param inputKey1 key name of summary file.
     * @param msg_parsed the message from the manager, parsed to the message protocol
     */
    private static void processTaskOutput(String outputFileName, S3Controller s3, String bucketName, String inputKey1, TaskProtocol msg_parsed) {
        String outputKey = msg_parsed.getField2();
        System.out.println("Summary file received");
        s3.downloadSummaryFile(bucketName, msg_parsed.getField2(), outputFileName);
        s3.emptyObjectFromBucket(bucketName, inputKey1);
        s3.emptyObjectFromBucket(bucketName, outputKey);
        s3.deleteBucket(bucketName);
    }
}
