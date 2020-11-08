import com.asprise.ocr.Ocr;

import javax.imageio.ImageIO;
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


public class Main {

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
        }  catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void img2TxtDemo(String url) throws MalformedURLException, URISyntaxException {

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata-master");
        URL url_IMG = new URL(url);
        String[] arr = url.split("/");
        File ne = new File(arr[arr.length - 1]);
        try {
            FileUtils.copyURLToFile(url_IMG, ne);
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Image not fund");
        }
        String s = null;
        try {
            s = tesseract.doOCR(ne);
        } catch (TesseractException e) {
            e.printStackTrace();
        }
        System.out.println("Result: " + s);


    }
}
