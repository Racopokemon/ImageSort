package com.github.racopokemon.imagesort;

import java.io.FilenameFilter;
import java.util.prefs.Preferences;
import java.io.File;

//Helper class, common methods that come in handy in several places

public class Common {
    
    private static FilenameFilter filter;

    //Weak check, let's see if the base requirements are met
    public static boolean isValidFolder(File f) {
        return f.exists() && f.isDirectory();
    }

    //Stronger check if the dir is valid, if sucessfull, returns an array of containing files. 
    //Also, weird warning: Giving a File("") in here returns the windows root dirs under windows. 
    public static File[] tryListFiles(File f) {
        if (f.toString().equals("") && isWindows()) {
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
                            suffix.equalsIgnoreCase("jpeg") || 
                            suffix.equalsIgnoreCase("png"));
                    //I see the advantages of python and probably other high level languages ...
                    //where you would have a 'ends with' ignore case ...
                    //or could access the last element of split in one line [-1] or .last (maybe c#)
                    //... this is too much writing for something trivial
                }
            };
        }

        return filter;
    }

    public static Preferences getPreferences() {
        return Preferences.userNodeForPackage(Main.class);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
