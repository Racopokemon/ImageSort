package com.github.racopokemon.imagesort;

import java.io.File;
import java.util.ArrayList;

//Creates a given folder, if it does not already exists (both is fine).
//If the folder was created sucessfully, it executes the given list of further jobs (which might be all based on this folder)
//If the folder did not exist and could not be created, all contained jobs are skipped. 
public class JobCreateDirectory extends Job {

    protected File directory;
    protected ArrayList<Job> dependentJobs;

    public JobCreateDirectory(File directory, ArrayList<Job> dependentJobs) {
        super();
        this.directory = directory;
        this.dependentJobs = dependentJobs;

        if (dependentJobs == null) {
            dependentJobs = new ArrayList<>();
        }
    }

    @Override
    public int getNumberOfSteps() {
        int steps = 1; //creating the folder is 1 step
        for (Job j : dependentJobs) {
            steps += j.getNumberOfSteps(); //and then there is all dependent steps
        }
        return steps;
    }

    @Override
    public void execute(JobReportingInterface target) { 
        if (!directory.exists()) {
            try {
                directory.mkdir();
            } catch (Exception e) {
                target.logError("Could not create folder " + directory.getAbsolutePath() + ": " + Common.formatException(e) 
                        + "\nWhen you close this progress window, you will be back at the gallery to try again. "+
                        "Note however, that some of the file operations may have been executed already\n");
                System.out.println("Could not create folder " + directory.getAbsolutePath());
                e.printStackTrace();
                showGalleryAgain = true; //<-- how do i even solve this? is this even a good idea? ... consider this again
                int steps = 0;
                for (Job j : dependentJobs) {
                    steps += j.getNumberOfSteps();
                }
                target.stepsFinished(steps);
                return;
            }
            for (Job j : dependentJobs) {
                j.execute(target);
            }
        }
    }
    
}