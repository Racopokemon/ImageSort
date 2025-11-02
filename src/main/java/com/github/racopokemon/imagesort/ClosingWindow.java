package com.github.racopokemon.imagesort;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Asking user for target dir & doing the file movement.
 */
public class ClosingWindow extends Dialog<ButtonType> {

    //With all these vars that we need to get from the Gallery, this would be more natural to be an inner class - but Gallery is already too big. 
    private Stage stage;
    
    //operations contains lists of file names that should be copied / moved to certain folders. All lists are contained in another list where you may access all lists with the following indices: 
    //0: images not to move (are essentially ignored in this class)
    //1 to numberOfMoveCategories + 1: images to be moved to their corresponding categories
    //numberOfMoveCategories + 1 to numberOfMoveCategories + numberOfCopyCategories + 1 images to copy, corresponding to the indices
    private ArrayList<ArrayList<String>> operations;
    private int numberOfMoveCategories, numberOfCopyCategories;
    //The app treats all files that have the same name but a different extension as a group that is moved together; 
    //this is already resolved in this hashtable: If there are several files for a name, a list of all additional files exists for the file name
    private Hashtable<String, ArrayList<String>> filesToMoveAlong;
    private File directory;

    private Preferences prefs;
    private TextField textFieldAbsolute;
    private RadioButton radioFolderRelative, radioFolderAbsolute;

    private static ButtonType APPLY_BUTON = new ButtonType("Move and close", ButtonBar.ButtonData.YES);
    private static ButtonType EXIT_BUTTON = new ButtonType("Close", ButtonBar.ButtonData.OTHER);
    private static ButtonType BACK_BUTTON = new ButtonType("Back", ButtonBar.ButtonData.CANCEL_CLOSE);

    public ClosingWindow(Stage stage, ArrayList<ArrayList<String>> operations, Hashtable<String, 
                ArrayList<String>> filesToMoveAlong, int numberOfCategories, int numberOfTicks, File directory) {
        
        this.operations = operations;
        this.filesToMoveAlong = filesToMoveAlong;
        this.numberOfMoveCategories = numberOfCategories;
        this.numberOfCopyCategories = numberOfTicks;
        this.directory = directory; 

        String summaryText, closeHeader;
        summaryText = "Summary: \n\n";
        for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
            if (!operations.get(i).isEmpty()) {
                if (i < numberOfCategories + 1) {
                    summaryText += "   move " + operations.get(i).size() + " ";
                    summaryText += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                    summaryText +=  "/" + i + "\n";
                } else {
                    summaryText += "   copy " + operations.get(i).size() + " ";
                    summaryText += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                    summaryText += "/" + Gallery.getTickName(i - numberOfCategories - 1) + "\n";
                }
            }
        }
        boolean[] operationTypes = getFileOperationTypes(numberOfTicks, numberOfCategories, operations);
        boolean moveOperation = operationTypes[0];
        boolean copyOperation = operationTypes[1];
        
        closeHeader = moveOperation ? copyOperation ? "Move & copy files now?" : "Move files now?" : "Copy files now?";
        
        this.setTitle(closeHeader);

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


        Label info1 = new Label("Move/copy files to");
        info1.setWrapText(true);
        VBox.setVgrow(info1, Priority.NEVER);

        Label info2 = new Label(summaryText);
        info2.setWrapText(true);
        VBox.setVgrow(info2, Priority.NEVER);

        VBox dialogContent = new VBox(Launcher.SMALL_GAP, info1, radioFolderRelative, radioFolderAbsolute, folderBox, info2);
        //dialogContent.setPadding(new Insets(14));
        
        DialogPane dialogPane = new DialogPane() {
            protected Node createButtonBar() {
                Node bar = super.createButtonBar();
                ((ButtonBar)bar).setButtonOrder("C++UY"); 
                return bar;
            };
        };
        dialogPane.setContent(dialogContent);
        dialogPane.getButtonTypes().addAll(BACK_BUTTON, EXIT_BUTTON, APPLY_BUTON);
        this.setDialogPane(dialogPane);
        updateButtons();
    }

    private void updateButtons() {
        File currentFolder =  new File(textFieldAbsolute.getText());
        getDialogPane().lookupButton(APPLY_BUTON).setDisable(!Common.isValidFolder(currentFolder));
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

        prefs.putBoolean("folderRelative", radioFolderRelative.isSelected());
        prefs.put("folderPath", textFieldAbsolute.getText());

        if (!result.isPresent() || 
                    result.get() == BACK_BUTTON) {
            return false; 
        }

        if (result.get() == APPLY_BUTON) {

            File targetDirectory = radioFolderRelative.isSelected() ? directory : new File(textFieldAbsolute.getText());

            if (!Common.isValidFolder(targetDirectory) || Common.tryListFiles(directory) == null) {
                
                Alert alert = new Alert(AlertType.NONE, "The provided directory is invalid. Sending you back to the gallery. \n\n"+textFieldAbsolute.getText(), ButtonType.OK);

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
            for (int i = 0; i < numberOfCopyCategories; i++) {
                ArrayList<String> copyOperations = operations.get(i + numberOfMoveCategories + 1);
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
            for (int i = 1; i < numberOfMoveCategories+1; i++) {
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