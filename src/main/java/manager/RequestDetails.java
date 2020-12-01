package manager;

import java.util.Vector;

public class RequestDetails {
    private final Vector<ImageOutput> imageOutputs = new Vector<>();
    private final String bucket;
    private final int amountOfUrls;

    public RequestDetails(String bucket, int amountOfUrls) {
        this.bucket = bucket;
        this.amountOfUrls = amountOfUrls;
    }
    public Vector<ImageOutput> getImageOutputs() {
        return imageOutputs;
    }

    public String getBucket() {
        return bucket;
    }

    public boolean addImageOutputAndCheckIfDone(ImageOutput imageOutput){
        imageOutputs.add(imageOutput);
        return imageOutputs.size() == amountOfUrls;
    }

}
