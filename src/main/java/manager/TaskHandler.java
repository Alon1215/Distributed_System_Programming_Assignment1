package manager;

import local.S3Controller;
import local.SQSController;
import local.TaskProtocol;
import com.google.gson.Gson;
import javafx.util.Pair;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskHandler {
    private ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal = new ConcurrentHashMap<String, Integer>();
    private ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages = new ConcurrentHashMap<String, Vector<Pair<String, String>>>();
    private final Gson gson = new Gson();
    private final String amiId = "AMI here";
    private final AtomicInteger amountOfActiveWorkers = new AtomicInteger(0);
    private final String M2W_queURL;
    private final String W2M_queURL;
    private final Ec2Client ec2;
    ExecutorService workersListenerPool = Executors.newFixedThreadPool(2);
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

        workersInstances = new ArrayList<String>();
    }

    public void handleNewTask(TaskProtocol msg_parsed, String replyUrl){
        //TODO: Debug only:
        System.out.println("-> Workers2Manager: " + W2M_queURL);

        String bucket = msg_parsed.getField1();
        String key = msg_parsed.getField2();
        String[] urls = s3.getUrls(bucket, key);



        //TODO: Create new workers if needed
        double requiredWorkers = (double) urls.length/imagesPerWorker;
        if(requiredWorkers > amountOfActiveWorkers.get()){
//            for(int i = 0; i < Math.ceil(requiredWorkers); i++){
//                createWorker();
//            }
            System.out.println("-> WorkerHandler: need to create new" + Math.round(requiredWorkers) + " workers");
            System.out.println("DEBUG: " + W2M_queURL + " " + M2W_queURL);

            amountOfActiveWorkers.set((int) Math.ceil(requiredWorkers));
        }
        amountOfMessagesPerLocal.put(replyUrl, 0);
        identifiedMessages.put(replyUrl, new Vector<Pair<String, String>>());
        amountOfMessagesPerLocal.replace(replyUrl, amountOfMessagesPerLocal.get(replyUrl) + urls.length);


        for(String imageUrl: urls){

            sqsController.sendMessage(M2W_queURL, gson.toJson(new TaskProtocol("new image task", imageUrl, "", replyUrl)));

        }
        WorkersListener listener = new WorkersListener(amountOfActiveWorkers,W2M_queURL, amountOfMessagesPerLocal, identifiedMessages, bucket);
        workersListenerPool.execute(listener);
    }


    public void createWorker() {
        final String USAGE =
                "Worker to manager QUE \n" + W2M_queURL +
                        "Both values can be obtained from the AWS Console\n" +
                        "Ex: CreateInstance <instance-name> <ami-image-id>\n";

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
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
            System.exit(1);
        }
        System.out.println("Done!");

    }

    public void handleTermination() {

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
            TerminateInstancesResponse response = ec2.terminateInstances(request);
            System.out.println("MANAGER: Terminated Worker: " + instanceId);

        } catch (Exception e){
            System.err.println("Failed to Terminate Worker: " + instanceId);
        }
    }

//    public boolean terminateWorkerById(String instanecID){
//
//    TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
//            .withInstanceIds(instanecID);
//
//    ec2.terminateInstances(terminateInstancesRequest)
//            .getTerminatingInstances()
//            .get(0)
//            .getPreviousState()
//            .getName();
//
//    }

}
