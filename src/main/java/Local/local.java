package Local;

import software.amazon.awssdk.services.s3.S3Client;

public class local {



    public static void main(String[] args) {

    // 1. Check if manager node is active (if not, initiate)
        CreateInstance newEC2 = new CreateInstance();
        // create new if doesn't exist, else return current one
        // check which parameters are needed



        // 2. Upload input file to S3

    // 3. Sends a message to an SQS queue, stating the location of the file on S3

    // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.

            // 4.1 Downloads the summary file from S3, and create an html file representing the results.

            // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.


    }
}
