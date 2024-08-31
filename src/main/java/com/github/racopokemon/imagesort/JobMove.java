package com.github.racopokemon.imagesort;

import java.io.File;
import java.nio.file.Files;

public class JobMove extends Job {

    protected File origin, dest;

    public JobMove(String origin, String dest) {
        this(new File(origin), new File(dest));
    }

    public JobMove(File origin, File dest) {
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
            Files.move(origin.toPath(), dest.toPath());
        } catch (Exception e) {
            target.logError("Could not move " + origin.getAbsolutePath() + " to " + dest.getAbsolutePath() + ": " + Common.formatException(e), false);
            e.printStackTrace();
        } finally {
            target.stepFinished();
        }
    }
    
}