package com.github.racopokemon.imagesort;

import javafx.scene.image.Image;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import com.drew.imaging.FileType;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.file.FileTypeDirectory;

public class RotatedImage extends Image {

    private File image;

    private int orientation = 1; //number indicating image orientation, read from image metadata
    private int fileOrientation;
    private Metadata metadata;

    public RotatedImage(File file) {
        // loading itself is simple (its all done in the Image class already, also that its loading in background and showing an empty pic until then)
        super(file.toURI().toString(), true);
        image = file;

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
        fileOrientation = orientation;
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

    //Changes the orientation flag inside this RotatedImage, but does nothing further: 
    //No files are written, and the Gallery is not notified about that. 
    //It is rather intended that the Gallery calls such a rotation (that the user requested) and then updates its views depending on that. 
    public void rotateBy90Degrees(boolean clockwise) {
        if (clockwise) {
            if (orientation == 8) {orientation = 1; return;}
            if (orientation == 3) {orientation = 8; return;}
            if (orientation == 6) {orientation = 3; return;}
            if (orientation == 1) {orientation = 6; return;}
    
            if (orientation == 7) {orientation = 2; return;}
            if (orientation == 4) {orientation = 7; return;}
            if (orientation == 5) {orientation = 4; return;}
            if (orientation == 2) {orientation = 5; return;}
        } else {
            if (orientation == 8) {orientation = 3; return;}
            if (orientation == 3) {orientation = 6; return;}
            if (orientation == 6) {orientation = 1; return;}
            if (orientation == 1) {orientation = 8; return;}
    
            if (orientation == 7) {orientation = 4; return;}
            if (orientation == 4) {orientation = 5; return;}
            if (orientation == 5) {orientation = 2; return;}
            if (orientation == 2) {orientation = 7; return;}
        }
    }

    //A debug method that prints all metadata available in this image into the console. Might come in handy again. 
    public void printImageMetadata() {
        if (metadata == null) {
            System.out.println("no metadata found for "+image.getName());
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
                String make = dir1.getString(ExifIFD0Directory.TAG_MAKE);
                if (make.matches(".*[a-zA-Z0-9].*")) { //had a weird case of make being "--", this matches at least one letter or number
                    makeModelAndLens += make;
                }
            }
            if (dir1.containsTag(ExifIFD0Directory.TAG_MODEL)) {
                String lens = dir1.getString(ExifIFD0Directory.TAG_MODEL);
                if (lens.matches(".*[a-zA-Z0-9].*")) {
                    if (!makeModelAndLens.equals("")) {
                        makeModelAndLens += " ";
                    }
                    makeModelAndLens += lens;
                }
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
        if (dir1 != null && dir1.containsTag(ExifIFD0Directory.TAG_SOFTWARE)) {
            String software = dir1.getString(ExifIFD0Directory.TAG_SOFTWARE);
            if (Common.isExternalSoftware(software)) {
                if (!makeModelAndLens.equals("")) makeModelAndLens += "\n";
                makeModelAndLens += software;
            }
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
            int camMode = 0;
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM)) {
                try {
                    camMode = dir2.getInt(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM);
                    //0	No info
                    //1	Manual
                    //2	Normal / Auto
                    //3	Aperture prio
                    //4	Shutter prio
                    //5	Creative (depth of field)
                    //6	Action (fast shutter speed)
                    //7	Portrait (closeup with background out of focus)
                    //8	Landscape (background focus)
                } catch (Exception e) {}
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                camBits.add(dir2.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) + " mm");
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                camBits.add("ISO "+dir2.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
            }
            //if (camMode == 2) { camBits.add("Auto");}
            if (camMode == 5) { camBits.add("Creative");}
            if (camMode == 6) { camBits.add("Action");}
            if (camMode == 7) { camBits.add("Portrait");}
            if (camMode == 8) { camBits.add("Landscape");}
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
                camBits.add("f/" + dir2.getString(ExifSubIFDDirectory.TAG_FNUMBER) + (camMode == 1 || camMode == 3 ? "*" : ""));
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                String suffix = " sec" + (camMode == 1 || camMode == 4 ? "*" : "");
                Rational exposureTime = dir2.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
                if (exposureTime != null) {
                    boolean isExposureTimeUgly = exposureTime.getNumerator() != 1;
                    //This is to tackle those oddly specifiy fractions (100016/5000000) that for example 
                    //certain google pixel phones fabricate..
                    if (isExposureTimeUgly) {
                        double expTime = exposureTime.doubleValue();
                        int digitPrecision;
                        if (expTime >= 1) {
                            if (expTime >= 5) {
                                digitPrecision = 0;
                            } else {
                                digitPrecision = 1;
                            }
                        } else {
                            digitPrecision = 2-(int)(Math.log10(expTime)+0.0001);
                        }
                        camBits.add(Common.formatDoubleUpToDecimalPlaces(expTime, digitPrecision) + suffix);
                    } else {
                        camBits.add(exposureTime.toSimpleString(true) + suffix);
                    }
                }
            }
            if (dir2.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS)) {
                Rational bias = dir2.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_BIAS);
                if (bias != null && !bias.isZero()) {
                    camBits.add(String.format(Locale.US, "%.1f EV", bias.doubleValue()));
                }
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

