package manager;

import java.util.Vector;

/**
 * Represents a request from local.
 * Contains the number of pictures,
 * the bucket for reply message,
 * and the result accumulator
 */
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
        System.out.println("    " + bucket + ": processed " + imageOutputs.size() + "/" + amountOfUrls + "urls" );
        return imageOutputs.size() == amountOfUrls;
    }

}
