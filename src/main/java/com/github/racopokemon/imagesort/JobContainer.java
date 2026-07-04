package com.github.racopokemon.imagesort;
import java.util.ArrayList;

public abstract class JobContainer extends Job {

    private ArrayList<Job> dependentJobs;

    public JobContainer(ArrayList<Job> dependentJobs) {
        this.dependentJobs = dependentJobs;
    }

    protected void executeAllDependentJobs(JobReportingInterface target) {
        for (Job j : dependentJobs) {
            j.execute(target);
        }
    }

    protected int getNumberOfStepsInDependentJobs() {
        int steps = 0;
        for (Job j : dependentJobs) {
            steps += j.getNumberOfSteps();
        }
        return steps;
    }
    
}