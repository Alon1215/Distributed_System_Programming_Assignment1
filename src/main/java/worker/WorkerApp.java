package worker;
import local.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;  // Import the File class
import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.Gson;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URL;
import java.util.*;
import java.util.List;


public class WorkerApp {

    public static void main(String[] args) {
        final String uniqueName = "worker" + System.currentTimeMillis();
        System.out.println(uniqueName + ": Start->");  // TODO: delete, test only

        if (args.length < 2){
            System.err.println(uniqueName + ": Not enough arguments, Worker shut down ungracefully");
            System.exit(1);
        }

        String ManagerQueueUrl = args[0]; //worker2manager queue
        String workersQueueUrl = args[1]; //manager2workers queue
        workerLoop(ManagerQueueUrl, workersQueueUrl);

    }

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
                            System.out.println("Bad task protocol");
                            break;
                    }
                    sqs.deleteSingleMessage(workersQueueUrl,msg);

                }
            }
        }
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
            bGr.drawImage(img, 0, 0, null);
            bGr.dispose();

        // Return the buffered image
        return bimage;
}

    public static String img2Txt(String url) {

        Tesseract tesseract = new Tesseract();
        tesseract.setLanguage("eng");
        tesseract.setOcrEngineMode(1);
        File tessdata = LoadLibs.extractTessResources("tessdata-master");
        tesseract.setDatapath(tessdata.getAbsolutePath());

        try {
            URL url_IMG = new URL(url);
            Image image = ImageIO.read(url_IMG);
            String s = null;
            if(image == null)
                return "";
            s = tesseract.doOCR(toBufferedImage(image));
            System.out.println("Result: " + s);
            return s;
        } catch (IOException | TesseractException e) {
            return (e instanceof TesseractException) ? "input file: error Image not found" : "input file: error OCR";
        }
    }
}
