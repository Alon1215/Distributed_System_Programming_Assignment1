package Worker;
import Local.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.IOException;
import java.net.MalformedURLException;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;
import java.util.List;


public class WorkerApp {

    public static void main(String[] args) {
        final String uniqueName = "Worker" + System.currentTimeMillis();
        System.out.println(uniqueName + ": Start->");  // TODO: delete, test only

        if (args.length < 2){ // TODO: args check, decide how many args required
            System.out.println(uniqueName + ": Not enough arguments, Worker shut down ungracefully");
            System.exit(1);
        }

        String ManagerQueueUrl = args[0]; //worker2manager queue
        String workersQueueUrl = args[1]; //worker2manager queue
        workerLoop(ManagerQueueUrl, workersQueueUrl);
        //terminateWorker(ManagerQueueUrl);    // TODO: how to terminate worker (done outside of loop)


//        convertDemo("Demo"); // TODO: delete. test only

    }

    private static void workerLoop(String managerUrl, String workersQueueUrl) {
        SQSController sqs = new SQSController();
        boolean isTerminated = false;

        while (!isTerminated) {
            List<Message> messages = sqs.getMessages(workersQueueUrl);
            for (Message msg : messages) {
                String[] msg_s;
                if (msg != null) {
                    msg_s = msg.body().split("\n");
                    String type = msg_s[0];

                    switch (type) {
                        case "new image task":
                            String textOutput = img2Txt(msg_s[1]);
                            sqs.sendMessage(managerUrl, new TaskProtocol("done ocr task",msg_s[1], textOutput,msg_s[3]).toString());
//                            sqs.deleteMessages(workersQueueUrl,new ArrayList<>(Collections.singleton(msg)));
                            break;

                        case "termination":
                            isTerminated = true;
                            break;
                        default:
                            // not suppose to happen
                            System.out.println("Bad task protocol");
                            break;
                    }
                }
            }
        }
    }

    public static void convertDemo(String path) {
        try {
            //File inputFile = new File(path);
            File inputFile = new File("text.images.txt");
            Scanner myReader = new Scanner(inputFile);


            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                System.out.println(data);
                img2TxtDemo(data);

            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("input file: No file found, program terminates.");
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
    public static void img2TxtDemo(String url) throws MalformedURLException {

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata-master");
        URL url_IMG = new URL(url);
        Image image = null;
        try {
             image = ImageIO.read(url_IMG);
        } catch (IOException e) {
            System.out.println("Image not found");
        }
        String s = null;
        try {
            if(image == null)
                return;
            s = tesseract.doOCR(toBufferedImage(image));
        } catch (TesseractException e) {
            System.out.println("Test");
        }
        System.out.println("Result: " + s);


    }

    public static String img2Txt(String url) {

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata-master");

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
