package com.github.racopokemon.imagesort;

import java.io.File;

public class JobCopy extends Job {

    protected File origin, dest;

    public JobCopy(JobReportingInterface target, File origin, File dest) {
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }
    
}