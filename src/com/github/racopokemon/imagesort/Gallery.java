package com.github.racopokemon.imagesort;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

/*
 * TODO: Watch out for ToDos!
 */

public class Gallery {

    private static final boolean HIDE_MOUSE_ON_IMAGE_SWITCH = true; //If true, the mouse pointer is hidden instantly as soon as you switch to another image (and shown instantly on mouse move). No hiding if set to false. 
    private static final boolean HIDE_MOUSE_ON_IDLE = true; //Stronger variant basically, automatically hide mouse if not moved for ~1 sec. (Fullscreen only)
    private static final boolean DEBUG_PRINT_IMAGE_METADATA = false; //if true, the current images metadata is written to comand line everytime the image changes. (For debugging)

    private File directory;
    private File targetDirectory;
    private File delDirectory;
    private boolean copy;
    private boolean reopenLauncherAfterwards;

    //The current list of images we are cycling through now (with filters not all images might be visible). Subset of allImages, which is all images in the folder
    private ArrayList<String> images = new ArrayList<>();
    //All images available in the users folder. Updated by updateFilesList()
    private ArrayList<String> allImages = new ArrayList<>();
    //The previous version of allImages. We use this to check the neighborhood of a suddenly deleted image (that still exists in allIamges) to prevent simply skipping 
    //to the first image in the entire folder. 
    private ArrayList<String> previouslyAllImages = new ArrayList<>();
    private ArrayList<String> deleteHistory = new ArrayList<>(); //last element added: First image name to be restored
    private ArrayList<Integer> deleteHistoryCategory = new ArrayList<>(); //We also store which category the deleted image was in

    private FilenameFilter filenameFilter;

    private String currentImage = null; //filename OR null if in the current filter category there are no images to show at all
    private String lastImageManuallySelected = null;
    private int filter = -1; //-1: No filter. 0: Keep only. 1-3: Only this category. 
    private boolean updateFilterOnNextImage = false; //slight acceleration, only reload the whole filter when this actually occured

    private Hashtable<String, Integer> imageCategory; //images are only added once seen. All not contained images are expected to have category 0. Iterate over allImages for all images instead. 
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

    private ImageView view;
    private StackPane zoomPane;
    private StackPane imageAndLoadingPane;
    private StackPane rootPane;
    private InteractiveLabel label;
    private InteractiveLabel filterLabel;
    private static final Color LR_HALF_TRANSPARENT = new Color(1, 1, 1, 0.08);
    private static final Color LR_ARROW_COLOR = new Color(0, 0, 0, 0.5);
    private static final double BUTTON_WIDTH = 100;

    private ImprovisedProgressBar progress;

    private ProgressIndicator loadingProgress; 
    private Label errorLabel; 
    private Label noImagesLabel; 
    private LRButton leftButton, rightButton;

    private Text wrapSearchIndicator;
    private Timeline wrapSearchIndicatorTimeline;
    //https://stackoverflow.com/questions/33066754/javafx-set-mouse-hidden-when-idle Thanks for the great answer, was halfway through Workers, Tasks and Services
    private PauseTransition hideMouseOnIdle;

    public void start(File directory, File targetDirectory, File deleteDirectory, boolean copy, boolean reopenLauncher, boolean showHints) {
        this.directory = directory;
        this.targetDirectory = targetDirectory;
        this.delDirectory = deleteDirectory;
        this.copy = copy;
        this.reopenLauncherAfterwards = reopenLauncher;

        //TODO make the 3 categories (which are hardcoded by now) a constant var

        Stage stage = new Stage();

        filenameFilter = Common.getFilenameFilter();
        imageCategory = new Hashtable<>();
        imageBuffer = new Hashtable<>();

        stage.setTitle("Image Sort");

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

        label = new InteractiveLabel(38, 200, Pos.BOTTOM_RIGHT, 
                () -> {incrementCurrentImageCategory();}, 
                () -> {decrementCurrentImageCategory();});
        StackPane.setAlignment(label, Pos.BOTTOM_RIGHT);

        filterLabel = new InteractiveLabel(28, 250, Pos.BOTTOM_CENTER,
                () -> {nextFilter();},
                () -> {previousFilter();});
        StackPane.setAlignment(filterLabel, Pos.BOTTOM_CENTER);
        
        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);
        errorLabel.setFont(new Font(15));
        errorLabel.setTextAlignment(TextAlignment.CENTER);
        loadingProgress = new ProgressIndicator(0.2);
        loadingProgress.setMaxSize(50, 50);
        loadingProgress.setVisible(false);

