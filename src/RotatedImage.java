import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;

public class RotatedImage extends Image {
    private int orientation = 1;

    public RotatedImage(File file) {
        super(file.toURI().toString(), true);
        //orientation meanings: https://jdhao.github.io/2019/07/31/image_rotation_exif_info/
        // obtain the Exif directory
        Metadata metadata;
        try {
            metadata = ImageMetadataReader.readMetadata(file);
            if (metadata != null) {
                ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            }
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
}