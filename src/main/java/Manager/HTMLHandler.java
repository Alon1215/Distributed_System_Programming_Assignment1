package Manager;

import javafx.util.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class HTMLHandler {

    public static File makeHTMLSummaryFile(Vector<Pair<String,String>> urlsToText){

        File f = new File("summary" + System.currentTimeMillis() +".html");
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("<html>\n\t<head>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"><title>OCR</title>\n\t</head>\n\t<body>\n");
            for(Pair<String, String> p : urlsToText){

                String paragraph = "\t\t<p>\n" +"\t\t<img src=" + p.getKey() +
                        "><br>\n\t\t" +
                        p.getValue() +
                        "\n\t\t</p>\n";
                bw.write(paragraph);
            }
            bw.write("\t</body>\n</html>");
            bw.close();
            return f;
        }catch (IOException e){
            System.out.println("problem with io");
            return null;
        }
    }


}
