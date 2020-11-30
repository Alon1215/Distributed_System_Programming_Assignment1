package manager;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class HTMLHandler {

    public static File makeHTMLSummaryFile(HashMap<String, String> urlsToText){

        File f = new File("summary" + System.currentTimeMillis() +".html");
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("<html>\n\t<head>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\"><title>OCR</title>\n\t</head>\n\t<body>\n");
            urlsToText.forEach((key, value) -> {

                String paragraph = "\t\t<p>\n" +"\t\t<img src=" + key +
                        "><br>\n\t\t" +
                        value +
                        "\n\t\t</p>\n";
                try {
                    bw.write(paragraph);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.write("\t</body>\n</html>");
            bw.close();
            return f;
        }catch (IOException e){
            System.out.println("problem with io");
            return null;
        }
    }


}
