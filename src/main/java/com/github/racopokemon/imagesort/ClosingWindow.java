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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
    private ArrayList<Label> operationTypeLabels = new ArrayList<>();
    private ArrayList<TextField> folderNameFields = new ArrayList<>();
    private ArrayList<Integer> operationIndex = new ArrayList<>();
    
    //operations contains lists of file names that should be copied / moved to certain folders. All lists are contained in another list where you may access all lists with the following indices: 
    //0: images not to move (are essentially ignored in this class)
    //1 to numberOfMoveCategories + 1: images to be moved to their corresponding categories
    //numberOfMoveCategories + 1 to numberOfMoveCategories + numberOfCopyCategories + 1 images to copy, corresponding to the indices
    private ArrayList<ArrayList<String>> operations;
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
            //previous approach: update on exit. However, if text fields are red, we expect them to instantly turn black when their input is valid. Therefore we now have ...
            textProperty().addListener((o,oldVal,newVal) -> {
                if (oldVal != newVal) updateUI();
            });

            //if focus left -> updateUI
            //focusedProperty().addListener((obs, oldV, newV) -> {if (!newV) updateUI();}); 
            //setOnAction((e) -> updateUI());
        }
    }

    public ClosingWindow(Stage stage, ArrayList<ArrayList<String>> operations, Hashtable<String, 
                ArrayList<String>> filesToMoveAlong, int numberOfMoveCategories, int numberOfCopyCategories, File directory) {
        this.operations = operations;
        this.filesToMoveAlong = filesToMoveAlong;
        this.directory = directory; 

        this.setTitle("ImageSort");
        stage.setIconified(false);
        
        GridPane grid = new GridPane();
        grid.setHgap(2.5);
        grid.setVgap(2);

        int wasMove = -1;
        
        // Create UI for each operation
        int row = 0;
        for (int i = 1; i < numberOfMoveCategories + numberOfCopyCategories + 1; i++) {
            if (!operations.get(i).isEmpty()) {
                boolean isCopy = i > numberOfMoveCategories;
                operationIndex.add(i);

                CheckBox enableOperation = new CheckBox();
                enableOperation.setSelected(true);
                operationCheckboxes.add(enableOperation);
                
                int moveAlongCount = 0;
                for (String s : operations.get(i)) {
                    ArrayList<String> moveAlongList = filesToMoveAlong.get(s);
                    if (moveAlongList != null) {
                        moveAlongCount += moveAlongList.size();
                    }
                }
                Label moveAlongLabel = new Label(
                    moveAlongCount == 0 ? " " : " " + moveAlongCount + "+");
                moveAlongLabel.setTextFill(Color.SILVER);

                Label operationsLabel = new Label(""+operations.get(i).size());
                
                HBox numberContainer = new HBox(moveAlongLabel, operationsLabel);
                numberContainer.setMaxHeight(0);
                GridPane.setHalignment(numberContainer, HPos.CENTER);
                GridPane.setHgrow(numberContainer, Priority.NEVER);
                numberContainer.setAlignment(Pos.CENTER_RIGHT);

                Label filesLabel = new Label(
                    (operations.get(i).size() == 1 ? "file is" : "files are"));
                
                Label typeLabel;
                if (isCopy) {
                    typeLabel = new Label("copied");
                } else {
                    typeLabel = new Label("moved");
                    typeLabel.setOnMouseClicked(e -> {
                        if (typeLabel.getText().equals("moved")) {
                            typeLabel.setText("copied");
                        } else {
                            typeLabel.setText("moved");
                        }
                        typeLabel.requestFocus();
                        updateUI();
                    });
                }
                operationTypeLabels.add(typeLabel);
                Font originalFont = typeLabel.getFont();
                Font boldFont = Font.font(originalFont.getFamily(), FontWeight.BOLD, originalFont.getSize());
                typeLabel.setFont(boldFont);
                operationsLabel.setFont(boldFont);
                
                HBox labelContainer = new HBox(4, filesLabel, typeLabel, new Label("to folder"));
                GridPane.setValignment(labelContainer, VPos.CENTER);
                labelContainer.setMaxHeight(0);
                
                TextField folderField = new TextFieldUpdateUI(i <= numberOfMoveCategories ? 
                    String.valueOf(i) : Gallery.getTickName(i - numberOfMoveCategories - 1));
                folderNameFields.add(folderField);
                folderField.setPrefColumnCount(5);

                GridPane.setHgrow(folderField, Priority.ALWAYS);
                GridPane.setHgrow(labelContainer, Priority.NEVER);

                Button swapButton = null;
                if (!isCopy) {
                    swapButton = new Button("...");
                    swapButton.setOnAction((e) -> {
                        if (typeLabel.getText().equals("moved")) {
                            typeLabel.setText("copied");
                        } else {
                            typeLabel.setText("moved");
                        }
                        updateUI();
                    });
                    GridPane.setHgrow(swapButton, Priority.NEVER);
                }
                
                // Bind disabling
                numberContainer.disableProperty().bind(enableOperation.selectedProperty().not());
                labelContainer.disableProperty().bind(enableOperation.selectedProperty().not());
                folderField.disableProperty().bind(enableOperation.selectedProperty().not());
                
                // Update validation state when checkbox changes
                enableOperation.setOnAction(e -> {
                    updateUI();
                    enableOperation.requestFocus();
                });
                
                // Add to grid
                grid.add(enableOperation, 0, row);
                grid.add(numberContainer, 1, row);
                grid.add(labelContainer, 2, row);
                grid.add(folderField, 3, row);
                if (swapButton == null) {
                    GridPane.setColumnSpan(folderField, GridPane.REMAINING);
                } else {
                    grid.add(swapButton, 5, row);
                }

                // Make gap between block of move and copy operations
                if (wasMove == 0 && isCopy) {
                    Insets margin = new Insets(6, 0, 0, 0);
                    GridPane.setMargin(enableOperation, margin);
                    GridPane.setMargin(numberContainer, margin);
                    GridPane.setMargin(labelContainer, margin);
                    GridPane.setMargin(folderField, margin);
                    if (swapButton != null) GridPane.setMargin(swapButton, margin);
                }
                wasMove = isCopy ? 1 : 0;
                
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
            File dir = chooser.showDialog(getDialogPane().getScene().getWindow());
            if (dir != null) {
                textFieldAbsolute.setText(dir.getAbsolutePath());
                updateUI();
            }
        });

        HBox folderBox = new HBox(textFieldAbsolute, buttonFolderBrowse);
        VBox.setMargin(folderBox, new Insets(0, 0, 0, 34));

        folderBox.disableProperty().bind(radioFolderRelative.selectedProperty());

        Label info1 = new Label("Choose a destination folder:");
        info1.setWrapText(true);
        VBox.setVgrow(info1, Priority.NEVER);

        Label info2 = new Label("Fine-tune your file operations:");
        info2.setWrapText(true);
        VBox.setVgrow(info2, Priority.NEVER);
        VBox.setMargin(info2, new Insets(Launcher.SMALL_GAP*2, 0, 0, 0));

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
        } else {
            textFieldAbsolute.setStyle(null);
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
                    if (folderName.length() == 0 || 
                            !Common.isValidPath(targetDirectory + FileSystems.getDefault().getSeparator() + folderName)) {
                        anyInvalidFolderNames = true;
                        folderNameFields.get(i).setStyle("-fx-text-inner-color: red");
                    }
                }
                if (isMoveOperation(i)) {
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

    private boolean isMoveOperation(int i) {
        if (operationTypeLabels.size() <= i) {
            return false;
        } else {
            if (operationTypeLabels.get(i).getText().equals("moved")) {
                return true;
            } else {
                return false;
            }
        }
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
        } else {
            return true;
        }
    }
    
    private boolean executeFileOperations(File targetDirectory) {
        ArrayList<Job> moveJobs = new ArrayList<>();
        ArrayList<Job> copyJobs = new ArrayList<>();
        
        // Process each enabled operation
        for (int i = 0; i < operationCheckboxes.size(); i++) {
            if (!operationCheckboxes.get(i).isSelected()) continue;
            
            String folderName = folderNameFields.get(i).getText().trim();
            boolean isCopyOperation = !operationTypeLabels.get(i).getText().equals("moved");
            
            ArrayList<String> fileList = operations.get(operationIndex.get(i)); 
            //some operations may be empty, therefore we have this lookup
            
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
            
            if (isCopyOperation) {
                copyJobs.add(new JobCreateDirectory(destPrefix, operationJobs, true));
            } else {
                moveJobs.add(new JobCreateDirectory(destPrefix, operationJobs, true));
            }
        }

        //Its important to do the copy jobs first - after the move jobs the original files to copy might be gone
        copyJobs.addAll(moveJobs); 
        JobCheckDirectory overallCheckJob = new JobCheckDirectory(targetDirectory, copyJobs);
        ArrayList<Job> finalJobList = new ArrayList<>();
        finalJobList.add(overallCheckJob);

        FileOperationsWindow fileOpWindow = new FileOperationsWindow(finalJobList, false, stage);
        fileOpWindow.showAndWait();
        
        return !fileOpWindow.shouldWeShowTheGalleryAgain();
    }
}