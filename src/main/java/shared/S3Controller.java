package shared;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import com.google.gson.Gson;
import manager.HTMLHandler;
import manager.RequestDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;


/**
 * Methods and logic for S3 operations and S3 adjusted methods for the assignment.
 * Used to abstract the use of S3 operations.
 * Implement method such as:
 * create bucket,
 * put input / output in bucket,
 * empty bucket,
 * delete bucket
 * download summary file
 */
public class S3Controller {

    private static final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();

    /**
     * Delete the bucket from S3
     * @param bucket bucket name to be deleted
     */
    public void deleteBucket(String bucket) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    /**
     * Delete a given object from the bucket.
     * @param bucket bucjet name
     * @param key name of the object
     */
    public void emptyObjectFromBucket(String bucket, String key){
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3.deleteObject(deleteObjectRequest);
    }

    /**
     * Create new Bucket with a unique name.
     * @return the name of the new bucket.
     */
    public String createNewBucket(){
        String bucketName = "bucket" + System.currentTimeMillis();

        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucketName)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .build())
                .build());


        return bucketName;
    }



    /**
     * put input text file of the assignment in the created bucket.
     * @param path indicates file current path
     * @return url address of the uploaded file in s3 storage
     */
    public String[] putInputInBucket(String path,String bucketName, String name){
        String keyName = name + System.currentTimeMillis();

        // convert path to file / byte buffer
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.err.println("putInputInBucket: ERROR Input file: "+ name +" not found");
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(keyName)
                        .build(),
                RequestBody.fromByteBuffer(buffer));
        return new String[]{bucketName, keyName};


    }

    /**
     * Uplaod summary file to S3
     * @param requestDetails data structure contains the result for a given request
     * @param bucketName name of the bucket for the operation
     * @param name key name for the file.
     * @return bucket and key names, generated in the method to be unique.
     */
    public String[] putOutputInBucket(RequestDetails requestDetails, String bucketName, String name){
        String keyName = name + System.currentTimeMillis();

        Gson gson = new Gson();
        String jsoned = gson.toJson(requestDetails);
        // convert path to file / byte buffer
        byte[] bytes = jsoned.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(keyName)
                        .build(),
                RequestBody.fromByteBuffer(buffer));
        return new String[]{bucketName, keyName};
    }


    /**
     * Download summary file from the manager's message.
     * First download the bytes from the given bucket / key names,
     * parse it back to Json, and from Json to the data-structure,
     * generate the html file and returns it.
     * @param bucket bucket name
     * @param key key name
     * @param outputName name of the output, as specified in LocalApp arguments
     * @return html file for the respected request.
     */
    public File downloadSummaryFile(String bucket, String key, String outputName){
        byte[] res = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toBytes()).asByteArray();

        String as = new String(res);
        String jsonRes = new String(res);
        Gson gson = new Gson();

        RequestDetails requestDetails = gson.fromJson(jsonRes, RequestDetails.class);

        return HTMLHandler.generateHtmlFile(requestDetails.getImageOutputs(), outputName);

    }

    /**
     * Used in ManagerApp.
     * Donwload from the given bucket in S3 the file, as Input steam.
     * return the the urls as and array of strings
     * @param bucket bucket name
     * @param key file name
     * @return String array, each cell contains url to be processed.
     */
    public String[] getUrls(String bucket, String key) {
        // Get Object
        InputStream inputStream = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toInputStream());
        return input2StringArr(inputStream);
    }

    /**
     * Convert input string to array of strings.
     * Used to parse the urls sent from the LocalApp.
     * Used in the ManagerApp Logic
     * @param inputStream stream of the new task's body (urls)
     * @return String Array with urls.
     */
    private String[] input2StringArr(InputStream inputStream) {
        //Creating a Scanner object
        Scanner sc = new Scanner(inputStream);

        //Reading line by line from scanner to StringBuffer
        StringBuilder sb = new StringBuilder();
        while(sc.hasNext()){
            sb.append(sc.nextLine());
            sb.append("\n");
        }

        return sb.toString().split("\n");
    }
}