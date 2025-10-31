package com.github.racopokemon.imagesort;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

public class Launcher {

    public static final double SMALL_GAP = 4;
    public static final double BIG_GAP = 16;

    private Stage stage;
    private ListView<BrowserItem> listBrowser;
    private TextField textFieldBrowser;
    private Button buttonLaunch;
    private CheckBox checkMiscShowUsage;

    public static File FALLBACK_DIRECTORY = new File(System.getProperty("user.home"));
    private File lastValidBrowserDirectory = null;

    //little bonus feature: If you start from clipboard with a certain file and end up launching the gallery for this image,
    //we make the gallery start exactly from this image (useful for folders of maany images)
    private String startFileName = null;
    //if we launch from this directory, start from startFileName, please. 
    private File startFileDirectory = null;

    private Preferences prefs = Common.getPreferences();

    public void start(Stage stage) {
        this.stage = stage;

        Label labelIntro = new Label(
                "In this gallery you can assign images in a folder to categories. When you close the window, these files can be automatically moved or copied to folders based on your assignment.");
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
            textFieldBrowser.setText(prefs.get("browserPath", FALLBACK_DIRECTORY.getAbsolutePath())); //2nd is the default value if nothing is stored yet
        } else {
            textFieldBrowser.setText(startPath.getAbsolutePath());
        }
        HBox.setHgrow(textFieldBrowser, Priority.ALWAYS);
        //eventfilter instead of onKeyPressed to act *before* the caret is moved
        textFieldBrowser.addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
            if (e.getCode() == KeyCode.DOWN && !e.isAltDown() && !e.isShortcutDown() && !e.isShiftDown()) {
                if (textFieldBrowser.getCaretPosition() == textFieldBrowser.getText().length()) {
                    if (!listBrowser.getSelectionModel().isEmpty()) {
                        listBrowser.requestFocus();
                        listBrowser.getSelectionModel().selectFirst();
                        e.consume();
                    }
                }
            }
        });
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

        checkMiscShowUsage = new CheckBox("Show usage hints");
        checkMiscShowUsage.setSelected(prefs.getBoolean("miscShowUsage", true));
        VBox miscBox = new VBox(SMALL_GAP, checkMiscShowUsage);

        Label labelClipboard = new Label(
                "Note: When launching this app, we check your clipboard and start there (if possible). This might save you some seconds.");
        labelClipboard.setWrapText(true);
        labelClipboard.setFont(fontItalic);
        buttonLaunch = new Button("LAUNCH GALLERY");
        buttonLaunch.setMaxWidth(Double.POSITIVE_INFINITY); // thats a VERY big boii
        buttonLaunch.setMaxHeight(70);
        buttonLaunch.setTextAlignment(TextAlignment.CENTER);
        VBox.setVgrow(buttonLaunch, Priority.ALWAYS);

        VBox mainVertical = new VBox(BIG_GAP,
                labelIntro,
                boxBrowser,
                labelClipboard, 
                miscBox,
                buttonLaunch);
        StackPane root = new StackPane(mainVertical);
        StackPane.setMargin(mainVertical, new Insets(14));

        Scene scene = new Scene(root, 500, 670);

        buttonBrowserBrowse.setOnAction((e) -> {
            DirectoryChooser folderDirChooser = new DirectoryChooser();
            folderDirChooser.setTitle("Select your image directory");
            File f = new File(textFieldBrowser.getText());
            if (Common.isValidFolder(f)) {
                folderDirChooser.setInitialDirectory(f);
            }
            File dir = folderDirChooser.showDialog(stage);
            boolean successful = dir != null;
            if (successful) {
                textFieldBrowser.setText(dir.getAbsolutePath());
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
        textFieldBrowser.setOnAction((e) -> {
            updateBrowser();
            listBrowser.requestFocus();
            listBrowser.getSelectionModel().selectFirst();
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

        //We have to split the handling; we need to intercept the UP button before the listView handles it itself without consuming it, which causes selecting the textfieldbrowser already from 2nd entry
        listBrowser.addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
            if (e.getCode() == KeyCode.UP && listBrowser.getSelectionModel().getSelectedIndex() == 0) {
                textFieldBrowser.requestFocus();
                textFieldBrowser.selectEnd();
                e.consume();
            }
        });
        listBrowser.setOnKeyPressed((e) -> {
            BrowserItem item = listBrowser.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER) {
                if (item == null) {
                    if (e.isShortcutDown() || e.isShiftDown()) {
                        Common.showDirInExplorer(textFieldBrowser.getText());
                    } else {
                        launch(); //If we can't launch here (button disabled), the launch call simply ignores this request.
                    }
                } else {
                    if (e.isShortcutDown() || e.isShiftDown()) {
                        item.onAlternativeAction();
                    } else {
                        item.onAction();
                    }
                }
            } else if (e.getCode() == KeyCode.BACK_SPACE || (e.getCode() == KeyCode.UP && e.isAltDown())) {
                dirUp();
            } else if (e.getCode() == KeyCode.UP && item == null) {
                if (listBrowser.getSelectionModel().isEmpty()) {
                    textFieldBrowser.requestFocus();
                    textFieldBrowser.selectEnd();
                } else {
                    listBrowser.getSelectionModel().selectLast();
                }
            } else if (e.getCode() == KeyCode.DOWN && item == null) {
                listBrowser.getSelectionModel().selectFirst();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                if (item == null) {
                    dirUp();
                } else {
                    listBrowser.getSelectionModel().clearSelection();
                }
            } 
            //this is sadly called once the up key press was already used to skip one item up, therefore entering the text field already from the 2nd element. 
            //I don't see an easy / valid workaround for this. 
            //else if (listBrowser.getSelectionModel().isSelected(0) && 
            //        e.getCode() == KeyCode.UP && !e.isAltDown() && !e.isShortcutDown() && !e.isShiftDown()) {
            //    listBrowser.getSelectionModel().clearSelection();
            //    textFieldBrowser.requestFocus();
            //    textFieldBrowser.selectEnd();
            //}
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
                listBrowser.requestFocus(); //used to be buttonLaunch, but now pressing enter in the browser launches as well. 
            }
        });
        scene.getAccelerators().put(KeyCombination.keyCombination("Shortcut+W"), () -> {
            stage.close();
        });

        stage.setScene(scene);
        stage.setTitle("Image Sort");
        stage.getIcons().add(Common.getResource("logo"));
        stage.show();
        
        //a start path is already written to the textFieldBrowser at its creation, now validate the browser. 
        updateBrowser(); 
        if (textFieldBrowser.getText().equals(FALLBACK_DIRECTORY.getAbsolutePath())) {
            //if we end up in the default dir, show the folder selection dialog already. 
            //Convenience when first starting the app
            buttonBrowserBrowse.fireEvent(new ActionEvent());
        }
        updateLaunchButton();

        //buttonBrowserBrowse.requestFocus();
        listBrowser.requestFocus();
        listBrowser.getSelectionModel().clearSelection();

        //fighting focus on mac (doesnt work though)
        Platform.runLater(() -> {stage.requestFocus();});
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
            startFileName = path.getName();
            path = path.getParentFile();
            startFileDirectory = path;
            if (path != null && !path.isDirectory()) {
                return null;
            }
        }
        return path;
    }

    private void launch() {
        if (buttonLaunch.isDisabled()) {
            //this call either comes from the launch button itself or from browser items that were double clicked etc. 
            //simply drop the request if the button is not enabled
            return;
        }

        File directory = getCurrentlySelectedDirectory();

        if (Common.tryListFiles(directory) == null) {
            Alert alert = new Alert(AlertType.NONE, "Could not launch the Gallery: \nWe can't read folder \n"+directory.getAbsolutePath(), ButtonType.OK);
            alert.setHeaderText("Could not launch");
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }

        if (getNumberOfSupportedImages(directory) == 0) {
            Alert alert = new Alert(AlertType.NONE, "Could not launch the Gallery: \nWe could find no supported files in folder \n"+textFieldBrowser.getText(), ButtonType.OK);
            alert.setHeaderText("Could not launch");
            alert.initOwner(stage);
            alert.showAndWait();
            return;
        }

        String startImage = null;
        if (directory.equals(startFileDirectory)) {
            if (Common.getImageFilter().accept(directory, startFileName) && new File(directory, startFileName).exists()) {
                startImage = startFileName;
            }
        }

        stage.close();

        new Gallery().start(directory, startImage, 
                    checkMiscShowUsage.isSelected());
    }

    //Returns either the base directory in the browser list, if no element is selected, or the selected directory in the browser list. 
    private File getCurrentlySelectedDirectory() {
        boolean folderSelected = false;
        BrowserItem selectedItem = null;
        if (!listBrowser.getSelectionModel().isEmpty()) {
            selectedItem = listBrowser.getSelectionModel().getSelectedItem();
            if (selectedItem.isDirectory()) {
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
        fallbackStack.add(FALLBACK_DIRECTORY);
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
            items.add(new BrowserItem(s, BrowserElement.DIRECTORY));
        }
        if (content.numberOfImages != 0) {
            items.add(new BrowserItem(content.numberOfImages + " supported " + Common.getSingularOrPluralOfFile(content.numberOfImages), BrowserElement.IMAGE));
        }
        if (content.numberOfRaws != 0) {
            items.add(new BrowserItem(content.numberOfRaws + " RAW " + Common.getSingularOrPluralOfFile(content.numberOfRaws) + " (only indirectly supported)", BrowserElement.RAW));
        }
        if (content.numberOfVideos != 0) {
            items.add(new BrowserItem(content.numberOfVideos + " video " + Common.getSingularOrPluralOfFile(content.numberOfVideos) + " (not yet supported)", BrowserElement.VIDEO));
        }
        if (content.numberOfOther != 0) {
            String otherText = content.numberOfOther + " other " + Common.getSingularOrPluralOfFile(content.numberOfOther);
            int extSize = content.otherExtensions.size();
            if (0 < extSize && extSize <= 5) {
                otherText += " (";
                for (String e : content.otherExtensions) {
                    otherText += "." + e;
                    if (--extSize > 0) {
                        otherText += ", ";
                    }
                }
                otherText += ")";
            }
            items.add(new BrowserItem(otherText, BrowserElement.OTHER));
        }

        updateLaunchButton();
    }

    private class DirectoryContents {
        public ArrayList<String> folders = new ArrayList<String>();
        public int numberOfImages = 0;
        public int numberOfRaws = 0;
        public int numberOfVideos = 0;
        public int numberOfOther = 0;
        public HashSet<String> otherExtensions = new HashSet<>();
    }

    private DirectoryContents getDirectoryContents(File dir) {
        boolean rootMode = dir.toString().equals("");

        DirectoryContents stats = new DirectoryContents();

        File[] contents = Common.tryListFiles(dir);
        if (contents == null) {
            return null;
        }

        FilenameFilter imageFilter = Common.getImageFilter();
        FilenameFilter rawFilter = Common.getRawFilter();
        FilenameFilter videoFilter = Common.getVideoFilter();
        FilenameFilter otherFilter = Common.getOtherFilter();
        for (File file : contents) {
            String filename = file.getName();
            if (Common.isValidFolder(file)) {
                if (rootMode) {
                    stats.folders.add(file.toString());
                } else {
                    //probably ignore linux hidden folders at some point
                    stats.folders.add(filename);
                }
            } else if (imageFilter.accept(dir, filename)) {
                stats.numberOfImages++;
            } else if (rawFilter.accept(dir, filename)) {
                stats.numberOfRaws++;
            } else if (videoFilter.accept(dir, filename)) {
                stats.numberOfVideos++;
            } else if (otherFilter.accept(dir, filename)) {
                //the otherFilter accepts all files except some system files like desktop.ini in Windows
                stats.numberOfOther++;
                if (filename.contains(".")) {
                    stats.otherExtensions.add(Common.getExtensionFromFilename(filename).toLowerCase());
                }
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
            int index = listBrowser.getItems().indexOf(new BrowserItem(name, BrowserElement.DIRECTORY));
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
        File mainDir = getCurrentlySelectedDirectory();
        boolean mainDirInvalid = !Common.isValidFolder(mainDir) || Common.tryListFiles(mainDir) == null;

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

        boolean disable = mainDirInvalid;
        disable |= imageCount == 0;

        buttonLaunch.setDisable(disable);
        buttonLaunch.setText(text);
    }

    private void browserEmptyClicked() {
        listBrowser.getSelectionModel().clearSelection();
    }

    private enum BrowserElement {        
        DIRECTORY("folder"), IMAGE("image"), RAW("raw1"), VIDEO("video2"), OTHER("other2");
        
        private String resource;
        private BrowserElement(String resourceName) {
            this.resource = resourceName;
        }
        public String getResourceName() {
            return resource;
        }
    }

    private class BrowserItem {
        public String name;
        public BrowserElement type;
        public BrowserItem(String name, BrowserElement type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BrowserItem) {
                BrowserItem i = (BrowserItem) o;
                return i.type == type && i.name.equals(name);
            } else {
                return false;
            }
        }

        public boolean isDirectory() {
            return type.equals(BrowserElement.DIRECTORY);
        }

        public Image getImage() {
            return Common.getResource(type.getResourceName());
        }

        //Called when enter is pressed on a browserCell of this item, or if it is double clicked
        public void onAction() {
            //were slowly getting to the point, where we could use inheritance instead of cases for BrowserItems
            if (isDirectory()) {
                textFieldBrowser.setText(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + name);
                updateBrowser(); //cheap and simple. We literally just write the new path into the text field, updateBrowser then does the validation. 
            } else if (type == BrowserElement.IMAGE) {
                launch(); //if we can't launch here, the method simply ignores the request
            } else {
                //delegate to show in explorer
                onAlternativeAction();
            }
        }

        //modified (shift, shortcut) double click or enter. (Basically show in explorer)
        public void onAlternativeAction() {
            //we never select an actual file, always just open the browser
            if (isDirectory()) {
                Common.showDirInExplorer(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + name);
            } else {
                Common.showDirInExplorer(textFieldBrowser.getText());
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
                } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    if (e.isShortcutDown() || e.isShiftDown()) {
                        getItem().onAlternativeAction();
                    } else {
                        getItem().onAction();
                    }
                }
                //I like that even if every list item now has its own listener ...
                //double clicks between two elements *still* are counted
                //and also two double clicks in a row are not counted, as the count goes up to 4. 
                //I *could* fix this ... but no
                e.consume(); //Otherwise, the onMouseClicked in the ListView is also called
            });

            this.setOnMousePressed((e) -> {
                if (isEmpty() || getItem() == null) {
                    browserEmptyClicked();
                }
                //if (e.getButton() == MouseButton.PRIMARY && !isEmpty()) {
                //    if (e.getClickCount() == 1 && isSelected()) {
                //        //the plan was to unselect elements again, but it doesnt work: 
                //        //listBrowser.getSelectionModel().clearSelection(); 
                //        //The element is selected before when this is called and then just gets instantly unselected. 
                //    }
                //}
            });
        }
        
        @Override
        public void updateItem(BrowserItem item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setGraphic(null);
                setText(null);
            } else {
                ImageView view = new ImageView(item.getImage());
                view.setFitHeight(22);
                view.setPreserveRatio(true);
                setGraphic(view);
                setText(item.name);
            }
        }
    }
}
