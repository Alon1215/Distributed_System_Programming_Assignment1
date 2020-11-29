package local;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
import java.util.List;

public class EC2 {
    Ec2Client ec2 = null;
    String name = "manager";
    String amiId = "ami-03fb8155c2b349f7a";
    String instanceId;

    public EC2(){
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        boolean isExist = checkIfManagerExist();
            if (!isExist) {
                createInstance();
                System.out.println("Finished making a Manager");

            }
            else{
                System.out.println("Manager already exist");
            }
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
                            this.instanceId = instance.instanceId(); // @TODO: Alon 12:00 : update field to running manager's Id
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void createInstance(){

        final String USAGE =
                "To run this example, supply an instance name and AMI image id\n" +
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

//        String instanceId = response.instances().get(0).instanceId();
        this.instanceId = response.instances().get(0).instanceId(); // @TODO: Alon 12:00 : stated ID as a field (instead of local)

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
        // snippet-end:[ec2.java2.create_instance.main]
        System.out.println("Done!");
    }
}
