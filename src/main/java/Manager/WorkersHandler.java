package Manager;

import Local.SQSController;
import Local.TaskProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersHandler {
    String amiId = "AMI here";
    int amountOfActiveWorkers = 0;
    String M2W_queURL;
    String W2M_queURL;
    private final Ec2Client ec2;
    ArrayList<String> workersInstances;
    SQSController controller;
    private int imagesPerWorker;

    public WorkersHandler(int n){
        this.imagesPerWorker = n;
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        controller = new SQSController();
        M2W_queURL = controller.createQueue("ManagerToWorkers" + new Date().getTime());
        W2M_queURL = controller.createQueue("WorkersToManager" + new Date().getTime());

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

        for(String url: urls){
            TaskProtocol task = new TaskProtocol("new image task", url, "", replyUrl);
            controller.sendMessage(M2W_queURL, task.toString());
        }
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
