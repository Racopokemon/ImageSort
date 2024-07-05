package com.github.racopokemon.imagesort;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

/*
 * TODO: Watch out for ToDos!
 * TODO: Afterwards, remove this ToDo
 */

 /*
  * TODO: Do not spam not necessary todos all over the code..
  */

public class Gallery {

    private static final boolean HIDE_MOUSE_ON_IMAGE_SWITCH = true; //If true, the mouse pointer is hidden instantly as soon as you switch to another image (and shown instantly on mouse move). No hiding if set to false. 
    private static final boolean HIDE_MOUSE_ON_IDLE = true; //Stronger variant basically, automatically hide mouse if not moved for ~1 sec. (Fullscreen only)
    private static final boolean DEBUG_PRINT_IMAGE_METADATA = false; //if true, the current images metadata is written to comand line everytime the image changes. (For debugging)
    private static final boolean MOVE_ALONG = true; //Feature that silently also moves/copies/deletes not supported files during file operations, if they have the same file name (but different extension). For .raws

    private File directory;
    private File targetDirectory;
    private File deleteDirectory;
    private boolean reopenLauncherAfterwards;

    private int numberOfCategories = 3;
    private int numberOfTicks = 4;

    //The current list of images we are cycling through now (with filters not all images might be visible). Subset of allImages, which is all images in the folder
    private ArrayList<String> images = new ArrayList<>();
    //All images available in the users folder. Updated by updateFilesList()
    private ArrayList<String> allImages = new ArrayList<>();
    //A lookup, for every supported image file we store a list of filenames inside this directory, that have the same filename, but another (not supported) extension. 
    //These files are silently copied / moved / deleted along with the image. (.raw feature, if images exist both as raw and jpg)
    private Hashtable<String, ArrayList<String>> filesToMoveAlong = new Hashtable<>();
    //The previous version of allImages. We use this to check the neighborhood of a suddenly deleted image (that still exists in allIamges) to prevent simply skipping 
    //to the first image in the entire folder. 
    private ArrayList<String> previouslyAllImages = new ArrayList<>();
    private ArrayList<ArrayList<String>> deleteHistory = new ArrayList<>(); //last element added: First set of image names to be restored (can be multiple because of moveAlong feature)
    private ArrayList<ImageFileOperations> deleteHistoryCategory = new ArrayList<>(); //We also store which category the deleted image was in

    private FilenameFilter filenameFilter;

    private String currentImage = null; //filename OR null if in the current filter category there are no images to show at all
    private String lastImageManuallySelected = null;
    private int filter = -1; //-1: No filter. 0: Keep only. 1-numberOfCategories: Only this category (move). From that on: Only this category (copy / ticks)
    private boolean updateFilterOnNextImage = false; //slight acceleration, only reload the whole filter when this actually occured

    private Hashtable<String, ImageFileOperations> imageOperations; //only added once seen, so might not contain all images. The expected state for non contained images is category 0, no copies.
    private Hashtable<String, RotatedImage> imageBuffer; //holds the current image and some images before and after it, updated with every loadImage. 
    //we use here that javaFX images already load in the background and provide an empty image until they finished loading, so just creating  and 
    //updating references to the (probably still loading) images is enough. 

    private static final int IMAGE_BUFFER_SIZE_MIN = 1; //setting it to 0 ... woah. Well never get to probe the RAM, and if some images load sometimes its still an improvement
    private static final int IMAGE_BUFFER_SIZE_MAX = 4;
    private int imageBufferSize = IMAGE_BUFFER_SIZE_MAX; //this many images before, and also this many after the current are already being loaded
    private int successfullLoadsWithoutMemoryProblems = 0; //we constantly probe if we exceed the memory limits by counting up here, and may increase the buffer

    private double zoom = 2.9;
    private boolean isZooming = false;
    private double mouseRelativeX, mouseRelativeY;
    private static final double MIN_ZOOM = 1.15;
    private static final double MAX_ZOOM = 10;

    private Stage stage;
    private ImageView view;
    private StackPane zoomPane;
    private StackPane imageAndLoadingPane;
    private StackPane rootPane;
    private InteractiveLabel label;
    private InteractiveLabel filterLabel;
    private InteractiveLabel[] tickLabels;
    private VBox tickLabelVBox;
    private boolean setAllTickLabelsToFullOpacity = false; 
    public static final double TICK_LABEL_HEIGHT = 48;
    private static final Color LR_HALF_TRANSPARENT = new Color(1, 1, 1, 0.08);
    private static final Color LR_ARROW_COLOR = new Color(0, 0, 0, 0.5);
    private static final double BUTTON_WIDTH = 100;

    private static final double HOT_CORNER_SIZE = 3; 
    private Rectangle hideUiHotcorner;

    private StackPane zoomIndicator;
    private Text zoomIndicatorText;
    private StackPane resolutionIndicator;
    private Text resolutionTextDimension;
    private Text resolutionTextEstimate;
    private static final int NUMBER_OF_RESOLUTION_RECTS = 15;
    private Rectangle[] resolutionGraphicRects;

    private ImprovisedProgressBar progress;

    private ProgressIndicator loadingProgress; 
    private Label errorLabel; 
    private Label noImagesLabel; 
    private LRButton leftButton, rightButton;

    private Text wrapSearchIndicator;
    private Timeline wrapSearchIndicatorTimeline;
    private Pane actionIndicatorPane;
    private Circle actionIndicatorC1, actionIndicatorC2;
    private Timeline actinoIndicateTimeline;
    //https://stackoverflow.com/questions/33066754/javafx-set-mouse-hidden-when-idle Thanks for the great answer, was halfway through Workers, Tasks and Services
    private PauseTransition hideMouseOnIdle;

    private StackPane rotationIndicator;

    private class ImageFileOperations {
        private int moveTo = 0; //0: Dont move. 1 - numberOfCategories: Move to this category
        private boolean[] copyTo = new boolean[numberOfTicks];

        public int getMoveTo() {
            return moveTo;
        }
        public void setMoveTo(int moveTo) {
            this.moveTo = moveTo;
        }

        public boolean getCopyTo(int i) {
            return copyTo[i];
        }
        public void setCopyTo(int i, boolean copyTo) {
            this.copyTo[i] = copyTo;
        }

        public boolean hasInitialState() {
            for (int i = 0; i < copyTo.length; i++) {
                if (copyTo[i]) {
                    return false;
                }
            }
            return moveTo == 0;
        }
    }

    //very bad OO incoming here...
    public class TickLabelClickAction implements InteractiveLabel.Action {
        private int tickNumber;
        public TickLabelClickAction(int tn) {
            tickNumber = tn;
        }
        @Override
        public void call() {
            toggleCurrentImageTick(tickNumber);
        }
    }
    //here as well
    public class TickLabelMidAction implements InteractiveLabel.Action {
        private int filterNumber;
        public TickLabelMidAction(int fn) {
            filterNumber = fn;
        }
        @Override
        public void call() {
            toggleFilter(filterNumber);
        }
    }

