package Manager;

import Local.SQSController;
import Local.TaskProtocol;
import javafx.util.Pair;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class WorkersHandler {
    ConcurrentHashMap<String, Integer> amountOfMessagesPerLocal = new ConcurrentHashMap<String, Integer>();
    ConcurrentHashMap<String, Vector<Pair<String, String>>> identifiedMessages = new ConcurrentHashMap<String, Vector<Pair<String, String>>>();

    String amiId = "AMI here";
    int amountOfActiveWorkers = 0;
    String M2W_queURL;
    String W2M_queURL;
    private final Ec2Client ec2;
    ArrayList<String> workersInstances;
    SQSController sqsController;
    private int imagesPerWorker;

    public WorkersHandler(int n){
        this.imagesPerWorker = n;
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        sqsController = new SQSController();
        M2W_queURL = sqsController.createQueue("ManagerToWorkers" + new Date().getTime());
        W2M_queURL = sqsController.createQueue("WorkersToManager" + new Date().getTime());

        workersInstances = new ArrayList<String>();
    }

    public void handleNewTask(String[] urls, String replyUrl){
        //TODO: Create new workers if needed
        double requiredWorkers = (double) urls.length/imagesPerWorker;
        if(requiredWorkers > amountOfActiveWorkers){
            for(int i = 0; i < Math.round(requiredWorkers); i++){
                createWorker();
            }
        }
        amountOfMessagesPerLocal.put(replyUrl, 0);
        identifiedMessages.put(replyUrl, new Vector<Pair<String, String>>());
        amountOfMessagesPerLocal.replace(replyUrl, amountOfMessagesPerLocal.get(replyUrl) + urls.length);

        for(String imageUrl: urls){
            TaskProtocol task = new TaskProtocol("new image task", imageUrl, "", replyUrl);
            sqsController.sendMessage(M2W_queURL, task.toString());

        }

        //new Thread(WaitForOutput());
    }
    public void createWorker() {
        final String USAGE =
                "Worker to manager QUE \n" + W2M_queURL +
                        "Both values can be obtained from the AWS Console\n" +
                        "Ex: CreateInstance <instance-name> <ami-image-id>\n";


        // snippet-start:[ec2.java2.create_instance.main]
        //  client = Ec2Client.create();

        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .userData(Base64.getEncoder().encodeToString(USAGE.getBytes())).build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

        String instanceId = response.instances().get(0).instanceId();
        workersInstances.add(instanceId);
        amountOfActiveWorkers++;
        Tag tag = Tag.builder()
                .key("Name")
                .value("Worker" + System.currentTimeMillis())
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
}
