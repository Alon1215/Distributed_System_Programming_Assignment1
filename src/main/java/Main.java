import com.asprise.ocr.Ocr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.IOException;
import java.net.MalformedURLException;
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

            Ocr.setUp(); // one time setup

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

        Ocr ocr = new Ocr(); // create a new OCR engine
        URL url_IMG = new URL(url);
        File ne = new File("download");
        try {
            FileUtils.copyURLToFile(url_IMG, ne);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ocr.startEngine("eng", Ocr.SPEED_FASTEST); // English
        String s = ocr.recognize(new File[] {(ne)},
                Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT); // PLAINTEXT | XML | PDF | RTF
        System.out.println("Result: " + s);
        ocr.stopEngine();


    }
}
