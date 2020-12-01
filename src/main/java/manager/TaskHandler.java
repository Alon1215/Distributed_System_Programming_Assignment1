package manager;

import shared.S3Controller;
import shared.SQSController;
import shared.TaskProtocol;
import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskHandler {
    private final ConcurrentHashMap<String, RequestDetails> localsDetails = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final String amiId = "ami-0b8ae442d9a63a4f8";
    private final AtomicInteger amountOfActiveWorkers = new AtomicInteger(0);
    private final String M2W_queURL;
    private final String W2M_queURL;
    private final int EXECUTORS_NUMBER = 3;
    private final ExecutorService workersListenerPool = Executors.newFixedThreadPool(EXECUTORS_NUMBER);
    private final Ec2Client ec2;
    private final ArrayList<String> workersInstances;
    private final S3Controller s3 = new S3Controller();
    private final SQSController sqsController = new SQSController();
    private final int imagesPerWorker;

    public TaskHandler(int n){
        this.imagesPerWorker = n;
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        M2W_queURL = sqsController.createQueue("ManagerToWorkers" + new Date().getTime());
        W2M_queURL = sqsController.createQueue("WorkersToManager" + new Date().getTime());

        workersInstances = new ArrayList<>();
    }

    public void handleNewTask(TaskProtocol msg_parsed, String replyUrl){
        String bucket = msg_parsed.getField1();
        String key = msg_parsed.getField2();
        String[] urls = s3.getUrls(bucket, key);


        double requiredWorkers = (double) urls.length/imagesPerWorker;
        if(requiredWorkers > amountOfActiveWorkers.get()){
            for(int i = 0; i < Math.ceil(requiredWorkers); i++){
                createWorker();
            }
            System.out.println("-> WorkerHandler: need to create new" + Math.round(requiredWorkers) + " workers");
            amountOfActiveWorkers.set((int) Math.ceil(requiredWorkers));
        }
        localsDetails.put(replyUrl, new RequestDetails(bucket, urls.length));

        for(String imageUrl: urls){

            sqsController.sendMessage(M2W_queURL, gson.toJson(new TaskProtocol("new image task", imageUrl, "", replyUrl)));

        }
        WorkersListener listener = new WorkersListener(amountOfActiveWorkers,W2M_queURL, localsDetails);
        workersListenerPool.execute(listener);
    }


    public void createWorker() {
        final String USAGE =
                "#!/bin/bash\n" +
                "wget https://alontomdsp211.s3.amazonaws.com/WorkerApp.jar\n" +
                "java -jar WorkerApp.jar " + W2M_queURL + " " + M2W_queURL + "\n";
        IamInstanceProfileSpecification role = IamInstanceProfileSpecification.builder().arn("arn:aws:iam::119201439262:instance-profile/ManagerDSP211AT").build();

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .iamInstanceProfile(role)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .userData(Base64.getEncoder().encodeToString(USAGE.getBytes())).build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

        String instanceId = response.instances().get(0).instanceId();
        workersInstances.add(instanceId);
        amountOfActiveWorkers.incrementAndGet();
        Tag tag = Tag.builder()
                .key("Name")
                .value("worker" + System.currentTimeMillis())
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 instance %s based on AMI %s",
                    instanceId, amiId);
        } catch (Ec2Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("Done!");

    }

    public void handleTermination() {

        while (localsDetails.size() > 0){
            try {
                localsDetails.wait();
            } catch (InterruptedException e) {
                System.err.println("InterruptedException, wait()");
            }
        }


        //send termination to all workers
        for(int i = 0; i < amountOfActiveWorkers.get(); i++){
            sqsController.sendMessage(M2W_queURL, gson.toJson(new TaskProtocol("terminate worker", "", "", "")));
        }

        // wait to all workers to finish their program
        workersListenerPool.shutdown();
        try {
            workersListenerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.err.println(e.toString());
        }

        // delete communication (sqs) between manager and workers
        sqsController.deleteQueue(M2W_queURL);
        sqsController.deleteQueue(W2M_queURL);

        // terminate Workers' EC2
        for ( String instanceId : workersInstances){
            terminateEC2ById(instanceId);
        }

    }

    public void terminateEC2ById(String instanceId) {

        try {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId).build();
            ec2.terminateInstances(request);
            System.out.println("MANAGER: Terminated Worker: " + instanceId);

        } catch (Exception e){
            System.err.println("Failed to Terminate Worker: " + instanceId);
        }
    }

}
