package com.github.racopokemon.imagesort;
import java.util.ArrayList;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

//Performs the file operations and displays a log of the operations performed
public class FileOperationsWindow extends Stage implements JobReportingInterface {

    //If true, we do a thread.sleep after every file operation. Solely for debug purposes, see if multithreading and progress bar works etc.
    private static final boolean SLOW_PROGRESS_DOWN = false;
    
    private boolean finished = true;
    private int stepsFinished = 0;
    private int overallSteps;
    private String errorText = "";
    private String currentOperation = "There are no jobs assigned yet, probably coming right away";

    private boolean error = false; //Set to true once the first error occurs, therefore equal to checking errorText != ""
    private boolean guiUpdatedForError = false;
    private boolean showGalleryAgain = false; //can be set to true by user choice when closing.
    
    private ArrayList<Job> jobs;
    private boolean autoClose;
    
    private AnimationTimer timer;
    
    /**
     * Initializes the window. 
     */
    public FileOperationsWindow(ArrayList<Job> jobs, boolean autoCloseOnFinish, Stage parentStage) {
        this.jobs = jobs;
        this.autoClose = autoCloseOnFinish;

        //Make the parent stage dependent on this stage. 
        initOwner(parentStage);
        initModality(Modality.WINDOW_MODAL);

        setTitle("Applying file operations");
        setIconified(false);

        VBox vbox = new VBox(Launcher.SMALL_GAP);

        Label label = new Label("File operations should start soon");
        ProgressBar progress = new ProgressBar();
        progress.setMinHeight(20);
        progress.setPrefWidth(10000000);
        Label errorLabel = new Label("There were errors while moving / coping the files:");
        errorLabel.setStyle("-fx-text-fill: #ff0000; ");
        Font errorLabelFont = errorLabel.getFont();
        //Bold font, same size
        errorLabel.setFont(Font.font(errorLabelFont.getFamily(), FontWeight.BOLD, errorLabelFont.getSize()));
        VBox.setMargin(errorLabel, new Insets(4,0,0,0));
        TextArea area = new TextArea("there were no errors why u even see me?");
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-highlight-text-fill: #000000; -fx-text-fill: #ff0000; ");
        Label finalHintLabel = new Label("Consider that other file types (videos) might also be in this folder.");
        VBox.setMargin(finalHintLabel, new Insets(Launcher.SMALL_GAP, 0, 0, 0));
        Button button = new Button("Close");
        button.setDisable(true);
        button.setOnAction((e) -> {close();});
        Button buttonBackToGallery = new Button("Back to gallery");
        buttonBackToGallery.setDisable(true);
        buttonBackToGallery.setVisible(false);
        buttonBackToGallery.setOnAction((e) -> {showGalleryAgain = true; close();});
        HBox buttonAtTheRight = new HBox(buttonBackToGallery, button);
        buttonAtTheRight.setSpacing(2);
        buttonAtTheRight.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(buttonAtTheRight, new Insets(Launcher.SMALL_GAP, 0, 0, 0));


        vbox.getChildren().addAll(label, progress, buttonAtTheRight);
        VBox.setVgrow(area, Priority.ALWAYS);


        BorderPane root = new BorderPane();
        root.setCenter(vbox);
        BorderPane.setMargin(vbox, new Insets(Launcher.BIG_GAP));

        Scene scene = new Scene(root, 400, 130); 
        setMinWidth(400);
        setMinHeight(130);
        setOnCloseRequest((e) -> {
            if (finished) {
                if (error) showGalleryAgain = true; //clicking X on error should not default to launcher
            } else {
                //If we are still in progress, the window can't be closed
                e.consume();
            }    
        });    
        setScene(scene);

        //the animationTimer is simply called every screen refresh
        timer = new AnimationTimer() {
            public void handle(long nanoTime) {
                boolean threadsafeFinished = finished;
                if (error) {
                    if (!guiUpdatedForError) {
                        vbox.getChildren().add(2, errorLabel);
                        vbox.getChildren().add(3, area);
                        if (getStage().getHeight() < 300) {
                            getStage().setHeight(350);
                        }    
                        getStage().setMinHeight(350);
                        guiUpdatedForError = true;
                        buttonBackToGallery.setVisible(true);
                        //button.setText("Close anyway");
                    }
                    area.setText(errorText);
                    area.positionCaret(errorText.length());
                }    
                if (threadsafeFinished) {
                    timer.stop();
                    button.setDisable(false);
                    buttonBackToGallery.setDisable(false);
                    progress.setProgress(1);
                    label.setText(errorText.equals("") ? "Finished!" : "Finished.");
                    if (error) {
                        area.positionCaret(errorText.length());
                    } else {
                        vbox.getChildren().add(vbox.getChildren().size() - 1, finalHintLabel);
                    }
                    if (autoClose && !error) {
                        getStage().close();
                    } else {
                        button.requestFocus();
                    }
                } else {
                    progress.setProgress(((double)stepsFinished) / overallSteps);
                    label.setText(currentOperation);
                }    
            }    
        };
    }

    @Override
    public void showAndWait() {
        setJobsAndStart(jobs);
        super.showAndWait();
    }

    //weird OO fix ... well well private classes and all
    private Stage getStage() {
        return this;
    }

    public void startJobs() {
        for (Job j : jobs) {
            j.execute(this);
        }
        if (error) {
            errorText += "------\nFinished. Because we encountered errors, you can also choose to return back to the gallery and try again.\n";
            errorText += "(Note however, that this might also be a hassle as some file operations shown in the gallery may have already been performed!)";
        }
        finished = true; 
    }    

    //Debug method that causes artificial delay to test the progress bar and multi threading
    private void debugSleep() {
        if (!SLOW_PROGRESS_DOWN) {return;}
        try {Thread.sleep(400);} catch (Exception e) {}
    }    

    public boolean shouldWeShowTheGalleryAgain() {
        return showGalleryAgain;
    }

    @Override
    public void setCurrentOperation(String operationText) {
        currentOperation = operationText;
    }

    @Override
    public void stepsFinished(int numberOfSteps) {
        stepsFinished += numberOfSteps;
        debugSleep();
    }

    @Override
    public void logError(String error) {
        errorText += ">>> " + error + "\n\n";
        this.error = true;
        System.out.println("Job error: " + error); //it makes sense to also print all error outputs
    }        

    private void setJobsAndStart(ArrayList<Job> jobs) {
        if (!finished) {
            System.out.println("WHAT IS GOING ON? We have not yet finished the old jobs. Cancelling the new job request.");
            return;
        }
        finished = false;
        
        this.jobs = jobs;
        finished = false;
        overallSteps = 0;
        for (Job j : jobs) {
            overallSteps += j.getNumberOfSteps();
        }
        stepsFinished = 0;
        errorText = "";
        currentOperation = "File operations should start soon";
        error = false;
        showGalleryAgain = false;
        
        //start the worker thread
        new Thread(() -> {startJobs();}).start();
        //start updating the gui
        timer.start();
    }

}
