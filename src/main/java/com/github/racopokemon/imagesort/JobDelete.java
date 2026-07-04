package com.github.racopokemon.imagesort;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Desktop;
import java.awt.Desktop.Action;

public class JobDelete extends Job {

    private static final int GROUP_THRESHOLD = 4;
    private ArrayList<String> files;
    private boolean grouped; //Trash every file separately or move them all to a folder that you delete afterwards? 
    private String directory, folderName;

    //Directory: Base dir where we creat a folder where to move all files before deleting the folder. delFolderName: How we name this folder. 
    public JobDelete(ArrayList<String> fileOrDirNames, String baseDirectory, String delFolderName) {
        this.files = fileOrDirNames;
        directory = baseDirectory;
        this.folderName = delFolderName; 
        grouped = fileOrDirNames.size() >= GROUP_THRESHOLD;
    }

    public JobDelete(String baseDirectory, String fileOrDirName) {
        directory = baseDirectory;
        this.files = new ArrayList<>();
        files.add(fileOrDirName);
        grouped = files.size() >= GROUP_THRESHOLD;
    }

    @Override
    public int getNumberOfSteps() {
        if (grouped) {
            return files.size();
        } else {
            return files.size() + 2; //create dir, move all, trash dir
        }
    }

    private static Random rand = null;
    public static String generateFolderName(String base) {
        if (rand == null) {rand = new Random();}
        return String.format("imgsort_%s_%05d", base, rand.nextInt(0,99999));
    }

    @Override
    public void execute(JobReportingInterface target) {
        if (grouped) {
            String dirName = generateFolderName(folderName);
            JobCreateDirectory createJob = new JobCreateDirectory(directory + FileSystems.getDefault().getSeparator() + dirName, null);
            createJob.execute(target);
            if (createJob.wasSuccessful()) {
                //move every file there
                boolean success = true;
                for (String s : files) {
                    String originDir = directory + FileSystems.getDefault().getSeparator() + s;
                    String destDir = directory + FileSystems.getDefault().getSeparator() + dirName + FileSystems.getDefault().getSeparator() + s;
                    JobMove m = new JobMove(originDir, destDir);
                    m.execute(target);
                    if (!m.wasSuccessful()) success = false;
                }
                if (success) {
                    JobDelete d = new JobDelete(directory, dirName);
                    d.execute(target);
                    if (d.wasSuccessful()) {
                        setSuccessful();
                        return;
                    }
                } else {
                    target.stepFinished();
                }
            } else {
                target.stepsFinished(files.size() + 1);
            }
            target.logError("Because of the above error we could not move "+files.size()+" element(s) to the trash ("+folderName+").");
        } else {
            //trash every file separately
            if (Desktop.getDesktop().isSupported(Action.MOVE_TO_TRASH)) {
                boolean success = true;
                for (String file : files) {
                    file = directory + FileSystems.getDefault().getSeparator() + file;
                    target.setCurrentOperation("Moving " + file + " to trash.");
                    try {
                        if (!Desktop.getDesktop().moveToTrash(new File(file))) {
                            target.logError("Could not move " + file + " to trash.");
                            success = false;
                        }
                    } catch (Exception e) {
                        target.logError("Exception when moving " + file + " to trash:\n" + e.toString());
                        e.printStackTrace();
                        success = false;
                    }
                    target.stepFinished();
                }
                if (success) setSuccessful();
            } else {    
                target.logError("Moving files to trash is not supported on this OS. :(");
                target.stepsFinished(files.size());
            }
        }
    }

}
