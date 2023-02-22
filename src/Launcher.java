import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.event.*;

public class Launcher {

    private static final double SMALL_GAP = 4;
    private static final double BIG_GAP = 16;
    private static final double BIG_INTEND_GAP = 34;

    private static final String CHECK_DELETE_FOLDER_SEL_TEXT = "Also create the 'delete' folder here. (Otherwise, it is created in the images folder)";
    private static final String CHECK_DELETE_FOLDER_UNSEL_TEXT = "Also create the 'delete' folder here. (Right now, it is created in the images folder)";

    private Stage stage;
    private ListView<String> listBrowser;
    private TextField textFieldBrowser;

    private File fallbackDirectory = new File(System.getProperty("user.home"));
    private File lastValidBrowserDirectory = null;

    // TODO: You promised it, now give us the persistency!

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
        HBox.setHgrow(textFieldBrowser, Priority.ALWAYS);
        Button buttonBrowserBrowse = new Button("Browse");
        HBox boxBrowserLine = new HBox(buttonBrowserDirUp, textFieldBrowser, buttonBrowserBrowse);

        listBrowser = new ListView<>();
        listBrowser.setPrefHeight(100);
        VBox.setVgrow(listBrowser, Priority.ALWAYS);

        VBox boxBrowser = new VBox(SMALL_GAP, labelBrowserIntro, boxBrowserLine, listBrowser);
        VBox.setVgrow(boxBrowser, Priority.ALWAYS);

        Label labelOperation = new Label("Once you finished sorting your files, what should we do with them?");
        labelOperation.setWrapText(true);
        RadioButton radioOperationMove = new RadioButton("Move to the folder of their category");
        RadioButton radioOperationCopy = new RadioButton("Copy to the folder of their category");
        ToggleGroup groupOperation = new ToggleGroup();
        radioOperationMove.setToggleGroup(groupOperation);
        radioOperationCopy.setToggleGroup(groupOperation);
        radioOperationMove.setSelected(true);
        VBox.setMargin(radioOperationMove, indent);
        VBox.setMargin(radioOperationCopy, indent);
        radioOperationMove.setMaxWidth(Double.POSITIVE_INFINITY);
        radioOperationCopy.setMaxWidth(Double.POSITIVE_INFINITY);
        VBox boxOperation = new VBox(SMALL_GAP, labelOperation, radioOperationMove, radioOperationCopy);

        Label labelFolder = new Label("Where should we create the category folders?");
        labelFolder.setWrapText(true);
        RadioButton radioFolderRelative = new RadioButton("Inside the images folder selected above");
        RadioButton radioFolderAbsolute = new RadioButton("In a separate folder:");
        ToggleGroup groupFolder = new ToggleGroup();
        radioFolderRelative.setToggleGroup(groupFolder);
        radioFolderAbsolute.setToggleGroup(groupFolder);
        radioFolderRelative.setSelected(true);
        VBox.setMargin(radioFolderRelative, indent);
        VBox.setMargin(radioFolderAbsolute, indent);
        radioFolderRelative.setMaxWidth(Double.POSITIVE_INFINITY);
        radioFolderAbsolute.setMaxWidth(Double.POSITIVE_INFINITY);

        TextField textFieldFolder = new TextField();
        HBox.setHgrow(textFieldFolder, Priority.ALWAYS);
        Button buttonFolderBrowse = new Button("Browse");
        HBox boxFolderBrowserLine = new HBox(textFieldFolder, buttonFolderBrowse);
        VBox.setMargin(boxFolderBrowserLine, indentBig);

        CheckBox checkDeleteFolder = new CheckBox(CHECK_DELETE_FOLDER_UNSEL_TEXT);
        checkDeleteFolder.setWrapText(true);
        checkDeleteFolder.setMaxWidth(Double.POSITIVE_INFINITY);
        VBox.setMargin(checkDeleteFolder, indentBig);

        VBox boxFolder = new VBox(SMALL_GAP, labelFolder, radioFolderRelative, radioFolderAbsolute,
                boxFolderBrowserLine, checkDeleteFolder);

        Label labelPermanent = new Label(
                "All settings you're making here are stored throughout sessions for your convenience.");
        labelPermanent.setWrapText(true);
        labelPermanent.setFont(fontItalic);
        Button buttonLaunch = new Button("LAUNCH GALLERY");
        buttonLaunch.setMaxWidth(Double.POSITIVE_INFINITY); // thats a VERY big boii
        buttonLaunch.setMaxHeight(50);
        VBox.setVgrow(buttonLaunch, Priority.ALWAYS);