        progress = new ImprovisedProgressBar(350, 30);
        StackPane.setAlignment(progress, Pos.TOP_CENTER);
        progress.setOnScroll(zoomPaneScrollHandler);
        
        MenuItem menuShowFile = new MenuItem("Show in explorer");
        menuShowFile.setOnAction((event) -> {showInExplorer();});
        MenuItem menuUndo = new MenuItem("Undo last deletion");
        menuUndo.setOnAction((event) -> {undoDelete();});
        MenuItem menuDelete = new MenuItem("Move to '/delete'");
        menuDelete.setOnAction((event) -> {deleteImage();});
        
        Rectangle invisibleContextMenuSource = new Rectangle();
        invisibleContextMenuSource.setVisible(false);
        ContextMenu contextMenu = new ContextMenu(menuShowFile, menuUndo, menuDelete);
        view.setOnContextMenuRequested((event) -> {
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

        wrapSearchIndicator = new Text("1/70");
        wrapSearchIndicator.setFont(new Font(85));
        wrapSearchIndicator.setFill(Color.WHITE);
        wrapSearchIndicator.setStroke(Color.BLACK);
        wrapSearchIndicator.setStrokeWidth(2);
        wrapSearchIndicator.setVisible(false);
        wrapSearchIndicator.setMouseTransparent(true);

        rootPane.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        rootPane.getChildren().add(invisibleContextMenuSource);
        rootPane.getChildren().add(noImagesLabel);
        rootPane.getChildren().add(imageAndLoadingPane);
        rootPane.getChildren().add(wrapSearchIndicator);

        leftButton = new LRButton(rootPane, true); //this also adds them to the rootPane
        rightButton = new LRButton(rootPane, false);
        
        rootPane.getChildren().add(label);
        rootPane.getChildren().add(filterLabel);
        rootPane.getChildren().add(progress);

        Scene scene = new Scene(rootPane, 800, 600);
        stage.setScene(scene);
        stage.maximizedProperty().addListener((observable) -> {if (stage.isMaximized()) stage.setFullScreen(true);});
        stage.setOnCloseRequest((event) -> {
            boolean unsavedChanges = false;
            for (Map.Entry<String, Integer> entry : imageCategory.entrySet()) {
                if (entry.getValue() != 0) {
                    unsavedChanges = true;
                    break;
                }
            }
            if (unsavedChanges) {

                //TODO: Better info management, like 'there are 14 images to be moved, 3 are moved to /1, 11 are moved to /2, 84 remain.'
                String closeMessage, closeHeader;
                if (copy) { 
                    closeMessage = "Copy all files now (to folder '" + targetDirectory.getName() + "') before closing?\n";
                    closeHeader = "Copy files now?";
                } else {
                    closeMessage = "Move all files now (to folder '" + targetDirectory.getName() + "') before closing?\n";
                    closeHeader = "Move files now?";
                }
                closeMessage += "'No' keeps the files unchanged, but discards your work here.";
                Alert closeAlert = new Alert(AlertType.NONE, closeMessage, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                closeAlert.setHeaderText(closeHeader);
                Optional<ButtonType> result = closeAlert.showAndWait();
                if (!result.isPresent() || result.get() == ButtonType.CANCEL) {
                    //prevent window close by consuming event
                    event.consume();
                    return;
                } else if (result.get() == ButtonType.YES) {
                    //rename (closes automatically on return)
                    //TODO: Better error communication! Like, moved abc files successfully, error with 2. 
                    //TODO: Also, what if there is an error in between? Should we present the retry / ignore / cancel dialog? 
                    if (copy) {
                        copyAllFiles();
                    } else {
                        moveAllFiles();
                    }
                    new Alert(AlertType.NONE, "Finished! \nConsider that other file types (videos) might also be in this folder.", ButtonType.OK).showAndWait();
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
                if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
                    nextImage();
                } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
                    prevImage();
                } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) {
                    incrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) {
                    decrementCurrentImageCategory();
                } else if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    deleteImage();
                } else if (event.getCode() == KeyCode.PLUS) {
                    increaseZoom(40);
                } else if (event.getCode() == KeyCode.MINUS) {
                    decreaseZoom(40);
                } else if (event.getCode() == KeyCode.Q) {
                    previousFilter();
                } else if (event.getCode() == KeyCode.E) {
                    nextFilter();
                } else if (event.getCode() == KeyCode.F5) {
                    updateFilesList();
                } else if (event.getCode().isDigitKey()) {
                    String keyName = event.getCode().getName();
                    int skim = Integer.valueOf(keyName.substring(keyName.length() - 1, keyName.length())); 
                    //hacky hacky. Its either '5' or 'Numpad 5', so we take the last char
                    skimTo(skim);
                } else if (event.getCode() == KeyCode.Z && event.isShortcutDown()) {
                    undoDelete();
                } else if (event.getCode() == KeyCode.F11 || (event.getCode() == KeyCode.ENTER && event.isAltDown()) || event.getCode() == KeyCode.F) {
                    //https://stackoverflow.com/questions/51386423/remove-beep-sound-upon-maximizing-javafx-stage-with-altenter
                    //I have no idea why windows plays the beep on alt+enter (and not on ANY other combination), accelerators also don't work. 
                    //TODO accelerators might actually be the better solution for all shortcuts. Except maybe the + and -?
                    stage.setFullScreen(!stage.isFullScreen());
                }
                //else if (event.getCode() == KeyCode.ESCAPE) {
                //    if (!stage.isFullScreen()) {
                //        stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));
                //    }
                //}
            }
        });

