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
    //isCritical is just forwarded from the job. At least JobCreateDirectories support being critical, this is set in the job constructor. 
    //Internally, we take note of critical errors to allow the user to return to the gallery instead of closing the window in such a case. 
    //The idea is, that if images cannot be moved because entire folders are missing, instead of dropping all user annotations, we let them try again. 
    public void logError(String error, boolean isCritical);
}