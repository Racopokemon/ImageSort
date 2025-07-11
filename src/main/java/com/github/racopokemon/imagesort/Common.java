package com.github.racopokemon.imagesort;

import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.prefs.Preferences;
import java.awt.Desktop;
import java.awt.Desktop.Action;

import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;

import java.io.File;

//Helper class, common methods that come in handy in several places

public class Common {
    
    private static FilenameFilter filterImages;
    private static FilenameFilter filterRaws;
    private static FilenameFilter filterVideos;
    private static FilenameFilter filterOther;
    private static Hashtable<String, Image> resources;

    private static String[] relevantSoftwareMatches = 
        {"adobe", "pixelmator", "gimp", "paint", "photopea", "corel", "cyberlink", "skylum", "capture", "dxo", "affinity", "darktable", "therapee", "movavi", "lightroom", "luminar", "scape", "processor", "aurora", "radiant"};
        //Just the result of googling "best raw software" and "best photo software" etc, should work in very most of the cases, at least I had like 5 of the raw softwares installed once

    private static class ResolutionEntry {
        private double big, small;
        private String name;
        public ResolutionEntry(double longSide, double shortSide, String name) {
            big = longSide;
            small = shortSide;
            this.name = name;
        }
        public String getName() {
            return name;
        }
        //Returns -1 if the dimension is not contained, 0 if the resolution is exactly contained, and 1 if it is contained
        public int containsDimension(double longSide, double shortSide) {
            if (longSide < big || shortSide < small) {
                return -1;
            } 
            if (longSide == big && small == shortSide) { //yes, technically we are working with doubles, but they will instantly result from ints as well, so we can do a simple comparison without error bounds
                return 0;
            } else {
                return 1;
            }
        }
    }

    //Accepts all files ending with a given set of file extensions.
    private static class ExtensionFilenameFilter implements FilenameFilter {
        private String[] extensions;

        public ExtensionFilenameFilter(String... acceptedExtensions) {
            extensions = acceptedExtensions;
        }

        @Override
        public boolean accept(File dir, String name) {
            String suffix = getExtensionFromFilename(name);
            for (String ext : extensions) {
                if (ext.equalsIgnoreCase(suffix)) {
                    return true;
                }
            }
            return false;
        }
    }
    //Filename filter that accepts everything except some annoying system files (windows links and windows desktop.ini for example)
    private static class HideStuffFilenameFilter implements FilenameFilter {
        private FilenameFilter unwantedExtensions = new ExtensionFilenameFilter("lnk"); //let's just hide links (in windows at least)
        private String[] unwantedFilenames = {"desktop.ini", ".DS_Store"};
        @Override
        public boolean accept(File dir, String name) {
            if (unwantedExtensions.accept(dir, name)) {
                return false;
            }
            for (String s : unwantedFilenames) {
                if (s.equals(name)) {
                    return false;
                }
            }
            return true;
        }        
    }

    private static ArrayList<ResolutionEntry> resolutions;
    static {
        //https://support.google.com/youtube/answer/6375112
        //Just taking the youtube resolutions here ... these dimensions are ofc not connected to photography, 
        //but everybody has a feeling for these steps, knows their screen dimensions in relation to it, and an expected resolution and so on
        resolutions = new ArrayList<>();
        resolutions.add(new ResolutionEntry(7680, 4320, "8k"));
        resolutions.add(new ResolutionEntry(3840, 2160, "4k"));
        resolutions.add(new ResolutionEntry(2560, 1440, "2k"));
        resolutions.add(new ResolutionEntry(1920, 1080, "1080p"));
        resolutions.add(new ResolutionEntry(1280, 720, "720p"));
        resolutions.add(new ResolutionEntry(854, 480, "480p"));
        resolutions.add(new ResolutionEntry(360, 360, "360p"));
        resolutions.add(new ResolutionEntry(426, 240, "240p"));

        filterImages = new ExtensionFilenameFilter("jpg", "jpeg", "png", "gif", "bmp");
        filterRaws = new ExtensionFilenameFilter("raw", "arw", "crw", "rw2", "cr2", "raf", "nef"); //no idea if any of the other extensions are actually used anywhere, just quickly googled for canon and panasonic raw formats
        filterVideos = new ExtensionFilenameFilter("mp4","m4a","m4v","m3u8"); //https://openjfx.io/javadoc/16/javafx.media/javafx/scene/media/package-summary.html this is the video extensions they mention at least
        filterOther = new HideStuffFilenameFilter();
    }

