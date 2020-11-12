package Local;

public class local {

    public static void main(String[] args) {
        final String LOCAL_NAME = "local" + System.currentTimeMillis();
        /*
        if (args.length < 3){
            System.out.println("To few arguments, program terminate");
            System.exit(1);
        }
        Region region;
        String inputFile = args[0];
        String outputFile = args[1];
        int n = Integer.parseInt(args[2]);
        */
        String inputFile = "text.images.txt";

    // 1. Check if manager node is active (if not, initiate)

        //EC2 newEC2 = new EC2(); // create new manager if doesn't exist, else represents current one

        // check which parameters are needed



    // 2. Upload input file to S3
        S3Controller s3 = new S3Controller();
        s3.createNewBucket();
        String fileS3Address = s3.putInputInBucket(inputFile, "inputFile"); // TODO: Alon 13:00: should it return the address?
        System.out.println(fileS3Address);

    // 3. Sends a message to an SQS queue, stating the location of the file on S3
        // 3.1 create SQS for local2manager & manager2local
        SQSController sqsLocal = new SQSController(LOCAL_NAME);
        String sqsM2L = new SQSController("manager2local");
    // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.

            // 4.1 Downloads the summary file from S3, and create an html file representing the results.

            // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.


    }
}
