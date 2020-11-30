package local;
// snippet-start:[s3.java2.s3_object_operations.complete]
// snippet-start:[s3.java2.s3_object_operations.import]

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Scanner;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;

public class S3Controller {

    private static final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();

    public void deleteBucket(String bucket) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    public void emptyObjectFromBucket(String bucket, String key){
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3.deleteObject(deleteObjectRequest);
    }



                                // ------------------- Alon 12:00 added functions  ------------------- //
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
            //e.printStackTrace();
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


    public void downloadSummaryFile(String bucket, String key, String outputName){
        File summary = new File(outputName + ".html");
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toFile(summary));
    }
    public String[] getUrls(String bucket, String key) {
        // Get Object

        InputStream inputStream = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toInputStream());
        return input2StringArr(inputStream);
    }

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