        stage.getIcons().add(Common.getRessource("logo"));
        stage.show();

        rootPane.widthProperty().addListener((a, oldV, newV) -> {updateViewport(newV.doubleValue(), rootPane.heightProperty().get());});
        rootPane.heightProperty().addListener((a, oldV, newV) -> {updateViewport(rootPane.widthProperty().get(), newV.doubleValue());});

        view.requestFocus();

        updateFilesList(); // includes a call to updateFilter and loadImage()

        if (showHints) {
            Alert useInfo = new Alert(AlertType.NONE, null, ButtonType.OK);
            useInfo.setHeaderText("How to use");
            useInfo.setContentText(
                "Arrow keys or WASD to look through images and change target folder (keep in current or move to \\1, \\2 or \\3). \n"+
                "Del or Backspace to instantly move to a 'delete' folder. Ctrl + Z to undo.\n"+
                "Click to zoom. Scroll, + and - to change the zoom strength. Generally, the whole interface is scrollable! \n"+
                "Context menu to show file in explorer. Youtube-like skimming with number keys.\n"+
                "Close the window to perform the file operations (you will be asked for confirmation).\n\n"+
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
    //Call this when the rootPane size has not changed
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
            view.setViewport(null);
        } else {
            if (ninetyDegrees) {
                //this line is brought to you by trial and error
                view.setViewport(new Rectangle2D((int)((iHeight-h)*0.5), (int)((iWidth-w)*0.5), h, w));
                //TODO zoom is simply wrong on this :(. Also we are not pixel accurate, ONLY on rotated images :(
            } else {
                view.setViewport(new Rectangle2D((int)((iWidth-w)*0.5), (int)((iHeight-h)*0.5), w, h));
            }
        }
    }

