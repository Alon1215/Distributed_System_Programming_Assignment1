package worker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;  // Import the File class
import java.io.IOException;

import com.google.gson.Gson;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import shared.SQSController;
import shared.TaskProtocol;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URL;
import java.util.List;

/**
 * Main class of WorkerApp.jar, which run in the cloud.
 * Listen to manager's tasks for the workers.
 * process a given OCR task, and return a result for the given task.
 * Programed to overcome OCR and IO exceptions, and implement the main task of the program,
 * which is to process Text from images (OCR in the cloud).
 */
public class WorkerApp {
    private static final Tesseract tesseract = new Tesseract();

    /**
     * Main method.
     * Initiate Worker's activity,
     * afterward run workers loop.
     * @param args args contain the urls of the manager and workers queue.
     */
    public static void main(String[] args) {
        final String uniqueName = "worker" + System.currentTimeMillis();
        tesseract.setLanguage("eng");
        tesseract.setOcrEngineMode(1);
        File tessdata = LoadLibs.extractTessResources("tessdata-master");
        tesseract.setDatapath(tessdata.getAbsolutePath());
        if (args.length < 2){
            System.err.println(uniqueName + ": Not enough arguments, Worker shut down ungracefully");
            System.exit(-1);
        }

        String ManagerQueueUrl = args[0]; //worker2manager queue
        String workersQueueUrl = args[1]; //manager2workers queue
        workerLoop(ManagerQueueUrl, workersQueueUrl);

    }

    /**
     * Main loop of the worker.
     * listen to manager's messages for the workers.
     * if a new OCR task arrive - process the task
     * else, a termination message received, response properly and finish run.
     * @param managerUrl  Workers2Manager queue
     * @param workersQueueUrl Manager2Workers queue
     */
    private static void workerLoop(String managerUrl, String workersQueueUrl) {
        SQSController sqs = new SQSController();
        Gson gson = new Gson();

        boolean isTerminated = false;

        while (!isTerminated) {
            List<Message> messages = sqs.getMessages(workersQueueUrl);
            for (Message msg : messages) {
                if (msg != null) {

                    TaskProtocol msg_parsed = gson.fromJson(msg.body(),TaskProtocol.class);
                    String type = msg_parsed.getType();

                    switch (type) {
                        case "new image task":
                            String textOutput = img2Txt(msg_parsed.getField1());

                            sqs.sendMessage(managerUrl, gson.toJson(new TaskProtocol("done OCR task", msg_parsed.getField1(), textOutput, msg_parsed.getReplyURL())));

                            break;

                        case "terminate worker":
                            sqs.sendMessage(managerUrl, gson.toJson(new TaskProtocol("worker died", " ", " ", " ")));
                            isTerminated = true;
                            break;
                        default:
                            // not suppose to happen
                            System.err.println("Bad task protocol");
                            break;
                    }
                    sqs.deleteSingleMessage(workersQueueUrl,msg);
                }
            }
        }
    }

    /**
     * Convert image to buffered image/
     * @param img given Image
     * @return Buffered image
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Create a buffered image with transparency
        BufferedImage bImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bImage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bImage;
    }

    /**
     * Process for a given url, the text within it.
     * Thus, convert the url to an input for the OCR,
     * handle exceptions,
     * and response with the text suited for the given url
     * @param url urk of an image
     * @return text - within the picture / exception description
     */
    public static String img2Txt(String url) {

        try {
            URL url_IMG = new URL(url);
            Image image = ImageIO.read(url_IMG);
            String s;
            if(image == null)
                return "Input file: Error OCR picture not found";
            s = tesseract.doOCR(toBufferedImage(image));
            return s;
        } catch (IOException | TesseractException e) {
            return (e instanceof TesseractException) ? "Input file: error Image not found" : "Input file: error OCR";
        }
    }


}
