package com.github.racopokemon.imagesort;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Asking user for target dir & doing the file movement.
 */
public class ClosingWindow extends Dialog<ButtonType> {

    //With all these vars that we need to get from the Gallery, this would be more natural to be an inner class - but Gallery is already too big. 
    private Stage stage;
    
    private ArrayList<CheckBox> operationCheckboxes = new ArrayList<>();
    private ArrayList<Button> operationTypeButtons = new ArrayList<>();
    private ArrayList<TextField> folderNameFields = new ArrayList<>();
    
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

    private static final ButtonType EXIT_BUTTON = new ButtonType("Just close", ButtonBar.ButtonData.OTHER);
    private static final ButtonType BACK_BUTTON = new ButtonType("Back", ButtonBar.ButtonData.CANCEL_CLOSE);
    private ButtonType applyButton = new ButtonType("Move and close", ButtonBar.ButtonData.YES);

    public class TextFieldUpdateUI extends TextField {
        public TextFieldUpdateUI(String s) {
            super(s);
            //if focus left -> updateUI
            focusedProperty().addListener((obs, oldV, newV) -> {if (!newV) updateUI();}); 
            setOnAction((e) -> updateUI());
        }
    }

    public ClosingWindow(Stage stage, ArrayList<ArrayList<String>> operations, Hashtable<String, 
                ArrayList<String>> filesToMoveAlong, int numberOfMoveCategories, int numberOfCopyCategories, File directory) {
        this.operations = operations;
        this.filesToMoveAlong = filesToMoveAlong;
        this.numberOfMoveCategories = numberOfMoveCategories;
        this.numberOfCopyCategories = numberOfCopyCategories;
        this.directory = directory; 

        this.setTitle("ImageSort");
        stage.setIconified(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        
        // Create UI for each operation
        int row = 0;
        for (int i = 1; i < numberOfMoveCategories + numberOfCopyCategories + 1; i++) {
            if (!operations.get(i).isEmpty()) {           
                CheckBox enableOperation = new CheckBox();
                enableOperation.setSelected(true);
                operationCheckboxes.add(enableOperation);
                
                Label filesLabel = new Label(operations.get(i).size() + " " + 
                    (operations.get(i).size() == 1 ? "file is" : "files are"));
                
                Button typeButton = new Button(i <= numberOfMoveCategories ? "moved" : "copied");
                typeButton.setOnAction(e -> {
                    if (typeButton.getText().equals("moved")) {
                        typeButton.setText("copied");
                    } else {
                        typeButton.setText("moved");
                    }
                    updateUI();
                });
                operationTypeButtons.add(typeButton);
                
                Label toLabel = new Label("to folder");
                
                TextField folderField = new TextFieldUpdateUI(i <= numberOfMoveCategories ? 
                    String.valueOf(i) : Gallery.getTickName(i - numberOfMoveCategories - 1));
                folderNameFields.add(folderField);
                GridPane.setHgrow(folderField, Priority.ALWAYS);
                
                // Bind disabling
                filesLabel.disableProperty().bind(enableOperation.selectedProperty().not());
                typeButton.disableProperty().bind(enableOperation.selectedProperty().not());
                toLabel.disableProperty().bind(enableOperation.selectedProperty().not());
                folderField.disableProperty().bind(enableOperation.selectedProperty().not());
                
                // Update validation state when checkbox changes
                enableOperation.setOnAction(e -> updateUI());
                
                // Add to grid
                grid.add(enableOperation, 0, row);
                grid.add(filesLabel, 1, row);
                grid.add(typeButton, 2, row);
                grid.add(toLabel, 3, row);
                grid.add(folderField, 4, row);
                
                row++;
            }
        }

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

        radioFolderRelative.selectedProperty().addListener((e) -> updateUI());

        textFieldAbsolute = new TextFieldUpdateUI(prefs.get("folderPath", Launcher.FALLBACK_DIRECTORY.getAbsolutePath()));
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
                updateUI();
            }
        });

        HBox folderBox = new HBox(textFieldAbsolute, buttonFolderBrowse);
        VBox.setMargin(folderBox, new Insets(0, 0, 0, 34));

        folderBox.disableProperty().bind(radioFolderRelative.selectedProperty());

        Label info1 = new Label("Choose destination folder:");
        info1.setWrapText(true);
        VBox.setVgrow(info1, Priority.NEVER);

        Label info2 = new Label("Fine-tune your file operations:");
        info2.setWrapText(true);
        VBox.setVgrow(info2, Priority.NEVER);

        VBox dialogContent = new VBox(Launcher.SMALL_GAP, info1, radioFolderRelative, radioFolderAbsolute, folderBox, info2, grid);
        dialogContent.setPadding(new Insets(14));
        
        DialogPane dialogPane = new DialogPane() {
            protected Node createButtonBar() {
                Node bar = super.createButtonBar();
                ((ButtonBar)bar).setButtonOrder("C++UY"); 
                return bar;
            };
        };
        dialogPane.setContent(dialogContent);
        dialogPane.getButtonTypes().addAll(BACK_BUTTON, EXIT_BUTTON, applyButton);
        this.setDialogPane(dialogPane);
        updateUI();
    }

    private void updateUI() {
        boolean validAbsoluteFolder = true; 
        
        String targetDirectory = directory.getAbsolutePath();
        if (radioFolderAbsolute.isSelected()) {
            targetDirectory = textFieldAbsolute.getText();
            validAbsoluteFolder = Common.isValidFolder(new File(textFieldAbsolute.getText()));
            textFieldAbsolute.setStyle(validAbsoluteFolder ? null : "-fx-text-inner-color: red");
        }
        
        // Check if any operations are enabled and their folder names are valid
        boolean anyInvalidFolderNames = false;
        boolean hasMoves = false;
        boolean hasCopies = false;
        
        for (TextField f : folderNameFields) {
            //default text color
            f.setStyle(null);
        }
        for (int i = 0; i < operationCheckboxes.size(); i++) {
            if (operationCheckboxes.get(i).isSelected()) {
                if (validAbsoluteFolder) {
                    String folderName = folderNameFields.get(i).getText().trim();
                    if (folderName.length() > 0 && 
                            !Common.isValidPath(targetDirectory + FileSystems.getDefault().getSeparator() + folderName)) {
                        anyInvalidFolderNames = true;
                        folderNameFields.get(i).setStyle("-fx-text-inner-color: red");
                    }
                }
                
                if (operationTypeButtons.get(i).getText().equals("moved")) {
                    hasMoves = true;
                } else {
                    hasCopies = true;
                }
            }
        }
        
        // Update apply button text
        String buttonText;
        if (hasMoves && hasCopies) {
            buttonText = "Move, Copy & Close";
        } else if (hasMoves) {
            buttonText = "Move & Close";
        } else if (hasCopies) {
            buttonText = "Copy & Close";
        } else {
            buttonText = "Nothing to do"; // No operations selected
        }
        
        // Update button type if text changed
        if (applyButton == null || !applyButton.getText().equals(buttonText)) {
            applyButton = new ButtonType(buttonText, ButtonBar.ButtonData.YES);
            DialogPane dialogPane = getDialogPane();
            dialogPane.getButtonTypes().remove(dialogPane.getButtonTypes().size() - 1);
            dialogPane.getButtonTypes().add(applyButton);
        }
        
        // Disable button if no valid operations or invalid folders
        Button button = (Button)getDialogPane().lookupButton(applyButton);
        boolean enableButton = (hasMoves || hasCopies) && 
                             (!radioFolderAbsolute.isSelected() || validAbsoluteFolder) &&
                             !anyInvalidFolderNames;
        button.setDisable(!enableButton);
    }

    private boolean executeFileOperations(File targetDirectory) {
        ArrayList<Job> jobs = new ArrayList<>();
        
        // Process each enabled operation
        for (int i = 0; i < operationCheckboxes.size(); i++) {
            if (!operationCheckboxes.get(i).isSelected()) continue;
            
            String folderName = folderNameFields.get(i).getText().trim();
            boolean isCopyOperation = operationTypeButtons.get(i).getText().equals("copied");
            
            ArrayList<String> fileList = operations.get(i + 1); // +1 because index 0 is for non-moved files
            if (fileList.isEmpty()) continue;
            
            ArrayList<Job> operationJobs = new ArrayList<>();
            String originPrefix = directory.getAbsolutePath() + FileSystems.getDefault().getSeparator();
            String destPrefix = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator()
                            + folderName + FileSystems.getDefault().getSeparator();
            
            for (String name : fileList) {
                Job job;
                if (isCopyOperation) {
                    job = new JobCopy(originPrefix + name, destPrefix + name);
                } else {
                    job = new JobMove(originPrefix + name, destPrefix + name);
                }
                operationJobs.add(job);
                
                ArrayList<String> moveAlongList = filesToMoveAlong.get(name);
                if (moveAlongList != null) {
                    for (String moveAlong : moveAlongList) {
                        if (isCopyOperation) {
                            job = new JobCopy(originPrefix + moveAlong, destPrefix + moveAlong);
                        } else {
                            job = new JobMove(originPrefix + moveAlong, destPrefix + moveAlong);
                        }
                        operationJobs.add(job);
                    }
                }
            }
            
            jobs.add(new JobCreateDirectory(destPrefix, operationJobs, true));
        }

        JobCheckDirectory overallCheckJob = new JobCheckDirectory(targetDirectory, jobs);
        ArrayList<Job> finalJobList = new ArrayList<>();
        finalJobList.add(overallCheckJob);

        FileOperationsWindow fileOpWindow = new FileOperationsWindow(finalJobList, false, stage);
        fileOpWindow.showAndWait();
        
        return !fileOpWindow.shouldWeShowTheGalleryAgain();
    }

    /**
     * Returns true if we can close the gallery after this and return to the launcher. 
     * Returns false if there was an error or the user clicked "cancel" and the gallery should stay. 
     */
    public boolean showWindow() {
        initOwner(stage);
        initStyle(StageStyle.UTILITY);
        Optional<ButtonType> result = showAndWait();

        prefs.putBoolean("folderRelative", radioFolderRelative.isSelected());
        prefs.put("folderPath", textFieldAbsolute.getText());

        if (!result.isPresent() || 
                    result.get() == BACK_BUTTON) {
            return false; 
        }

        if (result.get() == applyButton) {
            File targetDirectory = radioFolderRelative.isSelected() ? directory : new File(textFieldAbsolute.getText());

            if (!Common.isValidFolder(targetDirectory) || Common.tryListFiles(directory) == null) {
                Alert alert = new Alert(AlertType.NONE, "The provided directory is invalid. Sending you back to the gallery. \n\n"+textFieldAbsolute.getText(), ButtonType.OK);
                alert.setTitle("Cannot apply file operations");
                alert.initOwner(stage);
                alert.showAndWait();
                return false;
            }

            return executeFileOperations(targetDirectory);
        }
        return true;
    }
}