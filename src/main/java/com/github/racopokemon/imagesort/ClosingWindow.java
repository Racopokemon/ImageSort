package com.github.racopokemon.imagesort;

import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Asking user for target dir & doing the file movement.
 */
public class ClosingWindow extends Dialog<ButtonType> {

    //With all these vars that we need to get from the Gallery, this would be more natural to be an inner class - but Gallery is already too big. 
    private Stage stage;
    private ArrayList<ArrayList<String>> operations;
    private int numberOfCategories, numberOfTicks;
    private Hashtable<String, ArrayList<String>> filesToMoveAlong;
    private File directory;

    private Preferences prefs;
    private TextField textFieldAbsolute;
    private RadioButton radioFolderRelative, radioFolderAbsolute;

    public ClosingWindow(Stage stage, ArrayList<ArrayList<String>> operations, Hashtable<String, 
                ArrayList<String>> filesToMoveAlong, int numberOfCategories, int numberOfTicks, File directory) {
        
        this.operations = operations;
        this.filesToMoveAlong = filesToMoveAlong;
        this.numberOfCategories = numberOfCategories;
        this.numberOfTicks = numberOfTicks;
        this.directory = directory; 

        String closeMessage, closeHeader;
        closeMessage = "Should we now do the following?\n\n";
        for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
            if (!operations.get(i).isEmpty()) {
                if (i < numberOfCategories + 1) {
                    closeMessage += "   move " + operations.get(i).size() + " ";
                    closeMessage += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                    closeMessage +=  "targetDirectory.getName()" + "/" + i + "\n";
                } else {
                    closeMessage += "   copy " + operations.get(i).size() + " ";
                    closeMessage += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                    closeMessage += "targetDirectory.getName()" + "/" + Gallery.getTickName(i - numberOfCategories - 1) + "\n";
                }
            }
        }
        boolean[] operationTypes = getFileOperationTypes(numberOfTicks, numberOfCategories, operations);
        boolean moveOperation = operationTypes[0];
        boolean copyOperation = operationTypes[1];
        
        closeHeader = moveOperation ? copyOperation ? "Move & copy files now?" : "Move files now?" : "Copy files now?";
        closeMessage += "\n'No' keeps the files unchanged and closes the gallery, which discards your work here. ";
        
        this.setTitle(closeHeader);

        Label info = new Label(closeMessage);
        info.setWrapText(true);
        VBox.setVgrow(info, Priority.NEVER);

        radioFolderRelative = new RadioButton("In the same folder");
        radioFolderAbsolute = new RadioButton("In a separate folder:");
        ToggleGroup groupFolder = new ToggleGroup();
        radioFolderRelative.setToggleGroup(groupFolder);
        radioFolderAbsolute.setToggleGroup(groupFolder);
        Insets indent = new Insets(0, 0, 0, Launcher.BIG_GAP);
        VBox.setMargin(radioFolderRelative, indent);
        VBox.setMargin(radioFolderAbsolute, indent);
        radioFolderRelative.setMaxWidth(Double.POSITIVE_INFINITY);
        radioFolderAbsolute.setMaxWidth(Double.POSITIVE_INFINITY);

        prefs = Common.getPreferences();
        if (prefs.getBoolean("folderRelative", true)) {
            radioFolderRelative.setSelected(true);
        } else {
            radioFolderAbsolute.setSelected(true);
        }

        textFieldAbsolute = new TextField(prefs.get("folderPath", Launcher.FALLBACK_DIRECTORY.getAbsolutePath()));
        HBox.setHgrow(textFieldAbsolute, Priority.ALWAYS);
        Button buttonFolderBrowse = new Button("Browse");

