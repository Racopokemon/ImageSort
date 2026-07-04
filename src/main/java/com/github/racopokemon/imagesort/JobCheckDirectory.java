package com.github.racopokemon.imagesort;
import java.io.File;
import java.util.ArrayList;

/**
 * Checks, whether a given directory exists, and then performs all dependent jobs (that are probably inside this folder)
 */
public class JobCheckDirectory extends JobContainer {

    private File directory;

    public JobCheckDirectory(String dir, ArrayList<Job> dependentJobs) {
        this(new File(dir), dependentJobs);
    }

    public JobCheckDirectory(File dir, ArrayList<Job> dependentJobs) {
        super(dependentJobs);
        this.directory = dir;
    }

    @Override
    public int getNumberOfSteps() {
        return getNumberOfStepsInDependentJobs() + 1;
    }

    @Override
    public void execute(JobReportingInterface target) {
        target.setCurrentOperation("Checking " + directory.getName() + "/");
        if (!Common.isValidFolder(directory)) {
            target.logError("Folder " + directory.getAbsolutePath() + " does not exist / is not a valid folder. ");
            target.stepsFinished(getNumberOfStepsInDependentJobs()+1);
            return;
        }
        setSuccessful();
        target.stepFinished();
        executeAllDependentJobs(target);
    }
}