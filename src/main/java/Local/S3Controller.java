package Local;
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
    private String bucketName; // @TODO: Alon 12:00 : added field
    private String keyName; // @TODO: Alon 12:00 : added field
    private final Region region = Region.US_EAST_1;  // @TODO: Alon 12:00 : added field

//    private static void createBucket(String bucket, Region region) {
//        s3.createBucket(CreateBucketRequest
//                .builder()
//                .bucket(bucket)
//                .createBucketConfiguration(
//                        CreateBucketConfiguration.builder()
//                                .locationConstraint(region.id())
//                                .build())
//                .build());
//
//        System.out.println(bucket);
//    }

    private static void deleteBucket(String bucket) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    /**
     * Uploading an object to S3 in parts
     */
    private static void multipartUpload(String bucketName, String key) throws IOException {

        int mb = 1024 * 1024;
        // First create a multipart upload and get upload id 
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName).key(key)
                .build();
        CreateMultipartUploadResponse response = s3.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = response.uploadId();
        System.out.println(uploadId);

        // Upload all the different parts of the object
        UploadPartRequest uploadPartRequest1 = UploadPartRequest.builder().bucket(bucketName).key(key)
                .uploadId(uploadId)
                .partNumber(1).build();
        String etag1 = s3.uploadPart(uploadPartRequest1, RequestBody.fromByteBuffer(getRandomByteBuffer(5 * mb))).eTag();
        CompletedPart part1 = CompletedPart.builder().partNumber(1).eTag(etag1).build();

        UploadPartRequest uploadPartRequest2 = UploadPartRequest.builder().bucket(bucketName).key(key)
                .uploadId(uploadId)
                .partNumber(2).build();
        String etag2 = s3.uploadPart(uploadPartRequest2, RequestBody.fromByteBuffer(getRandomByteBuffer(3 * mb))).eTag();
        CompletedPart part2 = CompletedPart.builder().partNumber(2).eTag(etag2).build();


        // Finally call completeMultipartUpload operation to tell S3 to merge all uploaded
        // parts and finish the multipart operation.
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(part1, part2).build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                CompleteMultipartUploadRequest.builder().bucket(bucketName).key(key).uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload).build();
        s3.completeMultipartUpload(completeMultipartUploadRequest);
    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }



                                // ------------------- Alon 12:00 added functions  ------------------- //
    public String createNewBucket(){
        this.bucketName = "bucket" + System.currentTimeMillis();

        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucketName)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .build())
                .build());


        return this.bucketName;
    }



    /**
     * put input text file of the assignment in the created bucket.
     * @param path indicates file current path
     * @return url address of the uploaded file in s3 storage
     */
    public String[] putInputInBucket(String path,String bucketName, String name){
        this.keyName = name + System.currentTimeMillis();

        // convert path to file / byte buffer
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("putInputInBucket: ERROR Input file not found");
            return new String[]{"ERROR"};
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(keyName)
                        .build(),
                RequestBody.fromByteBuffer(buffer));
        return new String[]{bucketName, keyName};


    }

    public boolean deleteCurrBucket() {
        if (!bucketName.equals("")) {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(this.bucketName).build();
            s3.deleteBucket(deleteBucketRequest);

            this.bucketName = "";
            this.keyName = "";
            return true;
        }
        return false;
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
     //   System.out.println(sb.toString());

        return sb.toString().split("\n");
    }
}