    //The internal image format, no matter the file extension. 
    public boolean isJPG() {
        FileTypeDirectory dir = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);
        if (dir != null && dir.containsTag(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE)) {
            return dir.getString(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE).equals(FileType.Jpeg.getMimeType());
        }
        return false;
    }

    /**
     * Writes the currently stored orientation to disk (updates the exif-flag inside the file). 
     * The thread is blocked until the file operations are finished or an error occurs. 
     * If null is returned, everything worked, otherwise an error description is returned. 
     */
    public String writeCurrentOrientationToFile() {
        if (doPreviewAndFileOrientationMatch()) {
            return null;
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile(Common.removeExtensionFromFilename(image.getName()) + "_rotate", ".", image.getParentFile());
            
            //copied, even with comments, from WriteExifMetadataExample in the commons-imaging examples. Because, it actually helps. 
            //(https://github.com/apache/commons-imaging)
            
            //image is not a file anymore, as we just renamed (moved) it
            FileOutputStream fos = new FileOutputStream(tempFile);
            OutputStream os = new BufferedOutputStream(fos);
            
            TiffOutputSet newMetadata = null;
            
            // note that metadata might be null
            final ImageMetadata originalMetadata = Imaging.getMetadata(image);
            
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) originalMetadata;
            if (jpegMetadata != null) {
                // note that exif might be null as well
                TiffImageMetadata exif = jpegMetadata.getExif();
                
                if (exif != null) {
                    // The TiffImageMetadata class is read-only.
                    // getOutputSet returns a writeable copy of the metadata. 
                    newMetadata = exif.getOutputSet();
                }
            }
            
            if (newMetadata == null) {
                newMetadata = new TiffOutputSet();
            }

            // We are told to first remove the field, if it exists. If not, nothing happens.
            newMetadata.getOrCreateRootDirectory().removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
            newMetadata.getRootDirectory().add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short)orientation);
            
            new ExifRewriter().updateExifMetadataLossless(image, os, newMetadata);
            
            try {
                BasicFileAttributes oldAttributes = Files.getFileAttributeView(image.toPath(), BasicFileAttributeView.class).readAttributes();
                BasicFileAttributeView newAttributes = Files.getFileAttributeView(tempFile.toPath(), BasicFileAttributeView.class);
                newAttributes.setTimes(oldAttributes.lastModifiedTime(), oldAttributes.lastAccessTime(), oldAttributes.creationTime());
                //also applying properties like the windows 'hidden' flag could be done here ... yeah but this is probably hardly required. 
            } catch (Exception e) {
                System.out.println("Minor problem when rotating image: Could not copy attributes:");
                e.printStackTrace();
            }
            
            Files.move(tempFile.toPath(), image.toPath(), StandardCopyOption.REPLACE_EXISTING);

            fileOrientation = orientation;
        } catch (Exception e) {
            System.out.println("Error while rotating an image: ");
            e.printStackTrace();
            try {
                if (tempFile != null) Files.deleteIfExists(tempFile.toPath());
            } catch (Exception e1) {
                System.out.println("Error while trying to delete temp file after orientation change failed.");
                System.out.println(Common.formatException(e1));
                e1.printStackTrace();
            }
            orientation = fileOrientation;
            return Common.formatException(e);
        }
        return null;
    }

    public boolean isStillLoading() {
        return getHeight() == 0;
    }

    //essentially, if the image has been rotated (b the gallery) while being viewed. 
    //internally, we store both the orientation from the file and our shown orientation, and we check if these match
    public boolean doPreviewAndFileOrientationMatch() {
        return orientation == fileOrientation;
    }
}