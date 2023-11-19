package com.github.racopokemon.imagesort;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

public class Launcher {

    public static final double SMALL_GAP = 4;
    public static final double BIG_GAP = 16;
    public static final double BIG_INTEND_GAP = 34;

    private static final String CHECK_DELETE_FOLDER_SEL_TEXT = "Also create the 'delete' folder here. (Otherwise, it is created in the images folder)";
    private static final String CHECK_DELETE_FOLDER_UNSEL_TEXT = "Also create the 'delete' folder here. (Right now, it is created in the images folder)";

    private Stage stage;
    private ListView<BrowserItem> listBrowser;
    private TextField textFieldBrowser;
    private TextField textFieldFolder;
    private RadioButton radioFolderRelative;
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
        //If the users clipboard contains a valid path, we want start from this location. If this is not the case, we use the path used last time, if available. 
        File startPath = readPathFromClipboard();
        if (startPath != null) {
            //validation part: If the directory is valid, check if it contains media files of any kind. 
            //If yes, accepted & delete clipboard. 
            //If no, skip this path.  
            DirectoryContents cont = getDirectoryContents(startPath);
            if (cont.numberOfImages + cont.numberOfRaws + cont.numberOfVideos > 1) {
                Clipboard.getSystemClipboard().clear();
            } else {
                startPath = null;
            }
        }

