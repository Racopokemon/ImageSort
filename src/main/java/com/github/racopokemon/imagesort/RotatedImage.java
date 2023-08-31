package com.github.racopokemon.imagesort;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;

public class RotatedImage extends Image {

    private int orientation = 1; //number indicating image orientation, read from image metadata
    private Metadata metadata;

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
        try {
            metadata = ImageMetadataReader.readMetadata(file);
            if (metadata != null) {
                ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
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
    //Checks the orientation metadata flag for the image rotation. In case the image is rotated in any way its height and width change places (any 90° rotation), 
    //this returns true, false otherwise. 
    public boolean isRotatedBy90Degrees() {
        int rot = getRotation();
        return rot == 90 || rot == -90;
    }

    //reads out the orientation flag and returns the images rotation with one of the following values: 
    //0, 90, -90 or 180
    public int getRotation() {
        switch (getOrientation()) {
            case 1: case 2:
                return 0;
            case 3: case 4:
                return 180;
            case 5: case 6:
                return 90;
            case 7: case 8:
                return -90;
            default:
                //and whatever went on here...
                return 0;
        }
    }


    //A debug method that prints all metadata available in this image into the console. Might come in handy again. 
    public void printImageMetadata() {
        if (metadata == null) {
            System.out.println("no metadata found");
        } else {
        
        }
        //sample code taken from the library example code at 
        //https://github.com/drewnoakes/metadata-extractor/blob/master/Samples/com/drew/metadata/SampleUsage.java#L123
        //
        // A Metadata object contains multiple Directory objects
        //
        for (Directory directory : metadata.getDirectories()) {
        
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

    //Some things in Java are still ... a bit overcomplicated
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEE, dd. MMM yyyy, HH:mm"); //hehe german time format. If ppl want another one, I could also later offer a text box to change this. 

    //Based on the available metadata, returns a string of one or several lines of image properties (date, focal length, ...)
    public ArrayList<String> getSomeImageProperties() {

        ArrayList<String> output = new ArrayList<>();
        if (metadata == null) {
            output.add("Could not read metadata from this image (null).");
            return output;
        }

        Date date = null;

        //let's load some directories first
        ExifIFD0Directory dir1 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (dir1 != null && dir1.containsTag(ExifIFD0Directory.TAG_DATETIME)) {
            date = dir1.getDate(ExifIFD0Directory.TAG_DATETIME);
        }
        ExifSubIFDDirectory dir2 = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (dir2 != null && dir2.containsTag(ExifSubIFDDirectory.TAG_DATETIME)) {
            Date d2 = dir2.getDate(ExifSubIFDDirectory.TAG_DATETIME);
            if (date == null || d2.before(date)) {
                date = d2;
            }
        }
        FileSystemDirectory dir3 = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
        if (dir3 != null && dir3.containsTag(FileSystemDirectory.TAG_FILE_MODIFIED_DATE)) {
            Date d3 = dir3.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
            if (date == null || d3.before(date)) {
                date = d3;
            }
        }
        
        //model line
        String makeModelAndLens = "";
        if (dir1 != null) {
            if (dir1.containsTag(ExifIFD0Directory.TAG_MAKE)) {
                makeModelAndLens += dir1.getString(ExifIFD0Directory.TAG_MAKE);
            }
            if (dir1.containsTag(ExifIFD0Directory.TAG_MODEL)) {
                if (!makeModelAndLens.equals("")) {
                    makeModelAndLens += " ";
                }
                makeModelAndLens += dir1.getString(ExifIFD0Directory.TAG_MODEL);
            }
        }
        String lensLine = "";
        if (dir2 != null) {
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_LENS_MAKE)) {
                lensLine += dir2.getString(ExifSubIFDDirectory.TAG_LENS_MAKE);
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_LENS_MODEL)) {
                if (!lensLine.equals("")) {
                    lensLine += " ";
                }
                lensLine += dir2.getString(ExifSubIFDDirectory.TAG_LENS_MODEL);
            }
            if (!lensLine.equals("") && !makeModelAndLens.equals("")) {
                makeModelAndLens += "\n";
            }
            makeModelAndLens += lensLine; 
        }
        if (!makeModelAndLens.equals("")) {
            output.add(makeModelAndLens);
        }
        
        String secondLine = null;

        //date line
        if (date != null) {
            secondLine = DATE_FORMATTER.format(date);
        }

        //image properties line
        if (dir2 != null) {
            ArrayList<String> camBits = new ArrayList<String>(); //were doing the list-collecting thing AGAIN argh
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                camBits.add(dir2.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) + " mm");
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                camBits.add("ISO "+dir2.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
                camBits.add("f/" + dir2.getString(ExifSubIFDDirectory.TAG_FNUMBER));
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
                camBits.add(dir2.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) + " sec");
            }
            if (!camBits.isEmpty()) {
                String allBitsMerged = "";
                for (int i = 0; i < camBits.size(); i++) {
                    String bit = camBits.get(i);
                    if (i != 0) allBitsMerged += " | ";
                    allBitsMerged += bit;
                }
                if (secondLine == null) {
                    secondLine = allBitsMerged;
                } else {
                    secondLine += "\n";
                    secondLine += allBitsMerged;
                }
            }
        }
        if (secondLine != null) {
            output.add(secondLine);
        }

        if (output.isEmpty()) {
            output.add("no metadata found");
            return output;
        } else {
            //String out = "";
            //for (int i = 0; i < output.size(); i++) {
            //    out += output.get(i);
            //    if (i < output.size() - 1) {
            //        out += "\n";
            //    }
            //}
            return output;
        }
    }
}