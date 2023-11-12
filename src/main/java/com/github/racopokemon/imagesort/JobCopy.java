package com.github.racopokemon.imagesort;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class JobCopy extends Job {

    protected File origin, dest;

    public JobCopy(String origin, String dest) {
        this(new File(origin), new File(dest));
    }

    public JobCopy(File origin, File dest) {
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
        target.setCurrentOperation("Copying " + origin.getName());
        try {
            Files.copy(origin.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (Exception e) {
            target.logError("Could not copy " + origin.getAbsolutePath() + " to " + dest.getAbsolutePath() + ": " + Common.formatException(e), false);
            e.printStackTrace();
        } finally {
            target.stepFinished();
        }
    }
}