        //This is not the best coding style though, if our checks here are true but later when calling updateBrowser, it is not true anymore, we dont fall back to 
        //prefs.get browserPath on initialization..
        if (startPath == null) {
            //No clipboard option, use the other options
            textFieldBrowser.setText(prefs.get("browserPath", fallbackDirectory.getAbsolutePath())); //2nd is the default value if nothing is stored yet
        } else {
            textFieldBrowser.setText(startPath.getAbsolutePath());
        }
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
                "Note: When launching this app, we will start at the path, file or folder that is currently in your clipboard. This might save you some seconds.");
        //        "All settings you're making here are stored throughout sessions for your convenience.");
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
            dirUp();
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
            } else if (e.getButton() == MouseButton.BACK) {
                dirUp();
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
                updateLaunchButton();
            }
        });

        checkMiscRelaunch.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("miscRelaunch", newV);
        });
        checkMiscShowUsage.selectedProperty().addListener((obs, oldV, newV) -> {
            prefs.putBoolean("miscShowUsage", newV);
        });

        buttonLaunch.setOnAction((e) -> {
            launch();
        });

        scene.getAccelerators().put(KeyCombination.keyCombination("F5"), () -> {
            textFieldBrowser.setText("-%*:#[]"); //this should be invalid enough (hacky hacky, sorry)
            updateBrowser();
        });

        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+V"), () -> {
            File clipboardContent = readPathFromClipboard();
            if (clipboardContent != null) {
                //This is not the best coding style here. However, we know that readPathFromClipboard has mostly
                //ensured that a dir exists and is valid. We can therefore consume the clipboard once we pasted it, it should be okay. 
                textFieldBrowser.setText(clipboardContent.getAbsolutePath());
                updateBrowser();
                Clipboard.getSystemClipboard().clear();
            }
        });


        stage.setScene(scene);
        stage.setTitle("Launcher");
        stage.getIcons().add(Common.getResource("logo"));
        stage.show();

        //a start path is already written to the textFieldBrowser at its creation, now validate the browser. 
        updateBrowser(); 
        if (textFieldBrowser.getText().equals(fallbackDirectory.getAbsolutePath())) { //if we end up in the default dir, show the folder selection dialog already. 
            buttonFolderBrowse.fireEvent(new ActionEvent());
        }
        updateLaunchButton();


        //buttonBrowserBrowse.requestFocus();
        buttonLaunch.requestFocus();
    }

    private File readPathFromClipboard() {
        File path = null;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasContent(DataFormat.PLAIN_TEXT)) {
            path = new File(clipboard.getString());
        } else if (clipboard.hasContent(DataFormat.FILES)) {
            List<File> filesList = clipboard.getFiles();
            if (filesList.size() > 0) {
                path = filesList.get(0);
            }
        }
        //isDirectory also checks if the dir exists. So if we have a directory OR a file inside a valid directory, we return this, 
        //otherwise the clipboard content was not readable
        if (path != null && !path.isDirectory()) {
            path = path.getParentFile();
            if (path != null && !path.isDirectory()) {
                return null;
            }
        }
        return path;
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

        if (getNumberOfSupportedImages(directory) == 0) {
            Alert alert = new Alert(AlertType.NONE, "Could not launch the Gallery: \nWe could find no supported files in folder \n"+textFieldBrowser.getText(), ButtonType.OK);
            alert.setHeaderText("Could not launch");
            alert.showAndWait();
            return;
        }

        stage.close();

        new Gallery().start(directory, targetDirectory, delDirectory, 
                    checkMiscRelaunch.isSelected(), checkMiscShowUsage.isSelected());
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

        //we will traverse the stack from last to first element to find the first valid folder. 
        ArrayList<File> fallbackStack = new ArrayList<>();
        fallbackStack.add(fallbackDirectory);
        if (lastValidBrowserDirectory != null) fallbackStack.add(lastValidBrowserDirectory);

        fallbackStack.add(newDir);
        //adding all parent folders in reverse order 
        int placementIndex = fallbackStack.size() - 1;
        File parentDir = newDir;
        while ((parentDir = parentDir.getParentFile()) != null) {
            fallbackStack.add(placementIndex, parentDir);
        }
        
        int i = fallbackStack.size();
        File f = null;
        boolean loop = true;
        DirectoryContents content;
        do {
            i--;
            f = fallbackStack.get(i);
            content = getDirectoryContents(f);
            loop = content == null; 
        } while (loop && i > 0);
        if (loop) { //i == 0, none of the directories worked
            System.out.println("bro as if you just deleted your home folder");
            lastValidBrowserDirectory = null;
            return;
        }
        lastValidBrowserDirectory = f;
        
        //Hacky special case: Windows roots are shown for empty string
        boolean rootMode = f.toString().equals("");
        if (rootMode) {
            textFieldBrowser.setText(""); 
        } else {
            textFieldBrowser.setText(f.getAbsolutePath());
        }

        Collections.sort(content.folders, String.CASE_INSENSITIVE_ORDER);
        for (String s : content.folders) {
            items.add(new BrowserItem(s, true));
        }
        if (content.numberOfImages != 0) {
            items.add(new BrowserItem(content.numberOfImages + " supported files", false));
        }
        updateLaunchButton();
    }

    private class DirectoryContents {
        public ArrayList<String> folders = new ArrayList<String>();
        public int numberOfImages = 0;
        public int numberOfRaws = 0;
        public int numberOfVideos = 0;
        public int numberOfOther = 0;
    }

    private DirectoryContents getDirectoryContents(File dir) {
        boolean rootMode = dir.toString().equals("");

        DirectoryContents stats = new DirectoryContents();

        File[] contents = Common.tryListFiles(dir);
        if (contents == null) {
            return null;
        }

        FilenameFilter filter = Common.getFilenameFilter();
        for (File file : contents) {
            if (Common.isValidFolder(file)) {
                if (rootMode) {
                    stats.folders.add(file.toString());
                } else {
                    stats.folders.add(file.getName());
                }
            } else if (filter.accept(dir, file.getName())) {
                stats.numberOfImages++;
            }
        }

        return stats;
    }

    private void dirUp() {
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
    }

    private int getNumberOfSupportedImages(File directory) {
        DirectoryContents contents = getDirectoryContents(directory);
        if (contents == null) return 0;
        return contents.numberOfImages;
    }

    private void updateLaunchButton() {
        boolean relative = radioFolderRelative.isSelected();
        
        File mainDir = getCurrentlySelectedDirectory();
        boolean mainDirInvalid = !Common.isValidFolder(mainDir) || Common.tryListFiles(mainDir) == null;
        boolean absoulteDirInvalid = false;

        prefs.put("browserPath", mainDir.getAbsolutePath());

        String text = "LAUNCH\n for ";

        int imageCount = 0;
        if (mainDirInvalid) {
            text += "invalid directory";
        } else {
            text += "'" + mainDir.getName() + "' ";
            imageCount = getNumberOfSupportedImages(mainDir);
            if (imageCount == 0) {
                text += "(no valid files found!)";
            } else {
                text += "(" + imageCount + " vaild files)";
            }
        }
        text += "\n";
        File absoluteDir = null;
        if (relative) {
            text += "Target directory: The same directory";
        } else {
            absoluteDir = new File(textFieldFolder.getText());
            absoulteDirInvalid = !Common.isValidFolder(absoluteDir);
            if (absoulteDirInvalid) {
                text += "Target directory: Invalid";
            } else {
                text += "Target directory:  '" + absoluteDir.getName() + "'";
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
                if (e.getButton() == MouseButton.BACK) {
                    dirUp();
                } else if (isEmpty() || getItem() == null) {
                    return;
                } else if (getItem().isFolder) {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        if (e.getClickCount() == 2) {
                            textFieldBrowser.setText(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + getItem().name);
                            updateBrowser(); //cheap and simple. We literally just write the new path into the text field, updateBrowser then does the validation. 
                        }
                    }
                }
                //I like that even if every list item now has its own listener ...
                //double clicks between two elements still are counted
                //and also two double clicks in a row are not counted, as the count goes up to 4. 
                //I *could* fix this ... but no
                e.consume(); //Otherwise, the onMouseClicked in the ListView is also called
            });

            this.setOnMousePressed((e) -> {
                if (isEmpty() || getItem() == null) {
                    browserEmptyClicked();
                }
                if (e.getButton() == MouseButton.PRIMARY && !isEmpty()) {
                    if (e.getClickCount() == 1 && isSelected()) {
                        //the plan was to unselect elements again, but it doesnt work: 
                        //listBrowser.getSelectionModel().clearSelection(); //The element is selected before when this is called and then just gets instantly unselected. 
                    }
                }
            });
        }
        
        @Override
        public void updateItem(BrowserItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setGraphic(null);
                setText(null);
            } else {
                ImageView view = new ImageView(Common.getResource(item.isFolder ? "folder" : "image"));
                view.setFitHeight(22);
                view.setPreserveRatio(true);
                setGraphic(view);
                setText(item.name);
            }
        }
    }
}
