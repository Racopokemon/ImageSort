import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;

public class RotatedImage extends Image {
    private int orientation = 1; //number indicating image orientation, read from image metadata
    private String dateString = "no date available";
    private Metadata tempMetadata;

    public RotatedImage(File file) {
        // loading itself is simple (its all done in the Image class already, also that its loading in background and showing an empty pic until then)
        super(file.toURI().toString(), true);
        // but for weird reasons javafx does not support the exif orientation flag, which my cam already uses by 2014. 
        // (and which is also automatically used by windows in all previews and viewers)
        // ... so we have an external lib for it, and adapt the behavior of the image view to it

        //orientation meanings: https://jdhao.github.io/2019/07/31/image_rotation_exif_info/
        // obtain the Exif directory
        
        //TODO: Do this in a new thread
        //  Simple solution. new Thread(...).start();
        //but a better solution would be a static service that receives all requests in a row, and cancels images that are cancelled while loading. 
        //(Since here is a list of priorities, this would also be the first step to actually handling and loading images ourselves, which would definitely improve image loading during scrolling)
        //what if we load an image that has no metadata loaded yet, but then finishes? 
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(file);
            if (metadata != null) {
                ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            }
            tempMetadata = metadata;
        } catch (ImageProcessingException | MetadataException | IOException e) {
            e.printStackTrace();
        }
        if (orientation < 1 || orientation > 8) {
            orientation = 1; //1 is just the default orientation, 9 means undefined, and the rest should not occur anyway
        }
    }
    public int getOrientation() {
        return orientation;
    }


    //A debug method that prints all metadata available in this image into the console. Might come in handy again. 
    public void printImageMetadata() {
        if (tempMetadata == null) {
            System.out.println("no metadata found");
        } else {

        }
        //sample code taken from the library example code at 
        //https://github.com/drewnoakes/metadata-extractor/blob/master/Samples/com/drew/metadata/SampleUsage.java#L123
        //
        // A Metadata object contains multiple Directory objects
        //
        for (Directory directory : tempMetadata.getDirectories()) {

            //
            // Each Directory stores values in Tag objects
            //
            for (Tag tag : directory.getTags()) {
                System.out.println(tag);
            }

            //
            // Each Directory may also contain error messages
            //
            for (String error : directory.getErrors()) {
                System.err.println("ERROR: " + error);
            }
        }
    }
}