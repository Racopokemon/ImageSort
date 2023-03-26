package com.github.racopokemon.imagesort;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.prefs.*;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.collections.ObservableList;

public class Launcher {

    private static final double SMALL_GAP = 4;
    private static final double BIG_GAP = 16;
    private static final double BIG_INTEND_GAP = 34;

    private static final String CHECK_DELETE_FOLDER_SEL_TEXT = "Also create the 'delete' folder here. (Otherwise, it is created in the images folder)";
    private static final String CHECK_DELETE_FOLDER_UNSEL_TEXT = "Also create the 'delete' folder here. (Right now, it is created in the images folder)";

    private Stage stage;
    private ListView<BrowserItem> listBrowser;
    private TextField textFieldBrowser;
    private TextField textFieldFolder;
    private RadioButton radioFolderRelative, radioOperationMove;
    private Button buttonLaunch;
    private CheckBox checkDeleteFolderAbsolute;
    private CheckBox checkMiscRelaunch;
    private CheckBox checkMiscShowUsage;


    private File fallbackDirectory = new File(System.getProperty("user.home"));
    private File lastValidBrowserDirectory = null;

    private Preferences prefs = Common.getPreferences();

    public void start(Stage stage) {
        this.stage = stage;
        Insets indent = new Insets(0, 0, 0, BIG_GAP);
        Insets indentBig = new Insets(0, 0, 0, BIG_INTEND_GAP);

        Label labelIntro = new Label(
                "This app is an image gallery, where you cycle through all images in a folder you select below. You may assign them to categories. Once you have finished, these files can be automatically moved or copied to folders, corresponding to the category you selected. ");
        Font fontNormal = labelIntro.getFont();
        Font fontItalic = Font.font(fontNormal.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC,
                fontNormal.getSize());
        labelIntro.setFont(fontItalic);
        labelIntro.setWrapText(true);
        VBox.setVgrow(labelIntro, Priority.NEVER);
        Label labelBrowserIntro = new Label("Where are the images stored?");
        labelBrowserIntro.setWrapText(true);

        Button buttonBrowserDirUp = new Button("^");
        textFieldBrowser = new TextField();
        textFieldBrowser.setText(prefs.get("browserPath", fallbackDirectory.getAbsolutePath())); //2nd is the default value if nothing is stored yet
        HBox.setHgrow(textFieldBrowser, Priority.ALWAYS);
        Button buttonBrowserBrowse = new Button("Browse");
        HBox boxBrowserLine = new HBox(buttonBrowserDirUp, textFieldBrowser, buttonBrowserBrowse);

        listBrowser = new ListView<>();
        listBrowser.setPrefHeight(100);
        VBox.setVgrow(listBrowser, Priority.ALWAYS);
        listBrowser.setCellFactory(
            new Callback<ListView<BrowserItem>, ListCell<BrowserItem>>() {
                @Override 
                public ListCell<BrowserItem> call(ListView<BrowserItem> list) {
                    return new BrowserCell();
                }
            }
        );

        VBox boxBrowser = new VBox(SMALL_GAP, labelBrowserIntro, boxBrowserLine, listBrowser);
        VBox.setVgrow(boxBrowser, Priority.ALWAYS);

        Label labelOperation = new Label("Once you finished sorting your files, what should we do with them?");
        labelOperation.setWrapText(true);
        radioOperationMove = new RadioButton("Move to the folder of their category");
        RadioButton radioOperationCopy = new RadioButton("Copy to the folder of their category");
        ToggleGroup groupOperation = new ToggleGroup();
        radioOperationMove.setToggleGroup(groupOperation);
        radioOperationCopy.setToggleGroup(groupOperation);
        if (prefs.getBoolean("operationMove", true)) {
            radioOperationMove.setSelected(true);
        } else {
            radioOperationCopy.setSelected(true);
        }
        VBox.setMargin(radioOperationMove, indent);
        VBox.setMargin(radioOperationCopy, indent);
        radioOperationMove.setMaxWidth(Double.POSITIVE_INFINITY);
        radioOperationCopy.setMaxWidth(Double.POSITIVE_INFINITY);
        VBox boxOperation = new VBox(SMALL_GAP, labelOperation, radioOperationMove, radioOperationCopy);

        Label labelFolder = new Label("Where should we create the category folders?");
        labelFolder.setWrapText(true);
        radioFolderRelative = new RadioButton("Inside the images folder selected above");
        RadioButton radioFolderAbsolute = new RadioButton("In a separate folder:");
        ToggleGroup groupFolder = new ToggleGroup();
        radioFolderRelative.setToggleGroup(groupFolder);
        radioFolderAbsolute.setToggleGroup(groupFolder);
        if (prefs.getBoolean("folderRelative", true)) {
            radioFolderRelative.setSelected(true);
        } else {
            radioFolderAbsolute.setSelected(true);
        }
        VBox.setMargin(radioFolderRelative, indent);
        VBox.setMargin(radioFolderAbsolute, indent);
        radioFolderRelative.setMaxWidth(Double.POSITIVE_INFINITY);
        radioFolderAbsolute.setMaxWidth(Double.POSITIVE_INFINITY);

        textFieldFolder = new TextField();
        textFieldFolder.setText(prefs.get("folderPath", fallbackDirectory.getAbsolutePath()));
        HBox.setHgrow(textFieldFolder, Priority.ALWAYS);
        Button buttonFolderBrowse = new Button("Browse");
        HBox boxFolderBrowserLine = new HBox(textFieldFolder, buttonFolderBrowse);
        VBox.setMargin(boxFolderBrowserLine, indentBig);

        checkDeleteFolderAbsolute = new CheckBox(CHECK_DELETE_FOLDER_UNSEL_TEXT);
        checkDeleteFolderAbsolute.setWrapText(true);
        checkDeleteFolderAbsolute.setMaxWidth(Double.POSITIVE_INFINITY);
        VBox.setMargin(checkDeleteFolderAbsolute, indentBig);
        checkDeleteFolderAbsolute.setSelected(prefs.getBoolean("deleteFolderAbsolute", false));

        VBox boxFolder = new VBox(SMALL_GAP, labelFolder, radioFolderRelative, radioFolderAbsolute,
                boxFolderBrowserLine, checkDeleteFolderAbsolute);

        checkMiscRelaunch = new CheckBox("Reopen this launcher");
        checkMiscRelaunch.setSelected(prefs.getBoolean("miscRelaunch", false));
        checkMiscShowUsage = new CheckBox("Show usage hints when the gallery starts");
        checkMiscShowUsage.setSelected(prefs.getBoolean("miscShowUsage", true));
        VBox miscBox = new VBox(SMALL_GAP, checkMiscRelaunch, checkMiscShowUsage);

        Label labelPermanent = new Label(
                "All settings you're making here are stored throughout sessions for your convenience.");
        labelPermanent.setWrapText(true);
        labelPermanent.setFont(fontItalic);
        buttonLaunch = new Button("LAUNCH GALLERY");
        buttonLaunch.setMaxWidth(Double.POSITIVE_INFINITY); // thats a VERY big boii
        buttonLaunch.setMaxHeight(70);
        buttonLaunch.setTextAlignment(TextAlignment.CENTER);
        VBox.setVgrow(buttonLaunch, Priority.ALWAYS);

        VBox mainVertical = new VBox(BIG_GAP,
                labelIntro,
                boxBrowser,
                boxOperation,
                boxFolder,
                miscBox,
                labelPermanent, 
                buttonLaunch);
        StackPane root = new StackPane(mainVertical);
        StackPane.setMargin(mainVertical, new Insets(14));

        Scene scene = new Scene(root, 520, 800);

        checkDeleteFolderAbsolute.disableProperty().bind(radioFolderRelative.selectedProperty());
        boxFolderBrowserLine.disableProperty().bind(radioFolderRelative.selectedProperty());
        radioFolderRelative.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("folderRelative", newV);
            updateLaunchButton();
        });
        radioOperationMove.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("operationMove", newV);
            updateLaunchButton();
        });

        checkDeleteFolderAbsolute.selectedProperty().addListener((e) -> {
            boolean selected = checkDeleteFolderAbsolute.isSelected();
            if (selected) {
                checkDeleteFolderAbsolute.setText(CHECK_DELETE_FOLDER_SEL_TEXT);
            } else {
                checkDeleteFolderAbsolute.setText(CHECK_DELETE_FOLDER_UNSEL_TEXT);
            }
            prefs.putBoolean("deleteFolderAbsolute", selected);
        });

        buttonBrowserBrowse.setOnAction((e) -> {
            boolean success = showBrowserDialogForTextField("Select your image directory", textFieldBrowser);
            if (success) {
                updateBrowser();
            }
        });

        buttonBrowserDirUp.setOnAction((e) -> {
            File f = new File(textFieldBrowser.getText());
            if (!Common.isValidFolder(f)) {
                return;
            }

            String parent = f.getParent();
            if (parent == null && Common.isWindows()) {
                parent = "";
            }
            if (parent != null) {
                textFieldBrowser.setText(parent);
                updateBrowser();
                
                String name = f.getName();
                if (name.equals("") && Common.isWindows()) { //special case for the base folders C:\ etc
                    name = f.getAbsolutePath();
                }
                int index = listBrowser.getItems().indexOf(new BrowserItem(name, true));
                if (index != -1) {
                    listBrowser.getSelectionModel().select(index);
                    listBrowser.scrollTo(index);
                }

                listBrowser.requestFocus();
            }
        });
        textFieldBrowser.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                //focus left!
                updateBrowser();
            }
        });
        textFieldBrowser.setOnAction((e) -> {updateBrowser();});
        textFieldFolder.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                //focus left!
                prefs.put("folderPath", textFieldFolder.getText());
                updateLaunchButton();
            }
        });
        textFieldFolder.setOnAction((e) -> {
            prefs.put("folderPath", textFieldFolder.getText());
            updateLaunchButton();
        });
        
        listBrowser.setOnMouseClicked((e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                browserEmptyClicked();
            }
            //Actually only happens when the listview is completely empty, otherwise you click empty cells. 
            //The BrowserCell is handling all other click events. 
        });
        listBrowser.getSelectionModel().selectedItemProperty().addListener((e) -> {
            updateLaunchButton();
        });
        buttonFolderBrowse.setOnAction((e) -> {
            boolean success = showBrowserDialogForTextField("Select the target directory", textFieldFolder);
            if (success) {
                prefs.put("folderPath", textFieldFolder.getText());
            }
        });
        
        updateBrowser(); //a start path is already written to the textFieldBrowser at its creation. 

        checkMiscRelaunch.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("miscRelaunch", newV);
        });
        checkMiscShowUsage.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("miscShowUsage", newV);
        });

        buttonLaunch.setOnAction((e) -> {
            launch();
        });
        
        updateLaunchButton();

        stage.setScene(scene);
        stage.setTitle("Launcher");
        stage.getIcons().add(Common.getRessource("logo"));
        stage.show();

        buttonBrowserBrowse.requestFocus();
    }

    private void launch() {
        String deleteSuffix = FileSystems.getDefault().getSeparator() + "delete";
        File directory = getCurrentlySelectedDirectory();
        File delDirectory = new File(textFieldBrowser.getText() + deleteSuffix);
        File targetDirectory = directory;
        if (!radioFolderRelative.isSelected()) {
            targetDirectory = new File(textFieldFolder.getText());
            if (checkDeleteFolderAbsolute.isSelected()) {
                delDirectory = new File(textFieldFolder.getText() + deleteSuffix);
            }
        }

        ArrayList<File> foldersToCheck = new ArrayList<>();
        foldersToCheck.add(directory);
        if (targetDirectory != directory) foldersToCheck.add(targetDirectory);
        for (File f : foldersToCheck) {
            if (Common.tryListFiles(f) == null) {
                Alert alert = new Alert(AlertType.NONE, "Could not launch the Gallery: \nWe can't read folder \n"+f.getAbsolutePath(), ButtonType.OK);
                alert.setHeaderText("Could not launch");
                alert.showAndWait();
                return;
            }
        }

        if (getNumberSupportedImages(directory) == 0) {
            Alert alert = new Alert(AlertType.NONE, "Could not launch the Gallery: \nWe could find no supported files in folder \n"+textFieldBrowser.getText(), ButtonType.OK);
            alert.setHeaderText("Could not launch");
            alert.showAndWait();
            return;
        }

        stage.close();

        new Gallery().start(directory, targetDirectory, delDirectory, 
                !radioOperationMove.isSelected(), checkMiscRelaunch.isSelected(), checkMiscShowUsage.isSelected());
    }

    private boolean showBrowserDialogForTextField(String title, TextField field) {
        DirectoryChooser folderDirChooser = new DirectoryChooser();
        folderDirChooser.setTitle(title);

        File f = new File(field.getText());
        if (Common.isValidFolder(f)) {
            folderDirChooser.setInitialDirectory(f);
        }
        File dir = folderDirChooser.showDialog(stage);
        boolean successful = dir != null;
        if (successful) {
            field.setText(dir.getAbsolutePath());
        }
        return successful;
    }

    //Returns either the base directory in the browser list, if no element is selected, or the selected directory in the browser list. 
    private File getCurrentlySelectedDirectory() {
        boolean folderSelected = false;
        BrowserItem selectedItem = null;
        if (!listBrowser.getSelectionModel().isEmpty()) {
            selectedItem = listBrowser.getSelectionModel().getSelectedItem();
            if (selectedItem.isFolder) {
                folderSelected = true;
            }
        }
        if (folderSelected) {
            return new File(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + selectedItem.name);
        } else {
            return new File(textFieldBrowser.getText());
        }

    }

    private void updateBrowser() {
        File newDir = new File(textFieldBrowser.getText());
        if (newDir.equals(lastValidBrowserDirectory)) {
            //nothing changed
            return;
        }

        ObservableList<BrowserItem> items = listBrowser.getItems();
        items.clear();

        ArrayList<File> fallbackStack = new ArrayList<>();
        fallbackStack.add(fallbackDirectory);
        if (lastValidBrowserDirectory != null) fallbackStack.add(lastValidBrowserDirectory);
        fallbackStack.add(newDir);
        
        File[] contents = null; //this crashes sometimes, even though the directory exists, can read, etc.

        int i = fallbackStack.size();
        File f = null;
        boolean loop = true;
        do {
            i--;
            f = fallbackStack.get(i);
            contents = Common.tryListFiles(f);
            loop = contents == null; 
        } while (loop && i > 0);
        if (loop) { //i == 0, none of the directories worked
            System.out.println("bro as if you just deleted your home folder");
            lastValidBrowserDirectory = null;
            return;
        }
        lastValidBrowserDirectory = f;
        boolean windowsRootMode = f.toString().equals("");
        if (windowsRootMode) {
            textFieldBrowser.setText(""); //Hacky special case: Windows roots are shown for empty string
        } else {
            textFieldBrowser.setText(f.getAbsolutePath());
        }
        prefs.put("browserPath", f.getAbsolutePath());

        FilenameFilter filter = Common.getFilenameFilter();
        int currentImageCount = 0;
        ArrayList<String> dirs = new ArrayList<>();
        for (File file : contents) {
            if (Common.isValidFolder(file)) {
                if (windowsRootMode) {
                    dirs.add(file.toString());
                } else {
                    dirs.add(file.getName());
                }
            } else if (filter.accept(f, file.getName())) {
                currentImageCount++;
            }
        }
        Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
        for (String s : dirs) {
            items.add(new BrowserItem(s, true));
        }
        if (currentImageCount != 0) {
            items.add(new BrowserItem(currentImageCount + " supported files", false));
        }
        updateLaunchButton();
    }

    private int getNumberSupportedImages(File directory) {
        FilenameFilter filter = Common.getFilenameFilter();
        int number = 0;
        for (File f : Common.tryListFiles(directory)) {
            if (!f.isDirectory() && filter.accept(directory, f.getName())) {
                number++;
            }
        }
        return number;
    }

    private void updateLaunchButton() {
        boolean relative = radioFolderRelative.isSelected();
        boolean move = radioOperationMove.isSelected();
        
        File mainDir = getCurrentlySelectedDirectory();
        boolean mainDirInvalid = !Common.isValidFolder(mainDir);
        boolean absoulteDirInvalid = false;

        String text = "LAUNCH\n for ";

        int imageCount = 0;
        if (mainDirInvalid) {
            text += "invalid directory";
        } else {
            text += "'" + mainDir.getName() + "' ";
            imageCount = getNumberSupportedImages(mainDir);
            if (imageCount == 0) {
                text += "(no valid files found!)";
            } else {
                text += "(" + imageCount + " vaild files)";
            }
        }
        text += "\n";
        if (move) {
            text += "MOVE ";
        } else {
            text += "COPY ";
        }
        File absoluteDir = null;
        if (relative) {
            text += "the same directory";
        } else {
            absoluteDir = new File(textFieldFolder.getText());
            absoulteDirInvalid = !Common.isValidFolder(absoluteDir);
            if (absoulteDirInvalid) {
                text += "to invalid directory";
            } else {
                text += "to '" + absoluteDir.getName() + "'";
            }
        }

        boolean disable = mainDirInvalid;
        disable |= absoulteDirInvalid;
        disable |= imageCount == 0;

        buttonLaunch.setDisable(disable);
        buttonLaunch.setText(text);
    }

    private void browserEmptyClicked() {
        listBrowser.getSelectionModel().clearSelection();
    }

    private class BrowserItem {
        public String name;
        public boolean isFolder;
        public BrowserItem(String name, boolean isFolder) {
            this.name = name;
            this.isFolder = isFolder;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BrowserItem) {
                BrowserItem i = (BrowserItem) o;
                return i.isFolder == isFolder && i.name.equals(name);
            } else {
                return false;
            }
        }
    }

    private class BrowserCell extends ListCell<BrowserItem> {
        public BrowserCell() {
            this.setOnMouseClicked((e) -> {
                if (isEmpty() || getItem() == null) {
                    browserEmptyClicked();
                } else if (getItem().isFolder) {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        if (isEmpty()) {
                            browserEmptyClicked();
                        } else {
                            if (e.getClickCount() == 2) {
                                textFieldBrowser.setText(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + getItem().name);
                                updateBrowser(); //cheap and simple. We literally just write the new path into the text field, updateBrowser then does the validation. 
                            }
                        }
                    }
                }
                //I like that even if every list item now has its own listener ...
                //double clicks between two elements still are counted
                //and also two double clicks in a row are not counted, as the count goes up to 4. 
                //I *could* fix this ... but no
                e.consume(); //Otherwise, the onMouseClicked in the ListView is also called
            });
        }
        
        @Override
        public void updateItem(BrowserItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setGraphic(null);
                setText(null);
            } else {
                ImageView view = new ImageView(Common.getRessource(item.isFolder ? "folder" : "image"));
                view.setFitHeight(22);
                view.setPreserveRatio(true);
                setGraphic(view);
                setText(item.name);
            }
        }
    }
}
