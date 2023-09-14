package com.github.racopokemon.imagesort;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
//Performs the file operations and displays a log of the operations performed
import java.util.ArrayList;
import java.util.Hashtable;

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
import javafx.stage.Stage;

public class FileOperationsWindow extends Stage {

    //If true, we do a thread.sleep after every file operation. Solely for debug purposes, see if multithreading and progress bar works etc.
    private static final boolean SLOW_PROGRESS_DOWN = false;

    private boolean finished = false;
    private int stepsFinished = 0;
    private int overallSteps;
    private String errorText = "";
    private String currentOperation = "File operations should start soon";

    private boolean guiUpdatedForError = false;

    //In case of a big error, we return true, so that the user might fix it manually. 
    private boolean showGalleryAgain = false;

    private int numberOfTicks, numberOfCategories;
    private File imageDirectory, targetDirectory;

    private ArrayList<ArrayList<String>> operations;
    private Hashtable<String, ArrayList<String>> filesToCopyAlong;

    private AnimationTimer timer;

    public FileOperationsWindow(ArrayList<ArrayList<String>> operations, Hashtable<String, ArrayList<String>> filesToMoveAlong, int numberOfCategories, int numberOfTicks, File imageDirectory, File targetDirectory) {
        this.operations = operations;
        this.numberOfCategories = numberOfCategories;
        this.numberOfTicks = numberOfTicks;
        this.imageDirectory = imageDirectory;
        this.targetDirectory = targetDirectory;
        this.filesToCopyAlong = filesToMoveAlong;

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
        TextArea area = new TextArea("there were no errors why u even see me?");
        area.setEditable(false);
        area.setWrapText(true);
        area.setStyle("-fx-highlight-text-fill: #000000; -fx-text-fill: #ff0000; ");
        Label finalHintLabel = new Label("Consider that other file types (videos) might also be in this folder.");
        VBox.setMargin(finalHintLabel, new Insets(Launcher.SMALL_GAP, 0, 0, 0));
        Button button = new Button("Close");
        button.setDisable(true);
        HBox buttonAtTheRight = new HBox(button);
        buttonAtTheRight.setAlignment(Pos.CENTER_RIGHT);
        button.setOnAction((e) -> {close();});
        VBox.setMargin(buttonAtTheRight, new Insets(Launcher.SMALL_GAP, 0, 0, 0));

        vbox.getChildren().addAll(label, progress, buttonAtTheRight);
        VBox.setVgrow(area, Priority.ALWAYS);

        System.out.println(" TODO Write this to the end of the log on finish: ''");

        BorderPane root = new BorderPane();
        root.setCenter(vbox);
        BorderPane.setMargin(vbox, new Insets(Launcher.BIG_GAP));

        Scene scene = new Scene(root, 400, 130); 
        setMinWidth(400);
        setMinHeight(130);
        setOnCloseRequest((e) -> {
            if (!finished) {
                //If we are still in progress, the window can't be closed
                e.consume();
            }
        });
        setScene(scene);

        overallSteps = 0;
        for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
            overallSteps += operations.get(i).size();
        }
        
        //start the worker thread
        new Thread(() -> {executeAllFileOperations();}).start();
        
