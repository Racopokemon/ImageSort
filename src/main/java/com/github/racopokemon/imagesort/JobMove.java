package com.github.racopokemon.imagesort;

import java.io.File;

public class JobMove extends Job {

    protected File origin, dest;

    public JobMove(JobReportingInterface target, File origin, File dest) {
        super();
        this.origin = origin;
        this.dest = dest;
    }

    @Override
    public int getNumberOfSteps() {
        return 1;
    }

    @Override
    public void execute(JobReportingInterface target) {
        target.setCurrentOperation("Moving " + origin.getName());
        try {
            origin.renameTo(dest);
        } catch (Exception e) {
            target.logError("Could not move " + origin.getAbsolutePath() + " to " + dest.getAbsolutePath() + ": " + Common.formatException(e), false);
            e.printStackTrace();
        } finally {
            target.stepFinished();
        }
    }
    
}