package manager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class HTMLHandler {

    /**
     * Generate the output file, anHTML contains the pictures and the text obtained by the OCR
     * @param urlsToText Vector of the the resuls, which contains url and the text within it
     * @param name name of the output file (as described in the assignment
     * @return HTML file, output of the assignment
     */
    public static File generateHtmlFile(Vector<ImageOutput> urlsToText, String name){

        File f = new File(name +".html");
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
