package Local;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import software.amazon.awssdk.services.s3.S3Client;

public class local {


    public static void main(String[] args) {

        if (args.length < 3){
            System.out.println("To few arguments, program terminate");
            System.exit(1);
        }
//        Region region;
        String inputFile = args[0];
        String outputFile = args[1];
        int n = Integer.parseInt(args[2]);

    // 1. Check if manager node is active (if not, initiate)
        EC2 newEC2 = new EC2(); // create new manager if doesn't exist, else represents current one

        // check which parameters are needed



    // 2. Upload input file to S3
        S3Controller s3 = new S3Controller();
        s3.createNewBucket();
        String fileS3Address = s3.putInputInBucket(args[0]); // TODO: Alon 13:00: should it return the address?


    // 3. Sends a message to an SQS queue, stating the location of the file on S3

    // 4. Checks an SQS queue for a message indicating the process is done and the response (the summary file) is available on S3.

            // 4.1 Downloads the summary file from S3, and create an html file representing the results.

            // 4.2 Sends a termination message to the Manager if it was supplied as one of its input arguments.


    }
}
