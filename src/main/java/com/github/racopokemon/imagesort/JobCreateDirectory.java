package com.github.racopokemon.imagesort;

import java.io.File;
import java.util.ArrayList;

//Creates a given folder, if it does not already exists (both is fine).
//If the folder was created sucessfully, it executes the given list of further jobs (which might be all based on this folder)
//If the folder did not exist and could not be created, all contained jobs are skipped. 
public class JobCreateDirectory extends JobContainer {

    protected File directory;

    public JobCreateDirectory(String directory, ArrayList<Job> dependentJobs) {
        this(new File(directory), dependentJobs);
    }

    public JobCreateDirectory(File directory, ArrayList<Job> dependentJobs) {
        super(dependentJobs);
        this.directory = directory;
    }

    @Override
    public int getNumberOfSteps() {
        return getNumberOfStepsInDependentJobs() + 1;
    }

    @Override
    public void execute(JobReportingInterface target) { 
        target.setCurrentOperation("Creating " + directory.getName() + "/");
        if (directory.exists()) {
            setSuccessful();
        } else {
            try {
                directory.mkdirs();
                setSuccessful();
            } catch (Exception e) {
                target.logError("Could not create folder " + directory.getAbsolutePath() + ": " + Common.formatException(e));
                e.printStackTrace();
                target.stepsFinished(getNumberOfStepsInDependentJobs()+1);
                return;
            }
        }
        target.stepFinished();
        executeAllDependentJobs(target);
    }
    
}