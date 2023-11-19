package com.github.racopokemon.imagesort;

import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.prefs.Preferences;

import javafx.scene.image.Image;

import java.io.File;

//Helper class, common methods that come in handy in several places

public class Common {
    
    private static FilenameFilter filter;
    private static Hashtable<String, Image> resources;

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
    }

    //The weak check, let's see if the base requirements are met
    public static boolean isValidFolder(File f) {
        return f.exists() && f.isDirectory();
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
    public static FilenameFilter getFilenameFilter() {
        if (filter == null) {
            filter = new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    String[] split = name.split("[.]");
                    String suffix = split[split.length-1];
                    return (suffix.equalsIgnoreCase("jpg") || 
                            suffix.equalsIgnoreCase("jpeg")|| 
                            suffix.equalsIgnoreCase("png") ||
                            suffix.equalsIgnoreCase("gif") ||
                            suffix.equalsIgnoreCase("bmp"));
                    //I see the advantages of python and probably other high level languages ...
                    //where you would have a 'ends with' ignore case ...
                    //or could access the last element of split in one line [-1] or .last (maybe c#)
                    //... this is too much writing for something trivial
                }
            };
        }
        return filter;
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

}