    // Zooms into the whole scene (the easy way, just setting the scale properties)
    // Also updates the translation, that's the reason it is called so many times
    private void zoomIn() {
        double width = zoomPane.getWidth();
        double height = zoomPane.getHeight();
        double spaceX = (width*zoom) - (width);
        double spaceY = (height*zoom) - (height);
        double relTransX = -(mouseRelativeX - 0.5);
        double relTransY = -(mouseRelativeY - 0.5);

        zoomPane.setScaleX(zoom);
        zoomPane.setScaleY(zoom);

        zoomPane.setTranslateX(spaceX * relTransX);
        zoomPane.setTranslateY(spaceY * relTransY);

        zoomPane.setCursor(Cursor.NONE);
        isZooming = true;
    }
    // Resets the zoom to the usual 1:1 in the app
    private void zoomOut() {
        zoomPane.setScaleX(1);
        zoomPane.setScaleY(1);
        zoomPane.setTranslateX(0);
        zoomPane.setTranslateY(0);

        zoomPane.setCursor(Cursor.DEFAULT);
        isZooming = false;
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
        mouseRelativeX = event.getSceneX() / zoomPane.getWidth();
        mouseRelativeY = event.getSceneY() / zoomPane.getHeight();
        mouseRelativeX = Math.max(Math.min(mouseRelativeX, 1), 0);
        mouseRelativeY = Math.max(Math.min(mouseRelativeY, 1), 0);
    }

