package Local;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Base64;
public class EC2 {
    Ec2Client ec2 = null;
    public EC2(){
        boolean isExist = checkIfManagerExist();
        if (!isExist){
            createInstance();
        }
    }

    private boolean checkIfManagerExist() {
        Tag tag = Tag.builder()
                .key("Name")
                .value("Manager")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.println("Manager already exist");
            return true;

        } catch (Ec2Exception e) {
            System.out.println("Manager doesn't exist yet");
            return false;
        }
    }

    public void createInstance(){
        ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        final String USAGE =
                "To run this example, supply an instance name and AMI image id\n" +
                        "Both values can be obtained from the AWS Console\n" +
                        "Ex: CreateInstance <instance-name> <ami-image-id>\n";

        String name = "Manager";
        String amiId = "ami-076515f20540e6e0b";

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
