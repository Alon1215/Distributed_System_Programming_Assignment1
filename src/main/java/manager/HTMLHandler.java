package manager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class HTMLHandler {

    public static File generateHtmlFile(Vector<ImageOutput> urlsToText){

        File f = new File("summary" + System.currentTimeMillis() +".html");
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("<html>\n\t<head>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"><title>OCR</title>\n\t</head>\n\t<body>\n");
            for(ImageOutput output: urlsToText){

                String paragraph = "\t\t<p>\n" +"\t\t<img src=" + output.getURL() +
                        "><br>\n\t\t" +
                        output.getText() +
                        "\n\t\t</p>\n";

                bw.write(paragraph);

            }
            bw.write("\t</body>\n</html>");
            bw.close();
            return f;
        }catch (IOException e){
            System.err.println("problem with io");
            return null;
        }
    }


}
