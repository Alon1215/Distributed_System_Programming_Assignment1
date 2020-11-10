package worker;

import com.asprise.ocr.Ocr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.IOException;
import java.net.MalformedURLException;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.FileUtils;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner; // Import the Scanner class to read text files


public class workerApp {

    public static void main(String[] args) {
        System.out.println("Start->");
//        if (args.length > 2){
//            convertDemo(args[1]);
//        }
        convertDemo("Demo");

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
}