        VBox mainVertical = new VBox(BIG_GAP,
                labelIntro,
                boxBrowser,
                boxOperation,
                boxFolder,
                labelPermanent, buttonLaunch);
        StackPane root = new StackPane(mainVertical);
        StackPane.setMargin(mainVertical, new Insets(12));

        Scene scene = new Scene(root, 520, 800);

        checkDeleteFolder.disableProperty().bind(radioFolderRelative.selectedProperty());
        boxFolderBrowserLine.disableProperty().bind(radioFolderRelative.selectedProperty());

        checkDeleteFolder.selectedProperty().addListener((e) -> {
            if (checkDeleteFolder.isSelected()) {
                checkDeleteFolder.setText(CHECK_DELETE_FOLDER_SEL_TEXT);
            } else {
                checkDeleteFolder.setText(CHECK_DELETE_FOLDER_UNSEL_TEXT);
            }
        });

        buttonBrowserBrowse.setOnAction(new BrowseHandler("Select your image directory", textFieldBrowser, true));
        buttonBrowserDirUp.setOnAction((e) -> {
            File f = new File(textFieldBrowser.getText());
            if (!isValidFile(f)) {
                return;
            }
            String parent = f.getParent();
            if (parent != null) {
                textFieldBrowser.setText(parent);
                updateBrowser();
            }
        });
        textFieldBrowser.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                //focus left!
                updateBrowser();
            }
        });
        if (false) { //temp: preferences data

        } else {
            textFieldBrowser.setText(fallbackDirectory.getAbsolutePath());
        }
        updateBrowser();
        listBrowser.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && listBrowser.getSelectionModel().getSelectedItem() != null) {
                //Cheaply faking together the single list elements listening to double clicks. (Don't want to start the cell factory) Actually it's just the whole list listening, 
                //and let's hope the cursor didn't move between both clicks between two elements (well it can happen.) Also lets hope that the user doesn't keep clicking, we register only click 2
                textFieldBrowser.setText(textFieldBrowser.getText() + FileSystems.getDefault().getSeparator() + listBrowser.getSelectionModel().getSelectedItem());
                updateBrowser(); //cheap and simple. We literally just write the new path into the text field, updateBrowser then does the validation. 
            }
        });

        buttonFolderBrowse.setOnAction(new BrowseHandler("Select the target directory", textFieldFolder, false));

        stage.setScene(scene);
        stage.setTitle("Launcher");
        stage.show();

        buttonBrowserBrowse.requestFocus();
    }

    private class BrowseHandler implements EventHandler<ActionEvent> {
        private DirectoryChooser folderDirChooser;
        private TextField target;
        private boolean update;

        public BrowseHandler(String title, TextField field, boolean updateBrowser) {
            folderDirChooser = new DirectoryChooser();
            folderDirChooser.setTitle(title);
            target = field;
            update = updateBrowser;
        }

        @Override
        public void handle(ActionEvent event) {
            File f = new File(target.getText());
            if (isValidFile(f)) {
                folderDirChooser.setInitialDirectory(f);
            }
            File dir = folderDirChooser.showDialog(stage);
            if (dir != null) {
                target.setText(dir.getAbsolutePath());
                if (update) {
                    updateBrowser();
                }
            }
        }
    }

    private void updateBrowser() {
        File newDir = new File(textFieldBrowser.getText());
        if (newDir.equals(lastValidBrowserDirectory)) {
            //nothing changed
            return;
        }

        ObservableList<String> items = listBrowser.getItems();
        items.clear();

        ArrayList<File> fallbackStack = new ArrayList<>();
        fallbackStack.add(fallbackDirectory);
        fallbackStack.add(lastValidBrowserDirectory);
        fallbackStack.add(newDir);
        
        File[] contents = null; //this crashes sometimes, even though the directory exists, can read, etc.

        int i = fallbackStack.size();
        File f = null;
        boolean loop = true;
        do {
            i--;
            f = fallbackStack.get(i);
            if (isValidFile(f)) {
                contents = f.listFiles();
                loop = contents == null;
            } else {
                loop = true;
            }
        } while (loop && i > 0);
        if (loop) { //i == 0, none of the directories worked
            System.out.println("bro as if you just deleted your home folder");
            lastValidBrowserDirectory = null;
            return;
        }
        lastValidBrowserDirectory = f;
        textFieldBrowser.setText(f.getAbsolutePath());

        //TODO: Do I also need to sort the files?
        for (File file : contents) {
            if (isValidFile(file)) {
                items.add(file.getName());
            }
        }
    }

    private boolean isValidFile(File f) {
        return f.exists() && f.isDirectory();
    }
}
