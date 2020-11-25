package Local;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.util.Date;
import java.util.List;
import software.amazon.awssdk.regions.Region;

import java.awt.*;

public class SQSController {
    public SqsClient sqs;
    private String queueURL;

    public SQSController(){
        sqs = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
//        try {
//            CreateQueueRequest request = CreateQueueRequest.builder()
//                    .queueName(sqsName + new Date().getTime())
//                    .build(); // TODO: Alon 14.11: if name is unique (here), some trouble with manager might occur
//            CreateQueueResponse create_result = sqs.createQueue(request);
//        } catch (QueueNameExistsException e) {
//            throw e;
//        }
//        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
//                .queueName(sqsName)
//                .build();
//        this.queueURL = sqs.getQueueUrl(getQueueRequest).queueUrl();
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

    public void deleteMessages(String queueURL, List<Message> messages){
        for (Message m : messages) {
            deleteSingleMessage(queueURL, m);
        }
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

    public String getQueueURL() {
        return queueURL;
    }

    public String createQueue(String sqsName) {
        try {
            sqs = SqsClient.builder()
                    .region(Region.US_EAST_1)
                    .build();

            try {
                CreateQueueRequest request = CreateQueueRequest.builder()
                        .queueName(sqsName)
                        .build();
                CreateQueueResponse create_result = sqs.createQueue(request);
            } catch (QueueNameExistsException e) {
                throw e;
            }
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsName)
                    .build();
            this.queueURL = sqs.getQueueUrl(getQueueRequest).queueUrl();
        } catch (QueueNameExistsException e){
            this.queueURL = getQueueURLByName(sqsName);
        }
        return this.queueURL;
    }

}
