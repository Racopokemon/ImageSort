package com.github.racopokemon.imagesort;
//While jobs are executed, they report their progress to us, on current operations, finished steps, and possible errors
public interface JobReportingInterface {
    //like "copying image abc", "creating folder abc", ...
    public void setCurrentOperation(String operationText);
    
    //to realize a progress bar, this is called by every job once it is finished of has finished a sub-step
    public default void stepFinished() {
        this.stepsFinished(1);
    }

    //reporting that multiple steps were finished at once (eg if you skip some parts bc of errors)
    public void stepsFinished(int numberOfSteps);

    //like "could not copy abc, storage space", ...
    public void logError(String error);
}