package com.github.racopokemon.imagesort;
import java.util.ArrayList;

public abstract class JobCheckDirectory extends JobContainer {

    public JobCheckDirectory(ArrayList<Job> dependentJobs, boolean isCritical) {
        super(dependentJobs, isCritical);
        //TODO Auto-generated constructor stub
    }
    //abstract only so that it compiles rn
}