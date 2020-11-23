package Manager;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;

public class HTMLHandler {

    public static final String HTML_HEADER= "<html>\n\t<head>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"><title>OCR</title>\n\t</head>\n\t<body>\n";
    public static final String HTML_FOOTER = "\t</body>\n</html>";
    public static final String HTML_START_PARAGRAPH_AND_IMAGE = "\t\t<p>\n" +"\t\t<img src=";
    public static final String HTML_AFTER_IMAGE_BEFORE_TEXT = "><br>\n\t\t";
    public static final String HTML_AFTER_TEXT = "\n\t\t</p>\n";


    public HTMLHandler() {
    }

    public void parseListOfUrlAndTextToHTML(Vector<Pair<String,String>> urlsToText, String fileName){
        File f = new File(fileName+".html");
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(HTML_HEADER);
            for(Pair<String, String> p : urlsToText){

                String paragraph = HTML_START_PARAGRAPH_AND_IMAGE + p.getKey() +
                        HTML_AFTER_IMAGE_BEFORE_TEXT +
                        p.getValue() +
                        HTML_AFTER_TEXT;
                bw.write(paragraph);
            }
            bw.write(HTML_FOOTER);
            bw.close();
        }catch (IOException e){
            System.out.println("problem with io");
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        HTMLHandler parser = new HTMLHandler();
        Map<String,String> input = new HashMap<>();
        input.put("\"http://ct.mob0.com/Fonts/CharacterMap/ocraextended.png\"","...TEXT THAT WAS PARSED...");
        parser.parseListOfUrlAndTextToHTML(input,"output");
    }


}
