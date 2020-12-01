package shared;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import software.amazon.awssdk.regions.Region;

public class SQSController {
    public SqsClient sqs;

    public SQSController(){
        sqs = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }



    public void sendMessage(String url, String msg){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(msg)
                .delaySeconds(5)
                .build();
        sqs.sendMessage(send_msg_request);
    }

    public List<Message> getMessages(String queueURL){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .visibilityTimeout(60)
                .build();
        return sqs.receiveMessage(receiveRequest).messages();
    }

    public void deleteSingleMessage(String queueURL, Message m) {
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(m.receiptHandle())
                .build();
        sqs.deleteMessage(deleteRequest);
    }

    public String getQueueURLByName(String sqsName) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(sqsName)
                .build();
        return sqs.getQueueUrl(getQueueRequest).queueUrl();
    }


    public void deleteQueue(String queueURL){
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueURL)
                .build();
        sqs.deleteQueue(deleteQueueRequest);
    }

    public String createQueue(String sqsName) {
        String queueUrl = "";
        try {
            sqs = SqsClient.builder()
                    .region(Region.US_EAST_1)
                    .build();

            try {
                CreateQueueRequest request = CreateQueueRequest.builder()
                        .queueName(sqsName)
                        .build();
                sqs.createQueue(request);
            } catch (QueueNameExistsException e) {
                System.err.println("SQS Error: wait 60 seconds before creating SQS queue with the name: " + sqsName);
                System.exit(-1);
            }
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsName)
                    .build();
            queueUrl = sqs.getQueueUrl(getQueueRequest).queueUrl();
        } catch (QueueNameExistsException e){
            queueUrl = getQueueURLByName(sqsName);
        }
        return queueUrl;
    }

}