    public void start(File directory, String startFileName, File targetDirectory, File deleteDirectory, boolean reopenLauncher, boolean showHints) {
        this.directory = directory;
        this.targetDirectory = targetDirectory;
        this.deleteDirectory = deleteDirectory;
        this.reopenLauncherAfterwards = reopenLauncher;
        lastImageManuallySelected = startFileName;

        System.out.println("The origin of the micro-lags is apparently loading the image metadata on the main thread. ... Just saying - this *COULD BE FIXED*");

        stage = new Stage();

        filenameFilter = Common.getImageFilter();
        imageOperations = new Hashtable<>();
        imageBuffer = new Hashtable<>();

        stage.setTitle(directory.getName() + " - Image Sort");

        zoomPane = new StackPane();
        view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        zoomPane.getChildren().add(view);

        rootPane = new StackPane();
        zoomPane.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    setMousePosition(event);
                    zoomIn();
                    event.consume();
                } else if (event.getButton() == MouseButton.MIDDLE) {
                    if (isZooming) {
                        zoomTo100Percent();
                        event.consume();
                    }
                }
            }
        });
        EventHandler<ScrollEvent> zoomPaneScrollHandler = new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (isZooming) {
                    if (event.getDeltaY() > 0) {
                        increaseZoom(event.getDeltaY());
                    } else if (event.getDeltaY() < 0) {
                        decreaseZoom(-event.getDeltaY());
                    }
                } else {
                    if (event.getDeltaY() >= 4) {
                        prevImage();
                    } else if (event.getDeltaY() <= -4) {
                        nextImage();
                    }
                }
                event.consume();
            }
        };
        zoomPane.setOnScroll(zoomPaneScrollHandler);
        zoomPane.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    setMousePosition(event);
                    zoomIn();
                }
            }
        });
        zoomPane.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    zoomOut();
                    hideMouseOnIdle.playFromStart();
                }
            }
        });
        hideMouseOnIdle = new PauseTransition(Duration.seconds(0.8));
        if (HIDE_MOUSE_ON_IDLE) {
            hideMouseOnIdle.setOnFinished((e) -> {
                if (stage.isFullScreen()) zoomPane.setCursor(Cursor.NONE);
            });
        }
        zoomPane.setOnMouseMoved((e) -> {
            zoomPane.setCursor(Cursor.DEFAULT);
            hideMouseOnIdle.playFromStart();
        });
        zoomPane.setOnMouseExited((e) -> {
            zoomPane.setCursor(Cursor.DEFAULT);
            hideMouseOnIdle.stop();
        });

        label = new InteractiveLabel(38, 200, 70, Pos.BOTTOM_LEFT, 
                () -> {incrementCurrentImageCategory();}, 
                () -> {decrementCurrentImageCategory();},
                () -> {toggleFilter(imageOperations.get(currentImage).getMoveTo());});
        StackPane.setAlignment(label, Pos.BOTTOM_LEFT);

        filterLabel = new InteractiveLabel(28, 250, 70, Pos.BOTTOM_CENTER,
                () -> {nextFilter();},
                () -> {previousFilter();},
                null);
        StackPane.setAlignment(filterLabel, Pos.BOTTOM_CENTER);

        tickLabels = new InteractiveLabel[numberOfTicks];
        tickLabelVBox = new VBox();
        tickLabelVBox.setAlignment(Pos.CENTER_RIGHT);
        //its a bit weird, the tick label now is managing some of its values itself - while all other stuff is done in the gallery, not the best OO today..
        //also like text size and stuff. Still, its height is a public constant from this class. 
        for (int i = 0; i < numberOfTicks; i++) {
            TickLabel currentTickLabel = new TickLabel(i, numberOfTicks, Pos.CENTER_RIGHT, new TickLabelClickAction(i), new TickLabelMidAction(i + numberOfCategories + 1));
            currentTickLabel.setText(getTickName(i));
            tickLabelVBox.getChildren().add(0, currentTickLabel);
            tickLabels[i] = currentTickLabel;
        }
        tickLabelVBox.setMaxSize(0, 0); //also done in the ImprovisedProgressBar, makes the container only occupy the min possible size
        StackPane.setAlignment(tickLabelVBox, Pos.BOTTOM_RIGHT);

        tickLabelVBox.setOnMouseEntered((e) -> {
            setAllTickLabelsToFullOpacity = true;
            updateLabels();
        });
        tickLabelVBox.setOnMouseExited((e) -> {
            setAllTickLabelsToFullOpacity = false;
            updateLabels();
        });

        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        errorLabel.setFont(new Font(15));
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        loadingProgress = new ProgressIndicator(0.2);
        loadingProgress.setMaxSize(50, 50);
        loadingProgress.setVisible(false);
        
        Rectangle zoomIndicatorBack = new Rectangle(80, 30, Color.BLACK);
        zoomIndicatorBack.setArcHeight(15);
        zoomIndicatorBack.setArcWidth(15);
        zoomIndicatorBack.setOpacity(0.7);
        zoomIndicatorText = new Text("xx%");
        zoomIndicatorText.setFont(Font.font(zoomIndicatorText.getFont().getFamily(), FontWeight.NORMAL, FontPosture.REGULAR, 15));
        zoomIndicatorText.setFill(Color.WHITE);
        zoomIndicatorText.setTextAlignment(TextAlignment.CENTER);
        zoomIndicator = new StackPane(zoomIndicatorBack, zoomIndicatorText);
        zoomIndicator.setMaxSize(0, 0);
        zoomIndicator.setMouseTransparent(true);
        zoomIndicator.setVisible(false);
        
        resolutionTextEstimate = new Text("xK filler");
        resolutionTextEstimate.setFont(Font.font(resolutionTextEstimate.getFont().getFamily(), FontWeight.BOLD, FontPosture.REGULAR, 14));
        resolutionTextDimension = new Text("filler text");
        resolutionTextDimension.setFont(Font.font(resolutionTextDimension.getFont().getFamily(), 12));
        resolutionTextDimension.setFill(Color.BLACK);
        resolutionGraphicRects = new Rectangle[NUMBER_OF_RESOLUTION_RECTS];
        HBox resolutionGraphic = new HBox(3);
        for (int i = 0; i < NUMBER_OF_RESOLUTION_RECTS; i++) {
            Rectangle r = new Rectangle(5, 5);
            r.setFill(Color.BLACK);
            r.setStroke(Color.BLACK);
            resolutionGraphic.getChildren().add(r);
            resolutionGraphicRects[i] = r;
        }
        resolutionGraphic.setAlignment(Pos.CENTER);
        VBox resolutionIndicatorBox = new VBox(resolutionTextEstimate, resolutionTextDimension, resolutionGraphic);
        resolutionIndicatorBox.setMaxSize(0, 0);
        //resolutionIndicator.setSpacing(5);
        resolutionIndicatorBox.setAlignment(Pos.CENTER);
        VBox.setMargin(resolutionGraphic, new Insets(5, 0, 0, 0));
        resolutionIndicator = new StackPane(resolutionIndicatorBox);
        resolutionIndicator.setMouseTransparent(true);
        resolutionIndicator.setVisible(false);
        resolutionIndicator.setMaxSize(0, 0);
        StackPane.setMargin(resolutionIndicatorBox, new Insets(4, 7, 4, 7));
        resolutionIndicator.setBackground(new Background(new BackgroundFill(new Color(1, 1, 1, 0.6), new CornerRadii(5), null)));
        resolutionIndicator.setOpacity(0.7f);
        StackPane.setAlignment(resolutionIndicator, Pos.BOTTOM_CENTER);
        StackPane.setMargin(resolutionIndicator, new Insets(0, 0, 38, 0));
        
        progress = new ImprovisedProgressBar(350, 30, resolutionIndicator, zoomIndicator);
        StackPane.setAlignment(progress, Pos.TOP_CENTER);
        progress.setOnScroll(zoomPaneScrollHandler);
        
        rotationIndicator = new StackPane();
        Text rotationIndicatorText = new Text("(rotating image)");
        rotationIndicatorText.setFont(new Font(28));
        rotationIndicatorText.setFill(Color.WHITE);
        rotationIndicatorText.setStroke(Color.BLACK);
        rotationIndicatorText.setStrokeWidth(0.5);
        rotationIndicatorText.setTextAlignment(TextAlignment.CENTER);
        rotationIndicator.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.5), new CornerRadii(10), null)));
        StackPane.setMargin(rotationIndicatorText, new Insets(20, 40, 20, 40));

        rotationIndicator.setMaxSize(0, 0);
        rotationIndicator.getChildren().add(rotationIndicatorText);
        rotationIndicator.setMouseTransparent(true);
        rotationIndicator.setVisible(false);

        MenuItem menuFileName = new MenuItem("here should be the file name");
        menuFileName.setStyle("-fx-font-style: italic;");
        menuFileName.setDisable(true);
        MenuItem menuRotate = new MenuItem("Rotate JPG by 90°");
        menuRotate.setAccelerator(new KeyCodeCombination(KeyCode.R)); //somehow (but I'm glad about that) the accelerators do not actually do an onAction call. Whereever this stems from. Maybe I consume the keystroke events myself before or so. 
        menuRotate.setOnAction((event) -> {rotateBy90Degrees(true);});
        MenuItem menuShowOpenWith = new MenuItem("Open with ...");
        menuShowOpenWith.setOnAction((event) -> {showOpenWithDialog();});
        menuShowOpenWith.setAccelerator(new KeyCodeCombination(KeyCode.ENTER));
        MenuItem menuShowFile = new MenuItem("Show in explorer");
        menuShowFile.setAccelerator(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN));
        menuShowFile.setOnAction((event) -> {showInExplorer();});
        MenuItem menuCopy = new MenuItem("Copy image");
        menuCopy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        menuCopy.setOnAction((event) -> {copyImageToClipboard(false);});
        MenuItem menuCopyPath = new MenuItem("Copy image path");
        menuCopyPath.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        menuCopyPath.setOnAction((event) -> {copyImageToClipboard(true);});
        MenuItem menuUndo = new MenuItem("Undo last deletion");
        menuUndo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        menuUndo.setOnAction((event) -> {undoDelete();});
        MenuItem menuDelete = new MenuItem("Move to '/delete'");
        menuDelete.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        menuDelete.setOnAction((event) -> {deleteImage();});
        //accelerator styling is done in the scene style sheets further below
        
        Rectangle invisibleContextMenuSource = new Rectangle();
        invisibleContextMenuSource.setVisible(false);
        ContextMenu contextMenu = new ContextMenu(menuFileName, new SeparatorMenuItem(), menuShowOpenWith, menuShowFile, menuCopy, menuCopyPath, menuRotate, menuDelete, menuUndo);
        view.setOnContextMenuRequested((event) -> {
            menuFileName.setText(currentImage);
            menuRotate.setVisible(isRotateFeatureAvailable());
            contextMenu.show(invisibleContextMenuSource, event.getScreenX(), event.getScreenY());
            //note that we set invisibleContextMenuSource as anchor and not the view. This is because the context menu hides when
            //the ancor loses focus, and while zooming and scrolling on the view it doesnt so the context menu stays
            //so this is a slight hack, just use anything else thats probably not even visible that will instantly lose focus
        });

        noImagesLabel = new Label("There are no images. ");
        noImagesLabel.setTextFill(Color.GREY);
        noImagesLabel.setVisible(false);
        noImagesLabel.setFont(new Font(15));
        noImagesLabel.setTextAlignment(TextAlignment.CENTER);
        
        imageAndLoadingPane = new StackPane();
        imageAndLoadingPane.getChildren().add(loadingProgress);
        imageAndLoadingPane.getChildren().add(errorLabel);
        imageAndLoadingPane.getChildren().add(zoomPane);

        wrapSearchIndicator = new Text("./.");
        wrapSearchIndicator.setFont(new Font(85));
        wrapSearchIndicator.setFill(Color.WHITE);
        wrapSearchIndicator.setStroke(Color.BLACK);
        wrapSearchIndicator.setStrokeWidth(2);
        wrapSearchIndicator.setVisible(false);
        wrapSearchIndicator.setMouseTransparent(true);

        Rectangle exitFullscreenRect = new Rectangle(HOT_CORNER_SIZE, HOT_CORNER_SIZE);
        exitFullscreenRect.setFill(Color.TRANSPARENT);
        StackPane.setAlignment(exitFullscreenRect, Pos.TOP_RIGHT);
        Text exitFullscreenText = new Text("Click to exit full-screen");
        exitFullscreenText.setFont(new Font(40));
        exitFullscreenText.setFill(Color.WHITE);
        exitFullscreenText.setStroke(Color.BLACK);
        Rectangle exitFullscreenHintBackground = new Rectangle(600, 200);
        exitFullscreenHintBackground.setFill(Color.BLACK);
        exitFullscreenHintBackground.setOpacity(0.65);
        exitFullscreenHintBackground.setArcHeight(20);
        exitFullscreenHintBackground.setArcWidth(20);

        //this is neat, but on the long run I might replace this with a little console log that quickly disappears, then you can also add some text to it
        actionIndicatorC1 = new Circle(50, 50, 30, Color.WHITE);
        actionIndicatorC2 = new Circle(50, 50, 20, Color.LIGHTGRAY);
        actionIndicatorPane = new Pane();
        actionIndicatorPane.getChildren().addAll(actionIndicatorC1, actionIndicatorC2);
        actionIndicatorPane.setMaxSize(100, 100);
        actionIndicatorPane.setMinSize(100, 100);
        actionIndicatorPane.setMouseTransparent(true);
        actionIndicatorPane.setVisible(false);
        StackPane.setAlignment(actionIndicatorPane, Pos.TOP_RIGHT);
        StackPane.setMargin(actionIndicatorPane, new Insets(20));
        
        StackPane exitFullscreenHint = new StackPane(exitFullscreenHintBackground, exitFullscreenText); 
        exitFullscreenHint.setMouseTransparent(true);
        exitFullscreenHint.setMaxSize(0, 0);
        exitFullscreenHint.setVisible(false);

        exitFullscreenRect.setOnMouseEntered((e) -> {exitFullscreenHint.setVisible(true);});
        exitFullscreenRect.setOnMouseExited((e) -> {exitFullscreenHint.setVisible(false);});
        exitFullscreenRect.setOnMouseClicked((e) -> {stage.setFullScreen(false);});
        
        hideUiHotcorner = new Rectangle(HOT_CORNER_SIZE, HOT_CORNER_SIZE);
        hideUiHotcorner.setFill(Color.TRANSPARENT);
        StackPane.setAlignment(hideUiHotcorner, Pos.TOP_LEFT);
        hideUiHotcorner.setCursor(Cursor.NONE);
        hideUiHotcorner.setOnMouseEntered((e) -> {
            label.setVisible(false);
            progress.setVisible(false);
            tickLabelVBox.setVisible(false);
            filterLabel.setVisible(false);
        });
        hideUiHotcorner.setOnMouseExited((e) -> {
            label.setVisible(true);
            progress.setVisible(true);
            tickLabelVBox.setVisible(true);
            filterLabel.setVisible(true);
        });
        hideUiHotcorner.setOnScroll(zoomPaneScrollHandler);

        stage.fullScreenProperty().addListener((target, oldValue, newValue) -> {
            exitFullscreenRect.setVisible(newValue);
            hideUiHotcorner.setVisible(newValue);
        });
        
        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        rootPane.getChildren().add(invisibleContextMenuSource);
        rootPane.getChildren().add(noImagesLabel);
        rootPane.getChildren().add(imageAndLoadingPane);
        rootPane.getChildren().add(wrapSearchIndicator);
        
        leftButton = new LRButton(rootPane, true); //this also adds them to the rootPane
        rightButton = new LRButton(rootPane, false);
        leftButton.setOnScroll(zoomPaneScrollHandler);
        rightButton.setOnScroll(zoomPaneScrollHandler);
        
        rootPane.getChildren().add(label);
        rootPane.getChildren().add(filterLabel);
        rootPane.getChildren().add(tickLabelVBox);
        rootPane.getChildren().add(progress);
        rootPane.getChildren().add(resolutionIndicator);
        rootPane.getChildren().add(exitFullscreenRect);
        rootPane.getChildren().add(exitFullscreenHint);
        rootPane.getChildren().add(hideUiHotcorner);
        rootPane.getChildren().add(rotationIndicator);
        rootPane.getChildren().add(actionIndicatorPane);

        Scene scene = new Scene(rootPane, 800, 600);
        stage.setScene(scene);
        scene.getStylesheets().add(Common.getResourcePath("style.css"));
        stage.maximizedProperty().addListener((observable) -> {if (stage.isMaximized()) stage.setFullScreen(true);});
        stage.setOnCloseRequest((event) -> {
            handleImageRotationIfNecessary();
            updateImageRotation(); //special case: imagine we rotate an image and close, and the rotated image cant be written. Then the rotatedImage internally resets its orientation, but this is not yet shown in the gallery (if we choose to return instead of closing the window). Therefore, we update the rotation here. 

            boolean unsavedChanges = false;
            ArrayList<ArrayList<String>> operations = listAllFileOperations();
            //we start at 1, because 0 does not move the files
            for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
                if (!operations.get(i).isEmpty()) {
                    unsavedChanges = true;
                    break; 
                }
            }
            if (unsavedChanges) {
                String closeMessage, closeHeader;
                closeMessage = "Should we now do the following?\n\n";
                for (int i = 1; i < numberOfCategories + numberOfTicks + 1; i++) {
                    if (!operations.get(i).isEmpty()) {
                        if (i < numberOfCategories + 1) {
                            closeMessage += "   move " + operations.get(i).size() + " ";
                            closeMessage += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                            closeMessage +=  targetDirectory.getName() + "/" + i + "\n";
                        } else {
                            closeMessage += "   copy " + operations.get(i).size() + " ";
                            closeMessage += Common.getSingularOrPluralOfFile(operations.get(i).size()) + " to ";
                            closeMessage += targetDirectory.getName() + "/" + getTickName(i - numberOfCategories - 1) + "\n";
                        }
                    }
                }
                boolean[] operationTypes = getFileOperationTypes(numberOfTicks, numberOfCategories, operations);
                boolean moveOperation = operationTypes[0], copyOperation = operationTypes[1];
                closeHeader = moveOperation ? copyOperation ? "Move & copy files now?" : "Move files now?" : "Copy files now?";
                closeMessage += "\n'No' keeps the files unchanged and closes the gallery, which discards your work here. ";
                Alert closeAlert = new Alert(AlertType.NONE, closeMessage, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                closeAlert.setHeaderText(closeHeader);
                Optional<ButtonType> result = closeAlert.showAndWait();
                if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
                    //prevent window close by consuming event
                    event.consume();
                    return;
                } else if (result.get() == ButtonType.YES) {
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
                                + getTickName(i) + FileSystems.getDefault().getSeparator();
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
                        //Consume the close event, so that the window is actually not closed. 
                        event.consume();
                        return;
                    }
                }
            }
            //the event was not consumed: the window will continue closing now
            if (reopenLauncherAfterwards) {
                new Launcher().start(new Stage());
            }
        });

        rootPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                //Fuck switch cases. 
                if (event.getCode() == KeyCode.RIGHT) {// || event.getCode() == KeyCode.D) {
                    if (event.isShortcutDown() && event.getCode() == KeyCode.RIGHT) {
                        selectImageAtIndex(images.size()-1); //last image
                    } else if (event.isShiftDown() && event.getCode() == KeyCode.RIGHT) {
                        nImagesForth(25);
                    } else if (event.isAltDown() && event.getCode() == KeyCode.RIGHT) {
                        rotateBy90Degrees(true);
                    } else {
                        nextImage();
                    }
                } else if (event.getCode() == KeyCode.LEFT) {//} || event.getCode() == KeyCode.A) {
                    if (event.isShortcutDown() && event.getCode() == KeyCode.LEFT) {
                        selectImageAtIndex(0); //first image
                    } else if (event.isShiftDown() && event.getCode() == KeyCode.LEFT) {
                        nImagesBack(25);
                    } else if (event.isAltDown() && event.getCode() == KeyCode.LEFT) {
                        rotateBy90Degrees(false);
                    } else {
                        prevImage();
                    }
                } else if (event.getCode() == KeyCode.HOME) {
                    selectImageAtIndex(0);
                } else if (event.getCode() == KeyCode.END) {
                    selectImageAtIndex(images.size()-1);
                } else if (event.getCode() == KeyCode.UP) {// || event.getCode() == KeyCode.W) {
                    if (event.isShortcutDown()) {
                        nextFilter();
                    } else {
                        incrementCurrentImageCategory();
                    }
                } else if (event.getCode() == KeyCode.DOWN) {// || event.getCode() == KeyCode.S) {
                    if (event.isShortcutDown()) {
                        previousFilter();
                    } else {
                        decrementCurrentImageCategory();
                    }
                } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    deleteImage();
                } else if (event.getCode() == KeyCode.PLUS) {
                    increaseZoom(40);
                } else if (event.getCode() == KeyCode.MINUS) {
                    decreaseZoom(40);
                } else if (event.getCode() == KeyCode.SPACE) {
                    if (isZooming) zoomTo100Percent();
                //} else if (event.getCode() == KeyCode.ENTER) {
                //    view.setSmooth(!view.isSmooth());
                //    view.setCache(false);
                } else if (event.getCode() == KeyCode.F5) {
                    imageBuffer.clear(); // it is getting refilled with the updateFilesList() call, which ultimatively calls loadImage()
                    updateFilesList();
                    showActionIndicator();
                } else if (event.getCode().isDigitKey()) {
                    String keyName = event.getCode().getName();
                    int skim = Integer.valueOf(keyName.substring(keyName.length() - 1, keyName.length())); 
                    //hacky hacky. Its either '5' or 'Numpad 5', so we take the last char
                    skimTo(skim);
                } else if (event.getCode() == KeyCode.Z && event.isShortcutDown()) {
                    undoDelete();
                    showActionIndicator();
                } else if (event.getCode() == KeyCode.C && event.isShortcutDown()) {
                    copyImageToClipboard(event.isShiftDown() || event.isAltDown());
                } else if (event.getCode() == KeyCode.F11 || (event.getCode() == KeyCode.ENTER && event.isAltDown()) || (event.getCode() == KeyCode.F && numberOfTicks < 6)) {
                    //https://stackoverflow.com/questions/51386423/remove-beep-sound-upon-maximizing-javafx-stage-with-altenter
                    //I have no idea why windows plays the beep on alt+enter (and not on ANY other combination), accelerators also don't work. 
                    //TODO accelerators might actually be the better solution for all shortcuts. Except maybe the + and -?
                    stage.setFullScreen(!stage.isFullScreen());
                } else if (event.getCode() == KeyCode.ENTER) {
                    if (event.isShortcutDown() || event.isShiftDown()) {
                        showInExplorer();
                    } else {
                        showOpenWithDialog();
                    }
                } else if (event.getCode() == KeyCode.R) {
                    rotateBy90Degrees(!event.isShortcutDown() && !event.isShiftDown());
                } else if (event.getCode() == KeyCode.Q) {
                    rotateBy90Degrees(false);
                } else if (event.getCode() == KeyCode.E) {
                    rotateBy90Degrees(true);
                } else if (event.getCode().isLetterKey()) { //interestingly, is false for language specific letters like ö and ß in german. 
                    int pos = Common.getPositionInAlphabet(event.getCode().getChar().charAt(0));
                    if (pos >= 0 && pos < numberOfTicks) {
                        toggleCurrentImageTick(pos);
                    }
                }
                //else if (event.getCode() == KeyCode.ESCAPE) {
                //    if (!stage.isFullScreen()) {
                //        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
                //    }
                //}
            }

        });

        //stage.focusedProperty().addListener((a, oldV, newV) -> {if (!newV && stage.isFullScreen()) stage.setFullScreen(false);});

        stage.getIcons().add(Common.getResource("logo"));
        stage.setMinHeight(400);
        stage.setMinWidth(600);
        stage.show();

        rootPane.widthProperty().addListener((a, oldV, newV) -> {updateViewport(newV.doubleValue(), rootPane.heightProperty().get());});
        rootPane.heightProperty().addListener((a, oldV, newV) -> {updateViewport(rootPane.widthProperty().get(), newV.doubleValue());});

        view.requestFocus();

        updateFilesList(); // includes a call to updateFilter and loadImage()

        if (showHints) {
            Alert useInfo = new Alert(AlertType.NONE, null, ButtonType.OK);
            useInfo.setHeaderText("How to use");
            useInfo.setContentText(
                "Arrow keys to look through images and mark them to be moved to a folder (keep in current or move to \\1, \\2 or \\3). \n"+
                "Keys A, B, C, D to mark the images to be copied to the corresponding folder \\a, \\b, ... "+
                "Del or Backspace to instantly move to a 'delete' folder. Ctrl + Z to undo.\n"+
                "Click to zoom. Scroll, + and - to change the zoom strength. Generally, the whole interface is scrollable! \n"+
                "Context menu to show file in explorer or open with. Youtube-like skimming with number keys.\n"+
                "Close the window to perform the file operations (don't worry, you will be asked for confirmation).\n"+
                "When executing the file operations, first all copy operations are done, and afterwards the move operations.\n\n"+
                "Find this project on github.com/Racopokemon/ImageSort"
            );
            useInfo.showAndWait();
        }

        stage.setFullScreen(true);
    }

    private class LRButton extends StackPane {
        //private boolean left;
        public LRButton(StackPane root, boolean left) {
            Rectangle rect = new Rectangle(BUTTON_WIDTH, 10, LR_HALF_TRANSPARENT);
            //this.left = left;
            StackPane.setAlignment(this, left ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
            setMaxWidth(BUTTON_WIDTH);
            maxHeightProperty().bind(root.heightProperty());
            rect.heightProperty().bind(root.heightProperty());
            root.getChildren().add(this);
            setOnMouseEntered((event) -> {setOpacity(1.0);});
            setOnMouseExited((event) -> {setOpacity(0.0);});
            setOnMouseClicked((event) -> {
                if (left) {
                    prevImage();
                } else {
                    nextImage();
                }
            });

            double mirror = left ? 1 : -1;
            double scale = 10;
            Polyline arrow = new Polyline(
                scale*mirror, -scale*2, 
                -scale*mirror, 0,
                scale*mirror, scale*2);
            arrow.setStrokeWidth(3);
            arrow.setStrokeLineCap(StrokeLineCap.ROUND);
            arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
            arrow.setStroke(LR_ARROW_COLOR);

            setOpacity(0.0);
            getChildren().addAll(rect, arrow);
        }
    }

    //sets up the viewport for the current image in the view and the rootPane size: 
    //if the image is smaller than the viewport, dont scale it. 
    //Call this when the rootPane size has NOT changed (opposed to the overloaded updateViewport(w, h) if the size has changed)
    private void updateViewport() {
        updateViewport(rootPane.widthProperty().get(), rootPane.heightProperty().get());
    }

    //handle images that are smaller than the viewport, in this case make them NOT scale. 
    //Call this overloaded variant if the rootPane size is changing currently, provide the new size in the arguments. 
    private void updateViewport(double w, double h) {
        Image i = view.getImage();
        if (i == null) {
            return;
        }
        boolean ninetyDegrees = ((RotatedImage)i).isRotatedBy90Degrees();
        double iWidth = ninetyDegrees ? i.getHeight() : i.getWidth();
        double iHeight = ninetyDegrees ? i.getWidth() : i.getHeight();
        if (w < iWidth || h < iHeight) {
            //image is bigger than viewport, just let it fit automatically (I guess? Why didnt I write the comments when I was working on this?)
            view.setViewport(null);
        } else {
            //image is smaller than viewport, manually adapt the viewport
            if (ninetyDegrees) {
                //this line is brought to you by trial and error
                view.setViewport(new Rectangle2D((int)((iHeight-h)*0.5), (int)((iWidth-w)*0.5), h, w));
                //TODO zoom is simply wrong on this :(. Also we are not pixel accurate, ONLY on rotated images :(
            } else {
                view.setViewport(new Rectangle2D((int)((iWidth-w)*0.5), (int)((iHeight-h)*0.5), w, h));
            }
        }
    }

    //Calculates the pixel scale of the image, dependent on the window and image size (without considering current user zoom)
    //1 is 1 to 1 scaling, as it automatically happens when the image is smaller than the window. 
    //If the image is bigger than the window, we return the scale factor < 1, that is applied to it to make it fit the window
    private double calculateBaseImageScale() {
        Image i = view.getImage();
        boolean ninetyDegrees = ((RotatedImage)i).isRotatedBy90Degrees();
        double imgW = ninetyDegrees ? i.getHeight() : i.getWidth();
        double imgH = ninetyDegrees ? i.getWidth() : i.getHeight();
        
        double windowW = rootPane.getWidth();
        double windowH = rootPane.getHeight();
        if (windowW > imgW && windowH > imgH) {
            //this case is handeled manually in updateViewpoint - if the image is smaller than the viewport, we see it at 100%, so factor 1
            return 1;
        } else {
            //in the other case, we must keep the image ratio, so we calculate the required zoom factor for both axis and take the smaller one, to keep the image entirely inside the window
            double xFactor = windowW / imgW; //illuminati confirmed??
            double yFactor = windowH / imgH;
            return Math.min(xFactor, yFactor);
        }
    }

    //Tries to zoom into the image, so that pixels are scaled 1 to 1. 
    //However, always stays inside the valid bounds for zooming. 
    private void zoomTo100Percent() {
        if (currentImage == null) {
            return;
        }
        double requiredZoom = 1 / calculateBaseImageScale();
        zoom = Math.min(Math.max(requiredZoom, MIN_ZOOM), MAX_ZOOM);

        if (isZooming) {
            zoomIn();
        }
    }

    // Zooms into the whole scene (the easy way, just setting the scale properties)
    // Also updates the translation, that's the reason it is called so many times
    private void zoomIn() {
        //we used to work with the zoomPane (obvious choice) here. 
        //However, for some reasons does the zoomPane grow in certain situations above screen size, probably because the imageview 
        //makes weird space requirements merging the space of the rotated and un-rotated image ...
        //this additional space requirement is for some reason not visible (maybe because the stack pane centers anyway, no matter the space around)
        //but it destroyed our size calculations here
        double width = rootPane.getWidth(); 
        double height = rootPane.getHeight();
        //the non-visible space once zoomed in
        double spaceX = (width*zoom) - (width); //add stupid elon joke here
        double spaceY = (height*zoom) - (height);
        //we get the mouse position relative to the screen, 0 is left, 1 right etc. 
        //now just scale the additional space with this relative mouse percentage!
        double relTransX = -(mouseRelativeX - 0.5);
        double relTransY = -(mouseRelativeY - 0.5);

        zoomPane.setScaleX(zoom);
        zoomPane.setScaleY(zoom);

        zoomPane.setTranslateX(spaceX * relTransX);
        zoomPane.setTranslateY(spaceY * relTransY);

        zoomIndicator.setVisible(true);

        zoomPane.setCursor(Cursor.NONE);
        resolutionIndicator.setVisible(true);
        
        isZooming = true;

        updateZoomIndicatorText();
    }
    // Resets the zoom to the usual 1:1 in the app
    private void zoomOut() {
        zoomPane.setScaleX(1);
        zoomPane.setScaleY(1);
        zoomPane.setTranslateX(0);
        zoomPane.setTranslateY(0);

        zoomIndicator.setVisible(false);
        resolutionIndicator.setVisible(false);
        zoomPane.setCursor(Cursor.DEFAULT);
        isZooming = false;

        updateZoomIndicatorText();
    }
    private void increaseZoom(double scale) {
        if (currentImage == null) return;

        zoom *= Math.pow(1.01, scale);
        if (zoom > MAX_ZOOM) {
            zoom = MAX_ZOOM;
        }
        if (isZooming) {
            zoomIn();
        }
    }
    private void decreaseZoom(double scale) {
        if (currentImage == null) return;

        zoom /= Math.pow(1.01, scale);
        if (zoom < MIN_ZOOM) {
            zoom = MIN_ZOOM;
        }
        if (isZooming) {
            zoomIn();
        }
    }
    //Sets the mouse position from the given MouseEvent. 
    //We store it externally and dont simply pass it over when zooming in,
    //because when the zoom (scale) changes, we don't get new information on
    //the mouse, but need to update the zoom based on the mouse position
    private  void setMousePosition(MouseEvent event) {
        mouseRelativeX = event.getSceneX() / rootPane.getWidth();
        mouseRelativeY = event.getSceneY() / rootPane.getHeight();
        mouseRelativeX = Math.max(Math.min(mouseRelativeX, 1), 0);
        mouseRelativeY = Math.max(Math.min(mouseRelativeY, 1), 0);
    }

    private void showInExplorer() {
        if (currentImage == null) return;
        Common.showFileInExplorer(getFullPathForFileInThisFolder(currentImage));
        if (stage.isFullScreen()) stage.setFullScreen(false);
        showActionIndicator();
    }

    private void showOpenWithDialog() {
        if (currentImage == null) return;
        if (!Common.isWindows()) return;

        //runs the cmd command Rundll32 Shell32.dll,OpenAs_RunDLL any-file-name.ext to show the windows 'open with' dialog currentImage:
        try {
            Runtime.getRuntime().exec("Rundll32 Shell32.dll,OpenAs_RunDLL " + getFullPathForFileInThisFolder(currentImage));
            showActionIndicator();
        } catch (IOException e) {
            System.out.println("Could not show 'open with' dialog for file " + currentImage + ":");
            e.printStackTrace();
        }
}

    private ChangeListener<? super Number> numberListener = (a, b, c) -> {updateImageLoadingStatus();};
    private ChangeListener<? super Boolean> booleanListener = (a, b, c) -> {updateImageLoadingStatus();};
    private void loadImage() {
        //Preprocessing: Checking all currently contained images for loading errors due to memory limitations, and mitigation
        if (currentImage != null) {
            ArrayList<String> entriesToDelete = new ArrayList<>();
            for (Map.Entry<String, RotatedImage> entry : imageBuffer.entrySet()) {
                Exception e = entry.getValue().getException();
                if (e != null && isMemoryException(e)) {
                    entriesToDelete.add(entry.getKey());
                }
            }
            if (!entriesToDelete.isEmpty()) {
                //Memory errors occured
                for (String s : entriesToDelete) {
                    imageBuffer.remove(s);
                }
                handleMemoryError();
                successfullLoadsWithoutMemoryProblems--;
            } else {
                if (imageBufferSize == 0 || (imageBuffer.containsKey(currentImage) && imageBuffer.get(currentImage).getHeight() > 0)) {
                    //height > 0 means the image has finished loading
                    if (imageBufferSize < IMAGE_BUFFER_SIZE_MAX && ++successfullLoadsWithoutMemoryProblems > 4) {
                        imageBufferSize++;
                        successfullLoadsWithoutMemoryProblems = 0;
                        System.out.println("Incremented imageBufferSize to " + imageBufferSize);
                    }
                }
            }
        }
        //End preprocessing. ... never had these issues on my machine, for reconstruction I limited the jvm heap space

        //Step 1: We update the imageBuffer, which essentially does the actual image loading
        //If nothing has changed, thats no problem at all, but if it has, its already launched here

        int currentImageIndex = getCurrentImageIndex();
        Hashtable<String, RotatedImage> newImageBuffer = new Hashtable<>();

        //TEMP quick and dirty try to make the loading of the first image faster
        if (!imageBuffer.containsKey(currentImage)) {
            newImageBuffer.put(currentImage, new RotatedImage(new File (getFullPathForFileInThisFolder(currentImage))));
        }
        //END TEMP (since this call also happens in the loop down here:)

        for (int cursor = -imageBufferSize; cursor <= imageBufferSize; cursor++) {
            int actualIndex = (currentImageIndex + cursor) % images.size();
            if (actualIndex < 0) {
                actualIndex += images.size();
            }
            String imageNameAtCursor = images.get(actualIndex);
            if (imageBuffer.containsKey(imageNameAtCursor)) {
                newImageBuffer.put(imageNameAtCursor, imageBuffer.get(imageNameAtCursor));
            } else {
                newImageBuffer.put(imageNameAtCursor, new RotatedImage(new File (getFullPathForFileInThisFolder(imageNameAtCursor))));
            }
        }
        //initially we were cool and didnt have this loop, but this really helps when scrolling rapidly through the pics.
        for (Map.Entry<String, RotatedImage> e : imageBuffer.entrySet()) {
            if (!newImageBuffer.containsKey(e.getKey())) {
                //will be garbage collected. Lets stop the thread manually.
                e.getValue().cancel();
            }
        }
        imageBuffer = newImageBuffer; //this also discards all images only on the old imageBuffer 

        ///Even though we have this buffering and everything runs in background, it sometimes still lags for a sec until we get the next pic. 
        ///I think this comes from something I cant influence, since it also happens with pics that should be constantly loaded already 
        ///and the pictures dont show up from black, but just late. maybe the images are loaded onto the gpu for hardware acc rendering or so.

        //Step 2: the imageBuffer also contains the current image. If it was just added, or already
        //preloaded before, or still loading, who cares. We just show it! (If its still loading, it will show once its ready)
        RotatedImage img = imageBuffer.get(currentImage);
        RotatedImage oldImage = (RotatedImage)view.getImage();
        //unregister old image listeners (what if theyre still called?.. okay change the img once i guess and its fixed)
        if (oldImage != null) {
            oldImage.progressProperty().removeListener(numberListener);
            oldImage.heightProperty().removeListener(numberListener);
            oldImage.errorProperty().removeListener(booleanListener);
        }

        if (!img.isError() && img.isStillLoading()) {
            //means: still loading, need to register listeners because the view might change
            img.progressProperty().addListener(numberListener);
            img.heightProperty().addListener(numberListener);
            img.errorProperty().addListener(booleanListener);
        }

        if (oldImage != img) {
            handleImageRotationIfNecessary();
        }

        view.setImage(img);
        if (oldImage != img) {
            updateImageRotation();
            if (DEBUG_PRINT_IMAGE_METADATA) {
                System.out.println("\n-------------------------------------------------");
                System.out.println(currentImage);
                System.out.println("-------------------------------------------------");
                ((RotatedImage)view.getImage()).printImageMetadata();
            }
        }
        
        ArrayList<String> imageInfo = new ArrayList<String>();
        String imageNameWithAlongMovers = currentImage;
        if (filesToMoveAlong.containsKey(currentImage)) {
            ArrayList<String> alongMovers = filesToMoveAlong.get(currentImage);
            if (alongMovers.size() > 0)
                imageNameWithAlongMovers += " [also ." + Common.getExtensionFromFilename(alongMovers.get(0));
            if (alongMovers.size() > 1)
                imageNameWithAlongMovers += ", ." + Common.getExtensionFromFilename(alongMovers.get(1));
            if (alongMovers.size() > 3) {
                imageNameWithAlongMovers += " and " + (alongMovers.size()-2) + " more";
            } else if (alongMovers.size() > 2) {
                imageNameWithAlongMovers += ", " + Common.getExtensionFromFilename(alongMovers.get(2));
            }
            if (alongMovers.size() > 0)
                imageNameWithAlongMovers += "]";
        }
        imageInfo.add(imageNameWithAlongMovers);
        imageInfo.addAll(img.getSomeImageProperties());
        progress.setProgress(currentImageIndex, images.size(), deleteHistory.size(), filter != -1, imageInfo);
        wrapSearchIndicator.setText(currentImageIndex+1 + "/" + images.size()); //kind of doubled here, but I think its important that the indicator is updated as well
        updateLabels();
        updateImageLoadingStatus();
    }

    private void handleImageRotationIfNecessary() {
        RotatedImage img = (RotatedImage) view.getImage();
        if (img == null) {
            return;
        }
        if (!img.doPreviewAndFileOrientationMatch()) {
            rotationIndicator.setVisible(true); //The longer I think about it, the more I think that this will ofc never be seen by anyone
            String rotationResult = img.writeCurrentOrientationToFile();
            if (rotationResult != null) {
                Alert errorMessage = new Alert(AlertType.NONE, "Sadly, while writing the new image rotation to file, we encountered the following error: \n\n"+rotationResult, ButtonType.OK);
                errorMessage.setTitle("Rotation Error");
                errorMessage.showAndWait();
            }
            rotationIndicator.setVisible(false);
        }
    }

    private void updateImageLoadingStatus() {
        Image img = view.getImage();
        if (img.getHeight() > 0) {
            //image already loaded
            loadingProgress.setVisible(false);
            errorLabel.setVisible(false);
            updateViewport();
            updateResolutionIndicator(img);
            updateZoomIndicatorText();
        } else if (img.isError()) {
            //error while loading
            loadingProgress.setVisible(false);
            errorLabel.setVisible(true);
            if (isMemoryException(img.getException())) {
                errorLabel.setText("Sadly, we could not load " + currentImage + " because there is not enough memory.\n"+
                "We are already decreasing the number of preloaded images to counter this, so that you hopfeully \n" +
                "won't see this message again. ");
                handleMemoryError();
                loadImage();
            } else {
                errorLabel.setText("We could not load " + currentImage + ": \n" + img.getException().toString());
                System.out.println("Error occured when loading image " + currentImage);
                img.getException().printStackTrace();
            }
        } else {
            //just still loading
            loadingProgress.setVisible(true);
            errorLabel.setVisible(false);
            loadingProgress.setProgress(img.getProgress());
        }
        if (isZooming) {
            zoomIn();
        }
    }

    private boolean isMemoryException(Exception e) {
        String message = e.getMessage();
        if (message == null) { //Had a weird case crashing this line of code, where some NIO exception (or so) did actually return null as message. Didn't know this was a thing for exceptions. 
            return false;
        } else {
            return message.startsWith("java.lang.OutOfMemoryError");
        }
    }

    //On machines with small amounts of RAM (?) and with big photos, 
    //loading 4+4+1 images quickly runs into the heap space limitations. 
    //We check for this issue every time a new image is shown as well as when the current image runs into this error during loading. 
    //Then this method is called to decrease the buffer number. 
    //This does however not reload any images or refresh the buffer, that needs to be done in addition to calling here. 
    private void handleMemoryError() {
        if (--imageBufferSize < IMAGE_BUFFER_SIZE_MIN) {
            imageBufferSize = IMAGE_BUFFER_SIZE_MIN;
        }
        System.out.println("Reduced imageBufferSize to "+imageBufferSize);
    }

    private void updateResolutionIndicator(Image img) {
        resolutionTextEstimate.setText(Common.getBiggestContainingResolution(img.getHeight(), img.getWidth()));
        String status;
        if (((RotatedImage)img).isRotatedBy90Degrees()) {
            status = Math.round(img.getHeight()) + " x " + Math.round(img.getWidth());
        } else {
            status = Math.round(img.getWidth()) + " x " + Math.round(img.getHeight());
        }
        resolutionTextDimension.setText(status);
        double squareLength = Math.sqrt(img.getHeight() * img.getWidth());
        //I dont want log, i want a linear scale with the 1000s basically
        //double scaledLength = (Math.log(squareLength) - 6) * 3.5; //the scaling - its all just trial and error
        double scaledLength = squareLength / 500;
        double clipScale = Math.max(Math.min(scaledLength, NUMBER_OF_RESOLUTION_RECTS),0);
        for (int i = 0; i < NUMBER_OF_RESOLUTION_RECTS; i++) {
            resolutionGraphicRects[i].setFill(i > clipScale ? null : Color.BLACK);
        }
    }

    private void updateLabels() {
        if (currentImage == null) {
            return;
        }
        if (!imageOperations.containsKey(currentImage)) {
            imageOperations.put(currentImage, new ImageFileOperations());
        }
        ImageFileOperations operations = imageOperations.get(currentImage);
        
        int moveTo = operations.getMoveTo();
        label.setText(moveTo == 0 ? "no move" : "move to "+moveTo);
        label.setUnhoverOpacity(0.85);
        
        for (int tickNumber = 0; tickNumber < numberOfTicks; tickNumber++) {
            if (operations.getCopyTo(tickNumber)) {
                tickLabels[tickNumber].setText("copy to " + getTickName(tickNumber));
                tickLabels[tickNumber].setUnhoverOpacity(setAllTickLabelsToFullOpacity ? 1 : 0.85);
            } else {
                tickLabels[tickNumber].setText("no copy to " + getTickName(tickNumber));
                tickLabels[tickNumber].setUnhoverOpacity(setAllTickLabelsToFullOpacity ? 0.75 : 0.11);
            }
        }
    }

    //TODO: rename category to 'move' - and ticks are also rather misleading

    private void incrementCurrentImageCategory() {
        if (currentImage == null) {
            return;
        }
        ImageFileOperations operations = this.imageOperations.get(currentImage);
        int current = operations.getMoveTo();
        if (++current > numberOfCategories) {
            current = 0;
        }
        operations.setMoveTo(current);
        updateFilterOnNextImage = true;
        updateLabels();
    }
    private void decrementCurrentImageCategory() {
        if (currentImage == null) {
            return;
        }
        ImageFileOperations operations = this.imageOperations.get(currentImage);
        int current = operations.getMoveTo();
        if (--current < 0) {
            current = numberOfCategories;
        }
        operations.setMoveTo(current);
        updateFilterOnNextImage = true;
        updateLabels();
    }
    private void toggleCurrentImageTick(int tickNumber) {
        if (currentImage == null) {
            return;
        }
        ImageFileOperations operations = this.imageOperations.get(currentImage);
        operations.setCopyTo(tickNumber, !operations.getCopyTo(tickNumber));
        updateFilterOnNextImage = true;
        updateLabels();
    }

    private void nextFilter() {
        if (++filter > numberOfCategories+numberOfTicks) {
            filter = -1;
        }
        updateFilter();
    }
    private void previousFilter() {
        if (--filter < -1) {
            filter = numberOfCategories+numberOfTicks;
        }
        updateFilter();
    }
    //Sets the filter to the givenn number - except the filter is set already, then switch to no filter (so toggle)
    private void toggleFilter(int f) {
        if (filter == f) {
            filter = -1;
        } else {
            filter = f;
        }
        updateFilter();
    }


    private void updateFilter() {
        //Every call here might be the result of the files list changing, this means we know nothing for sure anymore, 
        //not our currentImage (which we recalculate), and also not our lastImageManuallySelected

        //Here, we basically restore our cursor lastImageManuallySelected. This does not mean that this image is visible with the given filter yet. 
        if (allImages.isEmpty()) {
            lastImageManuallySelected = null;
        } else {
            if (lastImageManuallySelected == null) {
                lastImageManuallySelected = allImages.get(0);
            } else {
                if (!allImages.contains(lastImageManuallySelected)) {
                    //The image seen lastly was ... deleted? Might have been by keystroke in the app, or externally and reload was pressed. 
                    //We used to simply reset the cursor 'lastImageManuallySelected' to the first image, but it is more comfortable 
                    //if we try to place the cursor on a still existing image close to where the deleted image used to be. 
                    //We have a copy of the previous state of allImages: 
                    int index = previouslyAllImages.indexOf(lastImageManuallySelected);
                    lastImageManuallySelected = null;
                    if (index != -1) {
                        //finding out if one image of earlier index is inside allImages, select the closest one
                        int i = index;
                        while (--i >= 0) {
                            if (allImages.contains(previouslyAllImages.get(i))) {
                                lastImageManuallySelected = previouslyAllImages.get(i);
                                break;
                            }
                        }
                        if (lastImageManuallySelected == null) {
                            //no images available here - let's check the other direction. 
                            i = index; 
                            while (++i < previouslyAllImages.size()) {
                                if (allImages.contains(previouslyAllImages.get(i))) {
                                    lastImageManuallySelected = previouslyAllImages.get(i);
                                    break;
                                }
                            }
                        }
                    }
                    if (lastImageManuallySelected == null) {
                        //Either none of the old images is still there OR the cursor is not even there in the previous list (not sure if this can actually happen) 
                        //However, at this point, what else can we do than reset the cursor?
                        lastImageManuallySelected = allImages.get(0);
                    }
                }
            }    
        }

        //Recalculate the contents of 'images', the currently iterated images. 
        //Also recalculate which is our currentImage (might have gotten deleted, or we changed categories so our 'cursor' lastImageManuallySelected 
        //might be a currently hidden image)
        //If there are actually no images in the images-array, we detect that below and show a message instead of loading/displaying an image
        currentImage = null;
        images.clear();
        if (filter == -1) {
            images.addAll(allImages);
            currentImage = lastImageManuallySelected;
        } else {
            boolean afterImageIndex = false;
            for (String i : allImages) {
                if (i.equals(lastImageManuallySelected)) {
                    afterImageIndex = true;
                }

                ImageFileOperations operations = imageOperations.get(i);
                boolean addImage = false; 
                if (filter <= numberOfCategories) {
                    int category = 0;
                    if (operations != null) {
                        category = operations.getMoveTo();
                    }
                    addImage = category == filter;
                } else {
                    if (operations != null) {
                        addImage = operations.getCopyTo(filter-numberOfCategories-1);
                    }
                }
                if (addImage) {
                    images.add(i);
                    if (afterImageIndex) {
                        afterImageIndex = false; //it wont become true again in this loop
                        currentImage = i;
                    }
                }
            }
            if (currentImage == null && !images.isEmpty()) {
                currentImage = images.get(images.size()-1);
            }
        }

        //actually, this place is the only one where no images can occur, and also the only place where there are images again. 
        //so we do all the basic organization around it
        boolean imageAvailable = !images.isEmpty(); 

        noImagesLabel.setVisible(!imageAvailable);
        imageAndLoadingPane.setVisible(imageAvailable);
        label.setVisible(imageAvailable);
        leftButton.setVisible(imageAvailable);
        rightButton.setVisible(imageAvailable);
        progress.setVisible(imageAvailable);
        tickLabelVBox.setVisible(imageAvailable);
        hideUiHotcorner.setVisible(imageAvailable && stage.isFullScreen());

        if (!imageAvailable) {
            if (filter == -1) {
                noImagesLabel.setText("The selected folder does not contain any supported files. " + 
                        "\nIn this folder, this app is not really useful, you may close it. \nWhen you restart the app, you can select another folder."+
                        "\nAlternatively, you can add files to this folder and press F5 to rescan the folder.");
            } else {
                if (filter == 0) {
                    noImagesLabel.setText("There are no images not to be moved. ");
                } else if (filter <= numberOfCategories) {
                    noImagesLabel.setText("There are no images that should be moved to /" + filter + ". ");
                } else {
                    noImagesLabel.setText("There are no images to be copied to /" + getTickName(filter - numberOfCategories - 1) + ". ");
                }
                noImagesLabel.setText(noImagesLabel.getText() + "\nYou can change the filter by clicking or scrolling the 'only show ...' below. "); 
            }
            rootPane.requestFocus();
        } else {
            loadImage(); //last line here, probably
        }

        //Also update the status text
        if (filter == -1) {
            filterLabel.setText("(showing everything)");
            filterLabel.setUnhoverOpacity(0.11);
        } else {
            filterLabel.setUnhoverOpacity(1.0);
            if (filter == 0) {
                filterLabel.setText("only show files not to be moved");
            } else if (filter <= numberOfCategories) {
                filterLabel.setText("only show move to " + filter);
            } else {
                filterLabel.setText("only show copy to " + getTickName(filter - numberOfCategories-1));
            }
        }
        //another TODO: when do we update the filter? Only on next / prev calls. Do we need to always update the filter? 
        //TODO: We should prompt a hint that the image will be invisible in this filter once you change the image.
        //TODO: We need to updateFilter for every nextImage and prevImage call. 

        //TODO: I have no idea what the following lines mean, maybe just delete them?
        //do we need this call here? 
        //does it stand showing the same image without reloading it?
        //what about the fact that the call could occur any time (esp after deleting)
        //rn im saying yes, replacing loadImage in the init with this call. 
    }

    //in images, not allImages. 
    private int getCurrentImageIndex() {
        //This might become unneccessary anyway, since it boils down to basically one line of code without the special treatment
        int index = images.indexOf(currentImage);
        if (index == -1) {
            throw new RuntimeException("The currentImage " + currentImage + " is not in the images ArrayList when calling getCurrentImageIndex, a status that should not be possible.");
            //The case that we cant find the image should never occur anymore, as other methods always take care of this.
            /*
            if (images.isEmpty()) {
                handleNoImages(); //shows an alert and System.exits
            } else {
                currentImage = images.get(0);
                return 0;
            }
            */
        }
        return index;
    }

    //Select & show the next image
    private void nextImage() {
        nImagesForth(1);
    }
    
    //Updates currentFile and shows the image
    //If the resulting index would exceed the image number, we stop at the last image. If we skip over from the last image, we always start from the first image again. 
    private void nImagesForth(int n) {        
        if (currentImage == null) {
            return;
        }
        int currIndex = getCurrentImageIndex();
        int newIndex = currIndex + n;
        if (newIndex >= images.size()) {
            if (currIndex == images.size() - 1) {
                newIndex = 0;
                indicateEndOfFolder(true);
            } else {
                newIndex = images.size() - 1;
            }
        }
        selectImageAtIndex(newIndex);    
    }

    private void prevImage() {
        nImagesBack(1);
    }
    
    //Updates currentFile and shows the image
    //Wrapping behavior analog to nImagesForth
    private void nImagesBack(int n) {
        if (currentImage == null) {
            return;
        }
        int currIndex = getCurrentImageIndex();
        int newIndex = currIndex - n;
        if (newIndex < 0) {
            if (currIndex == 0) {
                newIndex = images.size() - 1;
                indicateEndOfFolder(false);
            } else {
                newIndex = 0;
            }
        }
        selectImageAtIndex(newIndex);
    }

    private void selectImageAtIndex(int index) {
        if (currentImage == null) {
            return;
        }
        currentImage = images.get(index);
        lastImageManuallySelected = currentImage;
        updateImageFilterAndCursorAfterImageChange();
    }

    //always calls loadImage() to load a (probably new) image. Also calls updateFilter() [which itself calls loadImage] 
    //only if updateFilterOnNextImage. You could also just always call updateFilter(), and you would probably never
    //notice the slightly (slightly) worse performance. 
    private void updateImageFilterAndCursorAfterImageChange() {
        if (updateFilterOnNextImage) {
            updateFilter();
        } else {
            loadImage();
        }
        if (HIDE_MOUSE_ON_IMAGE_SWITCH) {
            zoomPane.setCursor(Cursor.NONE);
        }
        updateFilterOnNextImage = false;
    }

    private void deleteImage() {
        if (currentImage == null) {
            return;
        }
        try {
            //if folder doesnt exist, create it
            if (!deleteDirectory.exists()) {
                deleteDirectory.mkdir();
            }
            
            //move image file there
            String path = getFullPathForFileInThisFolder(currentImage);
            File origin = new File(path);
            File dest = new File(deleteDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + currentImage);
            origin.renameTo(dest);
            
            ArrayList<String> deletedFilenames = new ArrayList<>();
            deletedFilenames.add(currentImage);
            deleteHistory.add(deletedFilenames);
            //move along other files
            if (filesToMoveAlong.containsKey(currentImage)) {
                for (String moveAlong : filesToMoveAlong.get(currentImage)) {
                    try {
                        path = getFullPathForFileInThisFolder(moveAlong);
                        origin = new File(path);
                        dest = new File(deleteDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + moveAlong);
                        origin.renameTo(dest);     
                        deletedFilenames.add(moveAlong);
                    } catch (Exception e) {
                        System.out.println("Could not move file "+moveAlong+" to 'deleted' folder: ");
                        e.printStackTrace();            
                    }
                }
            }

            ImageFileOperations operations = this.imageOperations.get(currentImage);
            if (imageOperations.containsKey(currentImage)) operations = imageOperations.get(currentImage);
            deleteHistoryCategory.add(operations);
            
            imageOperations.remove(currentImage);

        } catch (Exception e) {
            System.out.println("Could not move image "+currentImage+" to 'deleted' folder: ");
            e.printStackTrace();
        }

        if (images.size() > 1) {
            nextImage();
        }
        updateFilesList();
    }

    private void undoDelete() {
        if (deleteHistory.isEmpty()) {
            return;
        }

        //First and most important: Restore the image(s) themelves! 
        ArrayList<String> imagesToRestore = deleteHistory.get(deleteHistory.size() - 1);
        String mainImageFileName = imagesToRestore.get(0);
        for (String restore : imagesToRestore) {
            String pathBefore = getFullPathForFileInThisFolder(restore);
            try {
                File origin = new File(deleteDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + restore);
                File dest = new File(pathBefore);
                origin.renameTo(dest);    
            } catch (Exception e) {
                System.out.println("Could not restore removed image "+restore+" back to its base folder. ");
                e.printStackTrace();
            }
        }
        deleteHistory.remove(deleteHistory.size() - 1);
        imageOperations.put(mainImageFileName, deleteHistoryCategory.remove(deleteHistoryCategory.size() - 1));
        
        //Side quest: Delete the delete folder if there is now no files inside anymore. 
        //again, weird solutions for the simplest IO tasks in java. Bro. 
        //However, thanks to baeldung.com/java-check-empty-directory where I got this from
        if (deleteDirectory.exists()) {
            try (Stream<Path> entries = Files.list(deleteDirectory.toPath())) {
                boolean empty = !entries.findFirst().isPresent();
                if (empty) {
                    try {
                        deleteDirectory.delete();
                    } catch (Exception e) {
                        //should be empty, just checked for that
                        System.out.println("Could not delete the delete folder (how ironic)");
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Internal error when trying to get the files in the delete folder. ");
                e.printStackTrace();
            }
        }

        //If the restore was sucessfull, we definetly jump to this image to give the user the feedback that the restore was sucessfull
        updateFilesList();
        if (allImages.contains(mainImageFileName)) {

            if (!images.contains(mainImageFileName)) {
                //If the image is not visible under the current filter, we disable the filter. 
                filter = -1; 
                updateFilterOnNextImage = true; 
            }
            currentImage = mainImageFileName;
            
            //lastImageManuallySelected = currentImage; Maybe it is clever to knowingly NOT do this. 
            //Scrolling up and down between the neighboring categories then brings back the original cursor position. 
            updateImageFilterAndCursorAfterImageChange();
        }
    }

    private String getFullPathForFileInThisFolder(String image) {
        return directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + image;
    }

    private void updateFilesList() {
        previouslyAllImages = allImages;
        allImages = new ArrayList<String>();

        File[] allFilesUnfiltered = directory.listFiles();
        Hashtable<String, String> imageNameFrequency = new Hashtable<>(); //contains either the full filename, or "" if there are several images with this name
        ArrayList<String> notSupportedFilesInDirectory = new ArrayList<>();
        for (File file : allFilesUnfiltered) {
            if (!file.isDirectory()) {
                String filename = file.getName();
                if (filenameFilter.accept(directory, filename)) {
                    String filenameWithoutExtension = Common.removeExtensionFromFilename(filename).toLowerCase();
                    allImages.add(filename);
                    if (imageNameFrequency.get(filenameWithoutExtension) == null) { //== null: does not contain this key yet
                        imageNameFrequency.put(filenameWithoutExtension, filename);
                    } else {
                        imageNameFrequency.put(filenameWithoutExtension, "");
                    }
                } else {
                    notSupportedFilesInDirectory.add(filename);
                }
            }
        }
        if (MOVE_ALONG) {
            filesToMoveAlong = new Hashtable<>();
            for (String notSupportedFile : notSupportedFilesInDirectory) {
                String fullFilename = imageNameFrequency.get(Common.removeExtensionFromFilename(notSupportedFile).toLowerCase());
                if (fullFilename != null && !fullFilename.equals("")) { //with "", there are several images (different extensions) with the same name, were powerless here..
                    if (!filesToMoveAlong.containsKey(fullFilename)) {
                        filesToMoveAlong.put(fullFilename, new ArrayList<String>());
                    }
                    filesToMoveAlong.get(fullFilename).add(notSupportedFile);
                }
            }
        }
        Collections.sort(allImages, String.CASE_INSENSITIVE_ORDER); //My windows directory was sorted already, but idk if its always like that, on other OS
        updateFilter();
    }

    //Youtube-like skimming: 
    //Key (and numberKey) 0 goes to the first image, 5 to the middle one, the rest interpolates in between. 
    private void skimTo(int numberKey) {
        if (currentImage == null) {
            return;
        }

        int skimDestination = (int)(images.size() * (numberKey * 0.1));
        selectImageAtIndex(skimDestination);
    }

    private void indicateEndOfFolder(boolean nowAtBeginning) {
        double initialOpacity = 0.9;
        //If not nowAtBeginning, then we're now at the end! 
        if (wrapSearchIndicatorTimeline == null) {
            wrapSearchIndicatorTimeline = new Timeline();
            //wrapSearchIndicatorTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(0.15), new KeyValue(wrapSearchIndicator.opacityProperty(), initialOpacity)));
            //wrapSearchIndicatorTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(0.7), new KeyValue(wrapSearchIndicator.opacityProperty(), initialOpacity)));
            wrapSearchIndicatorTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), new KeyValue(wrapSearchIndicator.opacityProperty(), 0.0)));

            wrapSearchIndicatorTimeline.setOnFinished((e) -> {wrapSearchIndicator.setVisible(false);});
        }
        wrapSearchIndicator.setOpacity(initialOpacity);
        wrapSearchIndicator.setVisible(true);
        wrapSearchIndicatorTimeline.playFromStart();
    }

    //Lookup for tick names. Right now we use letters, so index 0 is "a" etc.
    //So you may only look up ticks from 0 to 25
    public static String getTickName(int index) {
        return Common.getLetterInAlphabet(index);
    }

    //Based on the current state, returns ArrayLists of all file names, that require a file operation. 
    //Since its several arrayLists, we return them as an arrayList of arrayLists, with the following indices: 
    //0: images not to move
    //1 to numberOfCategories + 1: images to be moved to their corresponding categories
    //numberOfCategories + 1 to numberOfCategories + numberOfTicks + 1 images to copy, corresponding to the tick indices
    private ArrayList<ArrayList<String>> listAllFileOperations() {
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        for (int i = 0; i < numberOfCategories + numberOfTicks + 1; i++) {
            result.add(new ArrayList<String>());
        }

        //we need to iterate through the list of files instead of iterating through imageOperations, as some files might not have an entry in the set yet
        for (String filename : allImages) {
            ImageFileOperations operations = imageOperations.get(filename);
            if (operations == null) {
                result.get(0).add(filename);
            } else {
                int moveTo = operations.getMoveTo();
                result.get(moveTo).add(filename);
                for (int tickNumer = 0; tickNumer < numberOfTicks; tickNumer++) {
                    if (operations.getCopyTo(tickNumer)) {
                        result.get(numberOfCategories + tickNumer + 1).add(filename);
                    }
                }
            }
        }

        return result;
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

    //if pathOnly is false, we copy the image itself (as file)
    //otherwise, we only write the path to clipboard.
    private void copyImageToClipboard(boolean pathOnly) {
        if (currentImage == null) {
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent cc = new ClipboardContent();
        File imageFile = new File(directory, currentImage);
        if (!pathOnly) {
            ArrayList<File> files = new ArrayList<>();
            files.add(imageFile);
            cc.putFiles(files);
        }
        cc.putString(imageFile.getAbsolutePath());
        clipboard.setContent(cc);
        showActionIndicator();
    }

    private void showActionIndicator() {
        double initialOpacity = 0.5;
        double innerCircleStart = 23.5, outerCircleStart = 30;
        Duration duration = Duration.seconds(0.6);
        if (actinoIndicateTimeline == null) {
            actinoIndicateTimeline = new Timeline();
            actinoIndicateTimeline.getKeyFrames().add(new KeyFrame(duration, new KeyValue(actionIndicatorPane.opacityProperty(), 0.0)));
            actinoIndicateTimeline.getKeyFrames().add(new KeyFrame(duration, new KeyValue(actionIndicatorC2.radiusProperty(), 21.5)));
            actinoIndicateTimeline.getKeyFrames().add(new KeyFrame(duration, new KeyValue(actionIndicatorC1.radiusProperty(), 35)));
            actinoIndicateTimeline.setOnFinished((e) -> {actionIndicatorPane.setVisible(false);});
        }
        actionIndicatorPane.setOpacity(initialOpacity);
        actionIndicatorC2.setRadius(innerCircleStart);
        actionIndicatorC1.setRadius(outerCircleStart);
        actionIndicatorPane.setVisible(true);
        actinoIndicateTimeline.playFromStart();
    }

    private void updateZoomIndicatorText() {
        zoomIndicatorText.setText(String.format("%.0f%%", calculateBaseImageScale() * (isZooming ? zoom : 1) * 100));
    }
    
    private void rotateBy90Degrees(boolean clockwise) {
        if (currentImage == null) {
            return;
        }
        if (isRotateFeatureAvailable()) {
            ((RotatedImage)view.getImage()).rotateBy90Degrees(clockwise);
            updateImageRotation();
        }
    }

    private boolean isRotateFeatureAvailable() {
        if (currentImage == null) {
            return false; 
        }
        RotatedImage r = (RotatedImage) view.getImage();
        return !r.isStillLoading() && r.isJPG();
    }

    private void updateImageRotation() {
        RotatedImage img = (RotatedImage) view.getImage();
        view.setRotate(img.getRotation());
        view.fitHeightProperty().unbind();
        view.fitWidthProperty().unbind();
        if (img.isRotatedBy90Degrees()) {
            view.fitWidthProperty().bind(rootPane.heightProperty());
            view.fitHeightProperty().bind(rootPane.widthProperty());
        } else {
            view.fitWidthProperty().bind(rootPane.widthProperty());
            view.fitHeightProperty().bind(rootPane.heightProperty());
        }
        updateViewport();
    }

    //TODO do this EVERY TIME another image is loaded! <- loadImage? other buttons? numbers, arrows, load, F5 etc? 
    /*
    try {
        System.out.println("Starting to rewrite!");
        ((RotatedImage)view.getImage()).writeCurrentOrientationToFile();;
    } catch (Exception e) {
        System.out.println(Common.formatException(e));
        e.printStackTrace();
    } */       

}