    private void showInExplorer() {
        if (currentImage == null) return;
        
        //Desktop.getDesktop().browseFileDirectory(<file>) would be better, cross platform, but requires java 9 and im too lazy to install now
        if (Common.isWindows()) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select," + getFullPathForImage(currentImage));
            } catch (IOException e) {
                System.out.println("Could not show file " + currentImage + " in explorer:");
                e.printStackTrace();
            }
        }
    }

    private ChangeListener<? super Number> numberListener = (a, b, c) -> {updateImageStatus();};
    private ChangeListener<? super Boolean> booleanListener = (a, b, c) -> {updateImageStatus();};
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
            newImageBuffer.put(currentImage, new RotatedImage(new File (getFullPathForImage(currentImage))));
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
                newImageBuffer.put(imageNameAtCursor, new RotatedImage(new File (getFullPathForImage(imageNameAtCursor))));
            }
        }
        //initially we were cool and didnt have loop, but this really helps when scrolling rapidly through the pics.
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
        Image oldImage = view.getImage();
        //unregister old image listeners (what if theyre still called?.. okay change the img once i guess and its fixed)
        if (oldImage != null) {
            oldImage.progressProperty().removeListener(numberListener);
            oldImage.heightProperty().removeListener(numberListener);
            oldImage.errorProperty().removeListener(booleanListener);
        }

        if (!img.isError() && img.getHeight() == 0) {
            //means: still loading, need to register listeners because the view might change
            img.progressProperty().addListener(numberListener);
            img.heightProperty().addListener(numberListener);
            img.errorProperty().addListener(booleanListener);
        }
        view.setImage(img);
        if (oldImage != img) {
            updateViewport();
            if (DEBUG_PRINT_IMAGE_METADATA) {
                System.out.println("\n-------------------------------------------------");
                System.out.println(currentImage);
                System.out.println("-------------------------------------------------");
                ((RotatedImage)view.getImage()).printImageMetadata();
            }
        }
        
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

        ArrayList<String> imageInfo = new ArrayList<String>();
        imageInfo.add(currentImage);
        imageInfo.addAll(img.getSomeImageProperties());
        progress.setProgress(currentImageIndex, images.size(), imageInfo, filter != -1);
        wrapSearchIndicator.setText(currentImageIndex+1 + "/" + images.size()); //kind of doubled here, but I think its important that the indicator is updated as well
        updateLabel();
        updateImageStatus();
    }
    
    private void updateImageStatus() {
        Image img = view.getImage();
        if (img.getHeight() > 0) {
            //image already loaded
            loadingProgress.setVisible(false);
            errorLabel.setVisible(false);
            updateViewport();
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
                errorLabel.setText("We could not load " + currentImage + ": \n" + img.getException().getLocalizedMessage());
                System.out.println("Error occured when loading image " + currentImage);
                img.getException().printStackTrace();
            }
        } else {
            //just still loading
            loadingProgress.setVisible(true);
            errorLabel.setVisible(false);
            loadingProgress.setProgress(img.getProgress());
        }
    }

    private boolean isMemoryException(Exception e) {
        return e.getMessage().startsWith("java.lang.OutOfMemoryError");
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

    private void updateLabel() {
        if (currentImage == null) {
            return;
        }
        if (!imageCategory.containsKey(currentImage)) {
            imageCategory.put(currentImage, 0);
        }
        int index = imageCategory.get(currentImage);
        if (index == 0) {
            if (copy) {
                label.setText("no copy");
            } else {
                label.setText("keep");
            }
        } else {
            if (copy) {
                label.setText("copy to "+index);
            } else {
                label.setText("move to "+index);
            }
        }
    }
    private void incrementCurrentImageCategory() {
        if (currentImage == null) {
            return;
        }
        int current = imageCategory.get(currentImage);
        if (++current > 3) {
            current = 0;
        }
        imageCategory.put(currentImage, current);
        updateFilterOnNextImage = true;
        updateLabel();
    }
    private void decrementCurrentImageCategory() {
        if (currentImage == null) {
            return;
        }
        int current = imageCategory.get(currentImage);
        if (--current < 0) {
            current = 3;
        }
        imageCategory.put(currentImage, current);
        updateFilterOnNextImage = true;
        updateLabel();
    }
    private void nextFilter() {
        if (++filter > 3) {
            filter = -1;
        }
        updateFilter();
    }
    private void previousFilter() {
        if (--filter < -1) {
            filter = 3;
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

        //Recalculate the contents of images, the currently iterated images. 
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

                Integer category = imageCategory.get(i);
                if (category == null) {
                    category = 0;
                }
                if (category == filter) {
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

        if (!imageAvailable) {
            if (filter == -1) {
                noImagesLabel.setText("The selected folder does not contain any supported files. " + 
                        "\nIn this folder, this app is not really useful, you may close it. \nWhen you restart the app, you can select another folder."+
                        "\nAlternatively, you can add files to this folder and press F5 to rescan the folder.");
            } else {
                if (filter == 0) {
                    noImagesLabel.setText("There are no images not to be moved. ");
                } else {
                    noImagesLabel.setText("There are no images that should be moved to /" + filter + ". ");
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
                filterLabel.setText("only show 'keep'");
            } else {
                filterLabel.setText("only show " + filter);
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

    //Updates currentFile and shows the image
    private void nextImage() {
        if (currentImage == null) {
            return;
        }
        int index = getCurrentImageIndex();
        if (++index >= images.size()) {
            index = 0;
            indicateEndOfFolder(true);
        }
        currentImage = images.get(index);
        lastImageManuallySelected = currentImage;
        updateImageFilterAndCursorAfterImageChange();
    }

    //Updates currentFile and shows the image
    private void prevImage() {
        if (currentImage == null) {
            return;
        }
        int index = getCurrentImageIndex();
        if (--index < 0) {
            index = images.size() - 1;
            indicateEndOfFolder(false);
        }
        currentImage = images.get(index);
        lastImageManuallySelected = currentImage;
        updateImageFilterAndCursorAfterImageChange();
    }

    //always calls loadImage() to load a (probably new) image. Also calls updateFilter() [which itself calls loadImage] 
    //only if currentImagesCategoryWasChanged. You could also just always call updateFilter(), and you would probably never
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
            if (!delDirectory.exists()) {
                delDirectory.mkdir();
            }
            //move file there
            String path = getFullPathForImage(currentImage);
            File origin = new File(path);
            File dest = new File(delDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + currentImage);
            origin.renameTo(dest);
            deleteHistory.add(currentImage);
            
            int category = 0;
            if (imageCategory.containsKey(currentImage)) category = imageCategory.get(currentImage);
            deleteHistoryCategory.add(category);
            
            imageCategory.remove(currentImage);

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

        //First and most important: Restore the image itself! 
        String restore = deleteHistory.get(deleteHistory.size() - 1);
        String pathBefore = getFullPathForImage(restore);
        try {
            File origin = new File(delDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + restore);
            File dest = new File(pathBefore);
            origin.renameTo(dest);

            deleteHistory.remove(deleteHistory.size() - 1);
            imageCategory.put(restore, deleteHistoryCategory.remove(deleteHistoryCategory.size() - 1));
        } catch (Exception e) {
            System.out.println("Could not restore removed image "+restore+" back to its base folder. ");
            e.printStackTrace();
        }

        //Side quest: Delete the delete folder if there is now no files inside anymore. 
        //again, weird solutions for the simplest IO tasks in java. Bro. 
        //However, thanks to baeldung.com/java-check-empty-directory where I got this from
        if (delDirectory.exists()) {
            try (Stream<Path> entries = Files.list(delDirectory.toPath())) {
                boolean empty = !entries.findFirst().isPresent();
                if (empty) {
                    try {
                        delDirectory.delete();
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
        if (allImages.contains(restore)) {

            if (!images.contains(restore)) {
                //If the image is not visible under the current filter, we disable the filter. 
                filter = -1; 
                updateFilterOnNextImage = true; 
            }
            currentImage = restore;
            
            //lastImageManuallySelected = currentImage; Maybe it is clever to knowingly NOT do this. 
            //Scrolling up and down between the neighboring categories then brings back the original cursor position. 
            updateImageFilterAndCursorAfterImageChange();
        }
    }

    private String getFullPathForImage(String image) {
        return directory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + image;
    }

    private void updateFilesList() {
        //String sep = FileSystems.getDefault().getSeparator();
        //files.addAll(directory.list(filter)); //again, this should work. 
        //files = new ArrayList<String>(directory.list(filter)); //or this
        previouslyAllImages = allImages;
        allImages = new ArrayList<String>();
        String[] newDir = directory.list(filenameFilter);
        Collections.addAll(allImages, newDir);
        Collections.sort(allImages, String.CASE_INSENSITIVE_ORDER); //My directory was sorted already, but idk if its always like that, also on other OS
        updateFilter();
    }

    //The final call, that actually executes all the move operations. 
    //Right now it is only possible to do this on close (which makes sense, after that the program would close anyway)
    private void moveAllFiles() { 
        if (Common.isValidFolder(targetDirectory)) {
            System.out.println("The target directory is not vaild: " + targetDirectory.getAbsolutePath());
            //Should be added later to a proper error handling
        }

        for (String key : imageCategory.keySet()) {
            int category = imageCategory.get(key);
            if (category != 0) {
                //if folder doesnt exist: create. 
                String dirPath = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + category;
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    try {
                        dir.mkdir();
                    } catch (Exception e) {
                        System.out.println("Could not create folder for category " + category);
                        e.printStackTrace();
                    }
                }
                File origin = new File(getFullPathForImage(key));
                File dest = new File(dirPath + FileSystems.getDefault().getSeparator() + key);
                try {
                    origin.renameTo(dest);
                } catch (Exception e) {
                    System.out.println("Could not move file " + key + " to category folder "+ category);
                    e.printStackTrace();
                }
            }
        }
    }

    private void copyAllFiles() { 
        if (!Common.isValidFolder(targetDirectory)) {
            System.out.println("The target directory is not vaild: " + targetDirectory.getAbsolutePath());
            //Should be added later to a proper error handling
        }

        for (String filename : imageCategory.keySet()) {
            int category = imageCategory.get(filename);
            if (category != 0) {
                String dirPath = targetDirectory.getAbsolutePath() + FileSystems.getDefault().getSeparator() + category;
                File dir = new File(dirPath);
                if (!dir.exists()) {
                    try {
                        dir.mkdir();
                    } catch (Exception e) {
                        System.out.println("Could not create folder for category " + category);
                        e.printStackTrace();
                    }
                }
                File origin = new File(getFullPathForImage(filename));
                File dest = new File(dirPath + FileSystems.getDefault().getSeparator() + filename);
                try {
                    Files.copy(origin.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                } catch (Exception e) {
                    System.out.println("Could not copy file " + filename + " to "+ dest.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    //Youtube-like skimming: 
    //Key (and numberKey) 0 goes to the first image, 5 to the middle one, the rest interpolates in between. 
    private void skimTo(int numberKey) {
        if (currentImage == null) {
            return;
        }

        int skimDestination = (int)(images.size() * (numberKey * 0.1));
        currentImage = images.get(skimDestination);

        lastImageManuallySelected = currentImage;
        updateImageFilterAndCursorAfterImageChange();
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
}

//TODO: When implementing TICKS
// - Delete must delete from all lookups
// - ticking must update currentImagesCategoryWasChanged [which then needs a new name as well]