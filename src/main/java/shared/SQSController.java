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


/**
 * Methods and logic for SQS operations and SQS adjusted methods for the assignment.
 * Used to abstract the use of SQS operations.
 * Implement method such as:
 * Send message,
 * Get message,
 * delete message,
 * get queue URL,
 * create / delete queue.
 */
public class SQSController {
    public SqsClient sqs;

    public SQSController(){
        sqs = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }


    /**
     * Send a message to the given queue
     * @param url Queue's URL
     * @param msg String of the message to be sent
     */
    public void sendMessage(String url, String msg){
        SendMessageRequest send_msg_request = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(msg)
                .delaySeconds(5)
                .build();
        sqs.sendMessage(send_msg_request);
    }

    /**
     * Request the next message available in the queue
     * @param queueURL URL of the given queue
     * @return list of messages.
     */
    public List<Message> getMessages(String queueURL){
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .visibilityTimeout(60)
                .build();
        return sqs.receiveMessage(receiveRequest).messages();
    }

    /**
     * Delete a given message from the queue
     * @param queueURL URL of the given queue
     * @param m Message received form sqs queue
     */
    public void deleteSingleMessage(String queueURL, Message m) {
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(m.receiptHandle())
                .build();
        sqs.deleteMessage(deleteRequest);
    }

    /**
     * Get the sqs url for a given name
     * @param sqsName name to be looked for
     * @return url of the given queue, if exist.
     */
    public String getQueueURLByName(String sqsName) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(sqsName)
                .build();
        return sqs.getQueueUrl(getQueueRequest).queueUrl();
    }


    /**
     * Delete the queue from AWS, if exist.
     * @param queueURL  URL of queue to be terminated
     */
    public void deleteQueue(String queueURL){
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(queueURL)
                .build();
        sqs.deleteQueue(deleteQueueRequest);
    }

    /**
     * Create a new queue with a given name
     * @param sqsName name of the new queue
     * @return queue's URL
     */
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
