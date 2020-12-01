package manager;

public class ImageOutput {
    private String URL;
    private String text;

    public ImageOutput(String url, String text) {
        URL = url;
        this.text = text;
    }

    public String getURL() {
        return URL;
    }

    public String getText() {
        return text;
    }
}
