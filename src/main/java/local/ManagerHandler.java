package local;

import shared.SQSController;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
import java.util.List;

public class ManagerHandler {
    private final SQSController sqs = new SQSController();
    private final Ec2Client ec2;
    private final String name = "manager";
    private final String amiId = "ami-0b8ae442d9a63a4f8";
    private String instanceId;
    private final String sqsURL;

    public ManagerHandler(int n_input) {
        // create ec2 clients
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        // create new instance (if needed) & set sqs url
        if (!checkIfManagerExist()) {
            createInstance(n_input);
            System.out.println("Finished making a Manager");

            this.sqsURL = this.sqs.createQueue("Local2Manager");
            System.out.println("ManagerHandler: Manager queue created URL: " + sqsURL);
        }
        else{
            System.out.println("Manager already exist");

            this.sqsURL = sqs.getQueueURLByName("Local2Manager");
        }
    }

    private boolean checkIfManagerExist() {

        List<Reservation> reservList = ec2.describeInstances().reservations();

        //iterate on reservList and call
        for (Reservation reservation : reservList) {
            List<Instance> instanceList = reservation.instances();
            //Now on each instance you can call
            for (Instance instance : instanceList) {
                if (instance.state().name() != InstanceStateName.TERMINATED) {
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
                "#!/bin/bash\n" +
                        "wget https://alontomdsp211.s3.amazonaws.com/ManagerApp.jar\n" +
                        "java -jar ManagerApp.jar " + n_input + "\n";
        IamInstanceProfileSpecification role = IamInstanceProfileSpecification.builder().arn("arn:aws:iam::119201439262:instance-profile/ManagerDSP211AT").build();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .instanceType(InstanceType.T2_SMALL)
                .iamInstanceProfile(role)
                .imageId(amiId)
                .maxCount(1)
                .minCount(1)
                .userData(Base64.getEncoder().encodeToString(USAGE.getBytes())).build();

        RunInstancesResponse response = ec2.runInstances(runRequest);

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