        buttonFolderBrowse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select target directory");
            File f = new File(textFieldAbsolute.getText());
            if (Common.isValidFolder(f)) {
                chooser.setInitialDirectory(f);
            }
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                textFieldAbsolute.setText(dir.getAbsolutePath());
                updateButtons();
            }
        });

        HBox folderBox = new HBox(textFieldAbsolute, buttonFolderBrowse);
        VBox.setMargin(folderBox, new Insets(0, 0, 0, 34));

        folderBox.disableProperty().bind(radioFolderRelative.selectedProperty());

        textFieldAbsolute.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                //focus left!
                prefs.put("folderPath", textFieldAbsolute.getText());
                updateButtons();
            }
        });
        textFieldAbsolute.setOnAction((e) -> {
            prefs.put("folderPath", textFieldAbsolute.getText());
            updateButtons();
        });

        VBox dialogContent = new VBox(Launcher.SMALL_GAP, info, radioFolderRelative, radioFolderAbsolute, folderBox);
        dialogContent.setPadding(new Insets(Launcher.SMALL_GAP));
        this.getDialogPane().setContent(dialogContent);

        ButtonType yesBtn = new ButtonType("Move and exit", ButtonBar.ButtonData.YES);
        ButtonType noBtn = new ButtonType("Just exit", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Go back", ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().addAll(yesBtn, noBtn, cancelBtn);
    }

    private void updateButtons() {
        File currentFolder =  new File(textFieldAbsolute.getText());
        getDialogPane().lookupButton(ButtonType.YES).setDisable(!Common.isValidFolder(currentFolder));
    }

    //Returns a boolean array with 2 elements: {hasMoveOperations, hasCopyOperations}
    public static boolean[] getFileOperationTypes(int numberOfTicks, int numberOfCategories, ArrayList<ArrayList<String>> operations) {
        boolean moveOperation = false, copyOperation = false;
        for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
            if (!operations.get(i).isEmpty()) {
                if (i < numberOfCategories + 1) {
                    moveOperation = true;
                } else {
                    copyOperation = true;
                }
            }
        }
        return new boolean[] {moveOperation, copyOperation};
    }

    /**
     * Returns true if we can close the gallery after this and return to the launcher. 
     * Returns false if there was an error or the user clicked "cancel" and the gallery should stay. 
     */
    public boolean showWindow() {
        initOwner(stage);
        Optional<ButtonType> result = showAndWait();

        if (!result.isPresent() || 
                    result.get().getButtonData() == ButtonData.CANCEL_CLOSE) {
            return false; 
        }
        prefs.putBoolean("folderRelative", radioFolderRelative.isSelected());
        prefs.put("folderPath", textFieldAbsolute.getText());

        if (result.get().getButtonData() == ButtonData.YES) {

            File targetDirectory = radioFolderRelative.isSelected() ? directory : new File(textFieldAbsolute.getText());

            if (!Common.isValidFolder(targetDirectory)) {
                
                Alert alert = new Alert(AlertType.NONE, "The provided directory is invalid :(\n\n"+textFieldAbsolute.getText(), ButtonType.OK);

                alert.setTitle("Cannot apply file operations");
                alert.initOwner(stage);
                alert.showAndWait();

                return false;
            }

            //create jobs & send them to a file op window
            //and then the gallery closes automatically on return, if we do not consume the event
            
            //turn the user selections into a job list that can be executed by a FileOperationsWindow
            ArrayList<Job> jobs = new ArrayList<>();

            //first COPY files
            for (int i = 0; i < numberOfTicks; i++) {
                ArrayList<String> copyOperations = operations.get(i + numberOfCategories + 1);
                if (!copyOperations.isEmpty()) {
                    ArrayList<Job> copyJobs = new ArrayList<>();
                    String originPrefix = directory.getAbsolutePath() + FileSystems.getDefault().getSeparator();
                    String destPrefix = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator()
                        + Gallery.getTickName(i) + FileSystems.getDefault().getSeparator();
                    for (String name : copyOperations) {
                        copyJobs.add(new JobCopy(originPrefix + name, destPrefix + name));
                        ArrayList<String> copyAlongList = filesToMoveAlong.get(name);
                        if (copyAlongList != null) {
                            for (String copyAlong : copyAlongList) {
                                copyJobs.add(new JobCopy(originPrefix + copyAlong, destPrefix + copyAlong));
                            }
                        }
                    }
                    jobs.add(new JobCreateDirectory(destPrefix, copyJobs, true));
                }
            }

            //then MOVE files
            for (int i = 1; i < numberOfCategories+1; i++) {
                ArrayList<String> moveOperations = operations.get(i);
                if (!moveOperations.isEmpty()) {
                    ArrayList<Job> moveJobs = new ArrayList<>();
                    String originPrefix = directory.getAbsolutePath() + FileSystems.getDefault().getSeparator();
                    String destPrefix = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator()
                        + i + FileSystems.getDefault().getSeparator();
                    for (String name : moveOperations) {
                        moveJobs.add(new JobMove(originPrefix + name, destPrefix + name));
                        ArrayList<String> moveAlongList = filesToMoveAlong.get(name);
                        if (moveAlongList != null) {
                            for (String moveAlong : moveAlongList) {
                                moveJobs.add(new JobMove(originPrefix + moveAlong, destPrefix + moveAlong));
                            }
                        }
                    }
                    jobs.add(new JobCreateDirectory(destPrefix, moveJobs, true));
                }
            }

            JobCheckDirectory overallCheckJob = new JobCheckDirectory(targetDirectory, jobs);
            ArrayList<Job> finalJobList = new ArrayList<>();
            finalJobList.add(overallCheckJob);

            FileOperationsWindow fileOpWindow = new FileOperationsWindow(finalJobList, false, stage);
            fileOpWindow.showAndWait();

            if (fileOpWindow.shouldWeShowTheGalleryAgain()) {
                return false;
            }
        }

        return true;
    }
}