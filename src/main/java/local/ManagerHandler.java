package local;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
import java.util.List;

public class ManagerHandler {
    private SQSController sqs = new SQSController();
    private final Ec2Client ec2;
    private final String name = "manager";
    private final String amiId = "ami-076515f20540e6e0b";
    private String instanceId;
    private String sqsURL;

    public ManagerHandler(int n_input) {
        // create ec2 clients
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        // create new instance (if needed) & set sqs url
//        if (!checkIfManagerExist()) {
//            createInstance(n_input);
            System.out.println("Finished making a Manager");

            this.sqsURL = this.sqs.createQueue("Local2Manager");
            System.out.println("ManagerHandler: Manager queue created URL: " + sqsURL);
//        }
//        else{
//            System.out.println("Manager already exist");
//
//            // TODO: ALON 14.11:
//            this.sqsURL = sqs.getQueueURLByName("Local2Manager");
//        }
    }

    private boolean checkIfManagerExist() {

        List<Reservation> reservList = ec2.describeInstances().reservations();

        //iterate on reservList and call
        for (Reservation reservation : reservList) {
            List<Instance> instanceList = reservation.instances();
            //Now on each instance you can call
            for (Instance instance : instanceList) {
                if (instance.state().name() != InstanceStateName.TERMINATED) { // @TODO: Alon 12:00 : why not RUNNING?
                    List<Tag> tags = instance.tags();
                    for (Tag tag : tags) {
                        if (tag.value().equals(name)){
                            this.instanceId = instance.instanceId();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Create EC2 for manager node & create new sqs queue for local to manager communication
     * (manager listen to queue)
     */
    public void createInstance(int n_input){

        final String USAGE =
                "To run this example, supply an instance name and AMI image id\n" +
                        "Both values can be obtained from the AWS Console\n" +
                        "Ex: CreateInstance <instance-name> <ami-image-id>\n";


        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_MICRO)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .userData(Base64.getEncoder().encodeToString(USAGE.getBytes())).build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

//        String instanceId = response.instances().get(0).instanceId();
        this.instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value(name)
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

    public String getQueueURL() {
        return sqsURL;
    }

    public String getInstanceId() {
        return instanceId;
    }
}


