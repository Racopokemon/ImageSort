import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Launcher {

    private static final double SMALL_GAP = 4;
    private static final double BIG_GAP = 12;
    private static final double BIG_INTEND_GAP = 34;

    public static void start(Stage stage) {
        Insets indent = new Insets(0, 0, 0, BIG_GAP);
        Insets indentBig = new Insets(0, 0, 0, BIG_INTEND_GAP);

        Label labelIntro = new Label("This app is an image gallery, where you cycle through all images in a folder you select below. You may assign them to categories. Once you have finished, these files can be automatically moved or copied to folders, corresponding to the category you selected. ");
        Font fontNormal = labelIntro.getFont();
        Font fontItalic = Font.font(fontNormal.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, fontNormal.getSize());
        labelIntro.setFont(fontItalic);
        labelIntro.setWrapText(true);
        VBox.setVgrow(labelIntro, Priority.NEVER);
        Label labelBrowserIntro = new Label("Where are the images stored?");
        labelBrowserIntro.setWrapText(true);
        
        Button buttonBrowserDirUp = new Button("^");
        TextField buttonBrowserPath = new TextField();
        HBox.setHgrow(buttonBrowserPath, Priority.ALWAYS);
        Button buttonBrowserBrowse = new Button("Browse");
        HBox boxBrowserLine = new HBox(buttonBrowserDirUp, buttonBrowserPath, buttonBrowserBrowse);

        ListView<String> browserList = new ListView<>();
        browserList.setPrefHeight(100);
        VBox.setVgrow(browserList, Priority.ALWAYS);

        VBox boxBrowser = new VBox(SMALL_GAP, labelBrowserIntro, boxBrowserLine, browserList);
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
        VBox boxOperation = new VBox(SMALL_GAP, labelOperation, radioOperationMove, radioOperationCopy);

        Label labelFolder = new Label("Where should we create the category folders?");
        labelFolder.setWrapText(true);
        RadioButton radioFolderRelative = new RadioButton("Inside the images folder itself (%s)");
        RadioButton radioFolderAbsolute = new RadioButton("In a separate folder:");
        ToggleGroup groupFolder = new ToggleGroup();
        radioFolderRelative.setToggleGroup(groupFolder);
        radioFolderAbsolute.setToggleGroup(groupFolder);
        radioFolderRelative.setSelected(true);
        VBox.setMargin(radioFolderRelative, indent);
        VBox.setMargin(radioFolderAbsolute, indent);
        
        TextField buttonFolderPath = new TextField();
        HBox.setHgrow(buttonFolderPath, Priority.ALWAYS);
        Button buttonFolderBrowse = new Button("Browse");
        HBox boxFolderBrowserLine = new HBox(buttonFolderPath, buttonFolderBrowse);
        VBox.setMargin(boxFolderBrowserLine, indentBig);
        
        CheckBox checkDeleteFolder = new CheckBox("temp make this good");
        checkDeleteFolder.setWrapText(true);
        VBox.setMargin(checkDeleteFolder, indentBig);

        VBox boxFolder = new VBox(SMALL_GAP, labelFolder, radioFolderRelative, radioFolderAbsolute, boxFolderBrowserLine, checkDeleteFolder);

        Label labelPermanent = new Label("All settings you're making here are stored throughout sessions for your convenience.");
        labelPermanent.setWrapText(true);
        labelPermanent.setFont(fontItalic);
        Button buttonLaunch = new Button("LAUNCH GALLERY");
        buttonLaunch.setMaxWidth(Double.POSITIVE_INFINITY); //thats a VERY big boii
        buttonLaunch.setMaxHeight(50);
        VBox.setVgrow(buttonLaunch, Priority.ALWAYS);

        VBox mainVertical = new VBox(BIG_GAP, 
                labelIntro, 
                boxBrowser,
                boxOperation, 
                boxFolder,
                labelPermanent, buttonLaunch);
        StackPane root = new StackPane(mainVertical);
        StackPane.setMargin(mainVertical, new Insets(BIG_GAP));

        Scene scene = new Scene(root, 500, 600);

        checkDeleteFolder.disableProperty().bind(radioFolderRelative.selectedProperty());
        boxFolderBrowserLine.disableProperty().bind(radioFolderRelative.selectedProperty());

        checkDeleteFolder.selectedProperty().addListener((e) -> {
            if (checkDeleteFolder.isSelected()) {
                checkDeleteFolder.setText("Also create the 'delete' folder here. (Otherwise, it is created in the images folder)");
            } else {
                checkDeleteFolder.setText("Also create the 'delete' folder here. (Right now, it is created in the images folder)");
            }
        });

        stage.setScene(scene);
        stage.show();
    }
}