        //the animationTimer is simply called every screen refresh
        timer = new AnimationTimer() {
            public void handle(long nanoTime) {
                if (!errorText.equals("")) {
                    if (!guiUpdatedForError) {
                        guiUpdatedForError = true;
                        vbox.getChildren().add(2, errorLabel);
                        vbox.getChildren().add(3, area);
                        if (getStage().getHeight() < 300) {
                            getStage().setHeight(300);
                        }
                        getStage().setMinHeight(300);
                    }
                    if (showGalleryAgain) {
                        button.setText("Back to gallery");
                    }
                    area.setText(errorText);
                }
                if (finished) {
                    timer.stop();
                    button.setDisable(false);
                    progress.setProgress(1);
                    label.setText(errorText.equals("") ? "Finished!" : "Finished.");
                    if (errorText.equals("")) {
                        vbox.getChildren().add(vbox.getChildren().size() - 1, finalHintLabel);
                    }
                    button.requestFocus();
                } else {
                    progress.setProgress(((double)stepsFinished) / overallSteps);
                    label.setText(currentOperation);
                }
            }
        };
        timer.start();
    }

    //weird OO fix ... well well private classes and all
    private Stage getStage() {
        return this;
    }

    public void executeAllFileOperations() {
        if (!Common.isValidFolder(targetDirectory)) {
            errorText = ">>> The target directory is not vaild: " + targetDirectory.getAbsolutePath() 
                    + "\nNo file operations were performed. When you close this window, the gallery remains open for you to try again.\n";
            showGalleryAgain = true;
            finished = true;
            return;
        }

        try {
            //first COPY files
            for (int i = numberOfCategories + 1; i < numberOfCategories + numberOfTicks + 1; i++) {
                ArrayList<String> allFilesInTick = operations.get(i);
                if (allFilesInTick.isEmpty()) {
                    continue;
                }
                //if folder doesnt exist: create. 
                String tickName = Gallery.getTickName(i - numberOfCategories - 1);
                String destPath = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + tickName;
                currentOperation = "Creating folder " + tickName;
                if (tryCreateFolder(destPath)) {
                    //creation was successfull: Lets move all files now!
                    copyAllFiles(allFilesInTick, destPath);
                } else {
                    //error while creating..
                    stepsFinished += allFilesInTick.size();
                    break;
                }
            }

            //then MOVE the files
            for (int i = 1; i < numberOfCategories + 1; i++) {
                ArrayList<String> allFilesInCategory = operations.get(i);
                if (allFilesInCategory.isEmpty()) {
                    continue;
                }
                //if folder doesnt exist: create. 
                String destPath = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + i;
                currentOperation = "Creating folder " + i;
                if (tryCreateFolder(destPath)) {
                    //creation was successfull: Lets move all files now!
                    moveAllFiles(allFilesInCategory, destPath);
                } else {
                    //error while creating..
                    stepsFinished += allFilesInCategory.size();
                    break;
                }
            }
        } finally {
            //stepsFinished = overallSteps; //lets have a little bit trust in this code and hope it manages this simple maths also without us helping here..
            finished = true;
        }
    }

    //Returns true if the creation was sucessful, false otherwise. On error, the errorText is already written
    private boolean tryCreateFolder(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (Exception e) {
                errorText += ">>> Could not create folder " + dirPath + ": " + formatException(e) 
                        + "\nWhen you close this progress window, you will be back at the gallery to try again. Note however, that some of the file operations may have been executed already\n";
                System.out.println("Could not create folder " + dirPath);
                e.printStackTrace();
                showGalleryAgain = true;
                return false;
            }
        }
        return true;
    }

    private void moveAllFiles(ArrayList<String> allFilesInCategory, String destPath) {
        for (String key : allFilesInCategory) {
            ArrayList<String> filesToMove = new ArrayList<>();
            filesToMove.add(key);
            if (filesToCopyAlong.containsKey(key)) {
                filesToMove.addAll(filesToCopyAlong.get(key));
            }
            for (String fileToMove : filesToMove) {
                currentOperation = "Moving " + fileToMove;
                File origin = new File(imageDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + fileToMove);
                File dest = new File(destPath + FileSystems.getDefault().getSeparator() + fileToMove);
                try {
                    origin.renameTo(dest);
                } catch (Exception e) {
                    errorText += ">>> Could not move " + fileToMove + " to " + destPath + ": " + formatException(e) + "\n";
                    System.out.println("Could not move " + fileToMove + " to " + destPath);
                    e.printStackTrace();
                }
            }
            debugSleep();
            stepsFinished++;
        }
    }

    private void copyAllFiles(ArrayList<String> allFilesInCategory, String destPath) {
        for (String key : allFilesInCategory) {
            ArrayList<String> filesToCopy = new ArrayList<>();
            filesToCopy.add(key);
            if (filesToCopyAlong.containsKey(key)) {
                filesToCopy.addAll(filesToCopyAlong.get(key));
            }
            for (String fileToCopy : filesToCopy) {
                currentOperation = "Copying " + fileToCopy;
                File origin = new File(imageDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + fileToCopy);
                File dest = new File(destPath + FileSystems.getDefault().getSeparator() + fileToCopy);
                try {
                    Files.copy(origin.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                } catch (Exception e) {
                    errorText += ">>> Could not copy " + fileToCopy + " to " + destPath + ": " + formatException(e) + "\n";
                    System.out.println("Could not copy " + fileToCopy + " to " + destPath);
                    e.printStackTrace();
                }
            }
            debugSleep();
            stepsFinished++;
        }
    }

    private String formatException(Exception e) {
        return e.getClass() + " - '" + e.getMessage() + "'";
    }

    //Debug method that causes artificial delay to test the progress bar and multi threading
    private void debugSleep() {
        if (!SLOW_PROGRESS_DOWN) {return;}
        try {Thread.sleep(400);} catch (Exception e) {}
    }

    public boolean shouldWeShowTheGalleryAgain() {
        return showGalleryAgain;
    }

}
