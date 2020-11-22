package Local;
// snippet-start:[s3.java2.s3_object_operations.complete]
// snippet-start:[s3.java2.s3_object_operations.import]

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
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;

public class S3Controller {

    private static S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();
    private String bucketName; // @TODO: Alon 12:00 : added field
    private String bucketKey; // @TODO: Alon 12:00 : added field
    private final Region region = Region.US_EAST_1;  // @TODO: Alon 12:00 : added field

    public static void main(String[] args) throws IOException {
        Region region = Region.US_EAST_1;
        s3 = S3Client.builder().region(region).build();


        String bucket = "bucket" + System.currentTimeMillis();
        String key = "key";

        createBucket(bucket, region);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .build(),
                RequestBody.fromByteBuffer(getRandomByteBuffer(10_000)));


        // Multipart Upload a file
        String multipartKey = "multiPartKey";
        multipartUpload(bucket, multipartKey);

        // List all objects in bucket

        // Use manual pagination
        ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder()
                .bucket(bucket)
                .maxKeys(1)
                .build();

        boolean done = false;
        while (!done) {
            ListObjectsV2Response listObjResponse = s3.listObjectsV2(listObjectsReqManual);
            for (S3Object content : listObjResponse.contents()) {
                System.out.println(content.key());
            }

            if (listObjResponse.nextContinuationToken() == null) {
                done = true;
            }

            listObjectsReqManual = listObjectsReqManual.toBuilder()
                    .continuationToken(listObjResponse.nextContinuationToken())
                    .build();
        }
        // Build the list objects request
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .maxKeys(1)
                .build();

        ListObjectsV2Iterable listRes = s3.listObjectsV2Paginator(listReq);
        // Process response pages
        listRes.stream()
                .flatMap(r -> r.contents().stream())
                .forEach(content -> System.out.println(" Key: " + content.key() + " size = " + content.size()));

        // Helper method to work with paginated collection of items directly
        listRes.contents().stream()
                .forEach(content -> System.out.println(" Key: " + content.key() + " size = " + content.size()));
        // Use simple for loop if stream is not necessary
        for (S3Object content : listRes.contents()) {
            System.out.println(" Key: " + content.key() + " size = " + content.size());
        }

        // Get Object
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(),
                ResponseTransformer.toFile(Paths.get("multiPartKey")));
        // snippet-end:[s3.java2.s3_object_operations.download]

        // Delete Object
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3.deleteObject(deleteObjectRequest);

        // Delete Object
        deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(multipartKey).build();
        s3.deleteObject(deleteObjectRequest);

        deleteBucket(bucket);
    }


    private static void createBucket(String bucket, Region region) {
        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucket)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .locationConstraint(region.id())
                                .build())
                .build());

        System.out.println(bucket);
    }

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
    public String[] putInputInBucket(String path, String key){
        this.bucketKey = key + System.currentTimeMillis();

        // convert path to file / byte buffer
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Input file not found");
            return new String[]{"ERROR"};
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(this.bucketName).key(this.bucketKey)
                        .build(),
                RequestBody.fromByteBuffer(buffer));
        return new String[]{this.bucketName, this.bucketKey};


    }

    public boolean deleteCurrBucket() {
        if (!bucketName.equals("")) {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(this.bucketName).build();
            s3.deleteBucket(deleteBucketRequest);

            this.bucketName = "";
            this.bucketKey = "";
            return true;
        }
        return false;
    }

    public String[] getObject(String bucket, String key) {
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
        }
     //   System.out.println(sb.toString());

        return sb.toString().split("\n");
    }
}