    //The weak check, let's see if the base requirements are met
    public static boolean isValidFolder(File f) {
        return f.exists() && f.isDirectory();
    }

    public static String getExtensionFromFilename(String name) {
        String[] split = name.split("[.]");
        return split[split.length-1];
    }

    //Stronger check if the dir is valid, if sucessfull, returns an array of containing files. 
    //Also, weird warning: Giving a File("") in here returns all root dirs (esp required for windows).
    public static File[] tryListFiles(File f) {
        if (f.toString().equals("")) {
            return File.listRoots();
        }
        if (!isValidFolder(f)) {
            return null;
        }
        return f.listFiles();
        //stronger check if the directory is valid (in windows, there are some weird folders that seem valid but cant be read, like 'documents and settings' in newer windows versions)
    }

    //Filter for the files this app can work with. Everyone gets the same class, pretending to save resources here
    public static FilenameFilter getImageFilter() {
        return filterImages;
    }

    public static FilenameFilter getRawFilter() {
        return filterRaws;
    }

    public static FilenameFilter getVideoFilter() {
        return filterVideos;
    }

    //This filename filter is a bit special. It is used to hide some (mostly Windows) sytem files, the rest is accepted. 
    public static FilenameFilter getOtherFilter() {
        return filterOther;
    }


    public static String removeExtensionFromFilename(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return filename;
        } else {
            return filename.substring(0, lastDotIndex);
        }
    }

    public static Preferences getPreferences() {
        return Preferences.userNodeForPackage(Main.class);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
    
    public static boolean isMac() {
        return System.getProperty("os.name").startsWith("Mac");
    }

    //Just use the filename, without path, without extension. 
    public static Image getResource(String name) {
        if (resources == null) {
            resources = new Hashtable<>();
        }
        if (!resources.containsKey(name)) {
            resources.put(name, new Image(Common.class.getResourceAsStream(name + ".png")));
        }
        return resources.get(name);
    }

    public static String getResourcePath(String resource) {
        return Common.class.getResource(resource).toExternalForm();
    }

    //Right now only for upper case chars, no error checks
    public static int getPositionInAlphabet(char c) {
        return (int)c - (int)'A';
    }

    //Right now only for lower case chars
    public static String getLetterInAlphabet(int pos) {
        if (pos < 0 || pos > 25) {
            throw new IllegalArgumentException("tick index not in bounds (0-25)");
        }
        return Character.toString((char)('a' + pos));
    }

    public static String formatException(Exception e) {
        return e.getClass() + " - '" + e.getMessage() + "'";
    }

    //Returns a comparison to the well-known youtube resolutions for given image dimensions. 
    //Either something like "FHD+",
    //for exact matches without the greater sign "480p",
    public static String getBiggestContainingResolution(double h, double w) {
        double bigger = Math.max(h, w);
        double smaller = Math.min(h, w);
        //yes, we could do binary search as well. But no, its like 10 entries. 
        for (ResolutionEntry e : resolutions) {
            int containState = e.containsDimension(bigger, smaller);
            if (containState == 1) {
                return e.getName() + "+";
            } else if (containState == 0) {
                return e.getName();
            }
        }
        return "<" + resolutions.get(resolutions.size()-1).getName();
    }

    //Simply returns "file" if numberOfFiles is 1, and "files" otherwise. 
    //Needed in more places than I thought
    public static String getSingularOrPluralOfFile(int numberOfFiles) {
        return numberOfFiles == 1 ? "file" : "files";
    }

    public static void showFileInExplorer(String path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            //On windows 11, this for example already *doesnt* work ...
            Desktop.getDesktop().browseFileDirectory(new File(path));
        } else {
            //... so we keep our little fallback code here
            if (Common.isWindows()) {
                try {
                    nonDeprecatedExec("explorer.exe /select,\"" + path + "\"");
                } catch (IOException e) {
                    System.out.println("Could not show file " + path + " in explorer:");
                    e.printStackTrace();
                }
            }
        }
    }

    //https://stackoverflow.com/questions/6686592/runtime-exec-on-argument-containing-multiple-spaces
    //The suppresion here is the way better solution to this sad mess. 
    @SuppressWarnings("deprecation")
    private static void nonDeprecatedExec(String command) throws IOException {
        //Runtime.getRuntime().exec(command.split(" ")); <- will not work if the path contains multiple spaces in a row

        //all the hassle only because simple exec is now depricated in java 18, but exec with a string array is buggy as well.
        //the more i google the more i feel like i am working with a dying language, even though the issue above is from 2011, the deprecation caused all the trouble in the first place

        Runtime.getRuntime().exec(command); //well stay with this..
    }

    public static void showDirInExplorer(String path) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(new File(path));
            } catch (IOException e) {
                System.out.println("Could not open dir " + path);
                e.printStackTrace();
            }
        } else {
            if (Common.isWindows()) {
                try {
                    nonDeprecatedExec("explorer.exe " + path);
                } catch (IOException e) {
                    System.out.println("Could not open dir " + path);
                    e.printStackTrace();
                }
            }
        }
    }

    //Moves all 
    public static boolean moveToSystemTrash(ArrayList<String> paths) {
        if (Desktop.getDesktop().isSupported(Action.MOVE_TO_TRASH)) {
            for (String p : paths) {
                try {
                    if (!Desktop.getDesktop().moveToTrash(new File(p))) {
                        System.out.println("Could not move file " + p + " to trash.");
                        return false;
                    }
                } catch (Exception e) {
                    System.out.println("Exception when moving " + p + " to trash:");
                    e.printStackTrace();
                    return false;
                }
            }
            return true;
        } else {
            System.out.println("Moving files to trash is not supported on this OS.");
            return false;
        }
    }

    /*
     * Returns true if the command was executed sucessfully
     * and false if it didnt work (not supported OS, exception)
     */
    public static boolean showOpenWithDialog(String path) {
        if (!Common.isWindows()) return false;

        //runs the cmd command Rundll32 Shell32.dll,OpenAs_RunDLL any-file-name.ext to show the windows 'open with' dialog currentImage:
        try {
            nonDeprecatedExec("Rundll32 Shell32.dll,OpenAs_RunDLL "+path);
            return true;
        } catch (IOException e) {
            System.out.println("Could not show 'open with' dialog for file " + path + ":");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Did I miss anything substantial or did I google wrong? String.format can bring numbers like 0.2 down to a specific
     * number of digits, but it just cannot cut trailing 0 in the end, so format %.2f, 0.2 becomes 0.20. 
     * ... And there is no elegant solution, but I finally got along this:
     */
    public static String formatDoubleUpToDecimalPlaces(double value, int placesAtMost) {
        BigDecimal bd = new BigDecimal(value).setScale(placesAtMost, RoundingMode.HALF_UP);
        bd = bd.stripTrailingZeros();
        return bd.toPlainString();
    }

    public static boolean isExternalSoftware(String softwareName) {
        //Should work well enough to just check if common brand names are contained somewhere inside, cams usually only write "ver 4.2" or "ABC HDR+ 14534"
        softwareName = softwareName.toLowerCase();
        for (String s : relevantSoftwareMatches) {
            if (softwareName.contains(s)) {
                return true;
            }
        }
        return false; 
    }

    public static boolean isNoModifierDown(KeyEvent event) {
        return !event.isAltDown() && !event.isShiftDown() && !event.isShortcutDown();
    }


}
