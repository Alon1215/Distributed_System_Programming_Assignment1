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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * TaskHandler Abstract the "new task" message from locals, and part of the manger flow.
 * Responsible for updating the request in the data structure,
 * create workers if needed,
 * send each url as a task to workers' queue,
 * and initiate the workers listeners to listen for workers responses.
 */
public class TaskHandler {
    /* Data structures and logic */
    private static final ConcurrentHashMap<String, RequestDetails> localsDetails = new ConcurrentHashMap<>();
    private static final AtomicInteger amountOfActiveWorkers = new AtomicInteger(0);
    private static final ArrayList<String> workersInstances = new ArrayList<>();
    private final int imagesPerWorker;
    private static final Gson gson = new Gson();
    /* AWS tools and properties */
    private static final String amiId = "ami-0b8ae442d9a63a4f8";
    private static Ec2Client ec2;
    private static final S3Controller s3 = new S3Controller();
    private static final SQSController sqsController = new SQSController();
    private static String M2W_queURL = null;
    private static String W2M_queURL = null;
    /* Workers listeners */
    private static final int EXECUTORS_NUMBER = 1; // no need for more, but can be adjusted if needed
    private final Thread[] workersListenerPool;


    public TaskHandler(int n, AtomicBoolean isTerminated){
        this.imagesPerWorker = n;

        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        M2W_queURL = sqsController.createQueue("ManagerToWorkers" + new Date().getTime());
        W2M_queURL = sqsController.createQueue("WorkersToManager" + new Date().getTime());

        this.workersListenerPool = new Thread[EXECUTORS_NUMBER];
        for (int i=0 ; i < workersListenerPool.length; i++) {
            this.workersListenerPool[i] = new Thread(new WorkersListener(amountOfActiveWorkers, isTerminated, W2M_queURL, localsDetails));
            this.workersListenerPool[i].start();
        }
    }


    /**
     * Main logic of the class.
     * Responsible for updating the request in the data structure,
     * create workers if needed,
     * send each url as a task to workers' queue,
     * and initiate the workers listeners to listen for workers responses.
     * @param msg_parsed "new task" message parsed to the protocol
     * @param replyUrl Local's queue to be responded with the result.
     */
    public void handleNewTask(TaskProtocol msg_parsed, String replyUrl){
        String bucket = msg_parsed.getField1();
        String key = msg_parsed.getField2();
        String[] urls = s3.getUrls(bucket, key);


        double requiredWorkers = (double) urls.length/imagesPerWorker;
        if(requiredWorkers > amountOfActiveWorkers.get()){
            int additionalWorkers = (int)Math.ceil(requiredWorkers) - amountOfActiveWorkers.get();
            for(int i = 0; i < additionalWorkers; i++){
                createWorker();
            }
        }
        localsDetails.put(replyUrl, new RequestDetails(bucket, urls.length));

        System.out.println("    send " +urls.length + " 'new image task' messages to workers");
        for(String imageUrl: urls){

            sqsController.sendMessage(M2W_queURL, gson.toJson(new TaskProtocol("new image task", imageUrl, "", replyUrl)));

        }
    }


    /**
     * Initiate new EC2 instance,
     * afterwards download the WorkerApp.jar
     * initiate the worjer with the given arguments  (urls of queues).
     */
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
                    "Successfully started EC2 instance %s based on AMI %s\n",
                    instanceId, amiId);
        } catch (Ec2Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
//        System.out.println("Done!");

    }


    /**
     * If a termination arrived, start termination protocol.
     * Wait for all tasks to finish their job,
     * send termination to all workers,
     * wait for all worker listeners threads to stop,
     * terminate worker-manager communication (queues),
     * and finally terminate ec2 instances of all workers.
     */
    public void WaitForListenersTermination() {
        System.out.println("-> WaitForListenersTermination()");

        try {
            for (Thread thread : workersListenerPool) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("workersListener was interrupted: " + e.toString());
        }
    }

    public static void broadcastTerminate2Workers(){
        System.out.println("-> broadcastTerminate2Workers()");

        //send termination to all workers
        System.out.println("    Send "+ amountOfActiveWorkers.get() +" 'terminate worker' to workers");
        for(int i = 0; i < amountOfActiveWorkers.get(); i++){
            sqsController.sendMessage(M2W_queURL, gson.toJson(new TaskProtocol("terminate worker", "", "", "")));
        }
    }

    public static void terminateWorkersAndQueues(){
        System.out.println("-> terminateWorkersAndQueues()");

        // delete communication (sqs) between manager and workers
        sqsController.deleteQueue(M2W_queURL);
        sqsController.deleteQueue(W2M_queURL);

        // terminate Workers' EC2
        for ( String instanceId : workersInstances){
            terminateEC2ById(instanceId);
        }
    }


    /**
     * Terminate ec2 instance
     * @param instanceId instance id of the given instance
     */
    public static void terminateEC2ById(String instanceId) {
        System.out.println("Terminate EC2: " + instanceId);
        try {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId).build();
            ec2.terminateInstances(request);
            System.out.println("Terminate EC2: " + instanceId);

        } catch (Exception e){
            System.err.println("Failed to Terminate EC2: " + instanceId);
        }
    }

}
