package com.github.racopokemon.imagesort;
import java.io.File;
import java.util.ArrayList;

/**
 * Checks, whether a given directory exists, and then performs all dependent jobs (that are probably inside this folder)
 * Is always critical, why else should we otherwise check for the directory?
 */
public class JobCheckDirectory extends JobContainer {

    private File directory;

    public JobCheckDirectory(String dir, ArrayList<Job> dependentJobs) {
        this(new File(dir), dependentJobs);
    }

    public JobCheckDirectory(File dir, ArrayList<Job> dependentJobs) {
        super(dependentJobs, true);
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
            target.logError("Folder " + directory.getAbsolutePath() + " does not exist / is not a valid folder. ", isCritical());
            target.stepsFinished(getNumberOfStepsInDependentJobs());
            return;
        }
        executeAllDependentJobs(target);
    }
}